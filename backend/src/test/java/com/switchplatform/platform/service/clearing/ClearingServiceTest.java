package com.switchplatform.platform.service.clearing;

import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.model.clearing.NettingRecord;
import com.switchplatform.platform.model.clearing.ReconciliationRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClearingServiceTest {

    private ClearingService clearingService;
    private InterchangeService interchangeService;

    @BeforeEach
    void setUp() {
        interchangeService = new InterchangeService();
        clearingService = new ClearingService(interchangeService);
    }

    @Test
    void shouldProcessClearing() {
        UUID acquirerId = UUID.randomUUID();
        UUID issuerId = UUID.randomUUID();

        ClearingService.ClearingData data = ClearingService.ClearingData.builder()
                .transactionId("TXN001")
                .acquiringParticipantId(acquirerId)
                .issuingParticipantId(issuerId)
                .amount(BigDecimal.valueOf(250.00))
                .currencyCode("USD")
                .interchangeAmount(BigDecimal.valueOf(1.50))
                .feeAmount(BigDecimal.valueOf(0.50))
                .messageType("0200")
                .build();

        ClearingRecord record = clearingService.processClearing(data);

        assertNotNull(record.getId());
        assertEquals("TXN001", record.getTransactionId());
        assertEquals(acquirerId, record.getAcquiringParticipantId());
        assertEquals(issuerId, record.getIssuingParticipantId());
        assertEquals(0, BigDecimal.valueOf(250.00).compareTo(record.getAmount()));
        assertEquals("USD", record.getCurrencyCode());
        assertEquals(0, BigDecimal.valueOf(1.50).compareTo(record.getInterchangeAmount()));
        assertEquals(0, BigDecimal.valueOf(0.50).compareTo(record.getFeeAmount()));
        assertEquals(ClearingRecord.Status.PENDING, record.getStatus());
        assertNotNull(record.getCreatedAt());
    }

    @Test
    void shouldClearTransaction() {
        UUID acquirerId = UUID.randomUUID();
        UUID issuerId = UUID.randomUUID();

        ClearingRecord record = clearingService.processClearing(
                ClearingService.ClearingData.builder()
                        .transactionId("TXN002")
                        .acquiringParticipantId(acquirerId)
                        .issuingParticipantId(issuerId)
                        .amount(BigDecimal.valueOf(100.00))
                        .currencyCode("USD")
                        .build());

        ClearingRecord cleared = clearingService.clearTransaction(record.getId());

        assertEquals(ClearingRecord.Status.CLEARED, cleared.getStatus());
        assertEquals(record.getId(), cleared.getId());
    }

    @Test
    void shouldDisputeClearing() {
        UUID acquirerId = UUID.randomUUID();
        UUID issuerId = UUID.randomUUID();

        ClearingRecord record = clearingService.processClearing(
                ClearingService.ClearingData.builder()
                        .transactionId("TXN003")
                        .acquiringParticipantId(acquirerId)
                        .issuingParticipantId(issuerId)
                        .amount(BigDecimal.valueOf(500.00))
                        .currencyCode("USD")
                        .build());

        String disputeReason = "Cardholder does not recognize this transaction";
        ClearingRecord disputed = clearingService.disputeClearing(record.getId(), disputeReason);

        assertEquals(ClearingRecord.Status.DISPUTED, disputed.getStatus());
        assertEquals(disputeReason, disputed.getDisputeReason());
    }

    @Test
    void shouldCalculateNetting() {
        UUID p1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID p2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        LocalDate today = LocalDate.now();

        ClearingRecord rec1 = clearingService.processClearing(
                ClearingService.ClearingData.builder()
                        .transactionId("TXN004")
                        .acquiringParticipantId(p1)
                        .issuingParticipantId(p2)
                        .amount(BigDecimal.valueOf(500))
                        .currencyCode("USD")
                        .interchangeAmount(BigDecimal.ZERO)
                        .build());
        clearingService.clearTransaction(rec1.getId());

        ClearingRecord rec2 = clearingService.processClearing(
                ClearingService.ClearingData.builder()
                        .transactionId("TXN005")
                        .acquiringParticipantId(p2)
                        .issuingParticipantId(p1)
                        .amount(BigDecimal.valueOf(200))
                        .currencyCode("USD")
                        .interchangeAmount(BigDecimal.ZERO)
                        .build());
        clearingService.clearTransaction(rec2.getId());

        List<NettingRecord> netting = clearingService.calculateNetting(today);

        assertEquals(1, netting.size());

        NettingRecord net = netting.get(0);
        assertEquals(today, net.getNettingDate());
        assertEquals(p1, net.getParticipantId());
        assertEquals(p2, net.getCounterpartyId());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(net.getTotalSent()));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(net.getTotalReceived()));
        assertEquals(0, BigDecimal.valueOf(300).compareTo(net.getNetAmount()));
        assertEquals("USD", net.getCurrencyCode());
        assertEquals(2, net.getTransactionCount());
        assertEquals(NettingRecord.Status.PENDING, net.getStatus());
    }

    @Test
    void shouldConfirmNetting() {
        UUID p1 = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID p2 = UUID.fromString("00000000-0000-0000-0000-000000000004");
        LocalDate today = LocalDate.now();

        ClearingRecord rec = clearingService.processClearing(
                ClearingService.ClearingData.builder()
                        .transactionId("TXN006")
                        .acquiringParticipantId(p1)
                        .issuingParticipantId(p2)
                        .amount(BigDecimal.valueOf(300))
                        .currencyCode("USD")
                        .build());
        clearingService.clearTransaction(rec.getId());

        List<NettingRecord> netting = clearingService.calculateNetting(today);
        assertEquals(1, netting.size());

        NettingRecord confirmed = clearingService.confirmNetting(netting.get(0).getId());

        assertEquals(NettingRecord.Status.CONFIRMED, confirmed.getStatus());
        assertEquals(netting.get(0).getId(), confirmed.getId());
    }

    @Test
    void shouldCreateReconciliationReport() {
        UUID participantId = UUID.randomUUID();
        UUID otherParticipant = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        clearingService.processClearing(
                ClearingService.ClearingData.builder()
                        .transactionId("TXN007")
                        .acquiringParticipantId(participantId)
                        .issuingParticipantId(otherParticipant)
                        .amount(BigDecimal.valueOf(500))
                        .feeAmount(BigDecimal.valueOf(5))
                        .currencyCode("USD")
                        .build());

        clearingService.processClearing(
                ClearingService.ClearingData.builder()
                        .transactionId("TXN008")
                        .acquiringParticipantId(otherParticipant)
                        .issuingParticipantId(participantId)
                        .amount(BigDecimal.valueOf(300))
                        .feeAmount(BigDecimal.valueOf(3))
                        .currencyCode("USD")
                        .build());

        ReconciliationRecord report = clearingService.getReconciliationReport(today, participantId);

        assertNotNull(report.getId());
        assertEquals(today, report.getReconciliationDate());
        assertEquals(participantId, report.getParticipantId());
        assertEquals(ReconciliationRecord.Source.SWITCH, report.getSource());
        assertEquals(2, report.getTotalTransactions());
        assertEquals(0, BigDecimal.valueOf(800).compareTo(report.getTotalAmount()));
        assertEquals(0, BigDecimal.valueOf(8).compareTo(report.getTotalFees()));
        assertEquals(2, report.getMatchedCount());
        assertEquals(0, report.getUnmatchedCount());
        assertEquals(0, report.getDiscrepancyCount());
        assertEquals(ReconciliationRecord.Status.PENDING, report.getStatus());
    }

    @Test
    void shouldThrowWhenProcessingWithNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                clearingService.processClearing(
                        ClearingService.ClearingData.builder()
                                .transactionId("TXN009")
                                .acquiringParticipantId(UUID.randomUUID())
                                .issuingParticipantId(UUID.randomUUID())
                                .amount(BigDecimal.valueOf(-100))
                                .currencyCode("USD")
                                .build()));
    }

    @Test
    void shouldThrowWhenClearingRecordNotFound() {
        assertThrows(IllegalArgumentException.class, () ->
                clearingService.clearTransaction(UUID.randomUUID()));
    }

    @Test
    void shouldReturnClearingByDate() {
        UUID acquirerId = UUID.randomUUID();
        UUID issuerId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        clearingService.processClearing(
                ClearingService.ClearingData.builder()
                        .transactionId("TXN010")
                        .acquiringParticipantId(acquirerId)
                        .issuingParticipantId(issuerId)
                        .amount(BigDecimal.valueOf(100))
                        .currencyCode("USD")
                        .build());

        List<ClearingRecord> records = clearingService.getClearingByDate(today);
        assertFalse(records.isEmpty());
        assertTrue(records.stream().allMatch(r -> r.getClearingDate().equals(today)));
    }

    @Test
    void shouldReturnClearingByParticipant() {
        UUID participantId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        clearingService.processClearing(
                ClearingService.ClearingData.builder()
                        .transactionId("TXN011")
                        .acquiringParticipantId(participantId)
                        .issuingParticipantId(UUID.randomUUID())
                        .amount(BigDecimal.valueOf(100))
                        .currencyCode("USD")
                        .build());

        List<ClearingRecord> records = clearingService.getClearingByParticipant(participantId);
        assertFalse(records.isEmpty());
    }
}
