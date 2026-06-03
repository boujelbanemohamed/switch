package com.switchplatform.platform.controller.ecommerce;

import com.switchplatform.platform.model.ecommerce.AcsCardEnrollment;
import com.switchplatform.platform.service.ecommerce.AcsEnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/acs/enrollments")
@RequiredArgsConstructor
@Validated
public class AcsEnrollmentController {

    private final AcsEnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<AcsCardEnrollment> createEnrollment(@Valid @RequestBody Map<String, Object> request) {
        UUID cardId = UUID.fromString((String) request.get("cardId"));
        UUID cardholderId = request.get("cardholderId") != null
                ? UUID.fromString((String) request.get("cardholderId")) : null;
        UUID merchantId = request.get("merchantId") != null
                ? UUID.fromString((String) request.get("merchantId")) : null;
        AcsCardEnrollment enrollment = enrollmentService.enrollCard(
                cardId, cardholderId, merchantId,
                (String) request.get("phoneNumber"),
                (String) request.get("email"),
                (String) request.get("cardBrand"),
                (String) request.get("cardType"));
        return ResponseEntity.ok(enrollment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcsCardEnrollment> getEnrollment(@PathVariable UUID id) {
        AcsCardEnrollment enrollment = enrollmentService.getEnrollment(id);
        if (enrollment == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(enrollment);
    }

    @GetMapping("/by-card/{cardId}")
    public ResponseEntity<List<AcsCardEnrollment>> getByCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByCard(cardId));
    }

    @GetMapping("/by-cardholder/{cardholderId}")
    public ResponseEntity<List<AcsCardEnrollment>> getByCardholder(@PathVariable UUID cardholderId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByCardholder(cardholderId));
    }

    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<AcsCardEnrollment>> getByStatus(@PathVariable AcsCardEnrollment.Status status) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStatus(status));
    }

    @GetMapping("/check/{cardId}")
    public ResponseEntity<Map<String, Boolean>> checkEnrollment(@PathVariable UUID cardId) {
        return ResponseEntity.ok(Map.of("enrolled", enrollmentService.isCardEnrolled(cardId)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<AcsCardEnrollment> activateEnrollment(@PathVariable UUID id) {
        return ResponseEntity.ok(enrollmentService.activateEnrollment(id));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<AcsCardEnrollment> suspendEnrollment(@PathVariable UUID id) {
        return ResponseEntity.ok(enrollmentService.suspendEnrollment(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AcsCardEnrollment> cancelEnrollment(@PathVariable UUID id) {
        return ResponseEntity.ok(enrollmentService.cancelEnrollment(id));
    }

    @PostMapping("/{id}/unenroll")
    public ResponseEntity<AcsCardEnrollment> unenroll(@PathVariable UUID id) {
        return ResponseEntity.ok(enrollmentService.cancelEnrollment(id));
    }

    @GetMapping
    public ResponseEntity<List<AcsCardEnrollment>> getAllEnrollments() {
        return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }
}
