package com.switchplatform.platform.service.clearing.smt;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Cp50FileServiceTest {

    private Cp50FileService service;

    @BeforeEach
    void setUp() {
        ParticipantRepository repo = mock(ParticipantRepository.class);
        when(repo.findById(any())).thenReturn(Optional.empty());
        service = new Cp50FileService(repo);
    }

    @Test
    void headerIsExactly500Chars() {
        String header = service.generateHeader(LocalDate.of(2025, 6, 4), "00123");
        assertEquals(500, header.length(), "CP50 header must be exactly 500 chars");
    }

    @Test
    void headerFinEnregistrementX() {
        String header = service.generateHeader(LocalDate.of(2025, 6, 4), "00123");
        assertEquals('X', header.charAt(499), "Position 500 must be X");
    }

    @Test
    void headerCodeEnregistrement01() {
        String header = service.generateHeader(LocalDate.of(2025, 6, 4), "00123");
        assertEquals("01", header.substring(0, 2), "Pos 1-2 = CODE ENREGISTREMENT 01");
    }

    @Test
    void headerNumeroEnregistrement() {
        String header = service.generateHeader(LocalDate.of(2025, 6, 4), "00123");
        assertEquals("000001", header.substring(2, 8), "Pos 3-8 = NUMERO D'ENREGISTREMENT 000001");
    }

    @Test
    void headerCodeOperationSpaces() {
        String header = service.generateHeader(LocalDate.of(2025, 6, 4), "00123");
        assertEquals("  ", header.substring(8, 10), "Pos 9-10 = CODE OPERATION 2 espaces");
    }

    @Test
    void headerDateTraitement() {
        String header = service.generateHeader(LocalDate.of(2025, 6, 4), "00123");
        assertEquals("040625", header.substring(10, 16), "Pos 11-16 = DATE TRAITEMENT JJMMAA");
    }

    @Test
    void headerCodeFaconnier222222() {
        String header = service.generateHeader(LocalDate.of(2025, 6, 4), "00123");
        assertEquals("222222", header.substring(16, 22), "Pos 17-22 = CODE FACONNIER 222222");
    }

    @Test
    void headerCodeBanqueDestinataire() {
        String header = service.generateHeader(LocalDate.of(2025, 6, 4), "00123");
        assertEquals("00123", header.substring(22, 27), "Pos 23-27 = CODE BANQUE DESTINATAIRE");
    }

    @Test
    void type80IsExactly500Chars() {
        String line = service.generateType80(1, LocalDate.of(2025, 6, 4), "67890", "50", 150000L, true);
        assertEquals(500, line.length(), "CP50 type80 must be exactly 500 chars");
    }

    @Test
    void type80FinEnregistrementX() {
        String line = service.generateType80(1, LocalDate.of(2025, 6, 4), "67890", "50", 150000L, true);
        assertEquals('X', line.charAt(499), "Position 500 must be X");
    }

    @Test
    void type80CodeEnregistrement80() {
        String line = service.generateType80(1, LocalDate.of(2025, 6, 4), "67890", "50", 150000L, true);
        assertEquals("80", line.substring(0, 2), "Pos 1-2 = 80");
    }

    @Test
    void type80CodeOp50ForDebit() {
        String line = service.generateType80(1, LocalDate.of(2025, 6, 4), "67890", "50", 150000L, true);
        assertEquals("50", line.substring(8, 10), "Pos 9-10 = CODE OP 50 for debit");
    }

    @Test
    void type80CodeOp10ForCredit() {
        String line = service.generateType80(1, LocalDate.of(2025, 6, 4), "67890", "10", 75000L, false);
        assertEquals("10", line.substring(8, 10), "Pos 9-10 = CODE OP 10 for credit");
    }

    @Test
    void type80MontantDebitAtPosition34() {
        String line = service.generateType80(1, LocalDate.of(2025, 6, 4), "67890", "50", 150000L, true);
        assertEquals("000000150000", line.substring(33, 45), "Pos 34-45 = MONTANT DEBIT");
    }

    @Test
    void type80MontantCreditAtPosition46() {
        String line = service.generateType80(1, LocalDate.of(2025, 6, 4), "67890", "50", 150000L, true);
        assertEquals("000000000000", line.substring(45, 57), "Pos 46-57 = MONTANT CREDIT (zero for debit)");
    }

    @Test
    void trailerIsExactly500Chars() {
        String trailer = service.generateTrailer(LocalDate.of(2025, 6, 4), "00123", 5, 150000L, 75000L);
        assertEquals(500, trailer.length(), "CP50 trailer must be exactly 500 chars");
    }

    @Test
    void trailerFinEnregistrementX() {
        String trailer = service.generateTrailer(LocalDate.of(2025, 6, 4), "00123", 5, 150000L, 75000L);
        assertEquals('X', trailer.charAt(499), "Position 500 must be X");
    }

    @Test
    void trailerTotalEnregistrements() {
        String trailer = service.generateTrailer(LocalDate.of(2025, 6, 4), "00123", 7, 150000L, 75000L);
        assertEquals("000007", trailer.substring(2, 8), "Pos 3-8 = total records (incl header + trailer)");
    }

    @Test
    void trailerTotalDebit() {
        String trailer = service.generateTrailer(LocalDate.of(2025, 6, 4), "00123", 5, 150000L, 75000L);
        assertEquals("000000150000", trailer.substring(21, 33), "Pos 22-33 = TOTAL DEBIT");
    }

    @Test
    void trailerTotalCredit() {
        String trailer = service.generateTrailer(LocalDate.of(2025, 6, 4), "00123", 5, 150000L, 75000L);
        assertEquals("000000075000", trailer.substring(33, 45), "Pos 34-45 = TOTAL CREDIT");
    }

    @Test
    void generateFullFileStructure() {
        ClearingRecord r1 = ClearingRecord.builder()
                .amount(new BigDecimal("100.000")).messageType("0200").build();
        ClearingRecord r2 = ClearingRecord.builder()
                .amount(new BigDecimal("50.000")).messageType("0100").build();
        String content = service.generate(LocalDate.of(2025, 6, 4), "00123", List.of(r1, r2));
        String[] lines = content.split("\n");
        assertEquals(4, lines.length, "01 + 2×type80 + 99 = 4 lines (no type40)");
        assertEquals("01", lines[0].substring(0, 2));
        assertEquals("80", lines[1].substring(0, 2));
        assertEquals("80", lines[2].substring(0, 2));
        assertEquals("99", lines[3].substring(0, 2));
    }

    @Test
    void generateType40RecordThrows() {
        assertThrows(UnsupportedOperationException.class, Cp50FileService::generateType40Record);
    }
}
