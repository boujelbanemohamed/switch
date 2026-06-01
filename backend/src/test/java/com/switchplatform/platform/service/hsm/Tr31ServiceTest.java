package com.switchplatform.platform.service.hsm;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class Tr31ServiceTest {

    private final Tr31Service tr31Service = new Tr31Service();

    @Test
    void wrap_shouldReturnWrappedKey() {
        byte[] keyData = "TESTKEY12345678".getBytes(StandardCharsets.UTF_8);
        byte[] bdk = "BPK1234567890123".getBytes(StandardCharsets.UTF_8);
        String wrapped = tr31Service.wrap(keyData, bdk, "D", "A");
        assertNotNull(wrapped);
        assertFalse(wrapped.isEmpty());
        assertTrue(wrapped.length() > 40);
    }

    @Test
    void unwrap_shouldThrowOnInvalidInput() {
        byte[] bdk = "BPK1234567890123".getBytes(StandardCharsets.UTF_8);
        assertThrows(Exception.class,
                () -> tr31Service.unwrap("D0000INVALID", bdk));
    }
}
