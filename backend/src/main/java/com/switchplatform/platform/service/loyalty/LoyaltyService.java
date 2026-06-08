package com.switchplatform.platform.service.loyalty;

import com.switchplatform.platform.model.ledger.JournalEntry;
import com.switchplatform.platform.model.ledger.LedgerAccount;
import com.switchplatform.platform.model.ledger.LedgerEntry;
import com.switchplatform.platform.model.loyalty.*;
import com.switchplatform.platform.repository.ledger.JournalEntryRepository;
import com.switchplatform.platform.repository.ledger.LedgerAccountRepository;
import com.switchplatform.platform.repository.ledger.LedgerEntryRepository;
import com.switchplatform.platform.repository.loyalty.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private static final String POINTS_LIABILITY = "LOYALTY_POINTS_LIABILITY";
    private static final String LOYALTY_EXPENSE = "LOYALTY_EXPENSE";

    private final LoyaltyProgramRepository programRepository;
    private final LoyaltyTierRepository tierRepository;
    private final LoyaltyMembershipRepository membershipRepository;
    private final LoyaltyTransactionRepository txRepository;
    private final LoyaltyRewardRepository rewardRepository;
    private final LoyaltyRedemptionRepository redemptionRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    // ---------------------------------------------------------------
    // Programs
    // ---------------------------------------------------------------

    public List<LoyaltyProgram> listPrograms() {
        return programRepository.findAll();
    }

    public Optional<LoyaltyProgram> getProgram(UUID id) {
        return programRepository.findById(id);
    }

    @Transactional
    public LoyaltyProgram createProgram(String name, String description, BigDecimal earningRate, String currency) {
        LoyaltyProgram program = LoyaltyProgram.builder()
                .name(name)
                .description(description)
                .earningRate(earningRate)
                .currency(currency != null ? currency : "TND")
                .build();
        program = programRepository.save(program);
        log.info("Loyalty program created: id={}, name={}", program.getId(), name);
        return program;
    }

    @Transactional
    public void toggleProgramStatus(UUID id) {
        LoyaltyProgram program = programRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Program not found: " + id));
        program.setStatus(program.getStatus() == LoyaltyProgram.ProgramStatus.ACTIVE
                ? LoyaltyProgram.ProgramStatus.INACTIVE
                : LoyaltyProgram.ProgramStatus.ACTIVE);
        programRepository.save(program);
    }

    // ---------------------------------------------------------------
    // Tiers
    // ---------------------------------------------------------------

    public List<LoyaltyTier> listTiers(UUID programId) {
        return tierRepository.findByProgramIdOrderByMinLifetimePointsAsc(programId);
    }

    @Transactional
    public LoyaltyTier createTier(UUID programId, String name, BigDecimal minLifetimePoints,
                                  BigDecimal earningMultiplier, String benefits) {
        LoyaltyTier tier = LoyaltyTier.builder()
                .programId(programId)
                .name(name)
                .minLifetimePoints(minLifetimePoints)
                .earningMultiplier(earningMultiplier)
                .benefits(benefits)
                .build();
        tier = tierRepository.save(tier);
        log.info("Loyalty tier created: id={}, program={}, name={}", tier.getId(), programId, name);
        return tier;
    }

    // ---------------------------------------------------------------
    // Memberships
    // ---------------------------------------------------------------

    public Optional<LoyaltyMembership> getMembership(UUID cardholderId, UUID programId) {
        return membershipRepository.findByCardholderIdAndProgramId(cardholderId, programId);
    }

    public List<LoyaltyMembership> listMemberships(UUID cardholderId) {
        return membershipRepository.findByCardholderId(cardholderId);
    }

    public List<LoyaltyMembership> listAllMemberships() {
        return membershipRepository.findAll();
    }

    @Transactional
    public LoyaltyMembership enroll(UUID cardholderId, UUID programId) {
        membershipRepository.findByCardholderIdAndProgramId(cardholderId, programId)
                .ifPresent(m -> { throw new IllegalStateException("Cardholder already enrolled in program"); });

        List<LoyaltyTier> tiers = tierRepository.findByProgramIdOrderByMinLifetimePointsAsc(programId);
        if (tiers.isEmpty()) throw new IllegalStateException("Program has no tiers");

        LoyaltyTier defaultTier = tiers.get(0);

        LoyaltyMembership membership = LoyaltyMembership.builder()
                .cardholderId(cardholderId)
                .programId(programId)
                .tierId(defaultTier.getId())
                .pointsBalance(BigDecimal.ZERO)
                .lifetimePoints(BigDecimal.ZERO)
                .build();
        membership = membershipRepository.save(membership);
        log.info("Cardholder enrolled: cardholder={}, program={}, tier={}", cardholderId, programId, defaultTier.getId());
        return membership;
    }

    @Transactional
    public void suspendMembership(UUID membershipId) {
        LoyaltyMembership m = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));
        m.setStatus(LoyaltyMembership.MembershipStatus.SUSPENDED);
        membershipRepository.save(m);
    }

    // ---------------------------------------------------------------
    // Points (FIFO burn + expiry + ledger integration)
    // ---------------------------------------------------------------

    @Transactional
    public LoyaltyTransaction earnPoints(UUID membershipId, BigDecimal purchaseAmount,
                                         String transactionRef, String description) {
        LoyaltyMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));

        if (membership.getStatus() != LoyaltyMembership.MembershipStatus.ACTIVE) {
            throw new IllegalStateException("Membership is not active");
        }

        LoyaltyProgram program = programRepository.findById(membership.getProgramId())
                .orElseThrow(() -> new IllegalStateException("Program not found"));

        LoyaltyTier tier = tierRepository.findById(membership.getTierId())
                .orElseThrow(() -> new IllegalStateException("Tier not found"));

        BigDecimal basePoints = purchaseAmount.multiply(program.getEarningRate());
        BigDecimal multiplier = tier.getEarningMultiplier();
        BigDecimal points = basePoints.multiply(multiplier).setScale(3, RoundingMode.HALF_UP);

        membership.setPointsBalance(membership.getPointsBalance().add(points));
        membership.setLifetimePoints(membership.getLifetimePoints().add(points));
        updateTier(membership);
        membershipRepository.save(membership);

        OffsetDateTime expiresAt = OffsetDateTime.now().plusMonths(12);

        LoyaltyTransaction tx = LoyaltyTransaction.builder()
                .membershipId(membershipId)
                .type(LoyaltyTransaction.TransactionType.EARN)
                .points(points)
                .remaining(points)
                .expiresAt(expiresAt)
                .transactionRef(transactionRef)
                .description(description)
                .build();
        tx = txRepository.save(tx);

        postLiabilityEntry("LOYALTY-EARN-" + tx.getId().toString().substring(0, 8),
                points, program, "Points earned: " + description);

        log.info("Points earned: membership={}, amount={}, points={}, ref={}", membershipId, purchaseAmount, points, transactionRef);
        return tx;
    }

    @Transactional
    public LoyaltyTransaction burnPoints(UUID membershipId, BigDecimal points, String description) {
        LoyaltyMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));

        if (membership.getPointsBalance().compareTo(points) < 0) {
            throw new IllegalStateException("Insufficient points: balance="
                    + membership.getPointsBalance() + ", requested=" + points);
        }

        fifoBurn(membership, points);

        LoyaltyTransaction tx = LoyaltyTransaction.builder()
                .membershipId(membershipId)
                .type(LoyaltyTransaction.TransactionType.BURN)
                .points(points.negate())
                .description(description)
                .build();
        tx = txRepository.save(tx);

        postLiabilityEntry("LOYALTY-BURN-" + tx.getId().toString().substring(0, 8),
                points.negate(), null, "Points burned: " + description);

        log.info("Points burned: membership={}, points={}", membershipId, points);
        return tx;
    }

    public List<LoyaltyTransaction> getTransactionHistory(UUID membershipId) {
        return txRepository.findByMembershipIdOrderByCreatedAtDesc(membershipId);
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void expirePoints() {
        log.info("Starting loyalty points expiry check");
        OffsetDateTime now = OffsetDateTime.now();
        List<LoyaltyTransaction> expired = txRepository.findExpiredEarnBatches(now);
        BigDecimal totalExpired = BigDecimal.ZERO;

        for (LoyaltyTransaction batch : expired) {
            BigDecimal expireAmount = batch.getRemaining();
            if (expireAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

            LoyaltyMembership membership = membershipRepository.findById(batch.getMembershipId()).orElse(null);
            if (membership == null) continue;

            BigDecimal newBalance = membership.getPointsBalance().subtract(expireAmount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) newBalance = BigDecimal.ZERO;
            membership.setPointsBalance(newBalance);
            membershipRepository.save(membership);

            batch.setRemaining(BigDecimal.ZERO);
            txRepository.save(batch);

            txRepository.save(LoyaltyTransaction.builder()
                    .membershipId(membership.getId())
                    .type(LoyaltyTransaction.TransactionType.EXPIRE)
                    .points(expireAmount.negate())
                    .description("Points expired from batch " + batch.getId().toString().substring(0, 8))
                    .build());

            postLiabilityEntry("LOYALTY-EXP-" + batch.getId().toString().substring(0, 8),
                    expireAmount.negate(), null, "Points expired");

            totalExpired = totalExpired.add(expireAmount);
        }

        if (totalExpired.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Points expiry complete: {} points expired across {} batches", totalExpired, expired.size());
        }
    }

    // ---------------------------------------------------------------
    // Rewards
    // ---------------------------------------------------------------

    public List<LoyaltyReward> listRewards(UUID programId) {
        return rewardRepository.findByProgramId(programId);
    }

    @Transactional
    public LoyaltyReward createReward(UUID programId, String name, String description,
                                      BigDecimal pointsCost, Integer stock) {
        LoyaltyReward reward = LoyaltyReward.builder()
                .programId(programId)
                .name(name)
                .description(description)
                .pointsCost(pointsCost)
                .stock(stock)
                .build();
        reward = rewardRepository.save(reward);
        log.info("Reward created: id={}, program={}, name={}, cost={}", reward.getId(), programId, name, pointsCost);
        return reward;
    }

    // ---------------------------------------------------------------
    // Redemptions (FIFO burn + ledger)
    // ---------------------------------------------------------------

    @Transactional
    public LoyaltyRedemption redeemReward(UUID membershipId, UUID rewardId) {
        LoyaltyMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));

        LoyaltyReward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found: " + rewardId));

        if (reward.getStatus() != LoyaltyReward.RewardStatus.ACTIVE) {
            throw new IllegalStateException("Reward is not available");
        }

        if (reward.getStock() != null && reward.getStock() <= 0) {
            throw new IllegalStateException("Reward out of stock");
        }

        if (membership.getPointsBalance().compareTo(reward.getPointsCost()) < 0) {
            throw new IllegalStateException("Insufficient points: balance="
                    + membership.getPointsBalance() + ", cost=" + reward.getPointsCost());
        }

        fifoBurn(membership, reward.getPointsCost());

        if (reward.getStock() != null) {
            reward.setStock(reward.getStock() - 1);
            if (reward.getStock() <= 0) {
                reward.setStatus(LoyaltyReward.RewardStatus.OUT_OF_STOCK);
            }
            rewardRepository.save(reward);
        }

        LoyaltyRedemption redemption = LoyaltyRedemption.builder()
                .membershipId(membershipId)
                .rewardId(rewardId)
                .pointsSpent(reward.getPointsCost())
                .status(LoyaltyRedemption.RedemptionStatus.COMPLETED)
                .build();
        redemption = redemptionRepository.save(redemption);

        LoyaltyTransaction tx = txRepository.save(LoyaltyTransaction.builder()
                .membershipId(membershipId)
                .type(LoyaltyTransaction.TransactionType.BURN)
                .points(reward.getPointsCost().negate())
                .description("Reward: " + reward.getName())
                .build());

        postLiabilityEntry("LOYALTY-RDM-" + tx.getId().toString().substring(0, 8),
                reward.getPointsCost().negate(), null,
                "Reward redemption: " + reward.getName());

        log.info("Reward redeemed: membership={}, reward={}, points={}", membershipId, rewardId, reward.getPointsCost());
        return redemption;
    }

    @Transactional
    public LoyaltyRedemption redeemBalanceCredit(UUID membershipId, BigDecimal pointsToSpend) {
        LoyaltyMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));

        if (membership.getPointsBalance().compareTo(pointsToSpend) < 0) {
            throw new IllegalStateException("Insufficient points: balance="
                    + membership.getPointsBalance() + ", requested=" + pointsToSpend);
        }

        BigDecimal creditAmount = pointsToSpend.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        fifoBurn(membership, pointsToSpend);

        LoyaltyRedemption redemption = LoyaltyRedemption.builder()
                .membershipId(membershipId)
                .pointsSpent(pointsToSpend)
                .balanceCreditAmount(creditAmount)
                .status(LoyaltyRedemption.RedemptionStatus.PENDING)
                .build();
        redemption = redemptionRepository.save(redemption);

        LoyaltyTransaction tx = txRepository.save(LoyaltyTransaction.builder()
                .membershipId(membershipId)
                .type(LoyaltyTransaction.TransactionType.BURN)
                .points(pointsToSpend.negate())
                .description("Balance credit: " + creditAmount + " TND")
                .build());

        postLiabilityEntry("LOYALTY-BLC-" + tx.getId().toString().substring(0, 8),
                pointsToSpend.negate(), null,
                "Balance credit redemption: " + creditAmount + " TND");

        log.info("Balance credit redeemed: membership={}, points={}, credit={}",
                membershipId, pointsToSpend, creditAmount);
        return redemption;
    }

    public List<LoyaltyRedemption> getRedemptionHistory(UUID membershipId) {
        return redemptionRepository.findByMembershipIdOrderByCreatedAtDesc(membershipId);
    }

    // ---------------------------------------------------------------
    // Internal — FIFO burn
    // ---------------------------------------------------------------

    private void fifoBurn(LoyaltyMembership membership, BigDecimal pointsToBurn) {
        BigDecimal remaining = pointsToBurn;
        List<LoyaltyTransaction> batches = txRepository.findAvailableEarnBatches(
                membership.getId(), OffsetDateTime.now());

        for (LoyaltyTransaction batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal available = batch.getRemaining();
            BigDecimal consume = available.min(remaining);
            batch.setRemaining(available.subtract(consume));
            txRepository.save(batch);
            remaining = remaining.subtract(consume);
        }

        membership.setPointsBalance(membership.getPointsBalance().subtract(pointsToBurn));
        membershipRepository.save(membership);
    }

    // ---------------------------------------------------------------
    // Internal — Ledger
    // ---------------------------------------------------------------

    private void postLiabilityEntry(String ref, BigDecimal points, LoyaltyProgram program, String desc) {
        try {
            BigDecimal pointValue = program != null ? program.getPointValue() : new BigDecimal("0.01");
            BigDecimal amount = points.multiply(pointValue).setScale(3, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) == 0) return;

            LedgerAccount liability = getAccount(POINTS_LIABILITY);
            LedgerAccount expense = getAccount(LOYALTY_EXPENSE);

            JournalEntry journal = journalEntryRepository.save(JournalEntry.builder()
                    .reference(ref)
                    .postingDate(OffsetDateTime.now())
                    .status(JournalEntry.JournalStatus.POSTED)
                    .description(desc)
                    .build());

            UUID jId = journal.getId();
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                ledgerEntryRepository.saveAll(List.of(
                        entry(jId, expense.getId(), amount, null, "TND", ref, "Loyalty expense (points earned)"),
                        entry(jId, liability.getId(), null, amount, "TND", ref, "Loyalty liability accrual")
                ));
                expense.setBalance(expense.getBalance().add(amount));
                liability.setBalance(liability.getBalance().add(amount));
            } else {
                BigDecimal abs = amount.negate();
                ledgerEntryRepository.saveAll(List.of(
                        entry(jId, liability.getId(), abs, null, "TND", ref, "Loyalty liability reduction"),
                        entry(jId, expense.getId(), null, abs, "TND", ref, "Loyalty expense reversal")
                ));
                liability.setBalance(liability.getBalance().subtract(abs));
                expense.setBalance(expense.getBalance().subtract(abs));
            }

            ledgerAccountRepository.save(expense);
            ledgerAccountRepository.save(liability);
        } catch (Exception e) {
            log.warn("Ledger posting failed for loyalty {}: {}", ref, e.getMessage());
        }
    }

    private LedgerAccount getAccount(String accountNumber) {
        return ledgerAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalStateException("Ledger account not found: " + accountNumber));
    }

    private LedgerEntry entry(UUID journalId, UUID accountId, BigDecimal debit,
                              BigDecimal credit, String currency, String ref, String desc) {
        return LedgerEntry.builder()
                .journalId(journalId).accountId(accountId)
                .debitAmount(debit != null ? debit : BigDecimal.ZERO)
                .creditAmount(credit != null ? credit : BigDecimal.ZERO)
                .currency(currency).transactionReference(ref)
                .description(desc).build();
    }

    // ---------------------------------------------------------------
    // Internal — Tier upgrade
    // ---------------------------------------------------------------

    private void updateTier(LoyaltyMembership membership) {
        List<LoyaltyTier> tiers = tierRepository.findByProgramIdOrderByMinLifetimePointsAsc(membership.getProgramId());
        LoyaltyTier best = tiers.get(0);
        for (LoyaltyTier t : tiers) {
            if (membership.getLifetimePoints().compareTo(t.getMinLifetimePoints()) >= 0) {
                best = t;
            }
        }
        if (!best.getId().equals(membership.getTierId())) {
            membership.setTierId(best.getId());
            log.info("Tier updated: membership={}, newTier={}", membership.getId(), best.getName());
        }
    }
}
