package com.switchplatform.platform.service.clearing;

import com.switchplatform.platform.event.EventPublisher;
import com.switchplatform.platform.iso20022.Iso20022Engine;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.model.clearing.ReconciliationRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.clearing.ClearingRecordRepository;
import com.switchplatform.platform.repository.clearing.ReconciliationRecordRepository;
import com.switchplatform.platform.service.clearing.network.Iso20022ClearingGenerator;
import com.switchplatform.platform.service.clearing.network.MastercardIpmGenerator;
import com.switchplatform.platform.service.clearing.network.VisaBaseIIGenerator;
import com.switchplatform.platform.service.clearing.smt.CompconfFileService;
import com.switchplatform.platform.service.clearing.smt.Cp50FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SettlementFileServiceTest {

    private SettlementFileService service;
    private ClearingRecordRepository clearingRecordRepository;
    private ReconciliationRecordRepository reconciliationRecordRepository;
    private CompconfFileService compconfFileService;
    private Cp50FileService cp50FileService;

    private final UUID participantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        clearingRecordRepository = mock(ClearingRecordRepository.class);
        reconciliationRecordRepository = mock(ReconciliationRecordRepository.class);
        compconfFileService = mock(CompconfFileService.class);
        cp50FileService = mock(Cp50FileService.class);

        service = new SettlementFileService(
                clearingRecordRepository,
                mock(ParticipantRepository.class),
                reconciliationRecordRepository,
                new Iso20022Engine(),
                compconfFileService,
                cp50FileService,
                mock(EventPublisher.class),
                new Iso20022ClearingGenerator(new Iso20022Engine()),
                new VisaBaseIIGenerator(),
                new MastercardIpmGenerator()
        );
    }

    @Test
    void ingestCsv_matchedAndUnmatchedRecords() {
        String csv = "clearing_date,transaction_id,acquiring_participant,issuing_participant,amount\n" +
                "2026-06-05,TXN-MATCHED,p1,p2,100\n" +
                "2026-06-05,TXN-UNMATCHED,p3,p4,200\n";

        when(clearingRecordRepository.findByTransactionId("TXN-MATCHED"))
                .thenReturn(Optional.of(ClearingRecord.builder()
                        .id(UUID.randomUUID()).transactionId("TXN-MATCHED").status(ClearingRecord.Status.PENDING).build()));
        when(clearingRecordRepository.findByTransactionId("TXN-UNMATCHED"))
                .thenReturn(Optional.empty());

        SettlementFileService.ReconciliationResult result =
                service.ingestIncomingClearingFile(csv, "CSV", participantId);

        assertEquals(2, result.totalRecords());
        assertEquals(1, result.matched());
        assertEquals(1, result.unmatched());

        verify(reconciliationRecordRepository).save(any(ReconciliationRecord.class));
    }

    @Test
    void ingestCsv_allMatched() {
        String csv = "header,txId\n2026-06-05,TXN-001,p1,p2,100\n2026-06-05,TXN-002,p3,p4,200\n2026-06-05,TXN-003,p5,p6,300\n";

        when(clearingRecordRepository.findByTransactionId(anyString()))
                .thenReturn(Optional.of(ClearingRecord.builder()
                        .id(UUID.randomUUID()).status(ClearingRecord.Status.PENDING).build()));

        SettlementFileService.ReconciliationResult result =
                service.ingestIncomingClearingFile(csv, "CSV", participantId);

        assertEquals(3, result.totalRecords());
        assertEquals(3, result.matched());
        assertEquals(0, result.unmatched());
    }

    @Test
    void ingestCsv_emptyContent() {
        SettlementFileService.ReconciliationResult result =
                service.ingestIncomingClearingFile("header\n", "CSV", participantId);

        assertEquals(0, result.totalRecords());
        assertEquals(0, result.matched());
        assertEquals(0, result.unmatched());
    }

    @Test
    void ingestCompconf_parsesAndMatches() {
        String mockContent = "mock compconf content";
        List<ClearingRecord> parsed = List.of(
                ClearingRecord.builder().id(UUID.randomUUID()).transactionId("TXN-A").build(),
                ClearingRecord.builder().id(UUID.randomUUID()).transactionId("TXN-B").build()
        );
        when(compconfFileService.parse(mockContent)).thenReturn(parsed);
        when(clearingRecordRepository.findByTransactionId("TXN-A"))
                .thenReturn(Optional.of(ClearingRecord.builder().id(UUID.randomUUID()).transactionId("TXN-A").status(ClearingRecord.Status.PENDING).build()));
        when(clearingRecordRepository.findByTransactionId("TXN-B"))
                .thenReturn(Optional.empty());

        SettlementFileService.ReconciliationResult result =
                service.ingestIncomingClearingFile(mockContent, "COMPCONF", participantId);

        assertEquals(2, result.totalRecords());
        assertEquals(1, result.matched());
        assertEquals(1, result.unmatched());
    }

    @Test
    void ingestCp50_unmatchedCountEqualsTotal() {
        when(cp50FileService.parse(anyString())).thenReturn(List.of("line1", "line2", "line3"));

        SettlementFileService.ReconciliationResult result =
                service.ingestIncomingClearingFile("mock cp50", "CP50", participantId);

        assertEquals(3, result.totalRecords());
        assertEquals(0, result.matched());
        assertEquals(3, result.unmatched());
    }

    @Test
    void ingestUnsupportedFormat_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.ingestIncomingClearingFile("content", "UNSUPPORTED", participantId));
    }
}
