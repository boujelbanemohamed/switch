package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.CardProduct;
import com.switchplatform.platform.model.issuing.CardProgram;
import com.switchplatform.platform.repository.issuing.CardProductRepository;
import com.switchplatform.platform.repository.issuing.CardProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CardProgramServiceTest {

    private CardProgramService service;
    private CardProgramRepository programRepo;
    private CardProductRepository productRepo;
    private final Map<UUID, CardProgram> programStore = new ConcurrentHashMap<>();
    private final Map<UUID, CardProduct> productStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        programStore.clear();
        productStore.clear();

        programRepo = mock(CardProgramRepository.class);
        productRepo = mock(CardProductRepository.class);

        when(programRepo.save(any())).thenAnswer(inv -> {
            CardProgram p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            if (p.getStatus() == null) p.setStatus(CardProgram.Status.DRAFT);
            if (p.getCreatedAt() == null) p.setCreatedAt(java.time.OffsetDateTime.now());
            if (p.getUpdatedAt() == null) p.setUpdatedAt(java.time.OffsetDateTime.now());
            programStore.put(p.getId(), p);
            return p;
        });
        when(programRepo.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(programStore.get(inv.getArgument(0))));
        when(programRepo.findAll(any(Sort.class))).thenAnswer(inv -> new ArrayList<>(programStore.values()));
        when(programRepo.findAll(any(Pageable.class))).thenAnswer(inv -> {
            Pageable p = inv.getArgument(0);
            List<CardProgram> all = new ArrayList<>(programStore.values());
            int start = (int) p.getOffset();
            int end = Math.min(start + p.getPageSize(), all.size());
            List<CardProgram> content = start < all.size() ? all.subList(start, end) : Collections.emptyList();
            return new org.springframework.data.domain.PageImpl<>(content, p, all.size());
        });
        when(programRepo.findByStatus(any())).thenAnswer(inv -> {
            CardProgram.Status s = inv.getArgument(0);
            return programStore.values().stream().filter(p -> s == p.getStatus()).toList();
        });
        when(programRepo.findByProgramType(any())).thenAnswer(inv -> {
            CardProgram.ProgramType t = inv.getArgument(0);
            return programStore.values().stream().filter(p -> t == p.getProgramType()).toList();
        });
        when(programRepo.findByBrand(any())).thenAnswer(inv -> {
            String b = inv.getArgument(0);
            return programStore.values().stream().filter(p -> b.equals(p.getBrand())).toList();
        });
        doAnswer(inv -> {
            programStore.remove(inv.getArgument(0));
            return null;
        }).when(programRepo).deleteById(any());

        when(productRepo.save(any())).thenAnswer(inv -> {
            CardProduct p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            productStore.put(p.getId(), p);
            return p;
        });
        when(productRepo.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(productStore.get(inv.getArgument(0))));
        when(productRepo.findByProgramId(any())).thenAnswer(inv -> {
            UUID pid = inv.getArgument(0);
            return productStore.values().stream().filter(p -> pid.equals(p.getProgramId())).toList();
        });
        when(productRepo.findByProductCode(any())).thenAnswer(inv -> {
            String code = inv.getArgument(0);
            return productStore.values().stream().filter(p -> code.equals(p.getProductCode())).findFirst();
        });
        when(productRepo.findAll(any(Sort.class))).thenAnswer(inv -> new ArrayList<>(productStore.values()));
        when(productRepo.findAll(any(Pageable.class))).thenAnswer(inv -> {
            Pageable p = inv.getArgument(0);
            List<CardProduct> all = new ArrayList<>(productStore.values());
            int start = (int) p.getOffset();
            int end = Math.min(start + p.getPageSize(), all.size());
            List<CardProduct> content = start < all.size() ? all.subList(start, end) : Collections.emptyList();
            return new org.springframework.data.domain.PageImpl<>(content, p, all.size());
        });
        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Iterable<CardProduct> iterable = inv.getArgument(0);
            iterable.forEach(p -> productStore.remove(p.getId()));
            return null;
        }).when(productRepo).deleteAll(any());

        service = new CardProgramService(programRepo, productRepo);
    }

    @Test
    void shouldCreateProgram() {
        CardProgram p = CardProgram.builder()
                .name("Test Program")
                .programType(CardProgram.ProgramType.CONSUMER)
                .brand("VISA")
                .build();
        CardProgram created = service.createProgram(p);
        assertNotNull(created.getId());
        assertEquals(CardProgram.Status.DRAFT, created.getStatus());
    }

    @Test
    void shouldActivateAndDeactivateProgram() {
        CardProgram p = service.createProgram(CardProgram.builder()
                .name("Test").programType(CardProgram.ProgramType.CONSUMER).build());
        CardProgram active = service.activateProgram(p.getId());
        assertEquals(CardProgram.Status.ACTIVE, active.getStatus());
        CardProgram inactive = service.deactivateProgram(p.getId());
        assertEquals(CardProgram.Status.INACTIVE, inactive.getStatus());
    }

    @Test
    void shouldUpdateProgram() {
        CardProgram p = service.createProgram(CardProgram.builder()
                .name("Original").programType(CardProgram.ProgramType.CONSUMER).build());
        CardProgram update = CardProgram.builder().name("Updated")
                .programType(CardProgram.ProgramType.BUSINESS).build();
        CardProgram updated = service.updateProgram(p.getId(), update);
        assertEquals("Updated", updated.getName());
        assertEquals(CardProgram.ProgramType.BUSINESS, updated.getProgramType());
    }

    @Test
    void shouldDeleteProgram() {
        CardProgram p = service.createProgram(CardProgram.builder()
                .name("Test").programType(CardProgram.ProgramType.CONSUMER).build());
        service.deleteProgram(p.getId());
        assertThrows(IllegalArgumentException.class, () -> service.getProgram(p.getId()));
    }

    @Test
    void shouldCreateProduct() {
        UUID progId = UUID.randomUUID();
        CardProduct prod = CardProduct.builder()
                .programId(progId).name("Gold Card").productCode("GOLD01")
                .cardType(CardProduct.CardType.CREDIT).cardBrand(CardProduct.CardBrand.VISA)
                .currencyCode("TND").build();
        CardProduct created = service.createProduct(prod);
        assertNotNull(created.getId());
    }

    @Test
    void shouldGetProductByCode() {
        UUID progId = UUID.randomUUID();
        service.createProduct(CardProduct.builder().programId(progId)
                .name("Gold").productCode("GOLD01")
                .cardType(CardProduct.CardType.CREDIT).cardBrand(CardProduct.CardBrand.VISA)
                .currencyCode("TND").build());
        CardProduct found = service.getProductByCode("GOLD01");
        assertNotNull(found);
        assertThrows(IllegalArgumentException.class, () -> service.getProductByCode("NONEXISTENT"));
    }

    @Test
    void shouldListProgramsPaginated() {
        for (int i = 0; i < 5; i++) {
            service.createProgram(CardProgram.builder()
                    .name("P" + i).programType(CardProgram.ProgramType.CONSUMER).build());
        }
        Page<CardProgram> page = service.listPrograms(0, 3);
        assertEquals(3, page.getContent().size());
        assertEquals(5, page.getTotalElements());
    }

    @Test
    void shouldListProductsPaginated() {
        UUID progId = UUID.randomUUID();
        for (int i = 0; i < 8; i++) {
            service.createProduct(CardProduct.builder().programId(progId)
                    .name("Prod" + i).productCode("P" + i)
                    .cardType(CardProduct.CardType.CREDIT).cardBrand(CardProduct.CardBrand.VISA)
                    .currencyCode("TND").build());
        }
        Page<CardProduct> page = service.listProducts(0, 4);
        assertEquals(4, page.getContent().size());
        assertEquals(8, page.getTotalElements());
    }
}
