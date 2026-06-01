package com.switchplatform.platform.service.hsm;

public interface HsmService {

    String generatePinBlock(String pan, String pin);

    boolean verifyPinBlock(String pan, String pinBlock, String pinBlockFormat);

    String translatePin(String pan, String pinBlock, String targetFormat, byte[] targetKey);

    String generateMac(byte[] data, byte[] key);

    boolean verifyMac(byte[] data, byte[] key, String mac);

    String importKey(byte[] keyData, String keyType, String keyScheme);

    byte[] exportKey(String keyReference, String keyType);

    String generateKey(String keyType, String keyScheme);

    String wrapKey(byte[] keyData, byte[] wrappingKey);

    byte[] unwrapKey(String wrappedKey, byte[] wrappingKey);

    String getVendorName();

    boolean isHardware();
}
