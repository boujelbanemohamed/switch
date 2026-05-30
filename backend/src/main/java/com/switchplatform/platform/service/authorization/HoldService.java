package com.switchplatform.platform.service.authorization;

import com.switchplatform.platform.model.authorization.HoldRecord;
import com.switchplatform.platform.service.issuing.CardAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HoldService {

    private final Map<UUID, HoldRecord> holds = new ConcurrentHashMap<>();
    private final Map<String, List<UUID>> cardIndex = new ConcurrentHashMap<>();
    private final Map<String, List<UUID>> accountIndex = new ConcurrentHashMap<>();

    private final CardAccountService cardAccountService;

    public HoldRecord placeHold(String transactionId, String cardId, String cardAccountId,
                                BigDecimal amount, String currencyCode, Duration expiryDuration) {
        UUID holdId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiryDuration != null ? expiryDuration : Duration.ofMinutes(30));

        cardAccountService.hold(UUID.fromString(cardAccountId), amount);

        HoldRecord record = HoldRecord.builder()
                .id(holdId)
                .transactionId(transactionId)
                .cardId(cardId)
                .cardAccountId(cardAccountId)
                .amount(amount)
                .currencyCode(currencyCode)
                .status("ACTIVE")
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        holds.put(holdId, record);
        cardIndex.computeIfAbsent(cardId, k -> new CopyOnWriteArrayList<>()).add(holdId);
        accountIndex.computeIfAbsent(cardAccountId, k -> new CopyOnWriteArrayList<>()).add(holdId);

        log.info("Hold placed: id={}, cardId={}, amount={}, expiresAt={}", holdId, cardId, amount, expiresAt);
        return record;
    }

    public boolean releaseHold(UUID holdId) {
        HoldRecord record = holds.get(holdId);
        if (record == null || !"ACTIVE".equals(record.getStatus())) {
            log.warn("Cannot release hold: id={}, found={}, status={}", holdId, record != null, record != null ? record.getStatus() : "N/A");
            return false;
        }

        try {
            cardAccountService.releaseHold(UUID.fromString(record.getCardAccountId()), record.getAmount());
            record.setStatus("RELEASED");
            record.setReleasedAt(Instant.now());
            log.info("Hold released: id={}, accountId={}, amount={}", holdId, record.getCardAccountId(), record.getAmount());
            return true;
        } catch (Exception e) {
            log.error("Failed to release hold: id={}, error={}", holdId, e.getMessage());
            return false;
        }
    }

    public boolean captureHold(UUID holdId) {
        HoldRecord record = holds.get(holdId);
        if (record == null || !"ACTIVE".equals(record.getStatus())) {
            log.warn("Cannot capture hold: id={}, found={}, status={}", holdId, record != null, record != null ? record.getStatus() : "N/A");
            return false;
        }

        try {
            UUID accountId = UUID.fromString(record.getCardAccountId());
            cardAccountService.releaseHold(accountId, record.getAmount());
            cardAccountService.debit(accountId, record.getAmount(), record.getCurrencyCode());
            record.setStatus("CAPTURED");
            record.setReleasedAt(Instant.now());
            log.info("Hold captured: id={}, accountId={}, amount={}", holdId, accountId, record.getAmount());
            return true;
        } catch (Exception e) {
            log.error("Failed to capture hold: id={}, error={}", holdId, e.getMessage());
            return false;
        }
    }

    public void expireHolds() {
        Instant now = Instant.now();
        List<UUID> expired = holds.values().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()) && r.getExpiresAt().isBefore(now))
                .map(HoldRecord::getId)
                .toList();

        for (UUID id : expired) {
            try {
                HoldRecord record = holds.get(id);
                cardAccountService.releaseHold(UUID.fromString(record.getCardAccountId()), record.getAmount());
                record.setStatus("EXPIRED");
                record.setReleasedAt(Instant.now());
                log.info("Hold expired: id={}, accountId={}, amount={}", id, record.getCardAccountId(), record.getAmount());
            } catch (Exception e) {
                log.error("Failed to expire hold: id={}, error={}", id, e.getMessage());
            }
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} holds", expired.size());
        }
    }

    public Optional<HoldRecord> getHold(UUID holdId) {
        return Optional.ofNullable(holds.get(holdId));
    }

    public List<HoldRecord> getActiveHoldsForCard(String cardId) {
        List<UUID> ids = cardIndex.get(cardId);
        if (ids == null) return List.of();
        return ids.stream()
                .map(holds::get)
                .filter(Objects::nonNull)
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .collect(Collectors.toList());
    }

    public List<HoldRecord> getActiveHoldsForAccount(String accountId) {
        List<UUID> ids = accountIndex.get(accountId);
        if (ids == null) return List.of();
        return ids.stream()
                .map(holds::get)
                .filter(Objects::nonNull)
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .collect(Collectors.toList());
    }
}
