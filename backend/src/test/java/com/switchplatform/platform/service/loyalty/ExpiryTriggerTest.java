package com.switchplatform.platform.service.loyalty;

import com.switchplatform.platform.model.loyalty.LoyaltyMembership;
import com.switchplatform.platform.model.loyalty.LoyaltyTransaction;
import com.switchplatform.platform.repository.loyalty.LoyaltyMembershipRepository;
import com.switchplatform.platform.repository.loyalty.LoyaltyTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ExpiryTriggerTest {

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private LoyaltyTransactionRepository txRepository;

    @Autowired
    private LoyaltyMembershipRepository membershipRepository;

    private final UUID membershipId = UUID.fromString("0305e4ec-709f-40fc-9dda-277add3a2dc4");

    @BeforeEach
    void setUp() {
        txRepository.deleteAll(txRepository.findByMembershipIdOrderByCreatedAtDesc(membershipId));
        LoyaltyMembership mem = membershipRepository.findById(membershipId).orElseThrow();
        mem.setPointsBalance(BigDecimal.ZERO);
        membershipRepository.save(mem);
    }

    @Test
    @Order(1)
    void testFifoBurnThenExpire() {
        // ---------------------------------------------------------------
        // ÉTAPE 1 — Earn 100 TND → 120 pts (Test Program Silver: rate 1.0 × mult 1.2)
        // ---------------------------------------------------------------
        loyaltyService.earnPoints(membershipId, BigDecimal.valueOf(100),
                "FIFO-A", "Lot A — 100 TND en achat");
        assertBalance(new BigDecimal("120.000"), "120 pts après earn A");

        sleep(10); // force createdAt ordering

        // ---------------------------------------------------------------
        // ÉTAPE 2 — Earn 100 TND → 120 pts (lot B)
        // ---------------------------------------------------------------
        loyaltyService.earnPoints(membershipId, BigDecimal.valueOf(100),
                "FIFO-B", "Lot B — 100 TND en achat");
        assertBalance(new BigDecimal("240.000"), "240 pts après earns A+B");

        List<LoyaltyTransaction> earns = txRepository
                .findByMembershipIdAndTypeOrderByCreatedAtAsc(membershipId, LoyaltyTransaction.TransactionType.EARN);
        assertEquals(2, earns.size());
        UUID idA = earns.get(0).getId();
        UUID idB = earns.get(1).getId();

        // ---------------------------------------------------------------
        // ÉTAPE 3 — Burn 150 → FIFO : 120 du lot A + 30 du lot B
        // ---------------------------------------------------------------
        loyaltyService.burnPoints(membershipId, new BigDecimal("150.00"), "Achat 150 pts avec lot A puis B");

        earns = txRepository.findByMembershipIdAndTypeOrderByCreatedAtAsc(membershipId, LoyaltyTransaction.TransactionType.EARN);
        assertEquals(0, BigDecimal.ZERO.compareTo(earns.get(0).getRemaining()),
                "Lot A consumed 120/120, remaining=0");
        assertEquals(0, new BigDecimal("90.000").compareTo(earns.get(1).getRemaining()),
                "Lot B consumed 30/120, remaining=90");
        assertBalance(new BigDecimal("90.000"), "90 pts balance (240−150)");

        // ---------------------------------------------------------------
        // ÉTAPE 4 — Forcer l'expiration du lot B
        // ---------------------------------------------------------------
        LoyaltyTransaction batchB = txRepository.findById(idB).orElseThrow();
        batchB.setExpiresAt(OffsetDateTime.now().minusDays(1));
        txRepository.save(batchB);

        loyaltyService.expirePoints();

        // ---------------------------------------------------------------
        // ÉTAPE 5 — Vérifications
        // ---------------------------------------------------------------
        batchB = txRepository.findById(idB).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(batchB.getRemaining()),
                "Lot B remaining=0 après expire");

        List<LoyaltyTransaction> expires = txRepository
                .findByMembershipIdAndTypeOrderByCreatedAtAsc(membershipId, LoyaltyTransaction.TransactionType.EXPIRE);
        assertEquals(1, expires.size(),
                "Une seule transaction EXPIRE créée");
        assertEquals(0, new BigDecimal("-90.000").compareTo(expires.get(0).getPoints()),
                "EXPIRE = −90 (seulement le non-consommé du lot B)");

        // Le solde ne doit pas descendre sous 0
        assertBalance(BigDecimal.ZERO, "0 pts restant — pas de double décompte");
    }

    @Test
    @Order(2)
    void testNoExpireWhenUnusedBatchNotExpired() {
        loyaltyService.earnPoints(membershipId, BigDecimal.valueOf(100),
                "NO-EXP", "Lot non expiré");
        loyaltyService.burnPoints(membershipId, new BigDecimal("50.00"), "Burn partiel");
        assertBalance(new BigDecimal("70.000"), "70 après earn 120 − burn 50");

        loyaltyService.expirePoints();

        // Aucune transaction EXPIRE car le lot n'est pas expiré
        List<LoyaltyTransaction> expires = txRepository
                .findByMembershipIdAndTypeOrderByCreatedAtAsc(membershipId, LoyaltyTransaction.TransactionType.EXPIRE);
        assertTrue(expires.isEmpty(), "Aucune expire si le lot est encore valide");
        assertBalance(new BigDecimal("70.000"), "Toujours 70, rien expiré");
    }

    private void assertBalance(BigDecimal expected, String msg) {
        LoyaltyMembership mem = membershipRepository.findById(membershipId).orElseThrow();
        assertEquals(0, expected.compareTo(mem.getPointsBalance()),
                msg + " → balance=" + mem.getPointsBalance() + " (attendu=" + expected + ")");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
