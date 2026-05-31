package com.switchplatform.platform.repository.ecommerce;

import com.switchplatform.platform.model.ecommerce.AcsCardEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcsCardEnrollmentRepository extends JpaRepository<AcsCardEnrollment, UUID> {
    List<AcsCardEnrollment> findByCardId(UUID cardId);
    List<AcsCardEnrollment> findByCardholderId(UUID cardholderId);
    List<AcsCardEnrollment> findByStatus(AcsCardEnrollment.Status status);
    Optional<AcsCardEnrollment> findByCardIdAndStatus(UUID cardId, AcsCardEnrollment.Status status);
    long countByCardIdAndStatus(UUID cardId, AcsCardEnrollment.Status status);
}
