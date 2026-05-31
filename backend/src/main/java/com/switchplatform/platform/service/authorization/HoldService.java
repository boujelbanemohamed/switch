package com.switchplatform.platform.service.authorization;

import com.switchplatform.platform.model.authorization.HoldRecord;
import com.switchplatform.platform.repository.authorization.HoldRecordRepository;
import com.switchplatform.platform.service.issuing.CardAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HoldService {

    private final HoldRecordRepository holdRecordRepository;
    private final CardAccountService cardAccountService;

    @Transactional
    public HoldRecord placeHold(String transactionId, String cardId, String cardAccountId,
                                BigDecimal amount, String currencyCode, Duration expiryDuration) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiryDuration != null ? expiryDuration : Duration.ofMinutes(30));

        cardAccountService.hold(UUID.fromString(cardAccountId), amount);

        HoldRecord record = HoldRecord.builder()
                .id(UUID.randomUUID())
                .transactionId(transactionId)
                .cardId(cardId)
                .cardAccountId(cardAccountId)
                .amount(amount)
                .currencyCode(currencyCode)
                .status("ACTIVE")
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        HoldRecord saved = holdRecordRepository.save(record);
        log.info("Hold placed: id={}, cardId={}, amount={}, expiresAt={}", saved.getId(), cardId, amount, expiresAt);
        return saved;
    }

    @Transactional
    public boolean releaseHold(UUID holdId) {
        HoldRecord record = holdRecordRepository.findById(holdId).orElse(null);
        if (record == null || !"ACTIVE".equals(record.getStatus())) {
            log.warn("Cannot release hold: id={}, found={}, status={}", holdId, record != null, record != null ? record.getStatus() : "N/A");
            return false;
        }

        try {
            cardAccountService.releaseHold(UUID.fromString(record.getCardAccountId()), record.getAmount());
            record.setStatus("RELEASED");
            record.setReleasedAt(Instant.now());
            holdRecordRepository.save(record);
            log.info("Hold released: id={}, accountId={}, amount={}", holdId, record.getCardAccountId(), record.getAmount());
            return true;
        } catch (Exception e) {
            log.error("Failed to release hold: id={}, error={}", holdId, e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean captureHold(UUID holdId) {
        HoldRecord record = holdRecordRepository.findById(holdId).orElse(null);
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
            holdRecordRepository.save(record);
            log.info("Hold captured: id={}, accountId={}, amount={}", holdId, accountId, record.getAmount());
            return true;
        } catch (Exception e) {
            log.error("Failed to capture hold: id={}, error={}", holdId, e.getMessage());
            return false;
        }
    }

    @Scheduled(fixedRateString = "${switch.holds.expiry-check-ms:60000}")
    @Transactional
    public void expireHolds() {
        Instant now = Instant.now();
        List<HoldRecord> expired = holdRecordRepository.findByStatusAndExpiresAtBefore("ACTIVE", now);

        for (HoldRecord record : expired) {
            try {
                cardAccountService.releaseHold(UUID.fromString(record.getCardAccountId()), record.getAmount());
                record.setStatus("EXPIRED");
                record.setReleasedAt(Instant.now());
                holdRecordRepository.save(record);
                log.info("Hold expired: id={}, accountId={}, amount={}", record.getId(), record.getCardAccountId(), record.getAmount());
            } catch (Exception e) {
                log.error("Failed to expire hold: id={}, error={}", record.getId(), e.getMessage());
            }
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} holds", expired.size());
        }
    }

    @Transactional(readOnly = true)
    public Optional<HoldRecord> getHold(UUID holdId) {
        return holdRecordRepository.findById(holdId);
    }

    @Transactional(readOnly = true)
    public List<HoldRecord> getActiveHoldsForCard(String cardId) {
        return holdRecordRepository.findByCardIdAndStatus(cardId, "ACTIVE");
    }

    @Transactional(readOnly = true)
    public List<HoldRecord> getActiveHoldsForAccount(String accountId) {
        return holdRecordRepository.findByCardAccountIdAndStatus(accountId, "ACTIVE");
    }
}
