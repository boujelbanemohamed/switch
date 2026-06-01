package com.switchplatform.platform.service.hsm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;

@Service
@Slf4j
public class Tr31Service {

    public String wrap(byte[] keyData, byte[] keyBlockProtectionKey, String keyType, String algorithm) {
        byte[] header = buildHeader(keyType, algorithm);
        byte[] padded = pad(keyData);
        byte[] encrypted = encrypt(keyBlockProtectionKey, padded);
        byte[] mac = computeMac(keyBlockProtectionKey, header, encrypted);

        byte[] result = new byte[header.length + encrypted.length + mac.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(encrypted, 0, result, header.length, encrypted.length);
        System.arraycopy(mac, 0, result, header.length + encrypted.length, mac.length);

        String wrapped = HexFormat.of().formatHex(result).toUpperCase();
        log.debug("TR31 wrapped: type={}, algo={}, length={}", keyType, algorithm, wrapped.length());
        return wrapped;
    }

    public byte[] unwrap(String wrappedKey, byte[] keyBlockProtectionKey) {
        byte[] all = HexFormat.of().parseHex(wrappedKey);
        byte[] header = Arrays.copyOfRange(all, 0, 40);
        int encLen = all.length - 40 - 8;
        byte[] encrypted = Arrays.copyOfRange(all, 40, 40 + encLen);
        byte[] expectedMac = Arrays.copyOfRange(all, 40 + encLen, all.length);

        byte[] computedMac = computeMac(keyBlockProtectionKey, header, encrypted);
        if (!Arrays.equals(expectedMac, computedMac)) {
            throw new SecurityException("TR31 MAC verification failed");
        }

        byte[] decrypted = decrypt(keyBlockProtectionKey, encrypted);
        log.debug("TR31 unwrapped successfully");
        return Arrays.copyOf(decrypted, decrypted.length - (decrypted[decrypted.length - 1] & 0xFF));
    }

    private byte[] buildHeader(String keyType, String algorithm) {
        String header = "0001" + "D" + "000" + keyType
                + String.format("%02d", algorithm.equals("AES") ? 32 : 16)
                + algorithm + "0" + "0000" + "C" + "0000";
        return header.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private byte[] pad(byte[] data) {
        int padLen = 8 - (data.length % 8);
        byte[] padded = Arrays.copyOf(data, data.length + padLen);
        padded[data.length] = (byte) 0x80;
        for (int i = data.length + 1; i < padded.length; i++) padded[i] = 0;
        return padded;
    }

    private byte[] encrypt(byte[] key, byte[] data) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key, "AES"));
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("TR31 encryption failed", e);
        }
    }

    private byte[] decrypt(byte[] key, byte[] data) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key, "AES"));
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("TR31 decryption failed", e);
        }
    }

    private byte[] computeMac(byte[] key, byte[] header, byte[] data) {
        try {
            byte[] input = new byte[header.length + data.length];
            System.arraycopy(header, 0, input, 0, header.length);
            System.arraycopy(data, 0, input, header.length, data.length);
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"));
            return Arrays.copyOf(mac.doFinal(input), 8);
        } catch (Exception e) {
            throw new RuntimeException("TR31 MAC computation failed", e);
        }
    }
}
