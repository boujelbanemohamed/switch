package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.PinManagement;
import com.switchplatform.platform.repository.issuing.PinManagementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PinService {

    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int PBKDF2_KEY_LENGTH = 256;
    private static final int AES_KEY_SIZE = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    @Value("${switch.pin.encryption-key:}")
    private String encryptionKey;

    private final PinManagementRepository pinManagementRepository;
    private final CardService cardService;

    public record PinRecord(
            String cardId,
            String pinHash,
            String pinBlockHash,
            String algorithm,
            Instant createdAt,
            Instant updatedAt
    ) {}

    @Transactional
    public String createPin(String cardId, String rawPin, String pinBlock) {
        if (rawPin == null && pinBlock == null) {
            throw new IllegalArgumentException("Either rawPin or pinBlock must be provided");
        }

        String pinHash = null;
        String pinBlockHash = null;
        String algorithm = null;

        if (rawPin != null) {
            if (!rawPin.matches("\\d{4,12}")) {
                throw new IllegalArgumentException("Raw PIN must be 4-12 digits");
            }
            pinHash = hashWithPbkdf2(rawPin);
            algorithm = "PBKDF2";
        }

        if (pinBlock != null) {
            if (!pinBlock.matches("[0-9A-Fa-f]{16}")) {
                throw new IllegalArgumentException("PIN block must be 16 hex characters (ISO 9564-1 format 0)");
            }
            pinBlockHash = encryptAes(pinBlock.toUpperCase());
            if (algorithm == null) {
                algorithm = "ISO9564-1";
            }
        }

        PinManagement entity = PinManagement.builder()
                .cardId(UUID.fromString(cardId))
                .pinHash(pinHash)
                .pinBlock(pinBlockHash)
                .pinFormat(algorithm)
                .lastChanged(OffsetDateTime.now())
                .build();

        pinManagementRepository.save(entity);
        log.info("PIN {} for card {} using algorithm {}", algorithm != null ? "created" : "stored", cardId, algorithm);
        return "PIN created successfully";
    }

    @Transactional(readOnly = true)
    public boolean verifyPin(String cardId, String rawPinOrPinBlock) {
        Optional<PinManagement> opt = pinManagementRepository.findByCardId(UUID.fromString(cardId));
        if (opt.isEmpty()) {
            log.warn("No PIN record found for card {}", cardId);
            return false;
        }
        PinRecord record = toRecord(opt.get());

        if (rawPinOrPinBlock.matches("\\d{4,12}")) {
            return verifyRawPin(record, rawPinOrPinBlock);
        } else if (rawPinOrPinBlock.matches("[0-9A-Fa-f]{16}")) {
            return verifyPinBlock(record, cardId, rawPinOrPinBlock.toUpperCase());
        } else {
            log.warn("Invalid PIN format for card {}", cardId);
            return false;
        }
    }

    @Transactional
    public boolean changePin(String cardId, String oldPinBlock, String newPinBlock) {
        if (!verifyPin(cardId, oldPinBlock)) {
            log.warn("PIN change failed for card {} - old PIN verification failed", cardId);
            return false;
        }

        PinManagement mgmt = pinManagementRepository.findByCardId(UUID.fromString(cardId))
                .orElseThrow(() -> new RuntimeException("PIN record not found for card " + cardId));
        String algorithm = mgmt.getPinFormat() != null ? mgmt.getPinFormat() : "ISO9564-1";
        String pinHash = null;
        String pinBlockHash = encryptAes(newPinBlock.toUpperCase());

        if (mgmt.getPinHash() != null) {
            String extractedPin = extractPinFromPinBlock(cardId, newPinBlock);
            if (extractedPin != null) {
                pinHash = hashWithPbkdf2(extractedPin);
            }
        }

        mgmt.setPinHash(pinHash);
        mgmt.setPinBlock(pinBlockHash);
        mgmt.setPinFormat(algorithm);
        mgmt.setLastChanged(OffsetDateTime.now());
        pinManagementRepository.save(mgmt);
        log.info("PIN changed for card {}", cardId);
        return true;
    }

    @Transactional
    public void storePan(String cardId, String pan) {
        pinManagementRepository.findByCardId(UUID.fromString(cardId))
                .ifPresentOrElse(mgmt -> {
                    mgmt.setPan(pan);
                    pinManagementRepository.save(mgmt);
                }, () -> {
                    PinManagement mgmt = PinManagement.builder()
                            .cardId(UUID.fromString(cardId))
                            .pan(pan)
                            .build();
                    pinManagementRepository.save(mgmt);
                });
    }

    private PinRecord toRecord(PinManagement mgmt) {
        return new PinRecord(
                mgmt.getCardId().toString(),
                mgmt.getPinHash(),
                mgmt.getPinBlock(),
                mgmt.getPinFormat(),
                mgmt.getCreatedAt() != null ? mgmt.getCreatedAt().toInstant() : Instant.now(),
                mgmt.getLastChanged() != null ? mgmt.getLastChanged().toInstant() : Instant.now()
        );
    }

    private boolean verifyRawPin(PinRecord record, String rawPin) {
        if (record.pinHash() == null) {
            log.warn("No PBKDF2 hash stored for card {}, cannot verify raw PIN", record.cardId());
            return false;
        }
        boolean matches = verifyWithPbkdf2(rawPin, record.pinHash());
        log.info("Raw PIN verification for card {}: {}", record.cardId(), matches ? "success" : "failure");
        return matches;
    }

    private boolean verifyWithPbkdf2(String pin, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) return false;
            byte[] salt = HexFormat.of().parseHex(parts[0]);
            String expectedHash = parts[1];

            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            String computedHash = HexFormat.of().formatHex(hash);

            return expectedHash.equals(computedHash);
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 verification failed", e);
        }
    }

    private boolean verifyPinBlock(PinRecord record, String cardId, String incomingPinBlock) {
        String storedPinBlock = decryptAes(record.pinBlockHash());
        if (storedPinBlock == null) {
            log.warn("No PIN block stored for card {}, cannot verify PIN block", cardId);
            return false;
        }

        String pan = pinManagementRepository.findByCardId(UUID.fromString(cardId))
                .map(PinManagement::getPan).orElse(null);

        if (pan == null) {
            log.warn("No PAN available for card {}, using simplified PIN block comparison", cardId);
            boolean matches = storedPinBlock.equals(incomingPinBlock);
            log.info("PIN block comparison for card {}: {}", cardId, matches ? "success" : "failure");
            return matches;
        }

        byte[] storedPinBlockBytes = HexFormat.of().parseHex(storedPinBlock);
        byte[] panBlock = buildPanBlock(pan);
        byte[] incomingBlock = HexFormat.of().parseHex(incomingPinBlock);

        byte[] storedPinBytes = xor(storedPinBlockBytes, panBlock);
        byte[] incomingPinBytes = xor(incomingBlock, panBlock);

        for (int i = 0; i < storedPinBytes.length; i++) {
            if (storedPinBytes[i] != incomingPinBytes[i]) {
                log.warn("PIN block verification failed for card {}", cardId);
                return false;
            }
        }

        log.info("PIN block verification for card {}: success", cardId);
        return true;
    }

    private String extractPinFromPinBlock(String cardId, String pinBlockHex) {
        String pan = pinManagementRepository.findByCardId(UUID.fromString(cardId))
                .map(PinManagement::getPan).orElse(null);
        if (pan == null) {
            return null;
        }
        byte[] pinBlockBytes = HexFormat.of().parseHex(pinBlockHex);
        byte[] panBlock = buildPanBlock(pan);
        byte[] decoded = xor(pinBlockBytes, panBlock);

        int pinLength = decoded[0] & 0x0F;
        if (pinLength < 4 || pinLength > 12) {
            return null;
        }

        StringBuilder pin = new StringBuilder();
        for (int i = 1; i <= (pinLength + 1) / 2; i++) {
            int upper = (decoded[i] >> 4) & 0x0F;
            int lower = decoded[i] & 0x0F;
            if (pin.length() < pinLength) {
                if (upper <= 9) pin.append(upper);
                if (pin.length() < pinLength && lower <= 9) pin.append(lower);
            }
        }
        return pin.toString();
    }

    static byte[] buildPanBlock(String pan) {
        String cleanPan = pan.replaceAll("\\s", "");
        String panWithoutCheck = cleanPan.substring(0, cleanPan.length() - 1);
        String panDigits = panWithoutCheck.length() > 12
                ? panWithoutCheck.substring(panWithoutCheck.length() - 12)
                : String.format("%12s", panWithoutCheck).replace(' ', '0');

        byte[] panBlock = new byte[8];
        for (int i = 0; i < 12; i++) {
            int digit = panDigits.charAt(i) - '0';
            int byteIndex = 2 + (i / 2);
            if (i % 2 == 0) {
                panBlock[byteIndex] = (byte) ((digit << 4) & 0xF0);
            } else {
                panBlock[byteIndex] = (byte) (panBlock[byteIndex] | (digit & 0x0F));
            }
        }
        return panBlock;
    }

    private static byte[] xor(byte[] a, byte[] b) {
        int len = Math.min(a.length, b.length);
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }

    private String hashWithPbkdf2(String pin) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            String saltHex = HexFormat.of().formatHex(salt);
            String hashHex = HexFormat.of().formatHex(hash);
            return saltHex + ":" + hashHex;
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 hashing failed", e);
        }
    }

    private String encryptAes(String plaintext) {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            return plaintext;
        }
        try {
            byte[] keyBytes = deriveKey(encryptionKey);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES encryption failed", e);
            return plaintext;
        }
    }

    private String decryptAes(String ciphertext) {
        if (encryptionKey == null || encryptionKey.isBlank() || ciphertext == null) {
            return ciphertext;
        }
        try {
            byte[] keyBytes = deriveKey(encryptionKey);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(decoded, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES decryption failed", e);
            return null;
        }
    }

    private byte[] deriveKey(String password) {
        try {
            byte[] salt = "PinServiceSalt".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, AES_KEY_SIZE);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }
}
