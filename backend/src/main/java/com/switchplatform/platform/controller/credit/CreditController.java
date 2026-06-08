package com.switchplatform.platform.controller.credit;

import com.switchplatform.platform.model.credit.*;
import com.switchplatform.platform.service.credit.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credit")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CreditController {

    private final CreditLineService creditLineService;
    private final StatementService statementService;
    private final InstallmentService installmentService;

    // ─── Credit Lines ───────────────────────────────────────────────────────

    @PostMapping("/lines")
    public ResponseEntity<CreditLine> openCreditLine(@Valid @RequestBody OpenLineRequest req) {
        CreditLine line = creditLineService.openCreditLine(
                req.cardAccountId, req.creditLimit, req.apr,
                req.statementDay, req.paymentDueDays,
                req.minPaymentPct, req.minPaymentFloor);
        return ResponseEntity.ok(line);
    }

    @GetMapping("/lines")
    public ResponseEntity<Map<String, Object>> listAll(Pageable pageable) {
        List<CreditLine> lines = creditLineService.findAll(pageable);
        return ResponseEntity.ok(Map.of("content", lines));
    }

    @GetMapping("/lines/{id}")
    public ResponseEntity<CreditLine> getCreditLine(@PathVariable UUID id) {
        return creditLineService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/lines/by-card-account/{cardAccountId}")
    public ResponseEntity<CreditLine> getByCardAccount(@PathVariable UUID cardAccountId) {
        return creditLineService.findByCardAccountId(cardAccountId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/lines/{id}/authorize")
    public ResponseEntity<CreditLine> authorize(@PathVariable UUID id,
                                                 @Valid @RequestBody AmountRequest req) {
        return ResponseEntity.ok(creditLineService.authorize(id, req.amount));
    }

    @PostMapping("/lines/{id}/purchase")
    public ResponseEntity<CreditLine> postPurchase(@PathVariable UUID id,
                                                    @Valid @RequestBody PurchaseRequest req) {
        return ResponseEntity.ok(creditLineService.postPurchase(id, req.amount, req.transactionRef));
    }

    @PostMapping("/lines/{id}/payment")
    public ResponseEntity<CreditLine> postPayment(@PathVariable UUID id,
                                                   @Valid @RequestBody AmountRequest req) {
        return ResponseEntity.ok(creditLineService.postPayment(id, req.amount));
    }

    @PostMapping("/lines/{id}/release-hold")
    public ResponseEntity<CreditLine> releaseHold(@PathVariable UUID id,
                                                   @Valid @RequestBody ReleaseHoldRequest req) {
        BigDecimal amount = req.amount != null ? req.amount : BigDecimal.ZERO;
        return ResponseEntity.ok(creditLineService.releaseHold(id, amount));
    }

    @PostMapping("/lines/{id}/simulate")
    public ResponseEntity<SimulationResult> simulate(@PathVariable UUID id,
                                                      @Valid @RequestBody SimulateRequest req) {
        BigDecimal monthlyRate = req.apr
                .divide(BigDecimal.valueOf(12), 10, java.math.RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal interest = req.balance.multiply(monthlyRate)
                .setScale(3, java.math.RoundingMode.HALF_UP);

        BigDecimal minPct = req.balance
                .multiply(req.minPaymentPct)
                .divide(BigDecimal.valueOf(100), 3, java.math.RoundingMode.HALF_UP);
        BigDecimal minPayment = minPct.max(req.minPaymentFloor);

        SimulationResult result = new SimulationResult();
        result.monthlyRate = monthlyRate.setScale(4, java.math.RoundingMode.HALF_UP);
        result.interestCharged = interest;
        result.minimumPayment = minPayment;
        result.newBalance = req.balance.add(interest);

        return ResponseEntity.ok(result);
    }

    // ─── Statements ─────────────────────────────────────────────────────────

    @GetMapping("/lines/{id}/statements")
    public ResponseEntity<List<CreditStatement>> getStatements(@PathVariable UUID id) {
        return ResponseEntity.ok(statementService.getStatements(id));
    }

    @GetMapping("/statements/{id}")
    public ResponseEntity<CreditStatement> getStatement(@PathVariable UUID id) {
        return statementService.getStatement(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/lines/{id}/generate-statement")
    public ResponseEntity<CreditStatement> generateStatement(@PathVariable UUID id) {
        return ResponseEntity.ok(statementService.generateStatement(id));
    }

    // ─── Installments ───────────────────────────────────────────────────────

    @PostMapping("/lines/{id}/installments")
    public ResponseEntity<InstallmentPlan> convertToInstallments(
            @PathVariable UUID id, @Valid @RequestBody InstallmentRequest req) {
        InstallmentPlan plan = installmentService.convertToInstallments(
                id, req.transactionRef, req.totalAmount,
                req.count, req.feeAmount, req.apr);
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/lines/{id}/installments")
    public ResponseEntity<List<InstallmentPlan>> getInstallmentPlans(@PathVariable UUID id) {
        return ResponseEntity.ok(installmentService.getPlans(id));
    }

    @GetMapping("/installment-plans/{id}")
    public ResponseEntity<InstallmentPlan> getInstallmentPlan(@PathVariable UUID id) {
        return installmentService.getPlan(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/installment-plans/{id}/entries")
    public ResponseEntity<List<InstallmentEntry>> getInstallmentEntries(@PathVariable UUID id) {
        return ResponseEntity.ok(installmentService.getEntries(id));
    }

    @PostMapping("/installment-entries/{id}/pay")
    public ResponseEntity<InstallmentEntry> markEntryPaid(
            @PathVariable UUID id, @Valid @RequestBody Map<String, UUID> body) {
        UUID statementId = body.get("statementId");
        return ResponseEntity.ok(installmentService.markEntryPaid(id, statementId));
    }

    // ─── Request DTOs ───────────────────────────────────────────────────────

    public record OpenLineRequest(
            @NotNull UUID cardAccountId,
            @NotNull @DecimalMin("0.01") BigDecimal creditLimit,
            @NotNull @DecimalMin("0.01") BigDecimal apr,
            @Min(1) @Max(28) int statementDay,
            @Min(1) int paymentDueDays,
            @NotNull @DecimalMin("0.01") BigDecimal minPaymentPct,
            @NotNull @DecimalMin("0.01") BigDecimal minPaymentFloor
    ) {}

    public record AmountRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) {}

    public record PurchaseRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String transactionRef
    ) {}

    public record ReleaseHoldRequest(BigDecimal amount) {}

    public record SimulateRequest(
            @NotNull @DecimalMin("0") BigDecimal balance,
            @NotNull @DecimalMin("0.01") BigDecimal apr,
            @NotNull @DecimalMin("0.01") BigDecimal minPaymentPct,
            @NotNull @DecimalMin("0.01") BigDecimal minPaymentFloor
    ) {}

    public record InstallmentRequest(
            String transactionRef,
            @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
            @Min(1) int count,
            @NotNull BigDecimal feeAmount,
            BigDecimal apr
    ) {}

    public static class SimulationResult {
        public BigDecimal monthlyRate;
        public BigDecimal interestCharged;
        public BigDecimal minimumPayment;
        public BigDecimal newBalance;
    }
}
