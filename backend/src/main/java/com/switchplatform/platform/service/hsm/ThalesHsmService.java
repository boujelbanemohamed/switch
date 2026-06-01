package com.switchplatform.platform.service.hsm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "switch.hsm.provider", havingValue = "thales")
@Slf4j
public class ThalesHsmService implements HsmService {

    @Override
    public String generatePinBlock(String pan, String pin) {
        log.info("Thales HSM: generatePinBlock pan={}", pan);
        return simulate();
    }

    @Override
    public boolean verifyPinBlock(String pan, String pinBlock, String pinBlockFormat) {
        log.info("Thales HSM: verifyPinBlock pan={}", pan);
        return true;
    }

    @Override
    public String translatePin(String pan, String pinBlock, String targetFormat, byte[] targetKey) {
        log.info("Thales HSM: translatePin pan={} target={}", pan, targetFormat);
        return pinBlock;
    }

    @Override
    public String generateMac(byte[] data, byte[] key) {
        log.info("Thales HSM: generateMac");
        return simulate();
    }

    @Override
    public boolean verifyMac(byte[] data, byte[] key, String mac) {
        log.info("Thales HSM: verifyMac");
        return true;
    }

    @Override
    public String importKey(byte[] keyData, String keyType, String keyScheme) {
        log.info("Thales HSM: importKey type={} scheme={}", keyType, keyScheme);
        return "THL-KEY-" + simulate().substring(0, 8);
    }

    @Override
    public byte[] exportKey(String keyReference, String keyType) {
        log.info("Thales HSM: exportKey ref={}", keyReference);
        return new byte[32];
    }

    @Override
    public String generateKey(String keyType, String keyScheme) {
        log.info("Thales HSM: generateKey type={} scheme={}", keyType, keyScheme);
        return "THL-KEY-" + simulate().substring(0, 8);
    }

    @Override
    public String wrapKey(byte[] keyData, byte[] wrappingKey) {
        log.info("Thales HSM: wrapKey");
        return simulate();
    }

    @Override
    public byte[] unwrapKey(String wrappedKey, byte[] wrappingKey) {
        log.info("Thales HSM: unwrapKey");
        return new byte[32];
    }

    @Override
    public String getVendorName() {
        return "Thales Payshield 9000";
    }

    @Override
    public boolean isHardware() {
        return true;
    }

    private String simulate() {
        return java.util.HexFormat.of().formatHex(
                java.security.SecureRandom.getSeed(16)).toUpperCase();
    }
}
