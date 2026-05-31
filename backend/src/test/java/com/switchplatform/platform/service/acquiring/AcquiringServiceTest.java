package com.switchplatform.platform.service.acquiring;

import com.switchplatform.platform.model.acquiring.MdrPlan;
import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.model.acquiring.MerchantSettlement;
import com.switchplatform.platform.model.acquiring.Terminal;
import com.switchplatform.platform.repository.acquiring.MdrPlanRepository;
import com.switchplatform.platform.repository.acquiring.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcquiringServiceTest {

    private MerchantService merchantService;
    private TerminalService terminalService;
    private MerchantSettlementService settlementService;
    private MerchantRepository merchantRepository;
    private MdrPlanRepository mdrPlanRepository;
    private final java.util.Map<java.util.UUID, Merchant> merchantStore = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, MdrPlan> mdrPlanStore = new java.util.concurrent.ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        merchantStore.clear();
        mdrPlanStore.clear();
        merchantRepository = mock(MerchantRepository.class);
        mdrPlanRepository = mock(MdrPlanRepository.class);
        when(merchantRepository.existsByMerchantId(any())).thenAnswer(inv -> {
            String mid = inv.getArgument(0);
            return merchantStore.values().stream()
                    .anyMatch(m -> mid.equals(m.getMerchantId()));
        });
        when(merchantRepository.save(any())).thenAnswer(inv -> {
            Merchant m = inv.getArgument(0);
            merchantStore.put(m.getId(), m);
            return m;
        });
        when(merchantRepository.findById(any())).thenAnswer(inv ->
                java.util.Optional.ofNullable(merchantStore.get(inv.getArgument(0))));
        when(merchantRepository.findByMerchantId(any())).thenAnswer(inv -> {
            String mid = inv.getArgument(0);
            return merchantStore.values().stream()
                    .filter(m -> mid.equals(m.getMerchantId())).findFirst();
        });
        when(merchantRepository.findAll()).thenAnswer(inv ->
                java.util.List.copyOf(merchantStore.values()));
        when(mdrPlanRepository.save(any())).thenAnswer(inv -> {
            MdrPlan p = inv.getArgument(0);
            if (p.getId() == null) p.setId(java.util.UUID.randomUUID());
            mdrPlanStore.put(p.getId(), p);
            return p;
        });
        when(mdrPlanRepository.findByMerchantIdAndCardBrandAndCardType(any(), any(), any()))
                .thenAnswer(inv -> {
                    UUID mid = inv.getArgument(0);
                    String brand = inv.getArgument(1);
                    String type = inv.getArgument(2);
                    return mdrPlanStore.values().stream()
                            .filter(p -> mid.equals(p.getMerchantId())
                                    && brand.equalsIgnoreCase(p.getCardBrand())
                                    && (p.getCardType() == null || p.getCardType().equalsIgnoreCase(type)))
                            .findFirst();
                });
        merchantService = new MerchantService(merchantRepository, mdrPlanRepository);
        terminalService = new TerminalService();
        settlementService = new MerchantSettlementService();
    }

    @Test
    void shouldOnboardMerchant() {
        Merchant merchant = Merchant.builder()
                .legalName("Tech Retail SARL")
                .merchantId("MCH001")
                .merchantCategoryCode("5734")
                .tradingName("Tech Retail")
                .email("contact@techretail.tn")
                .countryCode("TN")
                .build();

        Merchant onboarded = merchantService.onboardMerchant(merchant);

        assertNotNull(onboarded.getId());
        assertEquals(Merchant.MerchantStatus.PENDING_ONBOARDING, onboarded.getStatus());
        assertNotNull(onboarded.getOnboardingDate());
        assertNotNull(onboarded.getCreatedAt());
    }

    @Test
    void shouldApproveMerchant() {
        Merchant merchant = Merchant.builder()
                .legalName("Fresh Market")
                .merchantId("MCH002")
                .build();
        Merchant onboarded = merchantService.onboardMerchant(merchant);

        Merchant approved = merchantService.approveMerchant(onboarded.getId());

        assertEquals(Merchant.MerchantStatus.ACTIVE, approved.getStatus());
        assertNotNull(approved.getActivationDate());
    }

    @Test
    void shouldSuspendMerchant() {
        Merchant merchant = Merchant.builder()
                .legalName("Cafe Tunisie")
                .merchantId("MCH003")
                .build();
        Merchant onboarded = merchantService.onboardMerchant(merchant);
        merchantService.approveMerchant(onboarded.getId());

        Merchant suspended = merchantService.suspendMerchant(onboarded.getId(), "REGULATORY_REVIEW");

        assertEquals(Merchant.MerchantStatus.SUSPENDED, suspended.getStatus());
    }

    @Test
    void shouldTerminateMerchant() {
        Merchant merchant = Merchant.builder()
                .legalName("Old Shop")
                .merchantId("MCH004")
                .build();
        Merchant onboarded = merchantService.onboardMerchant(merchant);

        Merchant terminated = merchantService.terminateMerchant(onboarded.getId());

        assertEquals(Merchant.MerchantStatus.TERMINATED, terminated.getStatus());
        assertNotNull(terminated.getTerminationDate());
    }

    @Test
    void shouldGetMerchantByCode() {
        Merchant merchant = Merchant.builder()
                .legalName("Bookstore")
                .merchantId("MCH005")
                .build();
        merchantService.onboardMerchant(merchant);

        Merchant found = merchantService.getMerchantByCode("MCH005").orElse(null);

        assertNotNull(found);
        assertEquals("Bookstore", found.getLegalName());
    }

    @Test
    void shouldGetMerchantById() {
        Merchant merchant = Merchant.builder()
                .legalName("Pharmacy Plus")
                .merchantId("MCH006")
                .build();
        Merchant onboarded = merchantService.onboardMerchant(merchant);

        Merchant found = merchantService.getMerchant(onboarded.getId()).orElse(null);

        assertNotNull(found);
        assertEquals(onboarded.getId(), found.getId());
    }

    @Test
    void shouldListByStatus() {
        Merchant m1 = Merchant.builder().legalName("S1").merchantId("MCH007").build();
        Merchant m2 = Merchant.builder().legalName("S2").merchantId("MCH008").build();
        merchantService.onboardMerchant(m1);
        merchantService.onboardMerchant(m2);

        List<Merchant> pending = merchantService.listMerchantsByStatus("PENDING_ONBOARDING");

        assertTrue(pending.size() >= 2);
    }

    @Test
    void shouldCalculateMdr() {
        Merchant merchant = Merchant.builder()
                .legalName("MDR Test Merchant")
                .merchantId("MCH009")
                .build();
        merchantService.onboardMerchant(merchant);
        merchantService.approveMerchant(merchant.getId());

        MdrPlan plan = MdrPlan.builder()
                .name("Visa Domestic Rate")
                .merchantId(merchant.getId())
                .cardBrand("VISA")
                .transactionType(MdrPlan.TransactionType.PURCHASE)
                .domesticRate(BigDecimal.valueOf(1.5))
                .fixedFeeDomestic(BigDecimal.valueOf(0.50))
                .build();
        merchantService.addMdrPlan(plan);

        BigDecimal fee = merchantService.calculateMdr(
                merchant.getId(), BigDecimal.valueOf(100), "VISA", "DEBIT");

        BigDecimal expected = BigDecimal.valueOf(2.000).setScale(3, java.math.RoundingMode.HALF_UP);
        assertEquals(expected, fee);
    }

    @Test
    void shouldCalculateMdrUsingDefaultRate() {
        Merchant merchant = Merchant.builder()
                .legalName("Default MDR Merchant")
                .merchantId("MCH010")
                .mdrPercentage(BigDecimal.valueOf(2.0))
                .mdrFixedFee(BigDecimal.valueOf(0.25))
                .build();
        merchantService.onboardMerchant(merchant);
        merchantService.approveMerchant(merchant.getId());

        BigDecimal fee = merchantService.calculateMdr(
                merchant.getId(), BigDecimal.valueOf(200), "MASTERCARD", "CREDIT");

        BigDecimal expected = BigDecimal.valueOf(4.250).setScale(3, java.math.RoundingMode.HALF_UP);
        assertEquals(expected, fee);
    }

    @Test
    void shouldThrowWhenNoMdrConfigured() {
        Merchant merchant = Merchant.builder()
                .legalName("No MDR Merchant")
                .merchantId("MCH011")
                .build();
        merchantService.onboardMerchant(merchant);

        assertThrows(IllegalStateException.class,
                () -> merchantService.calculateMdr(
                        merchant.getId(), BigDecimal.valueOf(100), "VISA", "DEBIT"));
    }

    @Test
    void shouldThrowForDuplicateMerchantId() {
        Merchant m1 = Merchant.builder().legalName("Dup1").merchantId("DUPLICATE").build();
        merchantService.onboardMerchant(m1);

        Merchant m2 = Merchant.builder().legalName("Dup2").merchantId("DUPLICATE").build();

        assertThrows(IllegalArgumentException.class,
                () -> merchantService.onboardMerchant(m2));
    }

    @Test
    void shouldRequireLegalName() {
        Merchant merchant = Merchant.builder()
                .legalName("")
                .merchantId("MCH012")
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> merchantService.onboardMerchant(merchant));
    }

    @Test
    void shouldRegisterTerminal() {
        Terminal terminal = Terminal.builder()
                .merchantId(UUID.randomUUID())
                .terminalId("TID00001")
                .terminalType(Terminal.TerminalType.PHYSICAL_TPE)
                .manufacturer("Ingenico")
                .model("ICT250")
                .firmwareVersion("2.1.3")
                .locationName("Main Checkout")
                .build();

        Terminal registered = terminalService.registerTerminal(terminal);

        assertNotNull(registered.getId());
        assertEquals(Terminal.TerminalStatus.ACTIVE, registered.getStatus());
        assertNotNull(registered.getCreatedAt());
    }

    @Test
    void shouldGetTerminalByTid() {
        Terminal terminal = Terminal.builder()
                .merchantId(UUID.randomUUID())
                .terminalId("TID00002")
                .terminalType(Terminal.TerminalType.PHYSICAL_TPE)
                .build();
        terminalService.registerTerminal(terminal);

        Terminal found = terminalService.getTerminalByTid("TID00002").orElse(null);

        assertNotNull(found);
        assertEquals("TID00002", found.getTerminalId());
    }

    @Test
    void shouldGetTerminalById() {
        Terminal terminal = Terminal.builder()
                .merchantId(UUID.randomUUID())
                .terminalId("TID00003")
                .terminalType(Terminal.TerminalType.ECOMMERCE)
                .build();
        Terminal registered = terminalService.registerTerminal(terminal);

        Terminal found = terminalService.getTerminal(registered.getId()).orElse(null);

        assertNotNull(found);
        assertEquals(registered.getId(), found.getId());
    }

    @Test
    void shouldListByMerchant() {
        UUID merchantId = UUID.randomUUID();

        Terminal t1 = Terminal.builder()
                .merchantId(merchantId)
                .terminalId("TID00004")
                .terminalType(Terminal.TerminalType.PHYSICAL_TPE)
                .build();
        Terminal t2 = Terminal.builder()
                .merchantId(merchantId)
                .terminalId("TID00005")
                .terminalType(Terminal.TerminalType.PHYSICAL_TPE)
                .build();
        terminalService.registerTerminal(t1);
        terminalService.registerTerminal(t2);

        List<Terminal> terminals = terminalService.listByMerchant(merchantId);

        assertEquals(2, terminals.size());
        assertTrue(terminals.stream().allMatch(t -> t.getMerchantId().equals(merchantId)));
    }

    @Test
    void shouldUpdateFirmware() {
        Terminal terminal = Terminal.builder()
                .merchantId(UUID.randomUUID())
                .terminalId("TID00006")
                .terminalType(Terminal.TerminalType.MOBILE)
                .firmwareVersion("1.0.0")
                .build();
        Terminal registered = terminalService.registerTerminal(terminal);

        Terminal updated = terminalService.updateFirmware(registered.getId(), "1.2.0");

        assertEquals("1.2.0", updated.getFirmwareVersion());
        assertNotNull(updated.getLastContact());
    }

    @Test
    void shouldUpdateTerminalStatus() {
        Terminal terminal = Terminal.builder()
                .merchantId(UUID.randomUUID())
                .terminalId("TID00007")
                .terminalType(Terminal.TerminalType.KIOSK)
                .build();
        Terminal registered = terminalService.registerTerminal(terminal);

        Terminal updated = terminalService.updateStatus(registered.getId(), "SUSPENDED");

        assertEquals(Terminal.TerminalStatus.SUSPENDED, updated.getStatus());
    }

    @Test
    void shouldThrowForDuplicateTerminalId() {
        Terminal t1 = Terminal.builder()
                .merchantId(UUID.randomUUID())
                .terminalId("DUP_TID")
                .terminalType(Terminal.TerminalType.PHYSICAL_TPE)
                .build();
        terminalService.registerTerminal(t1);

        Terminal t2 = Terminal.builder()
                .merchantId(UUID.randomUUID())
                .terminalId("DUP_TID")
                .terminalType(Terminal.TerminalType.PHYSICAL_TPE)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> terminalService.registerTerminal(t2));
    }

    @Test
    void shouldCreateSettlement() {
        UUID merchantId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        MerchantSettlement settlement = settlementService.createSettlement(
                merchantId, today, "TND");

        assertNotNull(settlement.getId());
        assertEquals(merchantId, settlement.getMerchantId());
        assertEquals(today, settlement.getSettlementDate());
        assertEquals("TND", settlement.getCurrencyCode());
        assertEquals(0, settlement.getTotalTransactions());
        assertEquals(BigDecimal.ZERO, settlement.getTotalAmount());
        assertEquals(BigDecimal.ZERO, settlement.getTotalFees());
        assertEquals(BigDecimal.ZERO, settlement.getTotalCommission());
        assertEquals(BigDecimal.ZERO, settlement.getNetAmount());
        assertEquals(MerchantSettlement.SettlementStatus.PENDING, settlement.getStatus());
    }

    @Test
    void shouldAddTransaction() {
        UUID merchantId = UUID.randomUUID();
        MerchantSettlement settlement = settlementService.createSettlement(
                merchantId, LocalDate.now(), "USD");

        MerchantSettlement updated = settlementService.addTransaction(
                settlement.getId(),
                BigDecimal.valueOf(150.00),
                BigDecimal.valueOf(2.25),
                BigDecimal.valueOf(1.50));

        assertEquals(1, updated.getTotalTransactions());
        assertEquals(0, BigDecimal.valueOf(150.00).compareTo(updated.getTotalAmount()));
        assertEquals(0, BigDecimal.valueOf(2.25).compareTo(updated.getTotalFees()));
        assertEquals(0, BigDecimal.valueOf(1.50).compareTo(updated.getTotalCommission()));

        settlementService.addTransaction(
                settlement.getId(),
                BigDecimal.valueOf(75.00),
                BigDecimal.valueOf(1.10),
                BigDecimal.valueOf(0.75));

        MerchantSettlement withTwo = settlementService.getSettlement(settlement.getId()).orElse(null);
        assertNotNull(withTwo);
        assertEquals(2, withTwo.getTotalTransactions());
        assertEquals(0, BigDecimal.valueOf(225.00).compareTo(withTwo.getTotalAmount()));
    }

    @Test
    void shouldConfirmAndPay() {
        UUID merchantId = UUID.randomUUID();
        MerchantSettlement settlement = settlementService.createSettlement(
                merchantId, LocalDate.now(), "EUR");

        MerchantSettlement confirmed = settlementService.confirmSettlement(settlement.getId());

        assertEquals(MerchantSettlement.SettlementStatus.CONFIRMED, confirmed.getStatus());

        MerchantSettlement paid = settlementService.markPaid(settlement.getId(), "PAYREF-001");

        assertEquals(MerchantSettlement.SettlementStatus.PAID, paid.getStatus());
        assertEquals("PAYREF-001", paid.getPaymentReference());
        assertNotNull(paid.getPaidAt());
    }

    @Test
    void shouldCalculateNetAmount() {
        UUID merchantId = UUID.randomUUID();
        MerchantSettlement settlement = settlementService.createSettlement(
                merchantId, LocalDate.now(), "TND");

        settlementService.addTransaction(
                settlement.getId(),
                BigDecimal.valueOf(500.00),
                BigDecimal.valueOf(7.50),
                BigDecimal.valueOf(5.00));

        BigDecimal net = settlementService.calculateNetAmount(settlement.getId());

        assertEquals(0, BigDecimal.valueOf(487.50).compareTo(net));
    }

    @Test
    void shouldGetMerchantSettlementsByDateRange() {
        UUID merchantId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        settlementService.createSettlement(merchantId, today.minusDays(1), "TND");
        settlementService.createSettlement(merchantId, today, "TND");

        List<MerchantSettlement> settlements = settlementService.getMerchantSettlements(
                merchantId, today.minusDays(2), today);

        assertEquals(2, settlements.size());
    }

    @Test
    void shouldReturnEmptyForNonExistentSettlement() {
        Optional<MerchantSettlement> result = settlementService.getSettlement(UUID.randomUUID());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowWhenSettlementNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> settlementService.confirmSettlement(UUID.randomUUID()));
    }
}
