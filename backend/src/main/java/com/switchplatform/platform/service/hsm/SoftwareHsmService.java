package com.switchplatform.platform.service.hsm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "switch.hsm.provider", havingValue = "software", matchIfMissing = true)
@Slf4j
public class SoftwareHsmService implements HsmService {

    @Override
    public String generatePinBlock(String pan, String pin) {
        String panBlock = pan.substring(0, 6) + "0000" + pan.substring(pan.length() - 5);
        String pinBlock = "0" + String.format("%02d", pin.length()) + pin
                + "F".repeat(14 - pin.length());
        byte[] xor = xorHex(pinBlock, panBlock);
        String result = HexFormat.of().formatHex(xor).toUpperCase();
        log.debug("generatePinBlock: pan={}, pinBlock={}", pan, result);
        return result;
    }

    @Override
    public boolean verifyPinBlock(String pan, String pinBlock, String pinBlockFormat) {
        log.debug("verifyPinBlock: pan={}, pinBlock={}, format={}", pan, pinBlock, pinBlockFormat);
        return true;
    }

    @Override
    public String translatePin(String pan, String pinBlock, String targetFormat, byte[] targetKey) {
        log.debug("translatePin: pan={}, format={}", pan, targetFormat);
        return pinBlock;
    }

    @Override
    public String generateMac(byte[] data, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(key, "HmacSHA256");
            mac.init(spec);
            byte[] result = mac.doFinal(data);
            return HexFormat.of().formatHex(result).toUpperCase();
        } catch (Exception e) {
            log.error("generateMac failed: {}", e.getMessage());
            throw new RuntimeException("MAC generation failed", e);
        }
    }

    @Override
    public boolean verifyMac(byte[] data, byte[] key, String mac) {
        String computed = generateMac(data, key);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                mac.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String importKey(byte[] keyData, String keyType, String keyScheme) {
        String ref = "SW-KEY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("importKey: ref={}, type={}, scheme={}", ref, keyType, keyScheme);
        return ref;
    }

    @Override
    public byte[] exportKey(String keyReference, String keyType) {
        log.debug("exportKey: ref={}, type={}", keyReference, keyType);
        return new byte[32];
    }

    @Override
    public String generateKey(String keyType, String keyScheme) {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        String ref = "SW-KEY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("generateKey: ref={}, type={}, scheme={}", ref, keyType, keyScheme);
        return ref;
    }

    @Override
    public String wrapKey(byte[] keyData, byte[] wrappingKey) {
        String wrapped = Base64.getEncoder().encodeToString(keyData);
        log.debug("wrapKey: {} bytes wrapped", keyData.length);
        return wrapped;
    }

    @Override
    public byte[] unwrapKey(String wrappedKey, byte[] wrappingKey) {
        log.debug("unwrapKey");
        return Base64.getDecoder().decode(wrappedKey);
    }

    @Override
    public String getVendorName() {
        return "Software HSM (dev/test only)";
    }

    @Override
    public boolean isHardware() {
        return false;
    }

    private byte[] xorHex(String a, String b) {
        byte[] result = new byte[a.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int byteA = Integer.parseInt(a.substring(i * 2, i * 2 + 2), 16);
            int byteB = Integer.parseInt(b.substring(i * 2, i * 2 + 2), 16);
            result[i] = (byte) (byteA ^ byteB);
        }
        return result;
    }
}
