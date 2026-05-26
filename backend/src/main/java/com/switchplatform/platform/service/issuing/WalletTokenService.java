package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.WalletToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletTokenService {

    private final ConcurrentMap<UUID, WalletToken> tokenStore = new ConcurrentHashMap<>();

    @Transactional
    public WalletToken tokenizeCard(UUID cardId, String walletProvider, String deviceId) {
        WalletToken.WalletProvider provider;
        try {
            provider = WalletToken.WalletProvider.valueOf(walletProvider);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid wallet provider: " + walletProvider);
        }

        WalletToken token = WalletToken.builder()
                .id(UUID.randomUUID())
                .cardId(cardId)
                .token(generateTokenValue())
                .tokenType(WalletToken.TokenType.DEVICE)
                .walletProvider(provider)
                .deviceId(deviceId)
                .tokenExpiry(LocalDate.now().plusYears(2))
                .status(WalletToken.TokenStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        tokenStore.put(token.getId(), token);
        log.info("Tokenized card {} with provider {} token {}", cardId, walletProvider, token.getId());
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> detokenize(String token) {
        return tokenStore.values().stream()
                .filter(t -> t.getToken().equals(token) && t.getStatus() == WalletToken.TokenStatus.ACTIVE)
                .map(WalletToken::getCardId)
                .findFirst();
    }

    @Transactional
    public Optional<WalletToken> suspendToken(String token) {
        return findTokenByValue(token).map(t -> {
            t.setStatus(WalletToken.TokenStatus.SUSPENDED);
            t.setUpdatedAt(OffsetDateTime.now());
            log.info("Suspended token {} for card {}", t.getId(), t.getCardId());
            return t;
        });
    }

    @Transactional
    public Optional<WalletToken> terminateToken(String token) {
        return findTokenByValue(token).map(t -> {
            t.setStatus(WalletToken.TokenStatus.TERMINATED);
            t.setUpdatedAt(OffsetDateTime.now());
            log.info("Terminated token {} for card {}", t.getId(), t.getCardId());
            return t;
        });
    }

    @Transactional(readOnly = true)
    public List<WalletToken> getTokensByCard(UUID cardId) {
        return tokenStore.values().stream()
                .filter(t -> t.getCardId().equals(cardId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<WalletToken> getToken(UUID tokenId) {
        return Optional.ofNullable(tokenStore.get(tokenId));
    }

    private Optional<WalletToken> findTokenByValue(String token) {
        return tokenStore.values().stream()
                .filter(t -> t.getToken().equals(token))
                .findFirst();
    }

    private String generateTokenValue() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
