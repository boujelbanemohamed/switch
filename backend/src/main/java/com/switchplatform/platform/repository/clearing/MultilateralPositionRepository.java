package com.switchplatform.platform.repository.clearing;

import com.switchplatform.platform.model.clearing.MultilateralPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MultilateralPositionRepository extends JpaRepository<MultilateralPosition, UUID> {

    List<MultilateralPosition> findBySessionId(UUID sessionId);

    List<MultilateralPosition> findByParticipantId(UUID participantId);

    List<MultilateralPosition> findBySessionIdAndPositionType(UUID sessionId, MultilateralPosition.PositionType positionType);
}
