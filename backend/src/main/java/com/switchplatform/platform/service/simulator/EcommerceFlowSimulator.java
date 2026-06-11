package com.switchplatform.platform.service.simulator;

import com.switchplatform.platform.model.ecommerce.AcsAuthentication;
import com.switchplatform.platform.model.ecommerce.AcsChallenge;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import com.switchplatform.platform.model.ecommerce.ThreeDsSession;
import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.repository.ecommerce.AcsAuthenticationRepository;
import com.switchplatform.platform.repository.ecommerce.AcsChallengeRepository;
import com.switchplatform.platform.service.ecommerce.AcsService;
import com.switchplatform.platform.service.ecommerce.EpgService;
import com.switchplatform.platform.service.ecommerce.ThreeDsService;
import com.switchplatform.platform.repository.authorization.VelocityCheckRepository;
import com.switchplatform.platform.service.issuing.CardService;
import com.switchplatform.platform.service.fraud.DeviceFingerprintService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcommerceFlowSimulator {

    private final EpgService epgService;
    private final ThreeDsService threeDsService;
    private final AcsService acsService;
    private final CardService cardService;
    private final SimulatedDirectoryServer directoryServer;
    private final AcsAuthenticationRepository acsAuthenticationRepository;
    private final AcsChallengeRepository acsChallengeRepository;
    private final DeviceFingerprintService deviceFingerprintService;
    private final VelocityCheckRepository velocityCheckRepository;

    public SimulationResult simulateFrictionlessFlow(SimulationRequest request) {
        log.info("=== Ecommerce Flow Simulator: starting frictionless flow ===");
        log.info("cardId={}, merchantId={}, amount={} {}",
                request.getCardId(), request.getMerchantId(),
                request.getAmount(), request.getCurrencyCode());

        // 1. Validate card exists
        Card card = cardService.getCard(request.getCardId())
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + request.getCardId()));

        // 2. Initiate EPG transaction
        EpgTransaction txn = epgService.initiateTransaction(
                request.getMerchantId(),
                request.getMerchantTransactionId(),
                request.getAmount(),
                request.getCurrencyCode());
        log.info("EPG transaction initiated: id={}", txn.getId());

        // 3. Create 3DS session
        ThreeDsSession session = threeDsService.createSession(
                request.getMerchantTransactionId(),
                txn.getId(),
                request.getCardId(),
                null);
        log.info("3DS session created: id={}", session.getId());

        // 4. Directory Server simulates routing to ACS
        SimulatedDirectoryServer.DsRoutingResult routing = directoryServer.routeForCard(
                request.getCardId(),
                request.getMerchantTransactionId(),
                null,
                session.getId(),
                request.getMerchantId());

        // 5. Create ACS authentication (DS -> ACS AReq)
        AcsAuthentication auth = acsService.createAuthentication(
                request.getMerchantTransactionId(),
                request.getCardId(),
                request.getMerchantId(),
                request.getAmount(),
                request.getCurrencyCode());
        log.info("ACS authentication created: id={}", auth.getId());

        // 6. Mark AReq sent via 3DS Server -> DS -> ACS
        threeDsService.sendAuthenticationRequest(
                session.getId(),
                routing.getAcsUrl(),
                "{\"threeDSRequestId\":\"" + auth.getId() + "\",\"amount\":\"" + request.getAmount() + "\"}");
        log.info("AReq sent to ACS via DS");

        // 7. Evaluate RBA (this decides AUTHENTICATED / CHALLENGE_REQUIRED / DECLINED)
        Map<String, Object> rbaContext = buildRbaContext(request, card);
        auth = acsService.evaluateRba(auth.getId(), rbaContext);
        log.info("RBA result: status={}, score={}, decision={}",
                auth.getStatus(), auth.getRiskScore(), auth.getRiskDecision());

        // 8. Route based on RBA outcome
        return switch (auth.getStatus()) {
            case AUTHENTICATED -> handleAuthenticated(txn, session, auth, routing, request);
            case CHALLENGE_REQUIRED -> handleChallengeRequired(txn, session, auth);
            case DECLINED -> handleDeclined(txn, session, auth);
            default -> {
                threeDsService.failSession(session.getId(), "Unexpected RBA status: " + auth.getStatus());
                epgService.failTransaction(txn.getId(), "99", "Unexpected RBA status: " + auth.getStatus());
                yield SimulationResult.builder()
                        .status("ERROR")
                        .epgTransactionId(txn.getId())
                        .threeDsSessionId(session.getId())
                        .acsAuthenticationId(auth.getId())
                        .message("Unexpected RBA status: " + auth.getStatus())
                        .build();
            }
        };
    }

    private SimulationResult handleAuthenticated(EpgTransaction txn, ThreeDsSession session,
                                                  AcsAuthentication auth,
                                                  SimulatedDirectoryServer.DsRoutingResult routing,
                                                  SimulationRequest request) {
        String cavv = auth.getAuthenticationValue();
        String eci = auth.getEci();

        // DS returns ARes -> 3DS Server receives authentication response
        threeDsService.receiveAuthenticationResponse(
                session.getId(),
                auth.getId().toString(),
                routing.getDsTransId(),
                cavv);

        // Complete 3DS session
        threeDsService.completeSession(session.getId(), cavv);
        log.info("3DS session completed: id={}", session.getId());

        // Mark 3DS status on EPG transaction
        epgService.setThreeDsStatus(txn.getId(), true, auth.getId());

        // Authorize the EPG transaction
        EpgTransaction authorized = epgService.authorizeTransaction(
                txn.getId(),
                request.getCardId(),
                cavv,
                eci);

        log.info("=== Flow complete: AUTHENTICATED + AUTHORIZED ===");
        log.info("EPG txn: id={}, status={}, cavv={}, eci={}",
                authorized.getId(), authorized.getStatus(), authorized.getCavv(), authorized.getEci());

        return SimulationResult.builder()
                .status("AUTHENTICATED")
                .epgTransactionId(txn.getId())
                .threeDsSessionId(session.getId())
                .acsAuthenticationId(auth.getId())
                .cavv(cavv)
                .eci(eci)
                .riskScore(auth.getRiskScore())
                .riskDecision(auth.getRiskDecision())
                .message("Frictionless flow complete: AUTHENTICATED + AUTHORIZED")
                .build();
    }

    private SimulationResult handleChallengeRequired(EpgTransaction txn, ThreeDsSession session,
                                                      AcsAuthentication auth) {
        threeDsService.receiveAuthenticationResponse(
                session.getId(),
                auth.getId().toString(),
                null,
                null);

        threeDsService.failSession(session.getId(), "Challenge required - frictionless not possible");
        epgService.setThreeDsStatus(txn.getId(), true, auth.getId());

        log.info("RBA requires challenge: authId={}, score={}", auth.getId(), auth.getRiskScore());

        return SimulationResult.builder()
                .status("CHALLENGE_REQUIRED")
                .epgTransactionId(txn.getId())
                .threeDsSessionId(session.getId())
                .acsAuthenticationId(auth.getId())
                .riskScore(auth.getRiskScore())
                .riskDecision(auth.getRiskDecision())
                .message("3DS challenge required (step-up) - frictionless not possible")
                .build();
    }

    private SimulationResult handleDeclined(EpgTransaction txn, ThreeDsSession session,
                                             AcsAuthentication auth) {
        threeDsService.failSession(session.getId(), "RBA declined: " + auth.getRiskDecision());
        epgService.failTransaction(txn.getId(), "05", "RBA declined");

        log.info("RBA declined: authId={}, score={}, decision={}",
                auth.getId(), auth.getRiskScore(), auth.getRiskDecision());

        return SimulationResult.builder()
                .status("DECLINED")
                .epgTransactionId(txn.getId())
                .threeDsSessionId(session.getId())
                .acsAuthenticationId(auth.getId())
                .riskScore(auth.getRiskScore())
                .riskDecision(auth.getRiskDecision())
                .message("Transaction declined by RBA")
                .build();
    }

    public SimulationResult simulateChallengeFlow(SimulationRequest request) {
        log.info("=== Ecommerce Flow Simulator: starting challenge flow ===");
        log.info("cardId={}, merchantId={}, amount={} {}",
                request.getCardId(), request.getMerchantId(),
                request.getAmount(), request.getCurrencyCode());

        Card card = cardService.getCard(request.getCardId())
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + request.getCardId()));

        EpgTransaction txn = epgService.initiateTransaction(
                request.getMerchantId(), request.getMerchantTransactionId(),
                request.getAmount(), request.getCurrencyCode());

        ThreeDsSession session = threeDsService.createSession(
                request.getMerchantTransactionId(), txn.getId(),
                request.getCardId(), null);

        SimulatedDirectoryServer.DsRoutingResult routing = directoryServer.routeForCard(
                request.getCardId(), request.getMerchantTransactionId(),
                null, session.getId(), request.getMerchantId());

        AcsAuthentication auth = acsService.createAuthentication(
                request.getMerchantTransactionId(), request.getCardId(),
                request.getMerchantId(), request.getAmount(), request.getCurrencyCode());

        threeDsService.sendAuthenticationRequest(
                session.getId(), routing.getAcsUrl(),
                "{\"threeDSRequestId\":\"" + auth.getId() + "\",\"amount\":\"" + request.getAmount() + "\"}");

        auth.setDsTransId(UUID.fromString(routing.getDsTransId()));
        auth.setStatus(AcsAuthentication.Status.CHALLENGE_REQUIRED);
        auth.setRiskScore(45);
        auth.setRiskDecision("CHALLENGE_REQUIRED");
        auth.setUpdatedAt(OffsetDateTime.now());
        acsAuthenticationRepository.save(auth);
        log.info("RBA simulated: status=CHALLENGE_REQUIRED, score=45");

        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(999999));
        AcsChallenge challenge = AcsChallenge.builder()
                .authenticationId(auth.getId())
                .challengeType(AcsChallenge.ChallengeType.OTP)
                .challengeData(otp)
                .status(AcsChallenge.Status.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        acsChallengeRepository.save(challenge);
        log.info("OTP challenge created: id={}", challenge.getId());

        threeDsService.receiveAuthenticationResponse(
                session.getId(), auth.getId().toString(), routing.getDsTransId(), null);
        epgService.setThreeDsStatus(txn.getId(), true, auth.getId());

        log.info("=== Challenge flow initiated: CHALLENGE_REQUIRED ===");

        return SimulationResult.builder()
                .status("CHALLENGE_REQUIRED")
                .epgTransactionId(txn.getId())
                .threeDsSessionId(session.getId())
                .acsAuthenticationId(auth.getId())
                .challengeId(challenge.getId())
                .challengeData(otp)
                .challengeType("OTP")
                .riskScore(45)
                .riskDecision("CHALLENGE_REQUIRED")
                .message("3DS challenge initiated: OTP generated (returned in response for demo)")
                .build();
    }

    public SimulationResult verifyChallengeFlow(UUID challengeId, String otp) {
        log.info("=== Ecommerce Flow Simulator: verifying challenge ===");

        AcsChallenge challenge = acsChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found: " + challengeId));

        if (challenge.getStatus() != AcsChallenge.Status.PENDING) {
            throw new IllegalStateException("Challenge is not PENDING: " + challenge.getStatus());
        }

        if (challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            challenge.setStatus(AcsChallenge.Status.EXPIRED);
            acsChallengeRepository.save(challenge);
            acsService.failAuthentication(challenge.getAuthenticationId());
            throw new IllegalStateException("Challenge has expired");
        }

        if (!otp.equals(challenge.getChallengeData())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            acsChallengeRepository.save(challenge);

            int remaining = challenge.getMaxAttempts() - challenge.getAttempts();
            log.info("Incorrect OTP: attempts={}/{}, remaining={}",
                    challenge.getAttempts(), challenge.getMaxAttempts(), remaining);

            if (remaining <= 0) {
                challenge.setStatus(AcsChallenge.Status.FAILED);
                acsChallengeRepository.save(challenge);
                acsService.failAuthentication(challenge.getAuthenticationId());

                AcsAuthentication auth = acsAuthenticationRepository
                        .findById(challenge.getAuthenticationId()).orElse(null);
                if (auth != null) {
                    ThreeDsSession session = threeDsService.getSessionByTransactionId(auth.getTransactionId());
                    if (session != null) {
                        threeDsService.failSession(session.getId(), "Incorrect OTP - max attempts reached");
                    }
                    EpgTransaction txn = epgService.getByMerchantTransaction(
                            auth.getMerchantId(), auth.getTransactionId());
                    if (txn != null) {
                        epgService.failTransaction(txn.getId(), "05", "Incorrect OTP - max attempts reached");
                    }
                }

                return SimulationResult.builder()
                        .status("FAILED")
                        .challengeId(challengeId)
                        .message("Incorrect OTP - max attempts reached")
                        .build();
            }

            return SimulationResult.builder()
                    .status("WRONG_OTP")
                    .challengeId(challengeId)
                    .message("Incorrect OTP - " + remaining + " attempt(s) remaining")
                    .build();
        }

        challenge.setStatus(AcsChallenge.Status.VERIFIED);
        challenge.setVerifiedAt(OffsetDateTime.now());
        challenge.setUpdatedAt(OffsetDateTime.now());
        acsChallengeRepository.save(challenge);
        log.info("Challenge verified: id={}", challengeId);

        String cavv = "AAAB" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
        String eci = "05";
        AcsAuthentication auth = acsService.authenticate(challenge.getAuthenticationId(), cavv, eci);
        log.info("Authentication completed: id={}, status=AUTHENTICATED", auth.getId());

        ThreeDsSession session = threeDsService.getSessionByTransactionId(auth.getTransactionId());
        if (session == null) {
            throw new IllegalStateException("3DS session not found for transaction: " + auth.getTransactionId());
        }
        threeDsService.completeSession(session.getId(), cavv);
        log.info("3DS session completed: id={}", session.getId());

        EpgTransaction txn = epgService.getByMerchantTransaction(auth.getMerchantId(), auth.getTransactionId());
        if (txn == null) {
            throw new IllegalStateException("EPG transaction not found");
        }
        epgService.setThreeDsStatus(txn.getId(), true, auth.getId());
        EpgTransaction authorized = epgService.authorizeTransaction(txn.getId(), auth.getCardId(), cavv, eci);

        if (authorized.getStatus() == EpgTransaction.Status.AUTHORIZED) {
            log.info("=== Challenge flow complete: AUTHENTICATED + AUTHORIZED ===");
            return SimulationResult.builder()
                    .status("AUTHENTICATED")
                    .epgTransactionId(txn.getId())
                    .threeDsSessionId(session.getId())
                    .acsAuthenticationId(auth.getId())
                    .challengeId(challengeId)
                    .cavv(cavv)
                    .eci(eci)
                    .riskScore(45)
                    .riskDecision("APPROVED")
                    .message("Challenge verified: AUTHENTICATED + AUTHORIZED")
                    .build();
        } else {
            log.warn("=== Challenge flow: 3DS AUTHENTICATED but authorization DECLINED: {} ===",
                    authorized.getErrorDescription());
            return SimulationResult.builder()
                    .status("AUTHENTICATED_BUT_DECLINED")
                    .epgTransactionId(txn.getId())
                    .threeDsSessionId(session.getId())
                    .acsAuthenticationId(auth.getId())
                    .challengeId(challengeId)
                    .cavv(cavv)
                    .eci(eci)
                    .riskScore(45)
                    .riskDecision("DECLINED")
                    .message("3DS authenticated but authorization declined (" + authorized.getErrorCode()
                            + "): " + authorized.getErrorDescription())
                    .build();
        }
    }

    public SimulationResult simulateAppChallengeFlow(SimulationRequest request) {
        log.info("=== Ecommerce Flow Simulator: starting MOBILE APP challenge flow ===");

        Card card = cardService.getCard(request.getCardId())
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + request.getCardId()));

        EpgTransaction txn = epgService.initiateTransaction(
                request.getMerchantId(), request.getMerchantTransactionId(),
                request.getAmount(), request.getCurrencyCode());

        ThreeDsSession session = threeDsService.createSession(
                request.getMerchantTransactionId(), txn.getId(),
                request.getCardId(), null);

        SimulatedDirectoryServer.DsRoutingResult routing = directoryServer.routeForCard(
                request.getCardId(), request.getMerchantTransactionId(),
                null, session.getId(), request.getMerchantId());

        AcsAuthentication auth = acsService.createAuthentication(
                request.getMerchantTransactionId(), request.getCardId(),
                request.getMerchantId(), request.getAmount(), request.getCurrencyCode());

        threeDsService.sendAuthenticationRequest(
                session.getId(), routing.getAcsUrl(),
                "{\"threeDSRequestId\":\"" + auth.getId()
                        + "\",\"amount\":\"" + request.getAmount()
                        + "\",\"deviceChannel\":\"MOBILE\",\"challengeType\":\"APP_NOTIFICATION\"}");

        auth.setDsTransId(UUID.fromString(routing.getDsTransId()));
        auth.setStatus(AcsAuthentication.Status.CHALLENGE_REQUIRED);
        auth.setRiskScore(45);
        auth.setRiskDecision("CHALLENGE_REQUIRED");
        auth.setUpdatedAt(OffsetDateTime.now());
        acsAuthenticationRepository.save(auth);

        String notificationToken = UUID.randomUUID().toString();
        String notificationPayload = "{\"title\":\"Approve Payment\",\"merchant\":\""
                + request.getMerchantId() + "\",\"amount\":\"" + request.getAmount()
                + " " + request.getCurrencyCode() + "\",\"token\":\"" + notificationToken + "\"}";

        AcsChallenge challenge = AcsChallenge.builder()
                .authenticationId(auth.getId())
                .challengeType(AcsChallenge.ChallengeType.APP_NOTIFICATION)
                .challengeData(notificationPayload)
                .status(AcsChallenge.Status.PENDING)
                .attempts(0)
                .maxAttempts(1)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        acsChallengeRepository.save(challenge);
        log.info("Mobile app challenge created: id={}, token={}", challenge.getId(), notificationToken);

        threeDsService.receiveAuthenticationResponse(
                session.getId(), auth.getId().toString(), routing.getDsTransId(), null);
        epgService.setThreeDsStatus(txn.getId(), true, auth.getId());

        return SimulationResult.builder()
                .status("CHALLENGE_REQUIRED")
                .epgTransactionId(txn.getId())
                .threeDsSessionId(session.getId())
                .acsAuthenticationId(auth.getId())
                .challengeId(challenge.getId())
                .challengeData(notificationPayload)
                .challengeType("APP_NOTIFICATION")
                .riskScore(45)
                .riskDecision("CHALLENGE_REQUIRED")
                .message("Mobile app challenge initiated: notification sent to app (returned in response for demo)")
                .build();
    }

    public SimulationResult respondToAppChallenge(UUID challengeId, String action) {
        log.info("=== Ecommerce Flow Simulator: responding to app challenge ===");

        AcsChallenge challenge = acsChallengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge not found: " + challengeId));

        if (challenge.getStatus() != AcsChallenge.Status.PENDING) {
            throw new IllegalStateException("Challenge is not PENDING: " + challenge.getStatus());
        }
        if (challenge.getChallengeType() != AcsChallenge.ChallengeType.APP_NOTIFICATION) {
            throw new IllegalArgumentException("Not an app notification challenge: " + challenge.getChallengeType());
        }
        if (challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            challenge.setStatus(AcsChallenge.Status.EXPIRED);
            acsChallengeRepository.save(challenge);
            acsService.failAuthentication(challenge.getAuthenticationId());
            throw new IllegalStateException("Challenge has expired");
        }

        if ("REJECT".equalsIgnoreCase(action)) {
            challenge.setStatus(AcsChallenge.Status.FAILED);
            acsChallengeRepository.save(challenge);

            AcsAuthentication auth = acsAuthenticationRepository
                    .findById(challenge.getAuthenticationId()).orElse(null);
            if (auth != null) {
                auth.setStatus(AcsAuthentication.Status.DECLINED);
                auth.setUpdatedAt(OffsetDateTime.now());
                acsAuthenticationRepository.save(auth);

                ThreeDsSession session = threeDsService.getSessionByTransactionId(auth.getTransactionId());
                if (session != null) {
                    threeDsService.failSession(session.getId(), "Cardholder rejected challenge");
                }
                EpgTransaction txn = epgService.getByMerchantTransaction(
                        auth.getMerchantId(), auth.getTransactionId());
                if (txn != null) {
                    epgService.failTransaction(txn.getId(), "05", "Cardholder rejected challenge");
                }
            }

            return SimulationResult.builder()
                    .status("DECLINED")
                    .challengeId(challengeId)
                    .message("Cardholder rejected the authentication challenge")
                    .build();
        }

        if (!"APPROVE".equalsIgnoreCase(action)) {
            throw new IllegalArgumentException("Invalid action: " + action + " (expected APPROVE or REJECT)");
        }

        challenge.setStatus(AcsChallenge.Status.VERIFIED);
        challenge.setVerifiedAt(OffsetDateTime.now());
        challenge.setUpdatedAt(OffsetDateTime.now());
        acsChallengeRepository.save(challenge);

        String cavv = "AAAB" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
        String eci = "05";
        AcsAuthentication auth = acsService.authenticate(challenge.getAuthenticationId(), cavv, eci);

        ThreeDsSession session = threeDsService.getSessionByTransactionId(auth.getTransactionId());
        if (session == null) {
            throw new IllegalStateException("3DS session not found for transaction: " + auth.getTransactionId());
        }
        threeDsService.completeSession(session.getId(), cavv);

        EpgTransaction txn = epgService.getByMerchantTransaction(auth.getMerchantId(), auth.getTransactionId());
        if (txn == null) {
            throw new IllegalStateException("EPG transaction not found");
        }
        epgService.setThreeDsStatus(txn.getId(), true, auth.getId());
        epgService.authorizeTransaction(txn.getId(), auth.getCardId(), cavv, eci);

        return SimulationResult.builder()
                .status("AUTHENTICATED")
                .epgTransactionId(txn.getId())
                .threeDsSessionId(session.getId())
                .acsAuthenticationId(auth.getId())
                .challengeId(challengeId)
                .cavv(cavv)
                .eci(eci)
                .riskScore(45)
                .riskDecision("CHALLENGE_REQUIRED")
                .message("Cardholder approved: AUTHENTICATED + AUTHORIZED")
                .build();
    }

    private Map<String, Object> buildRbaContext(SimulationRequest request, Card card) {
        Map<String, Object> context = new HashMap<>();
        context.put("countryCode", request.getCountryCode() != null ? request.getCountryCode() : "TN");
        context.put("deviceId", request.getDeviceId() != null ? request.getDeviceId() : UUID.randomUUID().toString());
        context.put("ipAddress", request.getIpAddress() != null ? request.getIpAddress() : "196.168.1.100");
        context.put("deviceType", "BROWSER");
        context.put("browser", "Chrome/120");
        context.put("os", "Windows 10");
        context.put("userAgent", "Mozilla/5.0 Chrome/120");
        context.put("merchantCategory", "retail");
        if (card.getCardholderId() != null) {
            context.put("cardholderId", card.getCardholderId().toString());
        }
        return context;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class SimulationRequest {
        private UUID cardId;
        private UUID merchantId;
        private BigDecimal amount;
        private String currencyCode;
        private String merchantTransactionId;
        private String countryCode;
        private String deviceId;
        private String ipAddress;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class SimulationResult {
        private String status;
        private UUID epgTransactionId;
        private UUID threeDsSessionId;
        private UUID acsAuthenticationId;
        private UUID challengeId;
        private String challengeData;
        private String challengeType;
        private String cavv;
        private String eci;
        private Integer riskScore;
        private String riskDecision;
        private String message;
    }

    /* ───────── Phase 5 : Batch via VRAI RBA —───────── */

    private enum RiskProfile {
        LOW_RISK,
        MEDIUM_DOMESTIC,
        MEDIUM_FOREIGN,
        HIGH_RISK
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class BatchRequest {
        private int totalTransactions;
        private int frictionlessPercent;
        private int challengePercent;
        private int appChallengePercent;
        private int declinedPercent;
        private List<UUID> cardIds;
        private UUID merchantId;
        private BigDecimal amountMin;
        private BigDecimal amountMax;
        private String currencyCode;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class BatchResult {
        private List<SimulationResult> transactions;
        private int totalCount;
        private int authenticatedCount;
        private int challengeCount;
        private int appChallengeCount;
        private int declinedCount;
        private String summary;
    }

    public BatchResult simulateBatch(BatchRequest request) {
        log.info("=== Batch simulator (REAL RBA): {} transactions across {} cards ===",
                request.getTotalTransactions(), request.getCardIds().size());
        log.info("Target profile distribution: low_risk {}%, medium_domestic {}%, medium_foreign {}%, high_risk {}%",
                request.getFrictionlessPercent(), request.getChallengePercent(),
                request.getAppChallengePercent(), request.getDeclinedPercent());
        log.info("NOTE: the RBA engine decides EACH outcome — target distribution is APPROXIMATE");

        for (UUID cardId : request.getCardIds()) {
            clearVelocity(cardId);
            preRegisterDevices(cardId);
        }

        List<RiskProfile> profiles = generateProfiles(request);
        List<SimulationResult> results = new ArrayList<>();
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        for (int i = 0; i < profiles.size(); i++) {
            UUID cardId = request.getCardIds().get(rand.nextInt(request.getCardIds().size()));
            String merchantTxnId = UUID.randomUUID().toString();
            BigDecimal amount = generateProfileAmount(profiles.get(i));
            log.info("Batch txn {}/{}: card={}, profile={}, amount={}", i + 1, profiles.size(), cardId, profiles.get(i), amount);

            SimulationResult result = executeBatchTxn(request, merchantTxnId, amount, profiles.get(i), cardId);
            results.add(result);
        }

        return buildBatchResult(results, request);
    }

    private void preRegisterDevices(UUID cardId) {
        for (int i = 0; i < 2; i++) {
            deviceFingerprintService.registerFingerprint(cardId, "device-trusted", "BROWSER", "Windows 10",
                    "Chrome/120",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    "196.168.1.100", null);
            deviceFingerprintService.registerFingerprint(cardId, "device-mobile", "MOBILE", "iOS",
                    "Safari",
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
                    "92.154.10.25", null);
            deviceFingerprintService.registerFingerprint(cardId, "device-suspicious", "BROWSER", "Linux",
                    "Firefox",
                    "Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0",
                    "203.0.113.50", null);
        }
        log.info("Pre-registered {} device fingerprints for batch simulation", 3);
    }

    private void clearVelocity(UUID cardId) {
        List<com.switchplatform.platform.model.authorization.VelocityCheck> records =
                velocityCheckRepository.findByCardId(cardId);
        if (!records.isEmpty()) {
            velocityCheckRepository.deleteAll(records);
            log.info("Cleared {} velocity records for card {} (clean slate for demo)", records.size(), cardId);
        }
    }

    private List<RiskProfile> generateProfiles(BatchRequest request) {
        List<RiskProfile> profiles = new ArrayList<>();
        int total = request.getTotalTransactions();
        for (int i = 0; i < total; i++) {
            int pct = i * 100 / total;
            int f = request.getFrictionlessPercent();
            int c = f + request.getChallengePercent();
            int a = c + request.getAppChallengePercent();
            if (pct < f) profiles.add(RiskProfile.LOW_RISK);
            else if (pct < c) profiles.add(RiskProfile.MEDIUM_DOMESTIC);
            else if (pct < a) profiles.add(RiskProfile.MEDIUM_FOREIGN);
            else profiles.add(RiskProfile.HIGH_RISK);
        }
        Collections.shuffle(profiles);
        return profiles;
    }

    private BigDecimal generateProfileAmount(RiskProfile profile) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return switch (profile) {
            case LOW_RISK -> BigDecimal.valueOf(15 + r.nextDouble(285)).setScale(3, RoundingMode.HALF_UP);
            case MEDIUM_DOMESTIC -> BigDecimal.valueOf(500 + r.nextDouble(4500)).setScale(3, RoundingMode.HALF_UP);
            case MEDIUM_FOREIGN -> BigDecimal.valueOf(200 + r.nextDouble(2800)).setScale(3, RoundingMode.HALF_UP);
            case HIGH_RISK -> BigDecimal.valueOf(15000 + r.nextDouble(35000)).setScale(3, RoundingMode.HALF_UP);
        };
    }

    private Map<String, Object> generateRiskContext(RiskProfile profile, Card card) {
        Map<String, Object> ctx = new HashMap<>();
        switch (profile) {
            case LOW_RISK -> {
                ctx.put("countryCode", "TN");
                ctx.put("deviceId", "device-trusted");
                ctx.put("ipAddress", "196.168.1.100");
                ctx.put("deviceType", "BROWSER");
                ctx.put("browser", "Chrome/120");
                ctx.put("os", "Windows 10");
                ctx.put("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0");
                ctx.put("merchantCategory", "retail");
            }
            case MEDIUM_DOMESTIC -> {
                ctx.put("countryCode", "TN");
                ctx.put("deviceId", "device-trusted");
                ctx.put("ipAddress", "10.0.0.50");
                ctx.put("deviceType", "BROWSER");
                ctx.put("browser", "Chrome/120");
                ctx.put("os", "Windows 10");
                ctx.put("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0");
                ctx.put("merchantCategory", "retail");
            }
            case MEDIUM_FOREIGN -> {
                ctx.put("countryCode", "FR");
                ctx.put("deviceId", "device-mobile");
                ctx.put("ipAddress", "92.154.10.25");
                ctx.put("deviceType", "MOBILE");
                ctx.put("browser", "Safari");
                ctx.put("os", "iOS");
                ctx.put("userAgent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0) Mobile/15E148");
                ctx.put("merchantCategory", "travel");
            }
            case HIGH_RISK -> {
                ctx.put("countryCode", "US");
                ctx.put("deviceId", "device-suspicious");
                ctx.put("ipAddress", "203.0.113.50");
                ctx.put("deviceType", "BROWSER");
                ctx.put("browser", "Firefox");
                ctx.put("os", "Linux");
                ctx.put("userAgent", "Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Firefox/120.0");
                ctx.put("merchantCategory", "electronics");
            }
        }
        if (card.getCardholderId() != null) {
            ctx.put("cardholderId", card.getCardholderId().toString());
        }
        return ctx;
    }

    private SimulationResult executeBatchTxn(BatchRequest request, String merchantTxnId,
                                              BigDecimal amount, RiskProfile profile, UUID cardId) {
        Card card = cardService.getCard(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));

        EpgTransaction txn = epgService.initiateTransaction(
                request.getMerchantId(), merchantTxnId, amount, request.getCurrencyCode());

        ThreeDsSession session = threeDsService.createSession(
                merchantTxnId, txn.getId(), cardId, null);

        SimulatedDirectoryServer.DsRoutingResult routing = directoryServer.routeForCard(
                cardId, merchantTxnId, null, session.getId(), request.getMerchantId());

        AcsAuthentication auth = acsService.createAuthentication(
                merchantTxnId, cardId, request.getMerchantId(), amount, request.getCurrencyCode());

        threeDsService.sendAuthenticationRequest(
                session.getId(), routing.getAcsUrl(),
                "{\"merchantTxnId\":\"" + merchantTxnId + "\",\"profile\":\"" + profile + "\"}");

        Map<String, Object> riskContext = generateRiskContext(profile, card);
        auth = acsService.evaluateRba(auth.getId(), riskContext);

        log.info("RBA: txnId={}, profile={}, score={}, status={}, rules={}",
                merchantTxnId, profile, auth.getRiskScore(), auth.getStatus(), auth.getRiskDecision());

        return switch (auth.getStatus()) {
            case AUTHENTICATED -> completeBatchFrictionless(txn, session, auth, routing, auth.getRiskScore());
            case CHALLENGE_REQUIRED -> completeBatchChallenge(txn, session, auth, routing, profile, auth.getRiskScore());
            case DECLINED -> completeBatchDeclined(txn, session, auth, auth.getRiskScore());
            default -> {
                threeDsService.failSession(session.getId(), "Unexpected status");
                epgService.failTransaction(txn.getId(), "99", "Unexpected status");
                yield SimulationResult.builder().status("ERROR").message("Unexpected status: " + auth.getStatus()).build();
            }
        };
    }

    private SimulationResult completeBatchFrictionless(EpgTransaction txn, ThreeDsSession session,
                                                        AcsAuthentication auth,
                                                        SimulatedDirectoryServer.DsRoutingResult routing,
                                                        int riskScore) {
        String cavv = auth.getAuthenticationValue();
        String eci = auth.getEci();

        threeDsService.receiveAuthenticationResponse(
                session.getId(), auth.getId().toString(), routing.getDsTransId(), cavv);
        threeDsService.completeSession(session.getId(), cavv);
        epgService.setThreeDsStatus(txn.getId(), true, auth.getId());
        epgService.authorizeTransaction(txn.getId(), auth.getCardId(), cavv, eci);

        return SimulationResult.builder()
                .status("AUTHENTICATED")
                .epgTransactionId(txn.getId())
                .threeDsSessionId(session.getId())
                .acsAuthenticationId(auth.getId())
                .cavv(cavv).eci(eci)
                .riskScore(riskScore).riskDecision(auth.getRiskDecision())
                .message("RBA: AUTHENTICATED (score=" + riskScore + ")")
                .build();
    }

    private SimulationResult completeBatchChallenge(EpgTransaction txn, ThreeDsSession session,
                                                     AcsAuthentication auth,
                                                     SimulatedDirectoryServer.DsRoutingResult routing,
                                                     RiskProfile profile, int riskScore) {
        threeDsService.receiveAuthenticationResponse(
                session.getId(), auth.getId().toString(), routing.getDsTransId(), null);
        epgService.setThreeDsStatus(txn.getId(), true, auth.getId());

        if (profile == RiskProfile.MEDIUM_FOREIGN) {
            AcsChallenge challenge = AcsChallenge.builder()
                    .authenticationId(auth.getId())
                    .challengeType(AcsChallenge.ChallengeType.APP_NOTIFICATION)
                    .challengeData("{\"title\":\"Approve Payment\",\"amount\":\"" + txn.getAmount() + "\"}")
                    .status(AcsChallenge.Status.VERIFIED)
                    .verifiedAt(OffsetDateTime.now())
                    .attempts(0).maxAttempts(1)
                    .expiresAt(OffsetDateTime.now().plusMinutes(10))
                    .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                    .build();
            acsChallengeRepository.save(challenge);

            String cavv = "AAAB" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
            acsService.authenticate(challenge.getAuthenticationId(), cavv, "05");
            threeDsService.completeSession(session.getId(), cavv);
            epgService.authorizeTransaction(txn.getId(), auth.getCardId(), cavv, "05");

            return SimulationResult.builder()
                    .status("AUTHENTICATED")
                    .epgTransactionId(txn.getId())
                    .threeDsSessionId(session.getId())
                    .acsAuthenticationId(auth.getId())
                    .challengeId(challenge.getId())
                    .challengeType("APP_NOTIFICATION")
                    .cavv(cavv).eci("05")
                    .riskScore(riskScore).riskDecision(auth.getRiskDecision())
                    .message("RBA: challenge (score=" + riskScore + ") → app approved → AUTHENTICATED")
                    .build();
        }

        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(999999));
        AcsChallenge challenge = AcsChallenge.builder()
                .authenticationId(auth.getId())
                .challengeType(AcsChallenge.ChallengeType.OTP)
                .challengeData(otp)
                .status(AcsChallenge.Status.VERIFIED)
                .verifiedAt(OffsetDateTime.now())
                .attempts(0).maxAttempts(3)
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();
        acsChallengeRepository.save(challenge);

        String cavv = "AAAB" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
        acsService.authenticate(challenge.getAuthenticationId(), cavv, "05");
        threeDsService.completeSession(session.getId(), cavv);
        epgService.authorizeTransaction(txn.getId(), auth.getCardId(), cavv, "05");

        return SimulationResult.builder()
                .status("AUTHENTICATED")
                .epgTransactionId(txn.getId())
                .threeDsSessionId(session.getId())
                .acsAuthenticationId(auth.getId())
                .challengeId(challenge.getId())
                .challengeData(otp)
                .challengeType("OTP")
                .cavv(cavv).eci("05")
                .riskScore(riskScore).riskDecision(auth.getRiskDecision())
                .message("RBA: challenge (score=" + riskScore + ") → OTP verified → AUTHENTICATED")
                .build();
    }

    private SimulationResult completeBatchDeclined(EpgTransaction txn, ThreeDsSession session,
                                                    AcsAuthentication auth, int riskScore) {
        threeDsService.failSession(session.getId(), "RBA declined: score=" + riskScore);
        epgService.failTransaction(txn.getId(), "05", "RBA declined: score=" + riskScore);

        return SimulationResult.builder()
                .status("DECLINED")
                .epgTransactionId(txn.getId())
                .threeDsSessionId(session.getId())
                .acsAuthenticationId(auth.getId())
                .riskScore(riskScore).riskDecision(auth.getRiskDecision())
                .message("RBA: DECLINED (score=" + riskScore + ")")
                .build();
    }

    private BatchResult buildBatchResult(List<SimulationResult> results, BatchRequest request) {
        int authCount = 0, challengeCount = 0, appChallengeCount = 0, declinedCount = 0;
        for (SimulationResult r : results) {
            switch (r.getStatus()) {
                case "AUTHENTICATED" -> {
                    if ("APP_NOTIFICATION".equals(r.getChallengeType())) appChallengeCount++;
                    else if (r.getChallengeId() != null) challengeCount++;
                    else authCount++;
                }
                case "DECLINED" -> declinedCount++;
            }
        }
        String summary = String.format(
                "Batch: %d/%d AUTHENTICATED (%d via OTP, %d via app), %d DECLINED — " +
                "RBA decisions (target profile distribution: low_risk %d%%, medium_domestic %d%%, medium_foreign %d%%, high_risk %d%%)",
                authCount + challengeCount + appChallengeCount, request.getTotalTransactions(),
                challengeCount, appChallengeCount, declinedCount,
                request.getFrictionlessPercent(), request.getChallengePercent(),
                request.getAppChallengePercent(), request.getDeclinedPercent());

        log.info("=== {} ===", summary);

        return BatchResult.builder()
                .transactions(results)
                .totalCount(results.size())
                .authenticatedCount(authCount)
                .challengeCount(challengeCount)
                .appChallengeCount(appChallengeCount)
                .declinedCount(declinedCount)
                .summary(summary)
                .build();
    }
}
