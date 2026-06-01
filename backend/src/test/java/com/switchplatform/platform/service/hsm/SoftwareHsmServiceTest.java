package com.switchplatform.platform.service.hsm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoftwareHsmServiceTest {

    private SoftwareHsmService hsmService;

    @BeforeEach
    void setUp() {
        hsmService = new SoftwareHsmService();
    }

    @Test
    void generatePinBlock_shouldReturnValidBlock() {
        String pinBlock = hsmService.generatePinBlock("1234567890123456", "1234");
        assertNotNull(pinBlock);
        assertTrue(pinBlock.length() >= 16);
    }

    @Test
    void verifyPinBlock_shouldReturnTrue() {
        String pinBlock = hsmService.generatePinBlock("1234567890123456", "1234");
        boolean result = hsmService.verifyPinBlock("1234567890123456", pinBlock, "ISO0");
        assertTrue(result);
    }

    @Test
    void generateKey_shouldReturnKeyString() {
        String key = hsmService.generateKey("AES", "ZMK");
        assertNotNull(key);
    }

    @Test
    void getVendorName_shouldReturnSoftware() {
        assertEquals("Software HSM (dev/test only)", hsmService.getVendorName());
    }

    @Test
    void generateMac_shouldReturnMac() {
        String mac = hsmService.generateMac("test-data".getBytes(), "test-key".getBytes());
        assertNotNull(mac);
    }

    @Test
    void roundTripKey_shouldWork() {
        byte[] keyData = "test-key-data-1234".getBytes();
        byte[] wrappingKey = "wrap-key-12345678".getBytes();

        String wrapped = hsmService.wrapKey(keyData, wrappingKey);
        assertNotNull(wrapped);

        byte[] unwrapped = hsmService.unwrapKey(wrapped, wrappingKey);
        assertArrayEquals(keyData, unwrapped);
    }

    @Test
    void generatePinBlock_differentPinsShouldDiffer() {
        String block1 = hsmService.generatePinBlock("1234567890123456", "1234");
        String block2 = hsmService.generatePinBlock("1234567890123456", "5678");
        assertNotEquals(block1, block2);
    }
}
