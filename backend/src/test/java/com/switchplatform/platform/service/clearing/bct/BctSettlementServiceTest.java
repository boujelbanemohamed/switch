package com.switchplatform.platform.service.clearing.bct;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.clearing.NettingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.clearing.NettingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BctSettlementServiceTest {

    private BctSettlementService service;
    private NettingRecordRepository nettingRecordRepository;
    private ParticipantRepository participantRepository;

    private final LocalDate DATE = LocalDate.of(2026, 6, 5);

    private UUID bankA;
    private UUID bankB;
    private UUID bankC;
    private UUID foreignBank;

    @BeforeEach
    void setUp() {
        nettingRecordRepository = mock(NettingRecordRepository.class);
        participantRepository = mock(ParticipantRepository.class);
        service = new BctSettlementService(nettingRecordRepository, participantRepository);

        bankA = UUID.randomUUID();
        bankB = UUID.randomUUID();
        bankC = UUID.randomUUID();
        foreignBank = UUID.randomUUID();

        Participant domA = Participant.builder().id(bankA).code("BNA01").name("Banque Nationale Agricole").isDomestic(true).build();
        Participant domB = Participant.builder().id(bankB).code("BH02").name("Banque de l'Habitat").isDomestic(true).build();
        Participant domC = Participant.builder().id(bankC).code("STB03").name("STB").isDomestic(true).build();
        Participant foreign = Participant.builder().id(foreignBank).code("CITI99").name("Citibank NA").isDomestic(false).build();

        when(participantRepository.findByIsDomesticFalse()).thenReturn(List.of(foreign));
    }

    @Test
    void equilibrium_sumOfNetPositionsIsZero_MultipleBanksAsymmetricAmounts() {
        // Asymmetric amounts: bankA owes, bankB/+bankC are owed
        // Total sent = total received => sum(net positions) = 0
        // bankA: sent 5000, received 1200  => net position (credit - debit) = -3800 (owes 3800)
        // bankB: sent 800, received 3500   => net position = +2700 (owed 2700)
        // bankC: sent 200, received 1300   => net position = +1100 (owed 1100)
        // sum = -3800 + 2700 + 1100 = 0 ✓
        when(nettingRecordRepository.findByNettingDate(DATE)).thenReturn(List.of(
                NettingRecord.builder().participantId(bankA).totalSent(new BigDecimal("5000")).totalReceived(new BigDecimal("1200")).currencyCode("TND").transactionCount(10).build(),
                NettingRecord.builder().participantId(bankB).totalSent(new BigDecimal("800")).totalReceived(new BigDecimal("3500")).currencyCode("TND").transactionCount(8).build(),
                NettingRecord.builder().participantId(bankC).totalSent(new BigDecimal("200")).totalReceived(new BigDecimal("1300")).currencyCode("TND").transactionCount(5).build()
        ));

        when(participantRepository.findById(bankA)).thenReturn(java.util.Optional.of(Participant.builder().id(bankA).code("BNA01").name("Banque Nationale Agricole").build()));
        when(participantRepository.findById(bankB)).thenReturn(java.util.Optional.of(Participant.builder().id(bankB).code("BH02").name("Banque de l'Habitat").build()));
        when(participantRepository.findById(bankC)).thenReturn(java.util.Optional.of(Participant.builder().id(bankC).code("STB03").name("STB").build()));

        String csv = service.generateBctSettlementFile(DATE);
        String[] lines = csv.split("\n");

        // Header + 3 data lines + blank + totals = 6 lines
        assertTrue(lines.length >= 5, "Should have header, 3 institutions, blank, and totals row");

        // Parse net positions from CSV — find by institution code (HashMap order non-deterministic)
        String bankANetStr = null, bankBNetStr = null, bankCNetStr = null;
        BigDecimal netSum = BigDecimal.ZERO;
        for (int i = 1; i <= 3; i++) {
            String[] cols = lines[i].split(",");
            String code = cols[1]; // institution_code at index 1
            BigDecimal netPos = new BigDecimal(cols[5]); // net_position at index 5
            netSum = netSum.add(netPos);
            if ("BNA01".equals(code)) bankANetStr = cols[5];
            else if ("BH02".equals(code)) bankBNetStr = cols[5];
            else if ("STB03".equals(code)) bankCNetStr = cols[5];
        }
        assertNotNull(bankANetStr);
        assertNotNull(bankBNetStr);
        assertNotNull(bankCNetStr);

        assertEquals(0, netSum.compareTo(BigDecimal.ZERO),
                "Sum of net positions across all domestic institutions must be EXACTLY zero. Sum was: " + netSum);

        // Verify each institution's net position sign is correct
        assertTrue(new BigDecimal(bankANetStr).compareTo(BigDecimal.ZERO) < 0,
                "bankA (code=BNA01, owes 3800) should have negative net position, got: " + bankANetStr);
        assertTrue(new BigDecimal(bankBNetStr).compareTo(BigDecimal.ZERO) > 0,
                "bankB (code=BH02, owed 2700) should have positive net position, got: " + bankBNetStr);
        assertTrue(new BigDecimal(bankCNetStr).compareTo(BigDecimal.ZERO) > 0,
                "bankC (code=STB03, owed 1100) should have positive net position, got: " + bankCNetStr);

        // Verify totals line (find by prefix, since data line count varies with HashMap order)
        String totalsLine = "";
        for (String l : lines) {
            if (l.startsWith("TOTALS")) { totalsLine = l; break; }
        }
        assertFalse(totalsLine.isEmpty(), "Should have a TOTALS line");
        String[] totCols = totalsLine.split(",");
        assertEquals(new BigDecimal("6000"), new BigDecimal(totCols[3]),
                "Total debit should be 6000 (5000+800+200)");
        assertEquals(new BigDecimal("6000"), new BigDecimal(totCols[4]),
                "Total credit should be 6000 (1200+3500+1300)");
        assertEquals(0, new BigDecimal(totCols[5]).compareTo(BigDecimal.ZERO),
                "Total net position should be zero");
    }

    @Test
    void foreignBanks_excludedFromBctCalculation() {
        // 3 domestic banks + 1 foreign bank with large amounts
        // Foreign bank should not appear in output
        when(nettingRecordRepository.findByNettingDate(DATE)).thenReturn(List.of(
                NettingRecord.builder().participantId(bankA).totalSent(new BigDecimal("1000")).totalReceived(new BigDecimal("1000")).currencyCode("TND").transactionCount(5).build(),
                NettingRecord.builder().participantId(foreignBank).totalSent(new BigDecimal("99999")).totalReceived(new BigDecimal("99999")).currencyCode("TND").transactionCount(100).build()
        ));

        when(participantRepository.findById(bankA)).thenReturn(java.util.Optional.of(Participant.builder().id(bankA).code("BNA01").name("Banque Nationale Agricole").build()));

        String csv = service.generateBctSettlementFile(DATE);
        String[] lines = csv.split("\n");

        // Should contain bankA but NOT foreignBank in data lines
        boolean hasDomestic = false;
        boolean hasForeign = false;
        String totalsLine = "";
        for (String l : lines) {
            if (l.startsWith("TOTALS")) { totalsLine = l; continue; }
            if (l.contains("BNA01")) hasDomestic = true;
            if (l.contains("CITI99")) hasForeign = true;
        }
        assertTrue(hasDomestic, "Domestic bank BNA01 should appear in BCT output");
        assertFalse(hasForeign, "Foreign bank CITI99 should NOT appear in BCT output");
        assertEquals(new BigDecimal("1000"), new BigDecimal(totalsLine.split(",")[3]),
                "Total debit should only include domestic banks");
    }

    @Test
    void emptyNetting_returnsOnlyHeadersAndTotals() {
        when(nettingRecordRepository.findByNettingDate(DATE)).thenReturn(List.of());

        String csv = service.generateBctSettlementFile(DATE);
        String[] lines = csv.split("\n");

        assertTrue(lines[0].startsWith("settlement_date"));
        assertTrue(lines[lines.length - 1].startsWith("TOTALS,,,0,0,0,TND,0"));
    }

    @Test
    void singleDomesticInstitution_netPositionIsZero() {
        // Single institution — sent = received so net = 0
        when(nettingRecordRepository.findByNettingDate(DATE)).thenReturn(List.of(
                NettingRecord.builder().participantId(bankA).totalSent(new BigDecimal("2500")).totalReceived(new BigDecimal("2500")).currencyCode("TND").transactionCount(7).build()
        ));

        when(participantRepository.findById(bankA)).thenReturn(java.util.Optional.of(Participant.builder().id(bankA).code("BNA01").name("Banque Nationale Agricole").build()));

        String csv = service.generateBctSettlementFile(DATE);
        String[] lines = csv.split("\n");

        String dataLine = "";
        for (String l : lines) {
            if (!l.startsWith("settlement_") && !l.startsWith("TOTALS") && !l.isEmpty()) {
                dataLine = l; break;
            }
        }
        assertFalse(dataLine.isEmpty(), "Should have a data line");
        assertEquals(0, new BigDecimal(dataLine.split(",")[5]).compareTo(BigDecimal.ZERO),
                "Single domestic institution with equal sent/received should have zero net position");
    }
}
