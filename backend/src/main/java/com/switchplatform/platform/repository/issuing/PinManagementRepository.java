package com.switchplatform.platform.repository.issuing;

import com.switchplatform.platform.model.issuing.PinManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PinManagementRepository extends JpaRepository<PinManagement, UUID> {
    Optional<PinManagement> findByCardId(UUID cardId);
    void deleteByCardId(UUID cardId);
}
