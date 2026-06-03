package com.switchplatform.platform.repository.dispute;

import com.switchplatform.platform.model.dispute.DisputeTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisputeTimelineRepository extends JpaRepository<DisputeTimeline, UUID> {

    List<DisputeTimeline> findByDisputeIdOrderByCreatedAtAsc(UUID disputeId);
}
