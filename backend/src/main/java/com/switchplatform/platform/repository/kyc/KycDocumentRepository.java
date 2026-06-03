package com.switchplatform.platform.repository.kyc;

import com.switchplatform.platform.model.kyc.KycDocument;
import com.switchplatform.platform.model.kyc.KycDocument.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    List<KycDocument> findByCardholderIdOrderByCreatedAtDesc(UUID cardholderId);

    List<KycDocument> findByCardholderIdAndVerificationStatus(UUID cardholderId, VerificationStatus status);

    List<KycDocument> findByVerificationStatus(VerificationStatus status);
}
