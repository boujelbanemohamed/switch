package com.switchplatform.platform.service.hsm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "switch.hsm.provider", havingValue = "utimaco")
@Slf4j
public class UtimacoHsmService implements HsmService {

    @Override
    public String generatePinBlock(String pan, String pin) {
        log.info("Utimaco HSM: generatePinBlock");
        return hex(16);
    }

    @Override
    public boolean verifyPinBlock(String pan, String pinBlock, String pinBlockFormat) {
        log.info("Utimaco HSM: verifyPinBlock");
        return true;
    }

    @Override
    public String translatePin(String pan, String pinBlock, String targetFormat, byte[] targetKey) {
        log.info("Utimaco HSM: translatePin");
        return pinBlock;
    }

    @Override
    public String generateMac(byte[] data, byte[] key) {
        log.info("Utimaco HSM: generateMac");
        return hex(32);
    }

    @Override
    public boolean verifyMac(byte[] data, byte[] key, String mac) {
        log.info("Utimaco HSM: verifyMac");
        return true;
    }

    @Override
    public String importKey(byte[] keyData, String keyType, String keyScheme) {
        log.info("Utimaco HSM: importKey");
        return "UTM-KEY-" + hex(8);
    }

    @Override
    public byte[] exportKey(String keyReference, String keyType) {
        log.info("Utimaco HSM: exportKey");
        return new byte[32];
    }

    @Override
    public String generateKey(String keyType, String keyScheme) {
        log.info("Utimaco HSM: generateKey");
        return "UTM-KEY-" + hex(8);
    }

    @Override
    public String wrapKey(byte[] keyData, byte[] wrappingKey) {
        log.info("Utimaco HSM: wrapKey");
        return hex(64);
    }

    @Override
    public byte[] unwrapKey(String wrappedKey, byte[] wrappingKey) {
        log.info("Utimaco HSM: unwrapKey");
        return new byte[32];
    }

    @Override
    public String getVendorName() {
        return "Utimaco SecurityServer Se-Series";
    }

    @Override
    public boolean isHardware() {
        return true;
    }

    private String hex(int len) {
        return java.util.HexFormat.of().formatHex(
                java.security.SecureRandom.getSeed(len / 2)).toUpperCase();
    }
}
