package com.switchplatform.platform.repository.ecommerce;

import com.switchplatform.platform.model.ecommerce.AcsChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcsChallengeRepository extends JpaRepository<AcsChallenge, UUID> {

    @Query("SELECT c FROM AcsChallenge c WHERE CAST(c.authenticationId AS string) = :challengeId")
    Optional<AcsChallenge> findByChallengeId(@Param("challengeId") String challengeId);

    @Query("SELECT c FROM AcsChallenge c WHERE c.authenticationId = :threeDsSessionId")
    Optional<AcsChallenge> findByThreeDsSessionId(@Param("threeDsSessionId") UUID threeDsSessionId);

    @Query("SELECT c FROM AcsChallenge c WHERE c.challengeData = :authenticationToken")
    Optional<AcsChallenge> findByAuthenticationToken(@Param("authenticationToken") String authenticationToken);

    List<AcsChallenge> findByAuthenticationId(UUID authenticationId);
}
