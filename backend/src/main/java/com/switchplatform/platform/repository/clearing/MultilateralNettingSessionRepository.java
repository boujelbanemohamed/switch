package com.switchplatform.platform.repository.clearing;

import com.switchplatform.platform.model.clearing.MultilateralNettingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MultilateralNettingSessionRepository extends JpaRepository<MultilateralNettingSession, UUID> {

    Optional<MultilateralNettingSession> findTopBySessionDateOrderByCreatedAtDesc(LocalDate sessionDate);

    Optional<MultilateralNettingSession> findTopByStatusOrderByCreatedAtDesc(MultilateralNettingSession.Status status);
}
