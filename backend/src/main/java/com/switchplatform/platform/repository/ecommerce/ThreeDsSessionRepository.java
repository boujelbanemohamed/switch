package com.switchplatform.platform.repository.ecommerce;

import com.switchplatform.platform.model.ecommerce.ThreeDsSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ThreeDsSessionRepository extends JpaRepository<ThreeDsSession, UUID> {

    Optional<ThreeDsSession> findByTransactionId(String transactionId);

    Optional<ThreeDsSession> findByAcsReferenceNumber(String acsReferenceNumber);
}
