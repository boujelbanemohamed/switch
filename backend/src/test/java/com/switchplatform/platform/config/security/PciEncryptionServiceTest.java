package com.switchplatform.platform.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PciEncryptionServiceTest {

    private PciEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new PciEncryptionService(null);
    }

    @Test
    void encryptDecrypt_shouldReturnOriginal() {
        String original = "4111111111111111";
        String encrypted = encryptionService.encrypt(original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void maskPan_shouldMaskCorrectly() {
        assertEquals("411111******1111", encryptionService.maskPan("4111111111111111"));
        assertEquals("****", encryptionService.maskPan("1234"));
        assertEquals("****", encryptionService.maskPan(null));
    }

    @Test
    void mask_shouldShortValues() {
        assertEquals("****", encryptionService.mask("ab"));
        assertEquals("****", encryptionService.mask(null));
    }
}
