package com.switchplatform.platform.service.credit;

import com.switchplatform.platform.model.credit.InstallmentEntry;
import com.switchplatform.platform.model.credit.InstallmentPlan;
import com.switchplatform.platform.repository.credit.InstallmentEntryRepository;
import com.switchplatform.platform.repository.credit.InstallmentPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstallmentService {

    private final InstallmentPlanRepository installmentPlanRepository;
    private final InstallmentEntryRepository installmentEntryRepository;

    @Transactional
    public InstallmentPlan convertToInstallments(UUID creditLineId, String transactionRef,
                                                  BigDecimal totalAmount, int count,
                                                  BigDecimal feeAmount, BigDecimal apr) {
        if (count <= 0) throw new IllegalArgumentException("Installment count must be positive");
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive");

        BigDecimal installmentAmount = totalAmount.divide(BigDecimal.valueOf(count), 3, RoundingMode.HALF_UP);
        BigDecimal feePerInstallment = feeAmount.divide(BigDecimal.valueOf(count), 3, RoundingMode.HALF_UP);
        BigDecimal totalWithFees = totalAmount.add(feeAmount);

        InstallmentPlan plan = InstallmentPlan.builder()
                .creditLineId(creditLineId)
                .originalTransactionRef(transactionRef)
                .totalAmount(totalWithFees)
                .installmentCount(count)
                .installmentAmount(installmentAmount.add(feePerInstallment))
                .feeAmount(feeAmount)
                .apr(apr)
                .startDate(LocalDate.now())
                .remainingCount(count)
                .status(InstallmentPlan.InstallmentStatus.ACTIVE)
                .build();

        plan = installmentPlanRepository.save(plan);
        UUID planId = plan.getId();

        // Create installment entries spread over consecutive months
        List<InstallmentEntry> entries = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            LocalDate dueDate = LocalDate.now().plusMonths(i);
            BigDecimal entryAmount = i < count
                    ? plan.getInstallmentAmount()
                    : installmentAmount.add(feePerInstallment)
                        .add(totalAmount.add(feeAmount)
                                .subtract(installmentAmount.add(feePerInstallment)
                                        .multiply(BigDecimal.valueOf(count))));
            // For the last installment, adjust for rounding
            if (i == count) {
                BigDecimal sumPrevious = plan.getInstallmentAmount()
                        .multiply(BigDecimal.valueOf(count - 1));
                entryAmount = totalWithFees.subtract(sumPrevious)
                        .setScale(3, RoundingMode.HALF_UP);
            }

            InstallmentEntry entry = InstallmentEntry.builder()
                    .installmentPlanId(planId)
                    .sequenceNumber(i)
                    .dueDate(dueDate)
                    .amount(entryAmount)
                    .paid(false)
                    .build();
            entries.add(entry);
        }

        installmentEntryRepository.saveAll(entries);

        log.info("Installment plan created: id={}, line={}, amount={}, count={}, fee={}",
                planId, creditLineId, totalAmount, count, feeAmount);
        return plan;
    }

    @Transactional
    public InstallmentEntry markEntryPaid(UUID entryId, UUID statementId) {
        InstallmentEntry entry = installmentEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Installment entry not found: " + entryId));
        UUID planId = entry.getInstallmentPlanId();
        entry.setPaid(true);
        entry.setStatementId(statementId);
        entry = installmentEntryRepository.save(entry);

        InstallmentPlan plan = installmentPlanRepository.findById(planId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Installment plan not found: " + planId));
        plan.setRemainingCount(plan.getRemainingCount() - 1);
        if (plan.getRemainingCount() <= 0) {
            plan.setStatus(InstallmentPlan.InstallmentStatus.COMPLETED);
        }
        installmentPlanRepository.save(plan);

        return entry;
    }

    public List<InstallmentPlan> getPlans(UUID creditLineId) {
        return installmentPlanRepository.findByCreditLineId(creditLineId);
    }

    public Optional<InstallmentPlan> getPlan(UUID planId) {
        return installmentPlanRepository.findById(planId);
    }

    public List<InstallmentEntry> getEntries(UUID planId) {
        return installmentEntryRepository.findByInstallmentPlanIdOrderBySequenceNumberAsc(planId);
    }
}
