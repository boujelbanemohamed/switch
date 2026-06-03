package com.switchplatform.platform.controller.kyc;

import com.switchplatform.platform.model.kyc.KycDocument;
import com.switchplatform.platform.model.kyc.KycVerification;
import com.switchplatform.platform.service.kyc.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @GetMapping("/documents")
    public ResponseEntity<List<KycDocument>> getDocuments(@RequestParam(required = false) UUID cardholderId) {
        if (cardholderId != null) {
            return ResponseEntity.ok(kycService.getDocuments(cardholderId));
        }
        return ResponseEntity.ok(kycService.getPendingDocuments());
    }

    @PostMapping("/documents")
    public ResponseEntity<KycDocument> uploadDocument(@RequestBody KycDocument doc) {
        return ResponseEntity.ok(kycService.uploadDocument(doc));
    }

    @PostMapping("/documents/{id}/verify")
    public ResponseEntity<KycDocument> verifyDocument(@PathVariable UUID id,
                                                       @RequestParam boolean approved,
                                                       @RequestParam(required = false) String verifiedBy,
                                                       @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(kycService.verifyDocument(id, approved, verifiedBy, reason));
    }

    @GetMapping("/verifications")
    public ResponseEntity<List<KycVerification>> getVerifications(@RequestParam(required = false) UUID cardholderId) {
        if (cardholderId != null) {
            return ResponseEntity.ok(kycService.getVerifications(cardholderId));
        }
        return ResponseEntity.ok(kycService.getPendingVerifications());
    }

    @PostMapping("/verifications")
    public ResponseEntity<KycVerification> startVerification(@RequestParam UUID cardholderId,
                                                              @RequestParam KycVerification.VerificationType type,
                                                              @RequestParam int requestedLevel) {
        return ResponseEntity.ok(kycService.startVerification(cardholderId, type, requestedLevel));
    }

    @PostMapping("/verifications/{id}/complete")
    public ResponseEntity<KycVerification> completeVerification(@PathVariable UUID id,
                                                                 @RequestParam boolean approved,
                                                                 @RequestParam(required = false) String verifiedBy,
                                                                 @RequestParam(required = false) String notes,
                                                                 @RequestParam(required = false) String rejectionReason) {
        return ResponseEntity.ok(kycService.completeVerification(id, approved, verifiedBy, notes, rejectionReason));
    }
}
