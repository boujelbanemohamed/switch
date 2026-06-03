package com.switchplatform.platform.service.auth;

import com.switchplatform.platform.model.auth.RefreshTokenBlacklist;
import com.switchplatform.platform.repository.auth.RefreshTokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RefreshTokenBlacklistRepository repository;

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Transactional
    public void revoke(String rawToken) {
        String tokenHash = hash(rawToken);
        if (repository.existsByTokenHash(tokenHash)) {
            return;
        }
        RefreshTokenBlacklist entry = RefreshTokenBlacklist.builder()
                .tokenHash(tokenHash)
                .username("unknown")
                .revokedAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusDays(30))
                .build();
        repository.save(entry);
        log.debug("Token revoked: hash={}", tokenHash.substring(0, 8));
    }

    public boolean isRevoked(String rawToken) {
        String tokenHash = hash(rawToken);
        return repository.existsByTokenHash(tokenHash);
    }

    @Transactional
    @Scheduled(fixedRate = 3600000)
    public void purgeExpired() {
        int deleted = repository.deleteExpired(OffsetDateTime.now());
        if (deleted > 0) {
            log.info("Purged {} expired token blacklist entries", deleted);
        }
    }
}
