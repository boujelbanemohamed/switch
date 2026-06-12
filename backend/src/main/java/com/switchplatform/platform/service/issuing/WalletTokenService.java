package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.WalletToken;
import com.switchplatform.platform.repository.issuing.WalletTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletTokenService {

    private final WalletTokenRepository walletTokenRepository;

    @Transactional
    public WalletToken tokenizeCard(UUID cardId, String walletProvider, String deviceId) {
        WalletToken.WalletProvider provider;
        try {
            provider = WalletToken.WalletProvider.valueOf(walletProvider);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid wallet provider: " + walletProvider);
        }

        WalletToken token = WalletToken.builder()
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

        walletTokenRepository.save(token);
        log.info("Tokenized card {} with provider {} token {}", cardId, walletProvider, token.getId());
        return token;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> detokenize(String token) {
        return walletTokenRepository.findByToken(token)
                .filter(t -> t.getStatus() == WalletToken.TokenStatus.ACTIVE)
                .map(WalletToken::getCardId);
    }

    @Transactional
    public Optional<WalletToken> suspendToken(String token) {
        return walletTokenRepository.findByToken(token).map(t -> {
            t.setStatus(WalletToken.TokenStatus.SUSPENDED);
            t.setUpdatedAt(OffsetDateTime.now());
            WalletToken saved = walletTokenRepository.save(t);
            log.info("Suspended token {} for card {}", saved.getId(), saved.getCardId());
            return saved;
        });
    }

    @Transactional
    public Optional<WalletToken> terminateToken(String token) {
        return walletTokenRepository.findByToken(token).map(t -> {
            t.setStatus(WalletToken.TokenStatus.TERMINATED);
            t.setUpdatedAt(OffsetDateTime.now());
            WalletToken saved = walletTokenRepository.save(t);
            log.info("Terminated token {} for card {}", saved.getId(), saved.getCardId());
            return saved;
        });
    }

    @Transactional(readOnly = true)
    public List<WalletToken> getTokensByCard(UUID cardId) {
        return walletTokenRepository.findByCardIdOrderByCreatedAtDesc(cardId);
    }

    @Transactional(readOnly = true)
    public Optional<WalletToken> getToken(UUID tokenId) {
        return walletTokenRepository.findById(tokenId);
    }

    private Optional<WalletToken> findTokenByValue(String token) {
        return walletTokenRepository.findByToken(token);
    }

    private String generateTokenValue() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
