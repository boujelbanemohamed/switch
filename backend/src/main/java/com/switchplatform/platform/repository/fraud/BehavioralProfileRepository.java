package com.switchplatform.platform.repository.fraud;

import com.switchplatform.platform.model.fraud.BehavioralProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BehavioralProfileRepository extends JpaRepository<BehavioralProfile, UUID> {
    Optional<BehavioralProfile> findByCardholderId(UUID cardholderId);
}
