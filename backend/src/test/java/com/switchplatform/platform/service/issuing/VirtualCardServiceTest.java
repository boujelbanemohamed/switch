package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.VirtualCard;
import com.switchplatform.platform.repository.issuing.VirtualCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VirtualCardServiceTest {

    private VirtualCardService service;
    private VirtualCardRepository repository;
    private final Map<UUID, VirtualCard> store = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(0);

    @BeforeEach
    void setUp() {
        store.clear();
        repository = mock(VirtualCardRepository.class);
        when(repository.save(any())).thenAnswer(inv -> {
            VirtualCard c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            store.put(c.getId(), c);
            return c;
        });
        when(repository.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(store.get(inv.getArgument(0))));
        when(repository.findByExternalId(any())).thenAnswer(inv -> {
            String eid = inv.getArgument(0);
            return store.values().stream().filter(c -> eid.equals(c.getExternalId())).findFirst();
        });
        when(repository.findByCardholderId(any())).thenAnswer(inv -> {
            UUID chId = inv.getArgument(0);
            return store.values().stream().filter(c -> chId.equals(c.getCardholderId())).toList();
        });
        when(repository.findByFundingCardId(any())).thenAnswer(inv -> {
            UUID fId = inv.getArgument(0);
            return store.values().stream().filter(c -> fId.equals(c.getFundingCardId())).toList();
        });
        when(repository.findByStatus(any())).thenAnswer(inv -> {
            VirtualCard.Status s = inv.getArgument(0);
            return store.values().stream().filter(c -> s == c.getStatus()).toList();
        });
        when(repository.findByCardholderIdAndStatus(any(), any())).thenAnswer(inv -> {
            UUID chId = inv.getArgument(0);
            VirtualCard.Status s = inv.getArgument(1);
            return store.values().stream().filter(c -> chId.equals(c.getCardholderId()) && s == c.getStatus()).toList();
        });
        when(repository.findAll(any(Sort.class))).thenAnswer(inv -> new ArrayList<>(store.values()));
        when(repository.findAll(any(Pageable.class))).thenAnswer(inv -> {
            Pageable p = inv.getArgument(0);
            List<VirtualCard> all = new ArrayList<>(store.values());
            int start = (int) p.getOffset();
            int end = Math.min(start + p.getPageSize(), all.size());
            List<VirtualCard> content = start < all.size() ? all.subList(start, end) : Collections.emptyList();
            return new org.springframework.data.domain.PageImpl<>(content, p, all.size());
        });

        service = new VirtualCardService(repository);
    }

    private VirtualCard buildCard() {
        return VirtualCard.builder()
                .cardholderId(UUID.randomUUID())
                .panHash("abc123")
                .panSuffix("1234")
                .expiryDate(LocalDate.now().plusYears(1))
                .usageType(VirtualCard.UsageType.MULTI_USE)
                .currencyCode("TND")
                .nameOnCard("Test User")
                .amountLimit(BigDecimal.valueOf(1000))
                .build();
    }

    @Test
    void shouldCreateVirtualCard() {
        VirtualCard card = buildCard();
        VirtualCard created = service.createVirtualCard(card);
        assertNotNull(created.getId());
        assertEquals(VirtualCard.Status.PENDING_ACTIVATION, created.getStatus());
        assertEquals(BigDecimal.ZERO, created.getAmountUsed());
        assertEquals(0, created.getTransactionCount());
    }

    @Test
    void shouldSetMaxTransactionsToOneForSingleUse() {
        VirtualCard card = buildCard();
        card.setUsageType(VirtualCard.UsageType.SINGLE_USE);
        VirtualCard created = service.createVirtualCard(card);
        assertEquals(Integer.valueOf(1), created.getMaxTransactions());
    }

    @Test
    void shouldActivateCard() {
        VirtualCard card = service.createVirtualCard(buildCard());
        VirtualCard activated = service.activateCard(card.getId());
        assertEquals(VirtualCard.Status.ACTIVE, activated.getStatus());
        assertNotNull(activated.getActivatedAt());
    }

    @Test
    void shouldSuspendAndResumeCard() {
        VirtualCard card = service.createVirtualCard(buildCard());
        service.activateCard(card.getId());
        VirtualCard suspended = service.suspendCard(card.getId());
        assertEquals(VirtualCard.Status.SUSPENDED, suspended.getStatus());
        VirtualCard resumed = service.resumeCard(card.getId());
        assertEquals(VirtualCard.Status.ACTIVE, resumed.getStatus());
    }

    @Test
    void shouldCancelCard() {
        VirtualCard card = service.createVirtualCard(buildCard());
        VirtualCard cancelled = service.cancelCard(card.getId(), "test reason");
        assertEquals(VirtualCard.Status.CANCELLED, cancelled.getStatus());
        assertEquals("test reason", cancelled.getCancelReason());
        assertNotNull(cancelled.getCancelledAt());
    }

    @Test
    void shouldUpdateLimits() {
        VirtualCard card = service.createVirtualCard(buildCard());
        service.activateCard(card.getId());
        VirtualCard updated = service.updateLimits(card.getId(), BigDecimal.valueOf(2000), 5);
        assertEquals(BigDecimal.valueOf(2000), updated.getAmountLimit());
        assertEquals(Integer.valueOf(5), updated.getMaxTransactions());
    }

    @Test
    void shouldThrowWhenCardNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getCard(UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class,
                () -> service.activateCard(UUID.randomUUID()));
    }

    @Test
    void shouldListAllPaginated() {
        for (int i = 0; i < 10; i++) {
            VirtualCard c = buildCard();
            c.setExternalId("ext-" + i);
            service.createVirtualCard(c);
        }
        Page<VirtualCard> page = service.listAll(0, 5);
        assertEquals(5, page.getSize());
        assertEquals(10, page.getTotalElements());
        assertEquals(0, page.getNumber());
    }

    @Test
    void shouldListByStatus() {
        VirtualCard c1 = service.createVirtualCard(buildCard());
        service.activateCard(c1.getId());
        VirtualCard c2 = service.createVirtualCard(buildCard());
        List<VirtualCard> pending = service.listByStatus(VirtualCard.Status.PENDING_ACTIVATION);
        assertEquals(1, pending.size());
        List<VirtualCard> active = service.listByStatus(VirtualCard.Status.ACTIVE);
        assertEquals(1, active.size());
    }

    @Test
    void shouldGetCardsByCardholder() {
        UUID chId = UUID.randomUUID();
        VirtualCard c1 = buildCard(); c1.setCardholderId(chId); service.createVirtualCard(c1);
        VirtualCard c2 = buildCard(); c2.setCardholderId(chId); service.createVirtualCard(c2);
        List<VirtualCard> cards = service.getCardsByCardholder(chId);
        assertEquals(2, cards.size());
    }
}
