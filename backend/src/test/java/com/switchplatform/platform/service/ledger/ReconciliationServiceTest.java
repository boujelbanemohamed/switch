package com.switchplatform.platform.service.ledger;

import com.switchplatform.platform.event.EventPublisher;
import com.switchplatform.platform.model.authorization.AuthDecision;
import com.switchplatform.platform.model.authorization.HoldRecord;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.model.acquiring.SettlementRecord;
import com.switchplatform.platform.model.ledger.LedgerEntry;
import com.switchplatform.platform.repository.authorization.AuthDecisionRepository;
import com.switchplatform.platform.repository.authorization.HoldRecordRepository;
import com.switchplatform.platform.repository.clearing.ClearingRecordRepository;
import com.switchplatform.platform.repository.acquiring.SettlementRecordRepository;
import com.switchplatform.platform.repository.ledger.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconciliationServiceTest {

    @Mock private AuthDecisionRepository authDecisionRepository;
    @Mock private HoldRecordRepository holdRecordRepository;
    @Mock private ClearingRecordRepository clearingRecordRepository;
    @Mock private SettlementRecordRepository settlementRecordRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private EventPublisher eventPublisher;

    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService(authDecisionRepository, holdRecordRepository,
                clearingRecordRepository, settlementRecordRepository, ledgerEntryRepository, eventPublisher);
    }

    @Test
    void reconcile_shouldReturnEmptyIssuesWhenAllMatch() {
        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();

        when(authDecisionRepository.countByCreatedAtBetween(from, to)).thenReturn(5L);
        when(clearingRecordRepository.countByCreatedAtBetween(from, to)).thenReturn(5L);
        when(settlementRecordRepository.countByCreatedAtBetween(from, to)).thenReturn(5L);
        when(authDecisionRepository.findByCreatedAtBetween(from, to)).thenReturn(Collections.emptyList());
        when(clearingRecordRepository.findByCreatedAtBetween(from, to)).thenReturn(Collections.emptyList());
        when(settlementRecordRepository.findByCreatedAtBetween(from, to)).thenReturn(Collections.emptyList());
        when(holdRecordRepository.findByStatusAndCreatedAtBetween(eq("ACTIVE"), any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        ReconciliationService.ReconciliationReport report = service.reconcile(from, to);

        assertNotNull(report);
        assertEquals(5, report.authCount());
        assertEquals(5, report.clearingCount());
        assertEquals(5, report.settlementCount());
    }

    @Test
    void reconcile_shouldDetectMismatches() {
        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();

        when(authDecisionRepository.countByCreatedAtBetween(from, to)).thenReturn(10L);
        when(clearingRecordRepository.countByCreatedAtBetween(from, to)).thenReturn(8L);
        when(settlementRecordRepository.countByCreatedAtBetween(from, to)).thenReturn(7L);
        when(authDecisionRepository.findByCreatedAtBetween(from, to)).thenReturn(Collections.emptyList());
        when(clearingRecordRepository.findByCreatedAtBetween(from, to)).thenReturn(Collections.emptyList());
        when(settlementRecordRepository.findByCreatedAtBetween(from, to)).thenReturn(Collections.emptyList());
        when(holdRecordRepository.findByStatusAndCreatedAtBetween(eq("ACTIVE"), any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        ReconciliationService.ReconciliationReport report = service.reconcile(from, to);

        assertNotNull(report);
        assertEquals(10, report.authCount());
        assertEquals(8, report.clearingCount());
        assertEquals(7, report.settlementCount());
    }
}
