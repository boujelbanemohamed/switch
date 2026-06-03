package com.switchplatform.platform.model.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_token_blacklist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "revoked_at", nullable = false)
    private OffsetDateTime revokedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
}
