package com.switchplatform.platform.controller;

import com.switchplatform.platform.model.BinTable;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.RoutingRule;
import com.switchplatform.platform.service.MonitoringService;
import com.switchplatform.platform.service.ParticipantService;
import com.switchplatform.platform.service.routing.BinTableService;
import com.switchplatform.platform.service.routing.RoutingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
public class AdminController {

    private final ParticipantService participantService;
    private final RoutingRuleService routingRuleService;
    private final BinTableService binTableService;
    private final MonitoringService monitoringService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(monitoringService.getDashboardStats());
    }

    @GetMapping("/participants")
    public ResponseEntity<List<Participant>> listParticipants() {
        return ResponseEntity.ok(participantService.findAll());
    }

    @PostMapping("/participants")
    public ResponseEntity<Participant> createParticipant(@Valid @RequestBody Participant participant) {
        return ResponseEntity.ok(participantService.create(participant));
    }

    @GetMapping("/participants/{id}")
    public ResponseEntity<Participant> getParticipant(@PathVariable UUID id) {
        return ResponseEntity.ok(participantService.findById(id));
    }

    @PutMapping("/participants/{id}")
    public ResponseEntity<Participant> updateParticipant(
            @PathVariable UUID id, @Valid @RequestBody Participant participant) {
        return ResponseEntity.ok(participantService.update(id, participant));
    }

    @DeleteMapping("/participants/{id}")
    public ResponseEntity<Void> deleteParticipant(@PathVariable UUID id) {
        participantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/routing-rules")
    public ResponseEntity<List<RoutingRule>> listRoutingRules() {
        return ResponseEntity.ok(routingRuleService.findAll());
    }

    @PostMapping("/routing-rules")
    public ResponseEntity<RoutingRule> createRoutingRule(@Valid @RequestBody RoutingRule rule) {
        return ResponseEntity.ok(routingRuleService.create(rule));
    }

    @GetMapping("/routing-rules/{id}")
    public ResponseEntity<RoutingRule> getRoutingRule(@PathVariable UUID id) {
        return ResponseEntity.ok(routingRuleService.findById(id));
    }

    @PutMapping("/routing-rules/{id}")
    public ResponseEntity<RoutingRule> updateRoutingRule(
            @PathVariable UUID id, @Valid @RequestBody RoutingRule rule) {
        return ResponseEntity.ok(routingRuleService.update(id, rule));
    }

    @DeleteMapping("/routing-rules/{id}")
    public ResponseEntity<Void> deleteRoutingRule(@PathVariable UUID id) {
        routingRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bin-tables")
    public ResponseEntity<List<BinTable>> listBinTables() {
        return ResponseEntity.ok(binTableService.findAll());
    }

    @PostMapping("/bin-tables")
    public ResponseEntity<BinTable> createBinTable(@Valid @RequestBody BinTable binTable) {
        return ResponseEntity.ok(binTableService.create(binTable));
    }

    @GetMapping("/bin-tables/{id}")
    public ResponseEntity<BinTable> getBinTable(@PathVariable UUID id) {
        return ResponseEntity.ok(binTableService.findById(id));
    }

    @PutMapping("/bin-tables/{id}")
    public ResponseEntity<BinTable> updateBinTable(
            @PathVariable UUID id, @Valid @RequestBody BinTable binTable) {
        return ResponseEntity.ok(binTableService.update(id, binTable));
    }

    @DeleteMapping("/bin-tables/{id}")
    public ResponseEntity<Void> deleteBinTable(@PathVariable UUID id) {
        binTableService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
