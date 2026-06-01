package com.switchplatform.platform.service.hsm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;

@Service
@Slf4j
public class DukptService {

    private static final int KEY_LENGTH = 16;

    public byte[] deriveSessionKey(byte[] bdk, byte[] ksn) {
        byte[] key = bdk.clone();
        byte[] ksnCopy = ksn.clone();
        int ec = ((ksnCopy[5] & 0xFF) << 16) | ((ksnCopy[6] & 0xFF) << 8) | (ksnCopy[7] & 0xFF);

        if ((ec & 0x10000) != 0) { key = encrypt(key, xor(key, hex("C0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C0"))); }
        if ((ec & 0x20000) != 0) { key = encrypt(key, xor(key, hex("0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C"))); }
        if ((ec & 0x40000) != 0) { key = encrypt(key, xor(key, hex("C0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C0"))); }
        if ((ec & 0x80000) != 0) { key = encrypt(key, xor(key, hex("0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C0C"))); }

        byte[] sessionKey = Arrays.copyOf(key, KEY_LENGTH);
        log.debug("DUKPT session key derived from KSN: {}", HexFormat.of().formatHex(ksn));
        return sessionKey;
    }

    public byte[] generateKsn(byte[] baseKsn, long transactionCounter) {
        byte[] ksn = baseKsn.clone();
        ksn[5] = (byte) ((transactionCounter >> 16) & 0xFF);
        ksn[6] = (byte) ((transactionCounter >> 8) & 0xFF);
        ksn[7] = (byte) (transactionCounter & 0xFF);
        return ksn;
    }

    public byte[] generateBdk() {
        byte[] bdk = new byte[KEY_LENGTH];
        new SecureRandom().nextBytes(bdk);
        log.debug("DUKPT BDK generated");
        return bdk;
    }

    public String encryptPinBlock(byte[] sessionKey, String pinBlock) {
        try {
            byte[] pinBytes = HexFormat.of().parseHex(pinBlock);
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sessionKey, "DES"));
            byte[] encrypted = cipher.doFinal(pinBytes);
            return HexFormat.of().formatHex(encrypted).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("DUKPT PIN encryption failed", e);
        }
    }

    private byte[] encrypt(byte[] key, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"));
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("DES encryption failed", e);
        }
    }

    private byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) result[i] = (byte) (a[i] ^ b[i]);
        return result;
    }

    private byte[] hex(String s) {
        return HexFormat.of().parseHex(s);
    }
}
