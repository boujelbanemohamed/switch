package com.switchplatform.platform.controller.fees;

import com.switchplatform.platform.model.fees.FeeRule;
import com.switchplatform.platform.model.fees.FeeSchedule;
import com.switchplatform.platform.model.fees.FeeSchedule.ScheduleType;
import com.switchplatform.platform.service.fees.FeeCalculationContext;
import com.switchplatform.platform.service.fees.FeeCalculationResult;
import com.switchplatform.platform.service.fees.FeeScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fees")
@RequiredArgsConstructor
public class FeeScheduleController {

    private final FeeScheduleService feeScheduleService;

    @GetMapping("/schedules")
    public ResponseEntity<List<FeeSchedule>> listSchedules(
            @RequestParam(required = false) ScheduleType type) {
        return ResponseEntity.ok(feeScheduleService.listSchedules(type));
    }

    @GetMapping("/schedules/{id}")
    public ResponseEntity<FeeSchedule> getSchedule(@PathVariable UUID id) {
        return ResponseEntity.ok(feeScheduleService.getSchedule(id));
    }

    @PostMapping("/schedules")
    public ResponseEntity<FeeSchedule> createSchedule(@Valid @RequestBody FeeSchedule schedule) {
        return ResponseEntity.ok(feeScheduleService.createSchedule(schedule));
    }

    @PutMapping("/schedules/{id}")
    public ResponseEntity<FeeSchedule> updateSchedule(@PathVariable UUID id,
                                                       @Valid @RequestBody FeeSchedule update) {
        return ResponseEntity.ok(feeScheduleService.updateSchedule(id, update));
    }

    @PostMapping("/schedules/{id}/activate")
    public ResponseEntity<FeeSchedule> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(feeScheduleService.activateSchedule(id));
    }

    @PostMapping("/schedules/{id}/deactivate")
    public ResponseEntity<FeeSchedule> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(feeScheduleService.deactivateSchedule(id));
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        feeScheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/schedules/{id}/rules")
    public ResponseEntity<List<FeeRule>> getRules(@PathVariable UUID id) {
        return ResponseEntity.ok(feeScheduleService.getRules(id));
    }

    @PostMapping("/schedules/{id}/rules")
    public ResponseEntity<FeeRule> addRule(@PathVariable UUID id, @Valid @RequestBody FeeRule rule) {
        return ResponseEntity.ok(feeScheduleService.addRule(id, rule));
    }

    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<FeeRule> updateRule(@PathVariable UUID ruleId, @Valid @RequestBody FeeRule update) {
        return ResponseEntity.ok(feeScheduleService.updateRule(ruleId, update));
    }

    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        feeScheduleService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/calculate")
    public ResponseEntity<FeeCalculationResult> calculate(@Valid @RequestBody FeeCalculateRequest req) {
        FeeCalculationContext ctx = new FeeCalculationContext(
                req.amount, req.currency, req.brand, req.cardType,
                req.mcc, req.region, req.entryMode, req.participantId,
                req.merchantId, null
        );
        if (req.scheduleId != null) {
            return ResponseEntity.ok(feeScheduleService.calculateFee(UUID.fromString(req.scheduleId), ctx));
        }
        return ResponseEntity.ok(feeScheduleService.calculateAllFees(ctx));
    }

    public record FeeCalculateRequest(
        BigDecimal amount,
        String currency,
        String brand,
        String cardType,
        String mcc,
        String region,
        String entryMode,
        String participantId,
        String merchantId,
        String scheduleId
    ) {}
}
