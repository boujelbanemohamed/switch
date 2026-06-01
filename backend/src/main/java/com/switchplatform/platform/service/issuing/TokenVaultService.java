package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.WalletToken;
import com.switchplatform.platform.repository.issuing.WalletTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenVaultService {

    private final WalletTokenRepository walletTokenRepository;

    @Transactional
    public WalletToken storeToken(UUID cardId, String pan, String walletProvider,
            String deviceId, String deviceName) {
        String dpan = generateDpan(walletProvider);
        WalletToken token = WalletToken.builder()
                .cardId(cardId)
                .token(dpan)
                .tokenType(WalletToken.TokenType.DEVICE)
                .walletProvider(WalletToken.WalletProvider.valueOf(walletProvider))
                .deviceId(deviceId)
                .deviceName(deviceName)
                .status(WalletToken.TokenStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();
        log.info("Token stored for card {} wallet {}", cardId, walletProvider);
        return walletTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public Optional<WalletToken> getTokenByDpan(String dpan) {
        return walletTokenRepository.findByToken(dpan);
    }

    @Transactional(readOnly = true)
    public List<WalletToken> getTokensForCard(UUID cardId) {
        return walletTokenRepository.findByCardIdOrderByCreatedAtDesc(cardId);
    }

    @Transactional
    public void revokeToken(String dpan) {
        walletTokenRepository.findByToken(dpan).ifPresent(t -> {
            t.setStatus(WalletToken.TokenStatus.SUSPENDED);
            walletTokenRepository.save(t);
        });
    }

    private String generateDpan(String walletProvider) {
        String prefix = switch (walletProvider) {
            case "APPLE_PAY"   -> "4770";
            case "GOOGLE_PAY"  -> "4895";
            case "SAMSUNG_PAY" -> "4896";
            default            -> "4800";
        };
        String suffix = String.format("%012d",
            (long)(Math.random() * 999999999999L));
        return prefix + suffix;
    }
}
