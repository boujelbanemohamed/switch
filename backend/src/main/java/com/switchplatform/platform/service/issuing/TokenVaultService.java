package com.switchplatform.platform.service.issuing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TokenVaultService {

    private static final String DPAN_PREFIX = "5299";
    private static final int DPAN_LENGTH = 16;

    private final Map<UUID, TokenRecord> tokenStore = new ConcurrentHashMap<>();
    private final Map<String, UUID> dpanIndex = new ConcurrentHashMap<>();
    private final Map<String, List<UUID>> cardIndex = new ConcurrentHashMap<>();

    public record TokenRecord(
            UUID id,
            String cardId,
            String fpanSuffix,
            String dpan,
            String dpanSuffix,
            String walletProvider,
            String deviceId,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public TokenRecord tokenize(String cardId, String walletProvider, String deviceId) {
        String dpan = generateDpan();
        String dpanSuffix = dpan.substring(dpan.length() - 4);
        UUID tokenId = UUID.randomUUID();

        TokenRecord record = new TokenRecord(
                tokenId,
                cardId,
                null,
                dpan,
                dpanSuffix,
                walletProvider,
                deviceId,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        tokenStore.put(tokenId, record);
        dpanIndex.put(dpan, tokenId);
        cardIndex.computeIfAbsent(cardId, k -> List.of()).add(tokenId);

        log.info("Token created for card {} with DPAN suffix {} provider {}",
                cardId, dpanSuffix, walletProvider);
        return record;
    }

    public TokenRecord tokenizeWithFpan(String cardId, String fpan, String walletProvider, String deviceId) {
        String fpanSuffix = fpan.substring(fpan.length() - 4);
        String dpan = generateDpan();
        String dpanSuffix = dpan.substring(dpan.length() - 4);
        UUID tokenId = UUID.randomUUID();

        TokenRecord record = new TokenRecord(
                tokenId,
                cardId,
                fpanSuffix,
                dpan,
                dpanSuffix,
                walletProvider,
                deviceId,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        tokenStore.put(tokenId, record);
        dpanIndex.put(dpan, tokenId);
        cardIndex.computeIfAbsent(cardId, k -> List.of()).add(tokenId);

        log.info("Token created for card {} FPAN suffix {} DPAN suffix {} provider {}",
                cardId, fpanSuffix, dpanSuffix, walletProvider);
        return record;
    }

    public TokenRecord getToken(String tokenUuid) {
        UUID uuid;
        try {
            uuid = UUID.fromString(tokenUuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token UUID: " + tokenUuid);
        }
        TokenRecord record = tokenStore.get(uuid);
        if (record == null) {
            throw new IllegalArgumentException("Token not found: " + tokenUuid);
        }
        return record;
    }

    public TokenRecord getByDpan(String dpan) {
        UUID tokenId = dpanIndex.get(dpan);
        if (tokenId == null) {
            throw new IllegalArgumentException("DPAN not found: " + dpan);
        }
        TokenRecord record = tokenStore.get(tokenId);
        if (record == null) {
            throw new IllegalArgumentException("Token not found for DPAN: " + dpan);
        }
        return record;
    }

    public TokenRecord suspendToken(String dpan) {
        return updateStatus(dpan, "SUSPENDED");
    }

    public TokenRecord activateToken(String dpan) {
        return updateStatus(dpan, "ACTIVE");
    }

    public TokenRecord deleteToken(String dpan) {
        return updateStatus(dpan, "DELETED");
    }

    public List<TokenRecord> listByCard(String cardId) {
        List<UUID> tokenIds = cardIndex.get(cardId);
        if (tokenIds == null) {
            return List.of();
        }
        return tokenIds.stream()
                .map(tokenStore::get)
                .collect(Collectors.toList());
    }

    private TokenRecord updateStatus(String dpan, String newStatus) {
        TokenRecord record = getByDpan(dpan);
        TokenRecord updated = new TokenRecord(
                record.id(),
                record.cardId(),
                record.fpanSuffix(),
                record.dpan(),
                record.dpanSuffix(),
                record.walletProvider(),
                record.deviceId(),
                newStatus,
                record.createdAt(),
                Instant.now()
        );
        tokenStore.put(record.id(), updated);
        log.info("Token {} for DPAN suffix {} status changed to {}", record.id(), record.dpanSuffix(), newStatus);
        return updated;
    }

    private String generateDpan() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(DPAN_PREFIX);
        for (int i = DPAN_PREFIX.length(); i < DPAN_LENGTH - 1; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append(luhnCheckDigit(sb.toString()));
        return sb.toString();
    }

    private int luhnCheckDigit(String digits) {
        int sum = 0;
        boolean alternate = true;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }
}
