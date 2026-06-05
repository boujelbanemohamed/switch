package com.switchplatform.platform.service.credit;

import com.switchplatform.platform.model.credit.InstallmentEntry;
import com.switchplatform.platform.model.credit.InstallmentPlan;
import com.switchplatform.platform.repository.credit.InstallmentEntryRepository;
import com.switchplatform.platform.repository.credit.InstallmentPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentServiceTest {

    @Mock private InstallmentPlanRepository installmentPlanRepository;
    @Mock private InstallmentEntryRepository installmentEntryRepository;

    private InstallmentService service;

    @BeforeEach
    void setUp() {
        service = new InstallmentService(installmentPlanRepository, installmentEntryRepository);
    }

    @Test
    void convertToInstallments_1200in12_shouldCreate12EntriesOf100() {
        UUID creditLineId = UUID.randomUUID();
        InstallmentPlan savedPlan = InstallmentPlan.builder()
                .id(UUID.randomUUID())
                .creditLineId(creditLineId)
                .installmentCount(12)
                .installmentAmount(BigDecimal.valueOf(100.000))
                .totalAmount(BigDecimal.valueOf(1200.000))
                .build();

        when(installmentPlanRepository.save(any())).thenReturn(savedPlan);
        when(installmentEntryRepository.saveAll(any())).thenReturn(List.of());

        service.convertToInstallments(creditLineId, "TXN-REF", BigDecimal.valueOf(1200), 12,
                BigDecimal.ZERO, new BigDecimal("18.00"));

        ArgumentCaptor<List<InstallmentEntry>> captor = ArgumentCaptor.captor();
        verify(installmentEntryRepository).saveAll(captor.capture());
        List<InstallmentEntry> entries = captor.getValue();

        assertEquals(12, entries.size(), "Should create 12 installment entries");

        BigDecimal sumAmounts = entries.stream()
                .map(InstallmentEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(BigDecimal.valueOf(1200.000).setScale(3), sumAmounts,
                "Sum of all installment amounts should equal the total (1200)");

        for (int i = 0; i < 12; i++) {
            assertEquals(i + 1, entries.get(i).getSequenceNumber());
        }
    }

    @Test
    void convertToInstallments_withFees_shouldIncludeFeesInTotal() {
        UUID creditLineId = UUID.randomUUID();
        InstallmentPlan savedPlan = InstallmentPlan.builder()
                .id(UUID.randomUUID())
                .creditLineId(creditLineId)
                .installmentCount(6)
                .installmentAmount(BigDecimal.valueOf(220.000))
                .totalAmount(BigDecimal.valueOf(1320.000))
                .feeAmount(BigDecimal.valueOf(120))
                .build();

        when(installmentPlanRepository.save(any())).thenReturn(savedPlan);
        when(installmentEntryRepository.saveAll(any())).thenReturn(List.of());

        service.convertToInstallments(creditLineId, "TXN-FEE", BigDecimal.valueOf(1200), 6,
                BigDecimal.valueOf(120), new BigDecimal("18.00"));

        ArgumentCaptor<List<InstallmentEntry>> captor = ArgumentCaptor.captor();
        verify(installmentEntryRepository).saveAll(captor.capture());
        List<InstallmentEntry> entries = captor.getValue();

        assertEquals(6, entries.size());

        // Total with fees = 1200 + 120 = 1320
        BigDecimal sumAmounts = entries.stream()
                .map(InstallmentEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(BigDecimal.valueOf(1320.000).setScale(3), sumAmounts,
                "Sum should include fee (1320)");
    }

    @Test
    void convertToInstallments_withZeroCount_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> service.convertToInstallments(UUID.randomUUID(), "TXN", BigDecimal.valueOf(100), 0,
                        BigDecimal.ZERO, null));
    }

    @Test
    void convertToInstallments_withNegativeAmount_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> service.convertToInstallments(UUID.randomUUID(), "TXN", BigDecimal.valueOf(-1), 3,
                        BigDecimal.ZERO, null));
    }

    @Test
    void markEntryPaid_shouldCompletePlanWhenAllPaid() {
        UUID planId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        InstallmentEntry entry = InstallmentEntry.builder()
                .id(entryId)
                .installmentPlanId(planId)
                .sequenceNumber(1)
                .amount(BigDecimal.valueOf(100))
                .paid(false)
                .build();

        InstallmentPlan plan = InstallmentPlan.builder()
                .id(planId)
                .remainingCount(1)
                .status(InstallmentPlan.InstallmentStatus.ACTIVE)
                .build();

        when(installmentEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(installmentEntryRepository.save(any())).thenReturn(entry);
        when(installmentPlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(installmentPlanRepository.save(any())).thenReturn(plan);

        UUID statementId = UUID.randomUUID();
        InstallmentEntry result = service.markEntryPaid(entryId, statementId);

        assertTrue(result.getPaid(), "Entry should be marked paid");
        assertEquals(statementId, result.getStatementId());
        assertEquals(0, plan.getRemainingCount().intValue());
        assertEquals(InstallmentPlan.InstallmentStatus.COMPLETED, plan.getStatus());
    }
}
