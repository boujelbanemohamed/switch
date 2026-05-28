package com.switchplatform.platform.service.fraud;

import com.switchplatform.platform.model.fraud.BehavioralProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BehavioralProfileService {

    private final Map<UUID, BehavioralProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, List<TransactionRecord>> transactionHistory = new ConcurrentHashMap<>();

    private static final int HISTORY_SIZE = 200;
    private static final BigDecimal ANOMALY_MULTIPLIER = new BigDecimal("3.0");
    private static final int ANOMALY_MIN_SAMPLES = 5;

    public BehavioralProfile getOrCreateProfile(UUID cardholderId) {
        return profiles.computeIfAbsent(cardholderId, id -> {
            BehavioralProfile profile = BehavioralProfile.builder()
                    .id(UUID.randomUUID())
                    .cardholderId(id)
                    .avgTransactionAmount(BigDecimal.ZERO)
                    .avgTransactionsPerDay(BigDecimal.ZERO)
                    .typicalMerchantCategories(new String[0])
                    .typicalCountries(new String[0])
                    .typicalHours(new Integer[0])
                    .typicalDays(new Integer[0])
                    .riskScore(0)
                    .modelVersion("1.0")
                    .profileData("{}")
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            log.info("Created behavioral profile: cardholderId={}", id);
            return profile;
        });
    }

    public void recordTransaction(UUID cardholderId, String merchantCategory, String countryCode,
                                  BigDecimal amount, LocalDateTime timestamp) {
        List<TransactionRecord> history = transactionHistory
                .computeIfAbsent(cardholderId, k -> new CopyOnWriteArrayList<>());

        TransactionRecord record = TransactionRecord.builder()
                .merchantCategory(merchantCategory)
                .countryCode(countryCode)
                .amount(amount)
                .timestamp(timestamp)
                .build();
        history.add(record);

        while (history.size() > HISTORY_SIZE) {
            history.remove(0);
        }

        updateProfile(cardholderId);
    }

    private void updateProfile(UUID cardholderId) {
        List<TransactionRecord> history = transactionHistory.get(cardholderId);
        if (history == null || history.isEmpty()) return;

        BehavioralProfile profile = getOrCreateProfile(cardholderId);

        BigDecimal avgAmount = history.stream()
                .map(TransactionRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size()), 3, RoundingMode.HALF_UP);

        Map<LocalDate, Long> txnsByDay = history.stream()
                .map(r -> r.getTimestamp().toLocalDate())
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));
        BigDecimal avgPerDay = BigDecimal.valueOf(history.size())
                .divide(BigDecimal.valueOf(Math.max(txnsByDay.size(), 1)), 2, RoundingMode.HALF_UP);

        Set<String> categories = history.stream()
                .map(TransactionRecord::getMerchantCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> countries = history.stream()
                .map(TransactionRecord::getCountryCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Integer> hours = history.stream()
                .map(r -> r.getTimestamp().getHour())
                .collect(Collectors.toSet());
        Set<Integer> days = history.stream()
                .map(r -> r.getTimestamp().getDayOfWeek().getValue())
                .collect(Collectors.toSet());

        profile.setAvgTransactionAmount(avgAmount);
        profile.setAvgTransactionsPerDay(avgPerDay);
        profile.setTypicalMerchantCategories(categories.toArray(new String[0]));
        profile.setTypicalCountries(countries.toArray(new String[0]));
        profile.setTypicalHours(hours.toArray(new Integer[0]));
        profile.setTypicalDays(days.toArray(new Integer[0]));
        profile.setLastUpdated(OffsetDateTime.now());
        profile.setUpdatedAt(OffsetDateTime.now());

        log.debug("Behavioral profile updated: cardholderId={}, avgAmount={}, avgTxnsPerDay={}",
                cardholderId, avgAmount, avgPerDay);
    }

    public AnomalyResult detectAnomalies(UUID cardholderId, BigDecimal amount,
                                          String merchantCategory, String countryCode,
                                          LocalDateTime timestamp) {
        BehavioralProfile profile = profiles.get(cardholderId);
        if (profile == null) {
            return new AnomalyResult(0, Collections.emptyList());
        }

        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (amount != null && profile.getAvgTransactionAmount() != null
                && profile.getAvgTransactionAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = amount.divide(profile.getAvgTransactionAmount(), 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(ANOMALY_MULTIPLIER) > 0) {
                score += 25;
                reasons.add("AMOUNT_ANOMALY: " + amount + " is " + ratio + "x avg "
                        + profile.getAvgTransactionAmount());
            }
        }

        if (merchantCategory != null && profile.getTypicalMerchantCategories() != null
                && profile.getTypicalMerchantCategories().length >= ANOMALY_MIN_SAMPLES) {
            boolean known = Arrays.asList(profile.getTypicalMerchantCategories()).contains(merchantCategory);
            if (!known) {
                score += 15;
                reasons.add("UNUSUAL_MERCHANT_CATEGORY: " + merchantCategory);
            }
        }

        if (countryCode != null && profile.getTypicalCountries() != null
                && profile.getTypicalCountries().length >= ANOMALY_MIN_SAMPLES) {
            boolean known = Arrays.asList(profile.getTypicalCountries()).contains(countryCode);
            if (!known) {
                score += 20;
                reasons.add("UNUSUAL_COUNTRY: " + countryCode);
            }
        }

        if (timestamp != null && profile.getTypicalHours() != null
                && profile.getTypicalHours().length >= ANOMALY_MIN_SAMPLES) {
            int hour = timestamp.getHour();
            boolean knownHour = Arrays.asList(profile.getTypicalHours()).contains(hour);
            if (!knownHour) {
                score += 10;
                reasons.add("UNUSUAL_HOUR: " + hour);
            }
        }

        if (timestamp != null && profile.getTypicalDays() != null
                && profile.getTypicalDays().length >= ANOMALY_MIN_SAMPLES) {
            int day = timestamp.getDayOfWeek().getValue();
            boolean knownDay = Arrays.asList(profile.getTypicalDays()).contains(day);
            if (!knownDay) {
                score += 5;
                reasons.add("UNUSUAL_DAY: " + DayOfWeek.of(day));
            }
        }

        return new AnomalyResult(Math.min(score, 75), reasons);
    }

    public Optional<BehavioralProfile> getProfile(UUID cardholderId) {
        return Optional.ofNullable(profiles.get(cardholderId));
    }

    public List<BehavioralProfile> listAllProfiles() {
        return profiles.values().stream()
                .sorted(Comparator.comparing(BehavioralProfile::getUpdatedAt).reversed())
                .collect(Collectors.toList());
    }

    public void updateRiskScore(UUID cardholderId, int score) {
        BehavioralProfile profile = getOrCreateProfile(cardholderId);
        profile.setRiskScore(score);
        profile.setUpdatedAt(OffsetDateTime.now());
    }

    @lombok.Data
    @lombok.Builder
    public static class AnomalyResult {
        private final int score;
        private final List<String> reasons;
    }

    @lombok.Data
    @lombok.Builder
    public static class TransactionRequest {
        private String merchantCategory;
        private String countryCode;
        private BigDecimal amount;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    private static class TransactionRecord {
        private String merchantCategory;
        private String countryCode;
        private BigDecimal amount;
        private LocalDateTime timestamp;
    }
}
