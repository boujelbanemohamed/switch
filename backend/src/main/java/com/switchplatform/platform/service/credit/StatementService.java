package com.switchplatform.platform.service.credit;

import com.switchplatform.platform.model.credit.CreditLine;
import com.switchplatform.platform.model.credit.CreditStatement;
import com.switchplatform.platform.model.credit.InstallmentEntry;
import com.switchplatform.platform.model.credit.InstallmentPlan;
import com.switchplatform.platform.repository.credit.CreditLineRepository;
import com.switchplatform.platform.repository.credit.CreditStatementRepository;
import com.switchplatform.platform.repository.credit.InstallmentEntryRepository;
import com.switchplatform.platform.repository.credit.InstallmentPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final CreditLineRepository creditLineRepository;
    private final CreditStatementRepository creditStatementRepository;
    private final InstallmentPlanRepository installmentPlanRepository;
    private final InstallmentEntryRepository installmentEntryRepository;

    @Scheduled(cron = "0 30 2 * * *")
    public void generateStatementsForDueLines() {
        int today = LocalDate.now().getDayOfMonth();
        List<CreditLine> lines = creditLineRepository.findByStatus(CreditLine.CreditLineStatus.ACTIVE);
        for (CreditLine line : lines) {
            if (line.getStatementDay() == today) {
                try {
                    generateStatement(line.getId());
                } catch (Exception e) {
                    log.error("Failed to generate statement for credit line {}: {}", line.getId(), e.getMessage());
                }
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void markOverdueStatements() {
        List<CreditStatement> openStatements = creditStatementRepository
                .findByStatus(CreditStatement.StatementStatus.OPEN);
        LocalDate today = LocalDate.now();
        for (CreditStatement stmt : openStatements) {
            if (stmt.getDueDate().isBefore(today)) {
                stmt.setStatus(CreditStatement.StatementStatus.OVERDUE);
                creditStatementRepository.save(stmt);
                log.info("Marked statement {} as OVERDUE (due was {})", stmt.getId(), stmt.getDueDate());
            }
        }
    }

    @Transactional
    public CreditStatement generateStatement(UUID creditLineId) {
        CreditLine cl = creditLineRepository.findById(creditLineId)
                .orElseThrow(() -> new IllegalArgumentException("Credit line not found: " + creditLineId));

        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.withDayOfMonth(1);
        LocalDate periodEnd = today;

        Optional<CreditStatement> previousOpt = creditStatementRepository
                .findTopByCreditLineIdOrderByStatementDateDesc(creditLineId);

        CreditStatement previous = previousOpt.orElse(null);
        BigDecimal openingBalance = previous != null ? previous.getClosingBalance() : BigDecimal.ZERO;
        boolean previousPaidInFull = previous != null && previous.getPaidInFull();

        // Fetch purchases and payments since last statement
        // For now we compute from current_balance movements since we don't have per-transaction tracking
        // In a phase 2 we can enrich with actual transaction records
        BigDecimal currentBalance = cl.getCurrentBalance();

        // Compute purchases: difference between current balance and opening balance + any payments
        // Since we don't track per-transaction, purchases = currentBalance - openingBalance + payments
        // Payments are tracked via current_balance reduction. For simplicity:
        BigDecimal purchasesTotal = currentBalance.subtract(openingBalance).max(BigDecimal.ZERO);
        BigDecimal paymentsTotal = openingBalance.subtract(currentBalance).max(BigDecimal.ZERO);

        // Add installment amounts due this period
        BigDecimal installmentDue = BigDecimal.ZERO;
        List<InstallmentPlan> activePlans = installmentPlanRepository
                .findByCreditLineIdAndStatus(creditLineId, InstallmentPlan.InstallmentStatus.ACTIVE);
        for (InstallmentPlan plan : activePlans) {
            List<InstallmentEntry> entries = installmentEntryRepository
                    .findByInstallmentPlanIdOrderBySequenceNumberAsc(plan.getId());
            for (InstallmentEntry entry : entries) {
                if (!entry.getPaid() && !entry.getDueDate().isAfter(periodEnd)
                        && entry.getStatementId() == null) {
                    installmentDue = installmentDue.add(entry.getAmount());
                }
            }
        }

        purchasesTotal = purchasesTotal.add(installmentDue);

        // Calculate interest
        BigDecimal interestCharged = BigDecimal.ZERO;
        if (!previousPaidInFull && openingBalance.compareTo(BigDecimal.ZERO) > 0) {
            // Interest = opening balance × (APR / 12 / 100)
            BigDecimal monthlyRate = cl.getApr()
                    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            interestCharged = openingBalance.multiply(monthlyRate)
                    .setScale(3, RoundingMode.HALF_UP);
        }

        BigDecimal feesCharged = BigDecimal.ZERO;
        BigDecimal closingBalance = openingBalance.add(purchasesTotal)
                .subtract(paymentsTotal).add(interestCharged).add(feesCharged);

        // Minimum payment
        BigDecimal minPctAmount = closingBalance
                .multiply(cl.getMinPaymentPct())
                .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
        BigDecimal minimumPayment = minPctAmount.max(cl.getMinPaymentFloor());

        LocalDate dueDate = today.plusDays(cl.getPaymentDueDays());

        CreditStatement statement = CreditStatement.builder()
                .creditLineId(creditLineId)
                .statementDate(today)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .openingBalance(openingBalance)
                .purchasesTotal(purchasesTotal)
                .paymentsTotal(paymentsTotal)
                .interestCharged(interestCharged)
                .feesCharged(feesCharged)
                .closingBalance(closingBalance)
                .minimumPayment(minimumPayment)
                .dueDate(dueDate)
                .paidInFull(false)
                .status(CreditStatement.StatementStatus.OPEN)
                .build();

        statement = creditStatementRepository.save(statement);

        // Link unpaid installment entries to this statement
        for (InstallmentPlan plan : activePlans) {
            List<InstallmentEntry> entries = installmentEntryRepository
                    .findByInstallmentPlanIdOrderBySequenceNumberAsc(plan.getId());
            for (InstallmentEntry entry : entries) {
                if (!entry.getPaid() && !entry.getDueDate().isAfter(periodEnd)
                        && entry.getStatementId() == null) {
                    entry.setStatementId(statement.getId());
                    installmentEntryRepository.save(entry);
                }
            }
        }

        log.info("Statement generated: id={}, line={}, opening={}, purchases={}, interest={}, "
                + "closing={}, minPayment={}, due={}",
                statement.getId(), creditLineId, openingBalance, purchasesTotal,
                interestCharged, closingBalance, minimumPayment, dueDate);

        return statement;
    }

    public List<CreditStatement> getStatements(UUID creditLineId) {
        return creditStatementRepository.findByCreditLineIdOrderByStatementDateDesc(creditLineId);
    }

    public Optional<CreditStatement> getStatement(UUID statementId) {
        return creditStatementRepository.findById(statementId);
    }
}
