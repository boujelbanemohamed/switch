package com.switchplatform.platform.service.fraud;

import com.switchplatform.platform.model.fraud.DeviceFingerprintRecord;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeviceFingerprintService {

    private final Map<UUID, DeviceFingerprintRecord> fingerprints = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> cardIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> deviceIndex = new ConcurrentHashMap<>();

    private final Set<String> highRiskUserAgents = ConcurrentHashMap.newKeySet();

    @PostConstruct
    void initHighRiskPatterns() {
        highRiskUserAgents.addAll(Set.of(
                "HeadlessChrome", "PhantomJS", "python-requests", "curl", "Puppeteer", "Selenium"
        ));
    }

    public DeviceFingerprintRecord registerFingerprint(
            String cardId, String deviceId, String deviceType, String os,
            String browser, String userAgent, String ipAddress,
            Map<String, String> attributes) {

        Optional<DeviceFingerprintRecord> existing = findByCardAndDevice(cardId, deviceId);
        if (existing.isPresent()) {
            DeviceFingerprintRecord record = existing.get();
            record.setIpAddress(ipAddress);
            record.setOs(os);
            record.setBrowser(browser);
            record.setUserAgent(userAgent);
            record.setDeviceType(deviceType);
            if (attributes != null) {
                record.setAttributes(attributes);
            }
            record.setUsageCount(record.getUsageCount() + 1);
            record.setLastSeen(OffsetDateTime.now());
            log.debug("Updated device fingerprint: cardId={}, deviceId={}, usageCount={}",
                    cardId, deviceId, record.getUsageCount());
            return record;
        }

        String country = resolveCountryFromIp(ipAddress);

        DeviceFingerprintRecord record = DeviceFingerprintRecord.builder()
                .id(UUID.randomUUID())
                .cardId(cardId)
                .deviceId(deviceId)
                .deviceType(deviceType != null ? deviceType : "UNKNOWN")
                .os(os)
                .browser(browser)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .country(country)
                .attributes(attributes != null ? attributes : Map.of())
                .usageCount(1)
                .firstSeen(OffsetDateTime.now())
                .lastSeen(OffsetDateTime.now())
                .build();

        fingerprints.put(record.getId(), record);
        cardIndex.computeIfAbsent(cardId, k -> ConcurrentHashMap.newKeySet()).add(record.getId());
        deviceIndex.computeIfAbsent(deviceId, k -> ConcurrentHashMap.newKeySet()).add(record.getId());

        log.info("Registered device fingerprint: cardId={}, deviceId={}, type={}, country={}",
                cardId, deviceId, deviceType, country);
        return record;
    }

    public List<DeviceFingerprintRecord> getFingerprintsForCard(String cardId) {
        Set<UUID> ids = cardIndex.get(cardId);
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .map(fingerprints::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(DeviceFingerprintRecord::getLastSeen).reversed())
                .collect(Collectors.toList());
    }

    public boolean isKnownDevice(String cardId, String deviceId) {
        return findByCardAndDevice(cardId, deviceId).isPresent();
    }

    public double scoreDevice(String cardId, String deviceId, String ipAddress) {
        Optional<DeviceFingerprintRecord> knownOpt = findByCardAndDevice(cardId, deviceId);

        if (knownOpt.isPresent()) {
            DeviceFingerprintRecord known = knownOpt.get();
            boolean sameIp = known.getIpAddress() != null && known.getIpAddress().equals(ipAddress);
            if (sameIp) return 0.0;
            return 0.3;
        }

        boolean deviceUsedByOtherCard = deviceIndex.containsKey(deviceId)
                && deviceIndex.get(deviceId).stream()
                        .map(fingerprints::get)
                        .filter(Objects::nonNull)
                        .anyMatch(r -> !r.getCardId().equals(cardId));
        if (deviceUsedByOtherCard) return 0.5;

        String country = resolveCountryFromIp(ipAddress);
        boolean knownCountry = cardIndex.getOrDefault(cardId, Set.of()).stream()
                .map(fingerprints::get)
                .filter(Objects::nonNull)
                .anyMatch(r -> r.getCountry() != null && r.getCountry().equals(country));
        if (knownCountry) return 0.7;

        return 0.9;
    }

    public DeviceScoreResult evaluate(
            String cardId, String deviceId, String deviceType, String os,
            String browser, String userAgent, String ipAddress) {

        List<String> reasons = new ArrayList<>();

        DeviceFingerprintRecord registered = registerFingerprint(
                cardId, deviceId, deviceType, os, browser, userAgent, ipAddress, null);

        boolean knownDevice = registered.getUsageCount() > 1;
        boolean suspiciousUserAgent = isSuspiciousUserAgent(userAgent);
        String country = resolveCountryFromIp(ipAddress);
        boolean newCountry = !knownDevice && isNewCountryForCard(cardId, country);

        double baseScore = scoreDevice(cardId, deviceId, ipAddress);
        double finalScore = baseScore;

        if (suspiciousUserAgent) {
            finalScore = Math.max(finalScore, 1.0);
            reasons.add("Suspicious user-agent: " + userAgent);
        }

        if (newCountry) {
            reasons.add("New country for card: " + country);
        }

        if (!knownDevice) {
            reasons.add("New device for card: " + deviceId);
        }

        String recommendation;
        if (finalScore >= 1.0) {
            recommendation = "BLOCK";
        } else if (finalScore >= 0.7) {
            recommendation = "CHALLENGE";
        } else if (finalScore >= 0.3) {
            recommendation = "FLAG";
        } else {
            recommendation = "ALLOW";
        }

        log.info("Device evaluation: cardId={}, deviceId={}, score={}, recommendation={}, reasons={}",
                cardId, deviceId, finalScore, recommendation, reasons);

        return DeviceScoreResult.builder()
                .score(finalScore)
                .isKnownDevice(knownDevice)
                .isSuspiciousUserAgent(suspiciousUserAgent)
                .isNewCountry(newCountry)
                .recommendation(recommendation)
                .reasons(reasons)
                .build();
    }

    private Optional<DeviceFingerprintRecord> findByCardAndDevice(String cardId, String deviceId) {
        Set<UUID> ids = cardIndex.get(cardId);
        if (ids == null) return Optional.empty();
        return ids.stream()
                .map(fingerprints::get)
                .filter(r -> r != null && r.getDeviceId().equals(deviceId))
                .findFirst();
    }

    private boolean isSuspiciousUserAgent(String userAgent) {
        if (userAgent == null) return false;
        String ua = userAgent.toLowerCase();
        return highRiskUserAgents.stream().anyMatch(pattern ->
                ua.contains(pattern.toLowerCase()));
    }

    private boolean isNewCountryForCard(String cardId, String country) {
        if (country == null) return false;
        return cardIndex.getOrDefault(cardId, Set.of()).stream()
                .map(fingerprints::get)
                .filter(Objects::nonNull)
                .noneMatch(r -> country.equals(r.getCountry()));
    }

    String resolveCountryFromIp(String ipAddress) {
        if (ipAddress == null) return null;
        if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.")
                || ipAddress.startsWith("172.")) {
            return null;
        }
        return null;
    }
}
