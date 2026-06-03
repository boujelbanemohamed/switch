package com.switchplatform.platform.service.fees;

import com.switchplatform.platform.model.fees.FeeRule;
import com.switchplatform.platform.model.fees.FeeRule.CalcMethod;
import com.switchplatform.platform.model.fees.FeeSchedule;
import com.switchplatform.platform.model.fees.FeeSchedule.ScheduleType;
import com.switchplatform.platform.repository.fees.FeeRuleRepository;
import com.switchplatform.platform.repository.fees.FeeScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FeeScheduleServiceTest {

    private FeeScheduleService service;
    private FeeScheduleRepository scheduleRepo;
    private FeeRuleRepository ruleRepo;
    private final Map<UUID, FeeSchedule> scheduleStore = new ConcurrentHashMap<>();
    private final Map<UUID, FeeRule> ruleStore = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        scheduleStore.clear();
        ruleStore.clear();

        scheduleRepo = mock(FeeScheduleRepository.class);
        ruleRepo = mock(FeeRuleRepository.class);

        when(scheduleRepo.save(any())).thenAnswer(inv -> {
            FeeSchedule s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            scheduleStore.put(s.getId(), s);
            return s;
        });
        when(scheduleRepo.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(scheduleStore.get(inv.getArgument(0))));
        when(scheduleRepo.findAll()).thenAnswer(inv -> new ArrayList<>(scheduleStore.values()));
        when(scheduleRepo.findByScheduleType(any())).thenAnswer(inv -> {
            ScheduleType t = inv.getArgument(0);
            return scheduleStore.values().stream().filter(s -> t == s.getScheduleType()).toList();
        });
        when(scheduleRepo.findActiveSchedules(any())).thenAnswer(inv -> {
            LocalDate today = inv.getArgument(0);
            return scheduleStore.values().stream()
                    .filter(s -> s.getStatus() == FeeSchedule.Status.ACTIVE)
                    .filter(s -> s.getEffectiveFrom() == null || !s.getEffectiveFrom().isAfter(today))
                    .filter(s -> s.getEffectiveUntil() == null || s.getEffectiveUntil().isAfter(today))
                    .toList();
        });
        doAnswer(inv -> {
            scheduleStore.remove(inv.getArgument(0));
            return null;
        }).when(scheduleRepo).deleteById(any());

        when(ruleRepo.save(any())).thenAnswer(inv -> {
            FeeRule r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            ruleStore.put(r.getId(), r);
            return r;
        });
        when(ruleRepo.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(ruleStore.get(inv.getArgument(0))));
        when(ruleRepo.findByScheduleIdOrderByRuleOrderAsc(any())).thenAnswer(inv -> {
            UUID sid = inv.getArgument(0);
            return ruleStore.values().stream()
                    .filter(r -> sid.equals(r.getScheduleId()))
                    .sorted(Comparator.comparingInt(FeeRule::getRuleOrder))
                    .toList();
        });
        doAnswer(inv -> {
            UUID sid = inv.getArgument(0);
            ruleStore.values().removeIf(r -> sid.equals(r.getScheduleId()));
            return null;
        }).when(ruleRepo).deleteByScheduleId(any());
        doAnswer(inv -> {
            ruleStore.remove(inv.getArgument(0));
            return null;
        }).when(ruleRepo).deleteById(any());

        service = new FeeScheduleService(scheduleRepo, ruleRepo);
    }

    @Test
    void shouldCreateSchedule() {
        FeeSchedule s = FeeSchedule.builder()
                .name("Test Schedule").scheduleType(ScheduleType.PROCESSING)
                .priority(1).effectiveFrom(LocalDate.now()).build();
        FeeSchedule created = service.createSchedule(s);
        assertNotNull(created.getId());
        assertEquals(FeeSchedule.Status.DRAFT, created.getStatus());
    }

    @Test
    void shouldActivateAndDeactivateSchedule() {
        FeeSchedule s = service.createSchedule(FeeSchedule.builder()
                .name("Test").scheduleType(ScheduleType.PROCESSING)
                .priority(1).effectiveFrom(LocalDate.now()).build());
        FeeSchedule active = service.activateSchedule(s.getId());
        assertEquals(FeeSchedule.Status.ACTIVE, active.getStatus());
        FeeSchedule inactive = service.deactivateSchedule(s.getId());
        assertEquals(FeeSchedule.Status.INACTIVE, inactive.getStatus());
    }

    @Test
    void shouldUpdateSchedule() {
        FeeSchedule s = service.createSchedule(FeeSchedule.builder()
                .name("Original").scheduleType(ScheduleType.PROCESSING)
                .priority(1).effectiveFrom(LocalDate.now()).build());
        FeeSchedule update = FeeSchedule.builder().name("Updated")
                .scheduleType(ScheduleType.INTERCHANGE)
                .priority(2).effectiveFrom(LocalDate.now()).build();
        FeeSchedule updated = service.updateSchedule(s.getId(), update);
        assertEquals("Updated", updated.getName());
        assertEquals(ScheduleType.INTERCHANGE, updated.getScheduleType());
        assertEquals(2, updated.getPriority());
    }

    @Test
    void shouldAddAndGetRules() {
        FeeSchedule s = service.createSchedule(FeeSchedule.builder()
                .name("Test").scheduleType(ScheduleType.PROCESSING)
                .priority(1).effectiveFrom(LocalDate.now()).build());
        FeeRule rule = FeeRule.builder().ruleName("Flat Fee").ruleOrder(1)
                .calcMethod(CalcMethod.FLAT).flatAmount(BigDecimal.TEN).build();
        FeeRule created = service.addRule(s.getId(), rule);
        assertNotNull(created.getId());
        assertEquals(s.getId(), created.getScheduleId());
        List<FeeRule> rules = service.getRules(s.getId());
        assertEquals(1, rules.size());
    }

    @Test
    void shouldDeleteRule() {
        FeeSchedule s = service.createSchedule(FeeSchedule.builder()
                .name("Test").scheduleType(ScheduleType.PROCESSING)
                .priority(1).effectiveFrom(LocalDate.now()).build());
        FeeRule r = service.addRule(s.getId(), FeeRule.builder().ruleName("R1")
                .ruleOrder(1).calcMethod(CalcMethod.FLAT).flatAmount(BigDecimal.ONE).build());
        service.deleteRule(r.getId());
        assertEquals(0, service.getRules(s.getId()).size());
    }

    @Test
    void shouldDeleteScheduleAndCascadeRules() {
        FeeSchedule s = service.createSchedule(FeeSchedule.builder()
                .name("Test").scheduleType(ScheduleType.PROCESSING)
                .priority(1).effectiveFrom(LocalDate.now()).build());
        service.addRule(s.getId(), FeeRule.builder().ruleName("R1")
                .ruleOrder(1).calcMethod(CalcMethod.FLAT).flatAmount(BigDecimal.ONE).build());
        service.deleteSchedule(s.getId());
        assertThrows(IllegalArgumentException.class, () -> service.getSchedule(s.getId()));
    }

    @Test
    void shouldCalculateFlatFee() {
        FeeSchedule s = service.createSchedule(FeeSchedule.builder()
                .name("Flat").scheduleType(ScheduleType.PROCESSING)
                .priority(1).effectiveFrom(LocalDate.now()).build());
        service.activateSchedule(s.getId());
        service.addRule(s.getId(), FeeRule.builder().ruleName("Flat Fee").ruleOrder(1)
                .calcMethod(CalcMethod.FLAT).flatAmount(BigDecimal.valueOf(5.00)).build());
        FeeCalculationContext ctx = new FeeCalculationContext(
                BigDecimal.valueOf(100), "TND", "VISA", "CREDIT",
                null, null, null, null, null, null);
        FeeCalculationResult result = service.calculateFee(s.getId(), ctx);
        assertEquals(BigDecimal.valueOf(5.00), result.totalFee());
    }

    @Test
    void shouldCalculatePercentageFee() {
        FeeSchedule s = service.createSchedule(FeeSchedule.builder()
                .name("Pct").scheduleType(ScheduleType.PROCESSING)
                .priority(1).effectiveFrom(LocalDate.now()).build());
        service.activateSchedule(s.getId());
        service.addRule(s.getId(), FeeRule.builder().ruleName("Pct Fee").ruleOrder(1)
                .calcMethod(CalcMethod.PERCENTAGE).percentageRate(BigDecimal.valueOf(1.5)).build());
        FeeCalculationContext ctx = new FeeCalculationContext(
                BigDecimal.valueOf(200), "TND", "VISA", "CREDIT",
                null, null, null, null, null, null);
        FeeCalculationResult result = service.calculateFee(s.getId(), ctx);
        assertEquals(new BigDecimal("3.000"), result.totalFee());
    }

    @Test
    void shouldRespectBrandFilter() {
        FeeSchedule s = service.createSchedule(FeeSchedule.builder()
                .name("VISA Only").scheduleType(ScheduleType.PROCESSING)
                .priority(1).effectiveFrom(LocalDate.now()).build());
        service.activateSchedule(s.getId());
        service.addRule(s.getId(), FeeRule.builder().ruleName("VISA Fee").ruleOrder(1)
                .calcMethod(CalcMethod.FLAT).flatAmount(BigDecimal.ONE)
                .brandFilter("VISA").build());
        FeeCalculationContext ctxVisa = new FeeCalculationContext(
                BigDecimal.valueOf(100), "TND", "VISA", "CREDIT",
                null, null, null, null, null, null);
        FeeCalculationContext ctxMc = new FeeCalculationContext(
                BigDecimal.valueOf(100), "TND", "MASTERCARD", "CREDIT",
                null, null, null, null, null, null);
        assertEquals(BigDecimal.ONE, service.calculateFee(s.getId(), ctxVisa).totalFee());
        assertEquals(BigDecimal.ZERO, service.calculateFee(s.getId(), ctxMc).totalFee());
    }

    @Test
    void shouldCalculateAllActiveSchedules() {
        FeeSchedule s1 = service.createSchedule(FeeSchedule.builder()
                .name("S1").scheduleType(ScheduleType.PROCESSING)
                .priority(1).effectiveFrom(LocalDate.now()).currencyCode("TND").build());
        service.activateSchedule(s1.getId());
        service.addRule(s1.getId(), FeeRule.builder().ruleName("R1").ruleOrder(1)
                .calcMethod(CalcMethod.FLAT).flatAmount(BigDecimal.valueOf(3)).build());

        FeeSchedule s2 = service.createSchedule(FeeSchedule.builder()
                .name("S2").scheduleType(ScheduleType.PROCESSING)
                .priority(2).effectiveFrom(LocalDate.now()).currencyCode("TND").build());
        service.activateSchedule(s2.getId());
        service.addRule(s2.getId(), FeeRule.builder().ruleName("R2").ruleOrder(1)
                .calcMethod(CalcMethod.FLAT).flatAmount(BigDecimal.valueOf(2)).build());

        FeeCalculationContext ctx = new FeeCalculationContext(
                BigDecimal.valueOf(100), "TND", "VISA", "CREDIT",
                null, null, null, null, null, null);
        FeeCalculationResult result = service.calculateAllFees(ctx);
        assertEquals(BigDecimal.valueOf(5), result.totalFee());
    }

    @Test
    void shouldListByType() {
        service.createSchedule(FeeSchedule.builder().name("Inter")
                .scheduleType(ScheduleType.INTERCHANGE).priority(1)
                .effectiveFrom(LocalDate.now()).build());
        service.createSchedule(FeeSchedule.builder().name("Proc")
                .scheduleType(ScheduleType.PROCESSING).priority(2)
                .effectiveFrom(LocalDate.now()).build());
        assertEquals(1, service.listSchedules(ScheduleType.INTERCHANGE).size());
        assertEquals(2, service.listSchedules(null).size());
    }

    @Test
    void shouldThrowWhenScheduleNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getSchedule(UUID.randomUUID()));
    }
}
