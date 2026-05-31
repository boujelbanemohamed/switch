package com.switchplatform.platform.repository.authorization;

import com.switchplatform.platform.model.authorization.VelocityCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VelocityCheckRepository extends JpaRepository<VelocityCheck, UUID> {
    List<VelocityCheck> findByCardId(UUID cardId);
}
