package com.switchplatform.platform.repository.authorization;

import com.switchplatform.platform.model.authorization.AuthDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthDecisionRepository extends JpaRepository<AuthDecision, Long> {
    List<AuthDecision> findByCardIdOrderByCreatedAtDesc(UUID cardId);
    Optional<AuthDecision> findByTransactionId(String transactionId);
}
