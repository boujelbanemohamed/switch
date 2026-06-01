package com.switchplatform.platform.service.hsm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HexFormat;

@Service
@Slf4j
public class Tr34Service {

    public byte[] generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048, new SecureRandom());
            KeyPair pair = gen.generateKeyPair();
            byte[] combined = new byte[550 + 550];
            System.arraycopy(pair.getPublic().getEncoded(), 0, combined, 0, 550);
            System.arraycopy(pair.getPrivate().getEncoded(), 0, combined, 550, 550);
            log.debug("TR34 key pair generated");
            return combined;
        } catch (Exception e) {
            throw new RuntimeException("TR34 key generation failed", e);
        }
    }

    public byte[] signKey(byte[] keyData, PrivateKey signingKey) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(signingKey);
            sig.update(keyData);
            byte[] signature = sig.sign();
            byte[] result = new byte[keyData.length + signature.length];
            System.arraycopy(keyData, 0, result, 0, keyData.length);
            System.arraycopy(signature, 0, result, keyData.length, signature.length);
            log.debug("TR34 key signed, total length={}", result.length);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("TR34 signing failed", e);
        }
    }

    public boolean verifySignedKey(byte[] signedData, PublicKey verifyingKey) {
        try {
            int keyLen = signedData.length - 256;
            byte[] keyData = Arrays.copyOf(signedData, keyLen);
            byte[] signature = Arrays.copyOfRange(signedData, keyLen, signedData.length);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(verifyingKey);
            sig.update(keyData);
            boolean valid = sig.verify(signature);
            log.debug("TR34 signature verification: {}", valid ? "PASSED" : "FAILED");
            return valid;
        } catch (Exception e) {
            log.error("TR34 verification failed: {}", e.getMessage());
            return false;
        }
    }

    public byte[] encryptKeyBlock(byte[] keyData, PublicKey recipientKey) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, recipientKey);
            byte[] encrypted = cipher.doFinal(keyData);
            log.debug("TR34 key block encrypted, size={}", encrypted.length);
            return encrypted;
        } catch (Exception e) {
            throw new RuntimeException("TR34 encryption failed", e);
        }
    }

    public byte[] decryptKeyBlock(byte[] encryptedData, PrivateKey recipientPrivateKey) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, recipientPrivateKey);
            byte[] decrypted = cipher.doFinal(encryptedData);
            log.debug("TR34 key block decrypted, size={}", decrypted.length);
            return decrypted;
        } catch (Exception e) {
            throw new RuntimeException("TR34 decryption failed", e);
        }
    }
}
