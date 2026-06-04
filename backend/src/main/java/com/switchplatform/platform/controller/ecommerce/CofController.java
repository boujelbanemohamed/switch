package com.switchplatform.platform.controller.ecommerce;

import com.switchplatform.platform.model.ecommerce.CofToken;
import com.switchplatform.platform.model.ecommerce.RecurringSchedule;
import com.switchplatform.platform.service.ecommerce.CofTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ecommerce/cof")
@RequiredArgsConstructor
public class CofController {

    private final CofTokenService cofTokenService;

    @GetMapping("/tokens")
    public ResponseEntity<List<CofToken>> listTokens(@RequestParam(required = false) UUID participantId) {
        return ResponseEntity.ok(cofTokenService.listTokens(participantId));
    }

    @PostMapping("/tokens")
    public ResponseEntity<CofToken> createToken(@Valid @RequestBody CofToken token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cofTokenService.createToken(token));
    }

    @GetMapping("/tokens/{id}")
    public ResponseEntity<CofToken> getToken(@PathVariable UUID id) {
        return ResponseEntity.ok(cofTokenService.getToken(id));
    }

    @PutMapping("/tokens/{id}")
    public ResponseEntity<CofToken> updateToken(@PathVariable UUID id, @Valid @RequestBody CofToken token) {
        return ResponseEntity.ok(cofTokenService.updateToken(id, token));
    }

    @DeleteMapping("/tokens/{id}")
    public ResponseEntity<Void> deleteToken(@PathVariable UUID id) {
        cofTokenService.deleteToken(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<RecurringSchedule>> listSchedules(
            @RequestParam(required = false) UUID cofTokenId) {
        return ResponseEntity.ok(cofTokenService.listSchedules(cofTokenId));
    }

    @PostMapping("/schedules")
    public ResponseEntity<RecurringSchedule> createSchedule(@Valid @RequestBody RecurringSchedule schedule) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cofTokenService.createSchedule(schedule));
    }

    @GetMapping("/schedules/{id}")
    public ResponseEntity<RecurringSchedule> getSchedule(@PathVariable UUID id) {
        return ResponseEntity.ok(cofTokenService.getSchedule(id));
    }

    @PutMapping("/schedules/{id}")
    public ResponseEntity<RecurringSchedule> updateSchedule(
            @PathVariable UUID id, @Valid @RequestBody RecurringSchedule schedule) {
        return ResponseEntity.ok(cofTokenService.updateSchedule(id, schedule));
    }

    @DeleteMapping("/schedules/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        cofTokenService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
