package com.switchplatform.platform.service.credit;

import com.switchplatform.platform.model.credit.CreditLine;
import com.switchplatform.platform.model.credit.CreditStatement;
import com.switchplatform.platform.repository.credit.CreditLineRepository;
import com.switchplatform.platform.repository.credit.CreditStatementRepository;
import com.switchplatform.platform.repository.credit.InstallmentEntryRepository;
import com.switchplatform.platform.repository.credit.InstallmentPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock private CreditLineRepository creditLineRepository;
    @Mock private CreditStatementRepository creditStatementRepository;
    @Mock private InstallmentPlanRepository installmentPlanRepository;
    @Mock private InstallmentEntryRepository installmentEntryRepository;

    private StatementService service;
    private UUID lineId;

    @BeforeEach
    void setUp() {
        service = new StatementService(creditLineRepository, creditStatementRepository,
                installmentPlanRepository, installmentEntryRepository);
        lineId = UUID.randomUUID();
    }

    @Test
    void generateStatement_withUnpaidPrevious_shouldChargeInterestAndCalculateMinPayment() {
        CreditLine line = CreditLine.builder()
                .id(lineId)
                .creditLimit(BigDecimal.valueOf(5000))
                .currentBalance(BigDecimal.valueOf(1000))
                .apr(new BigDecimal("18.00"))
                .statementDay(1)
                .paymentDueDays(20)
                .minPaymentPct(new BigDecimal("5.00"))
                .minPaymentFloor(new BigDecimal("10.000"))
                .currencyCode("TND")
                .status(CreditLine.CreditLineStatus.ACTIVE)
                .build();

        // Previous statement: closing = 1000, NOT paid in full
        CreditStatement previous = CreditStatement.builder()
                .id(UUID.randomUUID())
                .creditLineId(lineId)
                .openingBalance(BigDecimal.ZERO)
                .closingBalance(BigDecimal.valueOf(1000))
                .paidInFull(false)
                .build();

        when(creditLineRepository.findById(lineId)).thenReturn(Optional.of(line));
        when(creditStatementRepository.findTopByCreditLineIdOrderByStatementDateDesc(lineId))
                .thenReturn(Optional.of(previous));
        when(installmentPlanRepository.findByCreditLineIdAndStatus(any(), any()))
                .thenReturn(List.of());
        when(creditStatementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreditStatement statement = service.generateStatement(lineId);

        // Opening = 1000, purchases = max(1000-1000,0) = 0, payments = max(1000-1000,0) = 0
        // Interest = 1000 × (18/12/100) = 1000 × 0.015 = 15.000
        assertEquals(0, BigDecimal.valueOf(15.000).compareTo(statement.getInterestCharged()),
                "Interest should be 15 TND for 1000 balance at 18% APR");

        // Closing = 1000 + 0 - 0 + 15 + 0 = 1015
        assertEquals(0, BigDecimal.valueOf(1015.000).compareTo(statement.getClosingBalance()),
                "Closing balance should be 1015");

        // Min payment = max(1015 × 5%, 10) = max(50.750, 10) = 50.750
        assertEquals(0, BigDecimal.valueOf(50.750).compareTo(statement.getMinimumPayment()),
                "Minimum payment should be 50.750");
    }

    @Test
    void generateStatement_whenPreviousPaidInFull_shouldNotChargeInterest() {
        CreditLine line = CreditLine.builder()
                .id(lineId)
                .currentBalance(BigDecimal.valueOf(1000))
                .apr(new BigDecimal("18.00"))
                .minPaymentPct(new BigDecimal("5.00"))
                .minPaymentFloor(BigDecimal.TEN)
                .paymentDueDays(20)
                .currencyCode("TND")
                .status(CreditLine.CreditLineStatus.ACTIVE)
                .build();

        CreditStatement previous = CreditStatement.builder()
                .id(UUID.randomUUID())
                .creditLineId(lineId)
                .closingBalance(BigDecimal.valueOf(1000))
                .paidInFull(true)  // ← grace period applies
                .build();

        when(creditLineRepository.findById(lineId)).thenReturn(Optional.of(line));
        when(creditStatementRepository.findTopByCreditLineIdOrderByStatementDateDesc(lineId))
                .thenReturn(Optional.of(previous));
        when(installmentPlanRepository.findByCreditLineIdAndStatus(any(), any()))
                .thenReturn(List.of());
        when(creditStatementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreditStatement statement = service.generateStatement(lineId);

        assertEquals(BigDecimal.ZERO, statement.getInterestCharged(),
                "Interest should be zero when previous statement was paid in full");
    }

    @Test
    void generateStatement_withSmallBalance_shouldUseFloorMinPayment() {
        // Balance = 0 (no previous statement), current = 50
        // → purchases = 50, closing = 50, minPayment = max(50×5%,10) = max(2.5,10) = 10
        CreditLine line = CreditLine.builder()
                .id(lineId)
                .currentBalance(BigDecimal.valueOf(50))
                .apr(new BigDecimal("18.00"))
                .minPaymentPct(new BigDecimal("5.00"))
                .minPaymentFloor(new BigDecimal("10.000"))
                .paymentDueDays(20)
                .currencyCode("TND")
                .status(CreditLine.CreditLineStatus.ACTIVE)
                .build();

        when(creditLineRepository.findById(lineId)).thenReturn(Optional.of(line));
        when(creditStatementRepository.findTopByCreditLineIdOrderByStatementDateDesc(lineId))
                .thenReturn(Optional.empty());  // no previous statement
        when(installmentPlanRepository.findByCreditLineIdAndStatus(any(), any()))
                .thenReturn(List.of());
        when(creditStatementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreditStatement statement = service.generateStatement(lineId);

        // opening = 0, purchases = max(50-0,0) = 50, payments = 0, interest = 0
        // closing = 50, minPayment = max(50×5%,10) = 10
        assertEquals(0, BigDecimal.valueOf(10).compareTo(statement.getMinimumPayment()),
                "Floor min payment should apply when pct-based is below floor");
        assertEquals(0, BigDecimal.valueOf(50).compareTo(statement.getClosingBalance()));
    }

    @Test
    void generateStatement_withLargeBalance_shouldUsePctMinPayment() {
        // opening = 0, current = 500
        // → purchases = 500, closing = 500, min = max(500×5%,10) = max(25,10) = 25
        CreditLine line = CreditLine.builder()
                .id(lineId)
                .currentBalance(BigDecimal.valueOf(500))
                .apr(new BigDecimal("18.00"))
                .minPaymentPct(new BigDecimal("5.00"))
                .minPaymentFloor(new BigDecimal("10.000"))
                .paymentDueDays(20)
                .currencyCode("TND")
                .status(CreditLine.CreditLineStatus.ACTIVE)
                .build();

        when(creditLineRepository.findById(lineId)).thenReturn(Optional.of(line));
        when(creditStatementRepository.findTopByCreditLineIdOrderByStatementDateDesc(lineId))
                .thenReturn(Optional.empty());
        when(installmentPlanRepository.findByCreditLineIdAndStatus(any(), any()))
                .thenReturn(List.of());
        when(creditStatementRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreditStatement statement = service.generateStatement(lineId);

        assertEquals(0, BigDecimal.valueOf(25).compareTo(statement.getMinimumPayment()),
                "Pct-based min payment should be used when it exceeds the floor");
    }
}
