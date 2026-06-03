package com.switchplatform.platform.controller.dispute;

import com.switchplatform.platform.model.dispute.Dispute;
import com.switchplatform.platform.model.dispute.DisputeEvidence;
import com.switchplatform.platform.model.dispute.DisputeTimeline;
import com.switchplatform.platform.service.dispute.DisputeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
@Validated
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    public ResponseEntity<?> openDispute(@Valid @RequestBody OpenDisputeRequest request, Authentication auth) {
        try {
            Dispute dispute = disputeService.openDispute(
                    request.getTransactionId(),
                    Dispute.DisputeType.valueOf(request.getDisputeType().toUpperCase()),
                    request.getAmount(),
                    request.getCurrencyCode(),
                    request.getReasonCode(),
                    request.getReasonDescription(),
                    request.getInitiatedBy(),
                    request.getMerchantId(),
                    request.getClearingRecordId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(dispute);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Dispute>> listDisputes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID merchantId) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(disputeService.listAll().stream()
                    .filter(d -> d.getStatus().name().equalsIgnoreCase(status))
                    .toList());
        }
        if (merchantId != null) {
            return ResponseEntity.ok(disputeService.getDisputesByMerchant(merchantId));
        }
        return ResponseEntity.ok(disputeService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDispute(@PathVariable UUID id) {
        try {
            Dispute dispute = disputeService.getDisputeWithTimeline(id);
            List<DisputeTimeline> timeline = disputeService.getTimeline(id);
            return ResponseEntity.ok(Map.of("dispute", dispute, "timeline", timeline));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/transition")
    public ResponseEntity<?> transitionStatus(@PathVariable UUID id,
                                               @Valid @RequestBody TransitionRequest request,
                                               Authentication auth) {
        try {
            Dispute dispute = disputeService.transitionStatus(
                    id,
                    Dispute.DisputeStatus.valueOf(request.getStatus().toUpperCase()),
                    auth.getName(),
                    request.getNotes()
            );
            return ResponseEntity.ok(dispute);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<?> submitEvidence(@PathVariable UUID id,
                                             @Valid @RequestBody EvidenceRequest request,
                                             Authentication auth) {
        try {
            DisputeEvidence evidence = disputeService.submitEvidence(
                    id,
                    request.getSubmittedBy() != null ? request.getSubmittedBy() : auth.getName(),
                    DisputeEvidence.EvidenceType.valueOf(request.getEvidenceType().toUpperCase()),
                    request.getDescription(),
                    request.getFileReference()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(evidence);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/evidence")
    public ResponseEntity<List<DisputeEvidence>> listEvidence(@PathVariable UUID id) {
        return ResponseEntity.ok(disputeService.getEvidence(id));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<DisputeTimeline>> listTimeline(@PathVariable UUID id) {
        return ResponseEntity.ok(disputeService.getTimeline(id));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<Dispute>> getByTransaction(@PathVariable String transactionId) {
        return ResponseEntity.ok(disputeService.getDisputesByTransaction(transactionId));
    }

    @Data
    public static class OpenDisputeRequest {
        @NotBlank @Size(max = 64)
        private String transactionId;
        @NotBlank @Size(max = 30)
        private String disputeType;
        @NotNull @DecimalMin("0.001")
        private BigDecimal amount;
        @NotBlank @Size(min = 3, max = 3)
        private String currencyCode;
        @Size(max = 10)
        private String reasonCode;
        @Size(max = 500)
        private String reasonDescription;
        @NotBlank @Size(max = 20)
        private String initiatedBy;
        private UUID merchantId;
        private UUID clearingRecordId;
    }

    @Data
    public static class TransitionRequest {
        @NotBlank @Size(max = 30)
        private String status;
        @Size(max = 500)
        private String notes;
    }

    @Data
    public static class EvidenceRequest {
        @Size(max = 20)
        private String submittedBy;
        @NotBlank @Size(max = 30)
        private String evidenceType;
        @Size(max = 500)
        private String description;
        @Size(max = 256)
        private String fileReference;
    }
}
