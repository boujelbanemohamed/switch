package com.switchplatform.platform.service.clearing.network;

import com.switchplatform.platform.iso20022.Iso20022Engine;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.acquiring.MerchantRepository;
import com.switchplatform.platform.repository.clearing.ClearingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        ParticipantRepository participantRepository = mock(ParticipantRepository.class);
        MerchantRepository merchantRepository = mock(MerchantRepository.class);
        ClearingRecordRepository clearingRecordRepository = mock(ClearingRecordRepository.class);
        when(participantRepository.findById(any())).thenReturn(Optional.of(
                Participant.builder().id(UUID.randomUUID()).code("BNT").bankCode("BNT01").name("Test").build()));
        when(merchantRepository.findByMerchantId(any())).thenReturn(Optional.empty());
        visaGenerator = new VisaBaseIIGenerator(participantRepository, merchantRepository, clearingRecordRepository);

        mastercardGenerator = new MastercardIpmGenerator(participantRepository, merchantRepository, clearingRecordRepository);

        records = List.of(
                ClearingRecord.builder()
                        .id(UUID.randomUUID())
                        .transactionId("TXN001")
                        .amount(new BigDecimal("150.000"))
                        .currencyCode("TND")
                        .acquiringParticipantId(UUID.randomUUID())
                        .issuingParticipantId(UUID.randomUUID())
                        .cardNumber("1234567890123456")
                        .mcc("5411")
                        .tradingName("MARCHAND TEST")
                        .transactionDate(OffsetDateTime.now())
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
    void visaBaseIIGenerator_producesTc05Tcr0Lines() {
        String result = visaGenerator.generate(DATE, records);
        assertNotNull(result);
        assertFalse(result.isEmpty(), "VISA generator should produce output");
        String[] lines = result.split("\n");
        assertEquals(1, lines.length, "Should produce one line per record");
        assertEquals(168, lines[0].length(), "Each line should be exactly 168 characters");
        assertTrue(lines[0].startsWith("05"), "Line should start with TC 05");
    }

    @Test
    void visaBaseIIGenerator_ingest_returnsReconciliationResult() {
        var result = visaGenerator.ingest("dummy");
        assertNotNull(result);
        assertEquals(0, result.totalRecords());
        assertEquals(0, result.matched());
        assertEquals(1, result.unmatched());
    }

    @Test
    void mastercardIpmGenerator_producesType1000Lines() {
        String result = mastercardGenerator.generate(DATE, records);
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Mastercard generator should produce output");
        String[] lines = result.split("\n");
        assertEquals(1, lines.length, "Should produce one line per record");
        assertEquals(200, lines[0].length(), "Each line should be exactly 200 characters");
        assertTrue(lines[0].startsWith("1000"), "Line should start with record type 1000");
        String mcc = lines[0].substring(156, 160);
        assertEquals("5411", mcc.trim(), "MCC should be present at positions 157-160");
    }

    @Test
    void networkGenerator_implementsInterface() {
        assertInstanceOf(NetworkClearingGenerator.class, iso20022Generator);
        assertInstanceOf(NetworkClearingGenerator.class, visaGenerator);
        assertInstanceOf(NetworkClearingGenerator.class, mastercardGenerator);
    }
}
