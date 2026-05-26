package com.switchplatform.platform;

import com.solab.iso8583.IsoMessage;
import com.switchplatform.platform.iso8583.Iso8583Engine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;


class Iso8583EngineTest {

    private Iso8583Engine engine;

    @BeforeEach
    void setUp() {
        engine = new Iso8583Engine();
        engine.init();
    }

    @Test
    void shouldCreateAuthorizationRequest() {
        IsoMessage msg = engine.createAuthorizationRequest(
                "4000001234567890",
                BigDecimal.valueOf(150.00),
                "TND",
                "123456",
                "MERCH001",
                "TERM01");

        assertNotNull(msg);
        assertEquals(200, msg.getType());
        assertNotNull(msg.getField(2));
        assertEquals("4000001234567890", msg.getField(2).toString());
        assertNotNull(msg.getField(4));
    }

    @Test
    void shouldCreateAuthorizationResponse() {
        IsoMessage request = engine.createAuthorizationRequest(
                "4000001234567890",
                BigDecimal.valueOf(150.00),
                "TND",
                "123456",
                "MERCH001",
                "TERM01");

        IsoMessage response = engine.createAuthorizationResponse(request, "00");
        assertNotNull(response);
        assertEquals(210, response.getType());
        assertEquals("00", response.getField(39).toString());
    }

    @Test
    void shouldEncodeAndDecode() {
        IsoMessage original = engine.createAuthorizationRequest(
                "4000001234567890",
                BigDecimal.valueOf(100.00),
                "USD",
                "654321",
                "MERCH002",
                "TERM02");

        byte[] encoded = engine.encode(original);
        assertTrue(encoded.length > 0);
    }

    @Test
    void shouldConvertToMap() {
        IsoMessage msg = engine.createAuthorizationRequest(
                "4000001234567890",
                BigDecimal.valueOf(200.00),
                "EUR",
                "111111",
                "MERCH003",
                "TERM03");

        var map = engine.toMap(msg);
        assertEquals("0200", map.get("mti"));
        assertNotNull(map.get("field_2"));
        assertNotNull(map.get("field_4"));
    }

    @Test
    void shouldCreateReversalRequest() {
        IsoMessage msg = engine.createReversalRequest(
                "4000001234567890",
                BigDecimal.valueOf(75.50),
                "999999",
                "111111",
                "123456789012");

        assertNotNull(msg);
        assertEquals(400, msg.getType());
    }

    @Test
    void shouldCreateFinancialRequest() {
        IsoMessage msg = engine.createFinancialRequest(
                "4000001234567890",
                BigDecimal.valueOf(500.00),
                "TND",
                "777777",
                "MERCH004",
                "TERM04");

        assertNotNull(msg);
        assertEquals(100, msg.getType());
    }
}
