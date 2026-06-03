package com.switchplatform.platform.repository.dispute;

import com.switchplatform.platform.model.dispute.DisputeEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisputeEvidenceRepository extends JpaRepository<DisputeEvidence, UUID> {

    List<DisputeEvidence> findByDisputeIdOrderBySubmittedAtDesc(UUID disputeId);
}
