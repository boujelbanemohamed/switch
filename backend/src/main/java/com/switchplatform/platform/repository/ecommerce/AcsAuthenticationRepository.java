package com.switchplatform.platform.repository.ecommerce;

import com.switchplatform.platform.model.ecommerce.AcsAuthentication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcsAuthenticationRepository extends JpaRepository<AcsAuthentication, UUID> {

    @Query("SELECT a FROM AcsAuthentication a WHERE a.authenticationValue = :authenticationReference")
    Optional<AcsAuthentication> findByAuthenticationReference(@Param("authenticationReference") String authenticationReference);

    @Query("SELECT a FROM AcsAuthentication a WHERE a.id = :threeDsSessionId")
    Optional<AcsAuthentication> findByThreeDsSessionId(@Param("threeDsSessionId") UUID threeDsSessionId);

    List<AcsAuthentication> findByStatus(AcsAuthentication.Status status);

    List<AcsAuthentication> findByCardId(UUID cardId);
}
