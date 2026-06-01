package com.switchplatform.platform.service.ledger;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final AuthDecisionRepository authDecisionRepository;
    private final HoldRecordRepository holdRecordRepository;
    private final ClearingRecordRepository clearingRecordRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public ReconciliationReport reconcile(OffsetDateTime from, OffsetDateTime to) {
        List<ReconciliationIssue> issues = new ArrayList<>();

        long authCount = authDecisionRepository.countByCreatedAtBetween(from, to);
        long clearingCount = clearingRecordRepository.countByCreatedAtBetween(from, to);
        long settlementCount = settlementRecordRepository.countByCreatedAtBetween(from, to);

        issues.addAll(detectMissingTransactions(from, to));
        issues.addAll(detectDuplicateTransactions(from, to));
        issues.addAll(detectAmountMismatches(from, to));
        issues.addAll(detectSettlementGaps(from, to));

        return new ReconciliationReport(
                OffsetDateTime.now(), from, to,
                authCount, clearingCount, settlementCount,
                issues.size(), issues);
    }

    private List<ReconciliationIssue> detectMissingTransactions(OffsetDateTime from, OffsetDateTime to) {
        List<ReconciliationIssue> issues = new ArrayList<>();
        List<AuthDecision> decisions = authDecisionRepository.findByCreatedAtBetween(from, to);
        for (AuthDecision d : decisions) {
            String txnRef = d.getTransactionId() != null ? d.getTransactionId() : d.getStan();
            if (txnRef == null) continue;
            List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionReference(txnRef);
            if (entries.isEmpty()) {
                issues.add(new ReconciliationIssue(
                        "MISSING", "AUTHORIZATION", txnRef,
                        "No ledger entry found for authorized transaction", d.getAmount()));
            }
        }
        return issues;
    }

    private List<ReconciliationIssue> detectDuplicateTransactions(OffsetDateTime from, OffsetDateTime to) {
        List<ReconciliationIssue> issues = new ArrayList<>();
        List<SettlementRecord> settlements = settlementRecordRepository.findByCreatedAtBetween(from, to);
        Set<String> seen = new HashSet<>();
        for (SettlementRecord s : settlements) {
            if (s.getPaymentRef() != null && !seen.add(s.getPaymentRef())) {
                issues.add(new ReconciliationIssue(
                        "DUPLICATE", "SETTLEMENT", s.getPaymentRef(),
                        "Duplicate settlement reference", s.getNetAmount()));
            }
        }
        return issues;
    }

    private List<ReconciliationIssue> detectAmountMismatches(OffsetDateTime from, OffsetDateTime to) {
        List<ReconciliationIssue> issues = new ArrayList<>();
        List<AuthDecision> decisions = authDecisionRepository.findByCreatedAtBetween(from, to);
        for (AuthDecision d : decisions) {
            String txnRef = d.getTransactionId() != null ? d.getTransactionId() : d.getStan();
            if (txnRef == null || d.getAmount() == null) continue;
            List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionReference(txnRef);
            BigDecimal ledgerTotal = entries.stream()
                    .map(e -> {
                        BigDecimal dAmt = e.getDebitAmount() != null ? e.getDebitAmount() : BigDecimal.ZERO;
                        BigDecimal cAmt = e.getCreditAmount() != null ? e.getCreditAmount() : BigDecimal.ZERO;
                        return dAmt.subtract(cAmt);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add).abs();
            if (ledgerTotal.compareTo(BigDecimal.ZERO) > 0
                    && d.getAmount().compareTo(ledgerTotal) != 0) {
                issues.add(new ReconciliationIssue(
                        "MISMATCH", "AMOUNT", txnRef,
                        "Auth amount=" + d.getAmount() + " vs ledger=" + ledgerTotal, d.getAmount()));
            }
        }
        return issues;
    }

    private List<ReconciliationIssue> detectSettlementGaps(OffsetDateTime from, OffsetDateTime to) {
        List<ReconciliationIssue> issues = new ArrayList<>();
        List<HoldRecord> holds = holdRecordRepository.findByStatusAndCreatedAtBetween("ACTIVE", from.toInstant(), to.toInstant());
        for (HoldRecord h : holds) {
            issues.add(new ReconciliationIssue(
                    "GAP", "SETTLEMENT", h.getTransactionId(),
                    "Hold still active without settlement: amount=" + h.getAmount(), h.getAmount()));
        }
        return issues;
    }

    public record ReconciliationReport(
            OffsetDateTime generatedAt,
            OffsetDateTime from, OffsetDateTime to,
            long authCount, long clearingCount, long settlementCount,
            int issueCount, List<ReconciliationIssue> issues) {}

    public record ReconciliationIssue(
            String type, String source, String reference,
            String description, BigDecimal amount) {}
}
