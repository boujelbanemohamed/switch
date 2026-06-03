package com.switchplatform.platform.repository.auth;

import com.switchplatform.platform.model.auth.RefreshTokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface RefreshTokenBlacklistRepository extends JpaRepository<RefreshTokenBlacklist, UUID> {

    boolean existsByTokenHash(String hash);

    @Modifying
    @Query("DELETE FROM RefreshTokenBlacklist r WHERE r.expiresAt < :now")
    int deleteExpired(@Param("now") OffsetDateTime now);
}
