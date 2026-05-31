package com.switchplatform.platform.service.fraud;

import com.switchplatform.platform.model.fraud.BehavioralProfile;
import com.switchplatform.platform.repository.fraud.BehavioralProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BehavioralProfileService {

    private final BehavioralProfileRepository behavioralProfileRepository;
    private final ConcurrentMap<UUID, List<TransactionRecord>> transactionHistory = new ConcurrentHashMap<>();

    private static final int HISTORY_SIZE = 200;
    private static final BigDecimal ANOMALY_MULTIPLIER = new BigDecimal("3.0");
    private static final int ANOMALY_MIN_SAMPLES = 5;

    @Value("${switch.fraud.time-window-hours:24}")
    private int timeWindowHours;

    @Value("${switch.fraud.velocity-max-per-hour:10}")
    private int velocityMaxPerHour;

    @Transactional
    public BehavioralProfile getOrCreateProfile(UUID cardholderId) {
        return behavioralProfileRepository.findByCardholderId(cardholderId).orElseGet(() -> {
            BehavioralProfile profile = BehavioralProfile.builder()
                    .cardholderId(cardholderId)
                    .avgTransactionAmount(BigDecimal.ZERO)
                    .avgTransactionsPerDay(BigDecimal.ZERO)
                    .typicalMerchantCategories(new String[0])
                    .typicalCountries(new String[0])
                    .typicalHours(new Integer[0])
                    .typicalDays(new Integer[0])
                    .riskScore(0)
                    .modelVersion("1.0")
                    .profileData("{}")
                    .build();
            profile = behavioralProfileRepository.save(profile);
            log.info("Created behavioral profile: cardholderId={}", cardholderId);
            return profile;
        });
    }

    @Transactional
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

        behavioralProfileRepository.save(profile);

        log.debug("Behavioral profile updated: cardholderId={}, avgAmount={}, avgTxnsPerDay={}",
                cardholderId, avgAmount, avgPerDay);
    }

    @Transactional(readOnly = true)
    public TimeWindowAnalysis analyzeTimeWindow(UUID cardholderId) {
        List<TransactionRecord> history = transactionHistory.get(cardholderId);
        if (history == null || history.isEmpty()) {
            return new TimeWindowAnalysis(0, 0, 0, 0, 0, Collections.emptyList());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime window24h = now.minusHours(24);
        LocalDateTime window7d = now.minusDays(7);
        LocalDateTime window30d = now.minusDays(30);

        long count24h = history.stream().filter(r -> r.getTimestamp().isAfter(window24h)).count();
        long count7d = history.stream().filter(r -> r.getTimestamp().isAfter(window7d)).count();
        long count30d = history.stream().filter(r -> r.getTimestamp().isAfter(window30d)).count();

        BigDecimal sum24h = history.stream()
                .filter(r -> r.getTimestamp().isAfter(window24h))
                .map(TransactionRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal sum7d = history.stream()
                .filter(r -> r.getTimestamp().isAfter(window7d))
                .map(TransactionRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double txPerHour = count24h > 0 ? (double) count24h / Math.max(24, timeWindowHours) : 0;

        List<String> flags = new ArrayList<>();
        if (txPerHour > velocityMaxPerHour) {
            flags.add("HIGH_VELOCITY: " + String.format("%.1f", txPerHour) + " tx/hour (max " + velocityMaxPerHour + ")");
        }

        BehavioralProfile profile = behavioralProfileRepository.findByCardholderId(cardholderId).orElse(null);
        if (profile != null) {
            if (count7d > 0 && profile.getAvgTransactionsPerDay() != null
                    && profile.getAvgTransactionsPerDay().compareTo(BigDecimal.ZERO) > 0) {
                double expected7d = profile.getAvgTransactionsPerDay().doubleValue() * 7;
                if (count7d > expected7d * 2) {
                    flags.add("VOLUME_ANOMALY_7D: " + count7d + " tx (expected ~" + String.format("%.0f", expected7d) + ")");
                }
            }
        }

        return new TimeWindowAnalysis(count24h, count7d, count30d, sum24h.doubleValue(), sum7d.doubleValue(), flags);
    }

    @Transactional(readOnly = true)
    public AnomalyResult detectAnomalies(UUID cardholderId, BigDecimal amount,
                                          String merchantCategory, String countryCode,
                                          LocalDateTime timestamp) {
        BehavioralProfile profile = behavioralProfileRepository.findByCardholderId(cardholderId).orElse(null);
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

        TimeWindowAnalysis windowAnalysis = analyzeTimeWindow(cardholderId);
        if (windowAnalysis.getCount24h() > 0) {
            double txPerHour = (double) windowAnalysis.getCount24h() / Math.max(24, timeWindowHours);
            if (txPerHour > velocityMaxPerHour) {
                score += 15;
                reasons.add("HIGH_VELOCITY: " + String.format("%.1f", txPerHour) + " tx/hour");
            }
        }
        for (String flag : windowAnalysis.getFlags()) {
            if (!flag.startsWith("HIGH_VELOCITY")) {
                score += 10;
                reasons.add(flag);
            }
        }

        return new AnomalyResult(Math.min(score, 75), reasons);
    }

    @Transactional(readOnly = true)
    public Optional<BehavioralProfile> getProfile(UUID cardholderId) {
        return behavioralProfileRepository.findByCardholderId(cardholderId);
    }

    @Transactional(readOnly = true)
    public List<BehavioralProfile> listAllProfiles() {
        return behavioralProfileRepository.findAll().stream()
                .sorted(Comparator.comparing(BehavioralProfile::getUpdatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateRiskScore(UUID cardholderId, int score) {
        BehavioralProfile profile = getOrCreateProfile(cardholderId);
        profile.setRiskScore(score);
        profile.setUpdatedAt(OffsetDateTime.now());
        behavioralProfileRepository.save(profile);
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

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class TimeWindowAnalysis {
        private long count24h;
        private long count7d;
        private long count30d;
        private double sum24h;
        private double sum7d;
        private List<String> flags;
    }
}
