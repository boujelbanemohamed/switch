package com.switchplatform.platform.service.simulator;

import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.model.acquiring.Terminal;
import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.repository.TransactionRepository;
import com.switchplatform.platform.repository.acquiring.MerchantRepository;
import com.switchplatform.platform.repository.acquiring.TerminalRepository;
import com.switchplatform.platform.service.authorization.AuthorizationEngine;
import com.switchplatform.platform.service.issuing.CardService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosFlowSimulator {

    private static final BigDecimal CONTACTLESS_LIMIT = new BigDecimal("500.00");

    private final AuthorizationEngine authorizationEngine;
    private final CardService cardService;
    private final MerchantRepository merchantRepository;
    private final TerminalRepository terminalRepository;
    private final TransactionRepository transactionRepository;

    public PosResult simulatePosTransaction(PosRequest request) {
        log.info("=== POS Flow Simulator: {} card-present transaction ===", request.getEntryMode());
        log.info("cardId={}, merchantId={}, terminalId={}, amount={} {}",
                request.getCardId(), request.getMerchantId(), request.getTerminalId(),
                request.getAmount(), request.getCurrencyCode());

        Card card = cardService.getCard(request.getCardId())
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + request.getCardId()));

        Terminal terminal = terminalRepository.findById(request.getTerminalId())
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found: " + request.getTerminalId()));

        Merchant merchant = merchantRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + request.getMerchantId()));

        String posEntryMode;
        String posConditionCode;
        String pinStatus;

        if ("CHIP".equalsIgnoreCase(request.getEntryMode())) {
            posEntryMode = "051";
            if (!Boolean.TRUE.equals(terminal.getChipSupported())) {
                return buildDeclined("DECLINED", "57", "Terminal does not support chip");
            }
            if (!request.isPinVerified()) {
                log.warn("Chip transaction with incorrect PIN: cardId={}", request.getCardId());
                return buildDeclined("DECLINED", "55", "Incorrect PIN");
            }
            posConditionCode = "05";
            pinStatus = "PIN_OK";
        } else if ("CONTACTLESS".equalsIgnoreCase(request.getEntryMode())) {
            posEntryMode = "071";
            if (!Boolean.TRUE.equals(terminal.getContactlessSupported())) {
                return buildDeclined("DECLINED", "57", "Terminal does not support contactless");
            }
            if (request.getAmount().compareTo(CONTACTLESS_LIMIT) > 0) {
                log.warn("Contactless amount {} exceeds limit {}, would require PIN",
                        request.getAmount(), CONTACTLESS_LIMIT);
                return buildDeclined("DECLINED", "55", "Amount exceeds contactless limit, PIN required");
            }
            posConditionCode = "06";
            pinStatus = "NO_PIN";
        } else {
            throw new IllegalArgumentException("Unsupported entry mode: " + request.getEntryMode()
                    + " (expected CHIP or CONTACTLESS)");
        }

        String channel = "POS";
        String stan = UUID.randomUUID().toString();

        AuthorizationEngine.AuthorizationRequest authRequest = AuthorizationEngine.AuthorizationRequest.builder()
                .cardId(request.getCardId())
                .cardholderId(card.getCardholderId())
                .merchantId(merchant.getMerchantId())
                .terminalId(terminal.getTerminalId())
                .amount(request.getAmount())
                .currencyCode(request.getCurrencyCode())
                .mti("0100")
                .stan(stan)
                .panHash(card.getCardNumberHash())
                .cardType(card.getCardType() != null ? card.getCardType().name() : null)
                .cardBrand(card.getCardBrand() != null ? card.getCardBrand().name() : null)
                .merchantCategory(merchant.getMerchantCategoryCode())
                .countryCode(request.getCountryCode())
                .build();

        AuthorizationEngine.AuthorizationResponse authResponse = authorizationEngine.authorize(authRequest);

        String decisionStr = authResponse.getDecision().name();
        Transaction.TransactionStatus txnStatus = authResponse.getDecision()
                == AuthorizationEngine.AuthorizationResponse.Decision.APPROVED
                ? Transaction.TransactionStatus.COMPLETED : Transaction.TransactionStatus.FAILED;

        Transaction txn = Transaction.builder()
                .transactionId("POS-" + UUID.randomUUID())
                .messageType("0100")
                .protocol(Transaction.Protocol.ISO8583)
                .stan(stan.substring(0, 12))
                .panHash(card.getCardNumberHash())
                .amount(request.getAmount())
                .currencyCode(request.getCurrencyCode())
                .merchantId(merchant.getMerchantId())
                .terminalId(terminal.getTerminalId())
                .status(txnStatus)
                .responseCode(authResponse.getResponseCode())
                .channel(channel)
                .posEntryMode(posEntryMode)
                .posConditionCode(posConditionCode)
                .transactionType("PURC")
                .requestAt(OffsetDateTime.now())
                .responseAt(OffsetDateTime.now())
                .processingTimeMs((int) authResponse.getProcessingTimeMs())
                .build();

        transactionRepository.save(txn);
        log.info("POS transaction persisted: id={}, decision={}, code={}, brand={}, channel={}",
                txn.getId(), decisionStr, authResponse.getResponseCode(), card.getCardBrand(), channel);

        return PosResult.builder()
                .decision(decisionStr)
                .responseCode(authResponse.getResponseCode())
                .message(authResponse.getReason() != null ? authResponse.getReason() : decisionStr)
                .authorizationId(authResponse.getAuthDecisionId())
                .riskScore(authResponse.getFraudScore())
                .riskDecision(decisionStr.equals("APPROVED") ? "APPROVED" : "DECLINED")
                .transactionId(txn.getId())
                .stan(stan)
                .posEntryMode(posEntryMode)
                .posConditionCode(posConditionCode)
                .channel(channel)
                .cardBrand(card.getCardBrand() != null ? card.getCardBrand().name() : null)
                .cardType(card.getCardType() != null ? card.getCardType().name() : null)
                .pinStatus(pinStatus)
                .processingTimeMs(authResponse.getProcessingTimeMs())
                .build();
    }

    private PosResult buildDeclined(String decision, String responseCode, String message) {
        return PosResult.builder()
                .decision(decision)
                .responseCode(responseCode)
                .message(message)
                .riskScore(0)
                .riskDecision("DECLINED")
                .build();
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class PosRequest {
        private UUID cardId;
        private UUID merchantId;
        private UUID terminalId;
        private BigDecimal amount;
        private String currencyCode;
        private String countryCode;
        private String entryMode;
        private boolean pinVerified;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class PosResult {
        private String decision;
        private String responseCode;
        private String message;
        private Long authorizationId;
        private Integer riskScore;
        private String riskDecision;
        private UUID transactionId;
        private String stan;
        private String posEntryMode;
        private String posConditionCode;
        private String channel;
        private String cardBrand;
        private String cardType;
        private String pinStatus;
        private long processingTimeMs;
    }
}
