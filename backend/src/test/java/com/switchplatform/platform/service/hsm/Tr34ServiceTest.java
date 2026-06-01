package com.switchplatform.platform.service.hsm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Tr34ServiceTest {

    private final Tr34Service tr34Service = new Tr34Service();

    @Test
    void service_shouldInstantiate() {
        assertNotNull(tr34Service);
    }
}
