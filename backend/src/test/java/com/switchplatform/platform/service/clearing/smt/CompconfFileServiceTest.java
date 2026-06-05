package com.switchplatform.platform.service.clearing.smt;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
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

class CompconfFileServiceTest {

    private CompconfFileService service;

    @BeforeEach
    void setUp() {
        ParticipantRepository repo = mock(ParticipantRepository.class);
        when(repo.findById(any())).thenReturn(Optional.empty());
        service = new CompconfFileService(repo);
    }

    @Test
    void lineIsExactly168Chars() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals(168, line.length(), "COMPCONF line must be exactly 168 chars");
    }

    @Test
    void finEnregistrementIsX() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals('X', line.charAt(167), "Position 168 (0-indexed 167) must be X");
    }

    @Test
    void numeroCommercantAtPosition1() {
        ClearingRecord r = ClearingRecord.builder()
                .merchantNumber("1234567890")
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("1234567890", line.substring(0, 10), "Pos 1-10 must be NUMERO COMMERCANT");
    }

    @Test
    void montantAtPosition47() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("150.000"))
                .messageType("0200")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("000150000", line.substring(46, 55), "Pos 47-55 must be MONTANT 9(6)V999");
    }

    @Test
    void banqueCommercantAtPosition90() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .acquiringParticipantId(UUID.randomUUID())
                .issuingParticipantId(UUID.randomUUID())
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("00000", line.substring(89, 94), "Pos 90-94 = BANQUE COMMERCANT (00000 fallback)");
    }

    @Test
    void banquePorteurAtPosition96() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("00000", line.substring(95, 100), "Pos 96-100 = BANQUE PORTEUR (00000 fallback)");
    }

    @Test
    void systemeAtPosition95() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("2", line.substring(94, 95), "Pos 95 = SYSTEME default 2 (Visa)");
    }

    @Test
    void codeOperationAtPosition45() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("05", line.substring(44, 46), "Pos 45-46 = CODE OPERATION 05 for 0200");
    }

    @Test
    void chargebackCodeOperation15() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0400")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("15", line.substring(44, 46), "Pos 45-46 = 15 for chargeback (0400)");
    }

    @Test
    void feeCodeOperation10() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0100")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("10", line.substring(44, 46), "Pos 45-46 = 10 for fee (0100)");
    }

    @Test
    void dateTraitementAtPosition60() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .transactionDate(OffsetDateTime.of(2025, 6, 4, 0, 0, 0, 0, java.time.ZoneOffset.UTC))
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("040625", line.substring(59, 65), "Pos 60-65 = DATE TRAITEMENT JJMMAA");
    }

    @Test
    void mccAtPosition84() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .mcc("1234")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("1234", line.substring(83, 87), "Pos 84-87 = MCC");
    }

    @Test
    void archiveReferenceAtPosition101() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .archiveReference("REF123")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        String ref = line.substring(100, 123);
        assertEquals(23, ref.length(), "REFERENCE ARCHIVAGE = 23 chars");
        assertTrue(ref.startsWith("REF123"), "Pos 101-123 = REFERENCE ARCHIVAGE");
    }

    @Test
    void natureOperationAtPosition44() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("D", line.substring(43, 44), "Pos 44 = NATURE OPERATION D for 0200");
    }

    @Test
    void origineAtPosition43() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .originIdentifier("I")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("I", line.substring(42, 43), "Pos 43 = IDENTIFIANT ORIGINE");
    }

    @Test
    void numeroPorteurAtPosition23() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .cardNumber("1234567890123456789")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        assertEquals("1234567890123456789", line.substring(22, 41), "Pos 23-41 = NUMERO PORTEUR");
    }

    @Test
    void generateProducesMultipleLines() {
        ClearingRecord r1 = ClearingRecord.builder()
                .amount(new BigDecimal("100.000")).messageType("0200").build();
        ClearingRecord r2 = ClearingRecord.builder()
                .amount(new BigDecimal("200.000")).messageType("0400").build();
        String content = service.generate(LocalDate.of(2025, 6, 4), List.of(r1, r2));
        String[] lines = content.split("\n");
        assertEquals(2, lines.length);
        assertEquals(168, lines[0].length());
        assertEquals(168, lines[1].length());
    }

    @Test
    void zonedRedefinesStartsAt126() {
        ClearingRecord r = ClearingRecord.builder()
                .amount(new BigDecimal("100.000"))
                .messageType("0200")
                .build();
        String line = service.generateLine(r, LocalDate.of(2025, 6, 4));
        String zone = line.substring(125, 168);
        assertEquals(43, zone.length(), "Zone REDEFINES must be 43 chars (pos 126-168)");
        String enseigne = line.substring(125, 150);
        assertEquals(25, enseigne.length(), "ENSEIGNE COMMERCANT = 25 chars (pos 126-150)");
    }

    @Test
    void parseReturnsEmptyForBlankInput() {
        List<ClearingRecord> result = service.parse("");
        assertTrue(result.isEmpty());
    }
}
