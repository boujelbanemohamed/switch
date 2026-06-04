package com.switchplatform.platform.repository.ecommerce;

import com.switchplatform.platform.model.ecommerce.CofToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CofTokenRepository extends JpaRepository<CofToken, UUID> {
    List<CofToken> findByParticipantId(UUID participantId);
    List<CofToken> findByStatus(String status);
}
