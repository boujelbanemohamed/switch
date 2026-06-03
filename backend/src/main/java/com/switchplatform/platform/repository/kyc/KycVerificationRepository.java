package com.switchplatform.platform.repository.kyc;

import com.switchplatform.platform.model.kyc.KycVerification;
import com.switchplatform.platform.model.kyc.KycVerification.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KycVerificationRepository extends JpaRepository<KycVerification, UUID> {

    List<KycVerification> findByCardholderIdOrderByCreatedAtDesc(UUID cardholderId);

    List<KycVerification> findByCardholderIdAndStatus(UUID cardholderId, Status status);

    List<KycVerification> findByStatus(Status status);
}
