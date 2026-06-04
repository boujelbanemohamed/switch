package com.switchplatform.platform.controller.standin;

import com.switchplatform.platform.model.standin.StandInAuthorization;
import com.switchplatform.platform.model.standin.StandInRule;
import com.switchplatform.platform.service.standin.StandInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/standin")
@RequiredArgsConstructor
public class StandInController {

    private final StandInService standInService;

    @GetMapping("/rules")
    public ResponseEntity<List<StandInRule>> listRules() {
        return ResponseEntity.ok(standInService.getAllRules());
    }

    @PostMapping("/rules")
    public ResponseEntity<StandInRule> createRule(@Valid @RequestBody StandInRule rule) {
        return ResponseEntity.status(HttpStatus.CREATED).body(standInService.createRule(rule));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<StandInRule> updateRule(@PathVariable UUID id, @Valid @RequestBody StandInRule rule) {
        return ResponseEntity.ok(standInService.updateRule(id, rule));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        standInService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/authorizations")
    public ResponseEntity<List<StandInAuthorization>> listAuthorizations(
            @RequestParam(required = false) UUID issuerId) {
        return ResponseEntity.ok(standInService.getAuthorizations(issuerId));
    }

    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Long>> countPending() {
        return ResponseEntity.ok(Map.of("count", standInService.countPending()));
    }
}
