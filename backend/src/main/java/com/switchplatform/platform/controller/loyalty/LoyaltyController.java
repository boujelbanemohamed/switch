package com.switchplatform.platform.controller.loyalty;

import com.switchplatform.platform.model.loyalty.*;
import com.switchplatform.platform.service.loyalty.LoyaltyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
@Slf4j
@Validated
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    // ─── Programs ────────────────────────────────────────────────────────────

    @GetMapping("/programs")
    public ResponseEntity<List<LoyaltyProgram>> listPrograms() {
        return ResponseEntity.ok(loyaltyService.listPrograms());
    }

    @GetMapping("/programs/{id}")
    public ResponseEntity<LoyaltyProgram> getProgram(@PathVariable UUID id) {
        return loyaltyService.getProgram(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/programs")
    public ResponseEntity<LoyaltyProgram> createProgram(@Valid @RequestBody CreateProgramRequest req) {
        return ResponseEntity.ok(loyaltyService.createProgram(
                req.name, req.description, req.earningRate, req.currency));
    }

    @PostMapping("/programs/{id}/toggle")
    public ResponseEntity<Void> toggleProgram(@PathVariable UUID id) {
        loyaltyService.toggleProgramStatus(id);
        return ResponseEntity.ok().build();
    }

    // ─── Tiers ───────────────────────────────────────────────────────────────

    @GetMapping("/programs/{id}/tiers")
    public ResponseEntity<List<LoyaltyTier>> listTiers(@PathVariable UUID id) {
        return ResponseEntity.ok(loyaltyService.listTiers(id));
    }

    @PostMapping("/programs/{id}/tiers")
    public ResponseEntity<LoyaltyTier> createTier(@PathVariable UUID id,
                                                   @Valid @RequestBody CreateTierRequest req) {
        return ResponseEntity.ok(loyaltyService.createTier(
                id, req.name, req.minLifetimePoints, req.earningMultiplier, req.benefits));
    }

    // ─── Memberships ─────────────────────────────────────────────────────────

    @GetMapping("/memberships")
    public ResponseEntity<List<LoyaltyMembership>> listAllMemberships() {
        return ResponseEntity.ok(loyaltyService.listAllMemberships());
    }

    @PostMapping("/enroll")
    public ResponseEntity<LoyaltyMembership> enroll(@Valid @RequestBody EnrollRequest req) {
        return ResponseEntity.ok(loyaltyService.enroll(req.cardholderId, req.programId));
    }

    @GetMapping("/cardholders/{cardholderId}/memberships")
    public ResponseEntity<List<LoyaltyMembership>> listMemberships(@PathVariable UUID cardholderId) {
        return ResponseEntity.ok(loyaltyService.listMemberships(cardholderId));
    }

    @GetMapping("/cardholders/{cardholderId}/memberships/{programId}")
    public ResponseEntity<LoyaltyMembership> getMembership(
            @PathVariable UUID cardholderId, @PathVariable UUID programId) {
        return loyaltyService.getMembership(cardholderId, programId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/memberships/{id}/suspend")
    public ResponseEntity<Void> suspendMembership(@PathVariable UUID id) {
        loyaltyService.suspendMembership(id);
        return ResponseEntity.ok().build();
    }

    // ─── Points ──────────────────────────────────────────────────────────────

    @PostMapping("/memberships/{id}/earn")
    public ResponseEntity<LoyaltyTransaction> earnPoints(
            @PathVariable UUID id, @Valid @RequestBody EarnRequest req) {
        return ResponseEntity.ok(loyaltyService.earnPoints(
                id, req.amount, req.transactionRef, req.description));
    }

    @PostMapping("/memberships/{id}/burn")
    public ResponseEntity<LoyaltyTransaction> burnPoints(
            @PathVariable UUID id, @Valid @RequestBody BurnRequest req) {
        return ResponseEntity.ok(loyaltyService.burnPoints(id, req.points, req.description));
    }

    @GetMapping("/memberships/{id}/transactions")
    public ResponseEntity<List<LoyaltyTransaction>> getTransactions(@PathVariable UUID id) {
        return ResponseEntity.ok(loyaltyService.getTransactionHistory(id));
    }

    // ─── Rewards ─────────────────────────────────────────────────────────────

    @GetMapping("/programs/{id}/rewards")
    public ResponseEntity<List<LoyaltyReward>> listRewards(@PathVariable UUID id) {
        return ResponseEntity.ok(loyaltyService.listRewards(id));
    }

    @PostMapping("/programs/{id}/rewards")
    public ResponseEntity<LoyaltyReward> createReward(@PathVariable UUID id,
                                                       @Valid @RequestBody CreateRewardRequest req) {
        return ResponseEntity.ok(loyaltyService.createReward(
                id, req.name, req.description, req.pointsCost, req.stock));
    }

    // ─── Redemptions ─────────────────────────────────────────────────────────

    @PostMapping("/memberships/{id}/redeem-reward/{rewardId}")
    public ResponseEntity<LoyaltyRedemption> redeemReward(
            @PathVariable UUID id, @PathVariable UUID rewardId) {
        return ResponseEntity.ok(loyaltyService.redeemReward(id, rewardId));
    }

    @PostMapping("/memberships/{id}/redeem-credit")
    public ResponseEntity<LoyaltyRedemption> redeemBalanceCredit(
            @PathVariable UUID id, @Valid @RequestBody RedeemCreditRequest req) {
        return ResponseEntity.ok(loyaltyService.redeemBalanceCredit(id, req.points));
    }

    @GetMapping("/memberships/{id}/redemptions")
    public ResponseEntity<List<LoyaltyRedemption>> getRedemptions(@PathVariable UUID id) {
        return ResponseEntity.ok(loyaltyService.getRedemptionHistory(id));
    }

    // ─── Request DTOs ────────────────────────────────────────────────────────

    public record CreateProgramRequest(
            @NotBlank String name,
            String description,
            @NotNull @DecimalMin("0.0001") BigDecimal earningRate,
            String currency
    ) {}

    public record CreateTierRequest(
            @NotBlank String name,
            @NotNull @DecimalMin("0") BigDecimal minLifetimePoints,
            @NotNull @DecimalMin("0.01") BigDecimal earningMultiplier,
            String benefits
    ) {}

    public record EnrollRequest(
            @NotNull UUID cardholderId,
            @NotNull UUID programId
    ) {}

    public record EarnRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String transactionRef,
            String description
    ) {}

    public record BurnRequest(
            @NotNull @DecimalMin("0.01") BigDecimal points,
            String description
    ) {}

    public record CreateRewardRequest(
            @NotBlank String name,
            String description,
            @NotNull @DecimalMin("0.01") BigDecimal pointsCost,
            Integer stock
    ) {}

    public record RedeemCreditRequest(
            @NotNull @DecimalMin("0.01") BigDecimal points
    ) {}
}
