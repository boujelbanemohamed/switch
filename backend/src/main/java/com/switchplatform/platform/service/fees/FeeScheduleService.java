package com.switchplatform.platform.service.fees;

import com.switchplatform.platform.model.fees.FeeRule;
import com.switchplatform.platform.model.fees.FeeRule.CalcMethod;
import com.switchplatform.platform.model.fees.FeeSchedule;
import com.switchplatform.platform.model.fees.FeeSchedule.ScheduleType;
import com.switchplatform.platform.model.fees.FeeSchedule.Status;
import com.switchplatform.platform.repository.fees.FeeRuleRepository;
import com.switchplatform.platform.repository.fees.FeeScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeScheduleService {

    private final FeeScheduleRepository scheduleRepository;
    private final FeeRuleRepository ruleRepository;

    @Transactional
    public FeeSchedule createSchedule(FeeSchedule schedule) {
        schedule.setStatus(Status.DRAFT);
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public FeeSchedule updateSchedule(UUID id, FeeSchedule update) {
        FeeSchedule existing = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FeeSchedule not found: " + id));
        existing.setName(update.getName());
        existing.setDescription(update.getDescription());
        existing.setScheduleType(update.getScheduleType());
        existing.setPriority(update.getPriority());
        existing.setCurrencyCode(update.getCurrencyCode());
        existing.setEffectiveFrom(update.getEffectiveFrom());
        existing.setEffectiveUntil(update.getEffectiveUntil());
        existing.setParticipantId(update.getParticipantId());
        existing.setMerchantId(update.getMerchantId());
        existing.setCardProductId(update.getCardProductId());
        existing.setAppliesTo(update.getAppliesTo());
        existing.setMetadata(update.getMetadata());
        return scheduleRepository.save(existing);
    }

    @Transactional
    public FeeSchedule activateSchedule(UUID id) {
        FeeSchedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FeeSchedule not found: " + id));
        s.setStatus(Status.ACTIVE);
        return scheduleRepository.save(s);
    }

    @Transactional
    public FeeSchedule deactivateSchedule(UUID id) {
        FeeSchedule s = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FeeSchedule not found: " + id));
        s.setStatus(Status.INACTIVE);
        return scheduleRepository.save(s);
    }

    @Transactional
    public FeeRule addRule(UUID scheduleId, FeeRule rule) {
        rule.setScheduleId(scheduleId);
        return ruleRepository.save(rule);
    }

    @Transactional
    public FeeRule updateRule(UUID ruleId, FeeRule update) {
        FeeRule existing = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("FeeRule not found: " + ruleId));
        existing.setRuleName(update.getRuleName());
        existing.setRuleOrder(update.getRuleOrder());
        existing.setCalcMethod(update.getCalcMethod());
        existing.setFlatAmount(update.getFlatAmount());
        existing.setPercentageRate(update.getPercentageRate());
        existing.setMinAmount(update.getMinAmount());
        existing.setMaxAmount(update.getMaxAmount());
        existing.setMinTxAmount(update.getMinTxAmount());
        existing.setMaxTxAmount(update.getMaxTxAmount());
        existing.setBrandFilter(update.getBrandFilter());
        existing.setCardTypeFilter(update.getCardTypeFilter());
        existing.setMccFilter(update.getMccFilter());
        existing.setRegionFilter(update.getRegionFilter());
        existing.setEntryModeFilter(update.getEntryModeFilter());
        existing.setIsWaivable(update.getIsWaivable());
        existing.setDescription(update.getDescription());
        return ruleRepository.save(existing);
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    @Transactional
    public void deleteSchedule(UUID id) {
        ruleRepository.deleteByScheduleId(id);
        scheduleRepository.deleteById(id);
    }

    public List<FeeSchedule> getActiveSchedules() {
        return scheduleRepository.findActiveSchedules(LocalDate.now());
    }

    public List<FeeRule> getRules(UUID scheduleId) {
        return ruleRepository.findByScheduleIdOrderByRuleOrderAsc(scheduleId);
    }

    public FeeSchedule getSchedule(UUID id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FeeSchedule not found: " + id));
    }

    public List<FeeSchedule> listSchedules(ScheduleType type) {
        if (type != null) {
            return scheduleRepository.findByScheduleType(type);
        }
        return scheduleRepository.findAll();
    }

    public FeeCalculationResult calculateFee(UUID scheduleId, FeeCalculationContext ctx) {
        FeeSchedule schedule = getSchedule(scheduleId);
        List<FeeRule> rules = getRules(scheduleId);

        BigDecimal totalFee = BigDecimal.ZERO;
        Map<String, Object> breakdown = new HashMap<>();

        for (FeeRule rule : rules) {
            if (!matchesFilters(rule, ctx)) continue;

            BigDecimal fee = applyRule(rule, ctx);
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                totalFee = totalFee.add(fee);
                breakdown.put(rule.getRuleName(), Map.of(
                    "amount", fee,
                    "method", rule.getCalcMethod()
                ));
            }
        }

        return new FeeCalculationResult(totalFee, schedule.getCurrencyCode(), breakdown);
    }

    public FeeCalculationResult calculateAllFees(FeeCalculationContext ctx) {
        List<FeeSchedule> active = getActiveSchedules();
        BigDecimal total = BigDecimal.ZERO;
        Map<String, Object> allBreakdown = new HashMap<>();

        for (FeeSchedule schedule : active) {
            FeeCalculationResult r = calculateFee(schedule.getId(), ctx);
            if (r.totalFee().compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(r.totalFee());
                allBreakdown.put(schedule.getName(), Map.of(
                    "total", r.totalFee(),
                    "currency", r.currency(),
                    "items", r.breakdown()
                ));
            }
        }

        return new FeeCalculationResult(total, ctx.currency(), allBreakdown);
    }

    private boolean matchesFilters(FeeRule rule, FeeCalculationContext ctx) {
        if (rule.getBrandFilter() != null && !rule.getBrandFilter().equals("*")
                && !rule.getBrandFilter().equalsIgnoreCase(ctx.brand())) return false;
        if (rule.getCardTypeFilter() != null && !rule.getCardTypeFilter().equals("*")
                && !rule.getCardTypeFilter().equalsIgnoreCase(ctx.cardType())) return false;
        if (rule.getMccFilter() != null && !rule.getMccFilter().equals("*")
                && !rule.getMccFilter().equals(ctx.mcc())) return false;
        if (rule.getRegionFilter() != null && !rule.getRegionFilter().equals("*")
                && !rule.getRegionFilter().equalsIgnoreCase(ctx.region())) return false;
        if (rule.getMinTxAmount() != null && ctx.amount().compareTo(rule.getMinTxAmount()) < 0) return false;
        if (rule.getMaxTxAmount() != null && ctx.amount().compareTo(rule.getMaxTxAmount()) > 0) return false;
        return true;
    }

    private BigDecimal applyRule(FeeRule rule, FeeCalculationContext ctx) {
        return switch (rule.getCalcMethod()) {
            case FLAT -> safeAmount(rule.getFlatAmount());
            case PERCENTAGE -> {
                BigDecimal pct = safeRate(rule.getPercentageRate())
                        .multiply(ctx.amount())
                        .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
                yield clamp(pct, rule.getMinAmount(), rule.getMaxAmount());
            }
            case MIXED -> {
                BigDecimal flat = safeAmount(rule.getFlatAmount());
                BigDecimal pct = safeRate(rule.getPercentageRate())
                        .multiply(ctx.amount())
                        .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
                yield clamp(flat.add(pct), rule.getMinAmount(), rule.getMaxAmount());
            }
            case TIERED -> calculateTiered(rule, ctx);
            case INTERCHANGE_LOOKUP -> ctx.interchangeOverride() != null ? ctx.interchangeOverride() : BigDecimal.ZERO;
        };
    }

    private BigDecimal calculateTiered(FeeRule rule, FeeCalculationContext ctx) {
        BigDecimal pct = safeRate(rule.getPercentageRate());
        BigDecimal flat = safeAmount(rule.getFlatAmount());
        BigDecimal result = pct.multiply(ctx.amount())
                .divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP)
                .add(flat);
        return clamp(result, rule.getMinAmount(), rule.getMaxAmount());
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (min != null && value.compareTo(min) < 0) return min;
        if (max != null && value.compareTo(max) > 0) return max;
        return value;
    }

    private BigDecimal safeAmount(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private BigDecimal safeRate(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
