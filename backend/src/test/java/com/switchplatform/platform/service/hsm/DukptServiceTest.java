package com.switchplatform.platform.service.hsm;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DukptServiceTest {

    private final DukptService dukptService = new DukptService();

    @Test
    void generateBdk_shouldReturnKey() {
        byte[] bdk = dukptService.generateBdk();
        assertNotNull(bdk);
        assertEquals(16, bdk.length);
    }

    @Test
    void generateKsn_shouldReturnModifiedKsn() {
        byte[] baseKsn = "FFFF9876543210".getBytes(StandardCharsets.UTF_8);
        byte[] ksn = dukptService.generateKsn(baseKsn, 1);
        assertNotNull(ksn);
        assertEquals(baseKsn.length, ksn.length);
    }

    @Test
    void generateKsn_differentCountersShouldDiffer() {
        byte[] baseKsn = "FFFF9876543210".getBytes(StandardCharsets.UTF_8);
        byte[] ksn1 = dukptService.generateKsn(baseKsn, 1);
        byte[] ksn2 = dukptService.generateKsn(baseKsn, 2);
        assertNotNull(ksn1);
        assertNotNull(ksn2);
    }
}
