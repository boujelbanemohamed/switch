package com.switchplatform.platform.service.clearing.network;

import com.switchplatform.platform.iso20022.Iso20022Engine;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NetworkClearingGeneratorTest {

    private Iso20022ClearingGenerator iso20022Generator;
    private VisaBaseIIGenerator visaGenerator;
    private MastercardIpmGenerator mastercardGenerator;
    private Iso20022Engine iso20022Engine;

    private final LocalDate DATE = LocalDate.of(2026, 6, 5);
    private List<ClearingRecord> records;

    @BeforeEach
    void setUp() {
        iso20022Engine = new Iso20022Engine();
        iso20022Generator = new Iso20022ClearingGenerator(iso20022Engine);
        visaGenerator = new VisaBaseIIGenerator();
        mastercardGenerator = new MastercardIpmGenerator();

        records = List.of(
                ClearingRecord.builder()
                        .id(UUID.randomUUID())
                        .transactionId("TXN001")
                        .amount(new BigDecimal("150.00"))
                        .currencyCode("TND")
                        .acquiringParticipantId(UUID.randomUUID())
                        .issuingParticipantId(UUID.randomUUID())
                        .build()
        );
    }

    @Test
    void iso20022Generator_producesValidXml() {
        String xml = iso20022Generator.generate(DATE, records);
        assertNotNull(xml);
        assertTrue(xml.contains("<?xml"), "Should produce XML declaration");
        assertTrue(xml.contains("Document"), "Should contain Document element");
        assertTrue(xml.contains("SCHEME-CLEARING-"), "Should contain scheme reference");
        assertTrue(xml.contains("TND"), "Should contain currency code");
        assertTrue(xml.contains("150.00"), "Should contain transaction amount");
    }

    @Test
    void iso20022Generator_emptyRecords_producesValidXml() {
        String xml = iso20022Generator.generate(DATE, List.of());
        assertNotNull(xml);
        assertTrue(xml.contains("<?xml"), "Should produce valid XML even with empty records");
    }

    @Test
    void visaBaseIIGenerator_throwsUnsupportedOperationException() {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> visaGenerator.generate(DATE, records));
        assertTrue(ex.getMessage().contains("Visa BASE II"),
                "Error message should mention Visa BASE II");
        assertTrue(ex.getMessage().contains("proprietary"),
                "Error message should indicate format is proprietary");
    }

    @Test
    void mastercardIpmGenerator_throwsUnsupportedOperationException() {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                () -> mastercardGenerator.generate(DATE, records));
        assertTrue(ex.getMessage().contains("Mastercard IPM"),
                "Error message should mention Mastercard IPM");
        assertTrue(ex.getMessage().contains("proprietary"),
                "Error message should indicate format is proprietary");
    }

    @Test
    void networkGenerator_implementsInterface() {
        assertInstanceOf(NetworkClearingGenerator.class, iso20022Generator);
        assertInstanceOf(NetworkClearingGenerator.class, visaGenerator);
        assertInstanceOf(NetworkClearingGenerator.class, mastercardGenerator);
    }
}
