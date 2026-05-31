package com.switchplatform.platform.repository.fraud;

import com.switchplatform.platform.model.fraud.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {
    List<FraudAlert> findByCardIdOrderByCreatedAtDesc(UUID cardId);
    List<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudAlert.Status status);
    List<FraudAlert> findByStatus(FraudAlert.Status status);
    List<FraudAlert> findAllByOrderByCreatedAtDesc();
}
