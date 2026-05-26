package com.switchplatform.platform.controller;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.RoutingRule;
import com.switchplatform.platform.model.BinTable;
import com.switchplatform.platform.repository.BinTableRepository;
import com.switchplatform.platform.repository.RoutingRuleRepository;
import com.switchplatform.platform.service.MonitoringService;
import com.switchplatform.platform.service.ParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ParticipantService participantService;
    private final RoutingRuleRepository routingRuleRepository;
    private final BinTableRepository binTableRepository;
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
    public ResponseEntity<Participant> createParticipant(@RequestBody Participant participant) {
        return ResponseEntity.ok(participantService.create(participant));
    }

    @GetMapping("/participants/{id}")
    public ResponseEntity<Participant> getParticipant(@PathVariable UUID id) {
        return ResponseEntity.ok(participantService.findById(id));
    }

    @PutMapping("/participants/{id}")
    public ResponseEntity<Participant> updateParticipant(
            @PathVariable UUID id, @RequestBody Participant participant) {
        return ResponseEntity.ok(participantService.update(id, participant));
    }

    @DeleteMapping("/participants/{id}")
    public ResponseEntity<Void> deleteParticipant(@PathVariable UUID id) {
        participantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/routing-rules")
    public ResponseEntity<List<RoutingRule>> listRoutingRules() {
        return ResponseEntity.ok(routingRuleRepository.findAll());
    }

    @PostMapping("/routing-rules")
    public ResponseEntity<RoutingRule> createRoutingRule(@RequestBody RoutingRule rule) {
        return ResponseEntity.ok(routingRuleRepository.save(rule));
    }

    @GetMapping("/routing-rules/{id}")
    public ResponseEntity<RoutingRule> getRoutingRule(@PathVariable UUID id) {
        return routingRuleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/routing-rules/{id}")
    public ResponseEntity<RoutingRule> updateRoutingRule(
            @PathVariable UUID id, @RequestBody RoutingRule rule) {
        rule.setId(id);
        return ResponseEntity.ok(routingRuleRepository.save(rule));
    }

    @DeleteMapping("/routing-rules/{id}")
    public ResponseEntity<Void> deleteRoutingRule(@PathVariable UUID id) {
        routingRuleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bin-tables")
    public ResponseEntity<List<BinTable>> listBinTables() {
        return ResponseEntity.ok(binTableRepository.findAll());
    }

    @PostMapping("/bin-tables")
    public ResponseEntity<BinTable> createBinTable(@RequestBody BinTable binTable) {
        return ResponseEntity.ok(binTableRepository.save(binTable));
    }
}
