package com.switchplatform.platform.service.kyc;

import com.switchplatform.platform.model.issuing.Cardholder;
import com.switchplatform.platform.model.kyc.KycDocument;
import com.switchplatform.platform.model.kyc.KycDocument.VerificationStatus;
import com.switchplatform.platform.model.kyc.KycVerification;
import com.switchplatform.platform.model.kyc.KycVerification.Status;
import com.switchplatform.platform.repository.issuing.CardholderRepository;
import com.switchplatform.platform.repository.kyc.KycDocumentRepository;
import com.switchplatform.platform.repository.kyc.KycVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycService {

    private final KycDocumentRepository documentRepository;
    private final KycVerificationRepository verificationRepository;
    private final CardholderRepository cardholderRepository;

    @Transactional
    public KycDocument uploadDocument(KycDocument document) {
        return documentRepository.save(document);
    }

    @Transactional
    public KycDocument verifyDocument(UUID documentId, boolean approved, String verifiedBy, String reason) {
        KycDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        if (approved) {
            doc.setVerificationStatus(VerificationStatus.VERIFIED);
            doc.setVerifiedBy(verifiedBy);
            doc.setVerifiedAt(OffsetDateTime.now());
        } else {
            doc.setVerificationStatus(VerificationStatus.REJECTED);
            doc.setRejectionReason(reason);
        }
        return documentRepository.save(doc);
    }

    @Transactional
    public KycVerification startVerification(UUID cardholderId, KycVerification.VerificationType type, int requestedLevel) {
        KycVerification v = KycVerification.builder()
                .cardholderId(cardholderId)
                .verificationType(type)
                .status(Status.IN_PROGRESS)
                .requestedLevel(requestedLevel)
                .build();
        return verificationRepository.save(v);
    }

    @Transactional
    public KycVerification completeVerification(UUID verificationId, boolean approved, String verifiedBy, String notes, String rejectionReason) {
        KycVerification v = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification not found: " + verificationId));
        if (approved) {
            v.setStatus(Status.VERIFIED);
            v.setVerifiedBy(verifiedBy);
            v.setVerifiedAt(OffsetDateTime.now());
            v.setNotes(notes);

            Cardholder ch = cardholderRepository.findById(v.getCardholderId())
                    .orElseThrow(() -> new IllegalArgumentException("Cardholder not found: " + v.getCardholderId()));
            if (ch.getKycLevel() == null || ch.getKycLevel() < v.getRequestedLevel()) {
                ch.setKycLevel(v.getRequestedLevel());
                cardholderRepository.save(ch);
                log.info("Upgraded KYC level for cardholder {} to level {}", ch.getId(), v.getRequestedLevel());
            }
        } else {
            v.setStatus(Status.REJECTED);
            v.setRejectionReason(rejectionReason);
            v.setNotes(notes);
        }
        return verificationRepository.save(v);
    }

    public List<KycDocument> getDocuments(UUID cardholderId) {
        return documentRepository.findByCardholderIdOrderByCreatedAtDesc(cardholderId);
    }

    public List<KycVerification> getVerifications(UUID cardholderId) {
        return verificationRepository.findByCardholderIdOrderByCreatedAtDesc(cardholderId);
    }

    public List<KycDocument> getPendingDocuments() {
        return documentRepository.findByVerificationStatus(VerificationStatus.PENDING);
    }

    public List<KycVerification> getPendingVerifications() {
        return verificationRepository.findByStatus(Status.PENDING);
    }
}
