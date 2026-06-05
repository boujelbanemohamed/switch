package com.switchplatform.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solab.iso8583.IsoMessage;
import com.switchplatform.platform.iso8583.Iso8583Engine;
import com.switchplatform.platform.iso20022.Iso20022Engine;
import com.switchplatform.platform.model.BinTable;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.RoutingRule;
import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.repository.BinTableRepository;
import com.switchplatform.platform.repository.RoutingRuleRepository;
import com.switchplatform.platform.repository.TransactionRepository;
import com.switchplatform.platform.router.RoutingContext;
import com.switchplatform.platform.router.RoutingResult;
import com.switchplatform.platform.service.acquiring.MerchantService;
import com.switchplatform.platform.service.acquiring.SettlementService;
import com.switchplatform.platform.service.clearing.ClearingService;
import com.switchplatform.platform.service.clearing.InterchangeService;
import com.switchplatform.platform.service.standin.StandInService;
import com.switchplatform.platform.model.standin.StandInAuthorization;
import com.switchplatform.platform.service.ledger.LedgerPostingEngine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SwitchCore {

    private final Iso8583Engine iso8583Engine;
    private final Iso20022Engine iso20022Engine;
    private final TransactionRouterService routerService;
    private final TransactionRepository transactionRepository;
    private final RoutingRuleRepository routingRuleRepository;
    private final BinTableRepository binTableRepository;
    private final ParticipantService participantService;
    private final MessageHandlerService messageHandlerService;
    private final ObjectMapper objectMapper;
    private final MerchantService merchantService;
    private final ClearingService clearingService;
    private final SettlementService settlementService;
    private final InterchangeService interchangeService;
    private final StandInService standInService;
    private final LedgerPostingEngine ledgerPostingEngine;

    @Value("${switch.pan.hash-key}")
    private String panHashKey;

    @PostConstruct
    public void validateConfig() {
        if (panHashKey == null || panHashKey.isBlank()) {
            throw new IllegalStateException("switch.pan.hash-key must be set via PAN_HASH_KEY environment variable");
        }
    }

    public Transaction processIso8583Message(byte[] rawMessage, String sourceCode) {
        if (rawMessage == null || rawMessage.length == 0) {
            throw new IllegalArgumentException("Raw ISO 8583 message must not be null or empty");
        }
        if (rawMessage.length > 4096) {
            throw new IllegalArgumentException("Raw ISO 8583 message exceeds maximum length of 4096 bytes");
        }
        IsoMessage isoMsg = iso8583Engine.parse(rawMessage);
        String mti = String.format("%04d", isoMsg.getType());
        String pan = isoMsg.getField(2) != null ? isoMsg.getField(2).toString() : null;
        String stan = isoMsg.getField(11) != null ? isoMsg.getField(11).toString() : null;
        String amount = isoMsg.getField(4) != null ? isoMsg.getField(4).toString() : null;
        String currency = isoMsg.getField(49) != null ? isoMsg.getField(49).toString() : null;
        String merchantId = isoMsg.getField(42) != null ? isoMsg.getField(42).toString() : null;
        String terminalId = isoMsg.getField(41) != null ? isoMsg.getField(41).toString() : null;
        String rrn = isoMsg.getField(37) != null ? isoMsg.getField(37).toString() : null;

        String bin = pan != null && pan.length() >= 6 ? pan.substring(0, 6) : pan;

        Participant source = participantService.findByCode(sourceCode);

        if ("0800".equals(mti)) {
            return handleNetworkManagementRequest(isoMsg, source);
        }

        Map<String, Object> parsedFields = new HashMap<>();
        parsedFields.put("mti", mti);
        for (int i = 2; i <= 128; i++) {
            if (isoMsg.getField(i) != null) {
                parsedFields.put("field_" + i, isoMsg.getField(i).toString());
            }
        }

        String posEntryMode = isoMsg.getField(22) != null ? isoMsg.getField(22).toString() : null;
        String posConditionCode = isoMsg.getField(25) != null ? isoMsg.getField(25).toString() : null;
        String processingCode = isoMsg.getField(3) != null ? isoMsg.getField(3).toString() : null;
        String channel = deriveChannel(posEntryMode);
        String transactionType = deriveTransactionType(processingCode, mti);

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString().replace("-", "").substring(0, 20) + System.currentTimeMillis())
                .messageType(mti)
                .protocol(Transaction.Protocol.ISO8583)
                .stan(stan)
                .rrn(rrn)
                .panHash(hashPan(pan))
                .amount(amount != null ? new BigDecimal(amount) : null)
                .currencyCode(currency)
                .merchantId(merchantId)
                .terminalId(terminalId)
                .sourceParticipant(source)
                .posEntryMode(posEntryMode)
                .posConditionCode(posConditionCode)
                .channel(channel)
                .transactionType(transactionType)
                .status(Transaction.TransactionStatus.PENDING)
                .originalMessage(bytesToHex(rawMessage))
                .build();
        try {
            transaction.setParsedMessage(objectMapper.writeValueAsString(parsedFields));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise parsed message: {}", e.getMessage());
        }

        transaction = transactionRepository.save(transaction);

        if ("0220".equals(mti)) {
            return processAdvice(transaction);
        }

        RoutingContext context = RoutingContext.builder()
                .pan(pan)
                .bin(bin)
                .amount(transaction.getAmount())
                .currencyCode(currency)
                .mti(mti)
                .merchantId(merchantId)
                .terminalId(terminalId)
                .source(sourceCode)
                .protocol("ISO8583")
                .stan(stan)
                .transactionId(transaction.getId())
                .build();

        transaction.setStatus(Transaction.TransactionStatus.ROUTING);
        transactionRepository.save(transaction);

        RoutingResult routing = routerService.routeTransaction(context);

        if (!routing.isMatched()) {
            transaction.setStatus(Transaction.TransactionStatus.REJECTED);
            transaction.setResponseCode("30");
            transaction.setResponseAt(OffsetDateTime.now());
            transactionRepository.save(transaction);

            log.warn("Transaction {} rejected: no routing rule matched", transaction.getTransactionId());
            return transaction;
        }

        if ("0420".equals(mti)) {
            return processReversal(transaction, routing, rawMessage, pan, mti, sourceCode);
        }

        if ("0100".equals(mti)) {
            return processPreAuth(transaction, routing, rawMessage, pan, mti, sourceCode);
        }

        transaction.setDestinationParticipant(routing.getDestinationParticipant());
        transaction.setRoutingRule(
                routingRuleFromResult(routing));
        transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);

        IsoMessage response;
        try {
            Participant destination = routing.getDestinationParticipant();
            String endpointUrl = destination.getEndpointUrl();

            byte[] responseData = messageHandlerService.sendAndReceive(
                    destination, rawMessage);

            response = iso8583Engine.parse(responseData);
            String respCode = response.getField(39) != null ?
                    response.getField(39).toString() : "99";

            transaction.setResponseCode(respCode);
            transaction.setResponseMessage(bytesToHex(responseData));
            transaction.setStatus("00".equals(respCode) ?
                    Transaction.TransactionStatus.COMPLETED :
                    Transaction.TransactionStatus.FAILED);
            transaction.setResponseAt(OffsetDateTime.now());
            transaction.setProcessingTimeMs(calculateProcessingTime(transaction.getRequestAt()));

            if (transaction.getStatus() == Transaction.TransactionStatus.COMPLETED) {
                postProcessTransaction(transaction, pan, mti);
            }

        } catch (Exception e) {
            log.error("Transaction {} failed: {}", transaction.getTransactionId(), e.getMessage());
            StandInAuthorization standIn = standInService.attemptStandIn(
                    transaction.getTransactionId(),
                    transaction.getIssuingParticipant() != null ? transaction.getIssuingParticipant().getId() : null,
                    "VISA",
                    transaction.getPanHash() != null && transaction.getPanHash().length() >= 4
                            ? transaction.getPanHash().substring(transaction.getPanHash().length() - 4) : null,
                    transaction.getAmount(),
                    transaction.getCurrencyCode() != null ? transaction.getCurrencyCode() : "TND",
                    null);
            if (standIn.getDecision() == StandInAuthorization.Decision.APPROVED) {
                transaction.setResponseCode("00");
                transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                transaction.setStandInUsed(true);
                log.info("Stand-in approved for transaction {} (issuer unreachable)", transaction.getTransactionId());
            } else {
                transaction.setResponseCode("99");
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                log.warn("Stand-in declined for transaction {}: {}", transaction.getTransactionId(), standIn.getReason());
            }
            transaction.setResponseAt(OffsetDateTime.now());
        }

        return transactionRepository.save(transaction);
    }

    private Transaction processReversal(Transaction transaction, RoutingResult routing,
                                         byte[] rawMessage, String pan, String mti, String sourceCode) {
        Participant destination = routing.getDestinationParticipant();
        transaction.setDestinationParticipant(destination);
        transaction.setRoutingRule(routingRuleFromResult(routing));
        transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);

        try {
            byte[] responseData = messageHandlerService.sendAndReceive(destination, rawMessage);
            IsoMessage response = iso8583Engine.parse(responseData);
            String respCode = response.getField(39) != null ?
                    response.getField(39).toString() : "99";

            transaction.setResponseCode(respCode);
            transaction.setResponseMessage(bytesToHex(responseData));
            transaction.setStatus("00".equals(respCode) ?
                    Transaction.TransactionStatus.COMPLETED :
                    Transaction.TransactionStatus.FAILED);
            transaction.setResponseAt(OffsetDateTime.now());
            transaction.setProcessingTimeMs(calculateProcessingTime(transaction.getRequestAt()));

            if (transaction.getStatus() == Transaction.TransactionStatus.COMPLETED
                    && transaction.getAmount() != null) {
                try {
                    ledgerPostingEngine.postReversal(transaction.getTransactionId(),
                            transaction.getAmount(), transaction.getCurrencyCode(),
                            "Network reversal");
                } catch (Exception le) {
                    log.warn("Reversal ledger posting failed: {}", le.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Reversal transaction {} failed: {}", transaction.getTransactionId(), e.getMessage());
            transaction.setResponseCode("99");
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setResponseAt(OffsetDateTime.now());
        }
        return transactionRepository.save(transaction);
    }

    private Transaction handleNetworkManagementRequest(IsoMessage isoMsg, Participant source) {
        String functionCode = isoMsg.getField(70) != null ? isoMsg.getField(70).toString() : "301";
        log.info("Network management request received: functionCode={}", functionCode);

        String txId = UUID.randomUUID().toString().replace("-", "").substring(0, 20) + System.currentTimeMillis();
        Transaction transaction = Transaction.builder()
                .transactionId(txId)
                .messageType("0800")
                .protocol(Transaction.Protocol.ISO8583)
                .sourceParticipant(source)
                .status(Transaction.TransactionStatus.COMPLETED)
                .responseCode("00")
                .build();

        IsoMessage response = iso8583Engine.createNetworkManagementResponse(isoMsg, "00");
        byte[] responseData = iso8583Engine.encode(response);
        transaction.setResponseMessage(bytesToHex(responseData));
        transaction.setResponseAt(OffsetDateTime.now());
        return transactionRepository.save(transaction);
    }

    @Async("switchTaskExecutor")
    public CompletableFuture<Transaction> processIso8583MessageAsync(
            byte[] rawMessage, String sourceCode) {
        return CompletableFuture.completedFuture(
                processIso8583Message(rawMessage, sourceCode));
    }

    public Transaction processIso20022Message(String xmlMessage, String sourceCode) {
        Document doc = iso20022Engine.parse(xmlMessage);
        String msgType = iso20022Engine.detectMessageType(doc);
        Map<String, String> details = iso20022Engine.extractPaymentDetails(doc);

        return switch (msgType) {
            case "pacs.008" -> processIso20022CreditTransfer(doc, details, xmlMessage, sourceCode);
            case "pacs.004" -> processIso20022Return(doc, details, xmlMessage, sourceCode);
            case "camt.056" -> processIso20022Claim(doc, details, xmlMessage, sourceCode);
            case "pacs.002" -> processIso20022StatusReport(doc, details, xmlMessage, sourceCode);
            default -> {
                log.warn("Unsupported ISO 20022 message type: {}", msgType);
                Transaction tx = Transaction.builder()
                        .transactionId(UUID.randomUUID().toString().replace("-", "").substring(0, 30))
                        .messageType(msgType)
                        .protocol(Transaction.Protocol.ISO20022)
                        .status(Transaction.TransactionStatus.REJECTED)
                        .responseCode("RJCT")
                        .originalMessage(xmlMessage)
                        .build();
                yield transactionRepository.save(tx);
            }
        };
    }

    private Transaction processIso20022CreditTransfer(Document doc, Map<String, String> details,
                                                       String xmlMessage, String sourceCode) {
        String msgId = UUID.randomUUID().toString().replace("-", "").substring(0, 30);
        String pan = details.getOrDefault("IBAN", "UNKNOWN");
        String amount = details.getOrDefault("IntrBkSttlmAmt", "0");
        String currency = details.getOrDefault("Ccy", "EUR");
        String merchantId = details.getOrDefault("BICFI", "");

        String bin = pan.length() >= 6 ? pan.substring(0, 6) : pan;
        Participant source = participantService.findByCode(sourceCode);

        Transaction transaction = Transaction.builder()
                .transactionId(msgId)
                .messageType("pacs.008")
                .protocol(Transaction.Protocol.ISO20022)
                .panHash(hashPan(pan))
                .amount(new BigDecimal(amount))
                .currencyCode(currency)
                .sourceParticipant(source)
                .status(Transaction.TransactionStatus.PENDING)
                .originalMessage(xmlMessage)
                .parsedMessage(details.toString())
                .build();

        transaction = transactionRepository.save(transaction);
        transaction.setStatus(Transaction.TransactionStatus.ROUTING);
        transaction = transactionRepository.save(transaction);

        RoutingContext context = RoutingContext.builder()
                .pan(pan)
                .bin(bin)
                .amount(transaction.getAmount())
                .currencyCode(currency)
                .mti("pacs.008")
                .merchantId(merchantId)
                .source(sourceCode)
                .protocol("ISO20022")
                .transactionId(transaction.getId())
                .build();

        RoutingResult routing = routerService.routeTransaction(context);

        if (!routing.isMatched()) {
            transaction.setStatus(Transaction.TransactionStatus.REJECTED);
            transaction.setResponseCode("RJCT");
            transaction.setResponseAt(OffsetDateTime.now());
            transactionRepository.save(transaction);
            return transaction;
        }

        transaction.setDestinationParticipant(routing.getDestinationParticipant());
        transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);

        try {
            Participant destination = routing.getDestinationParticipant();
            String responseXml = messageHandlerService.sendAndReceiveXml(destination, xmlMessage);
            Document responseDoc = iso20022Engine.parse(responseXml);
            String status = "ACCP";
            transaction.setResponseCode(status);
            transaction.setResponseMessage(responseXml);
            transaction.setStatus("ACCP".equals(status) ?
                    Transaction.TransactionStatus.COMPLETED : Transaction.TransactionStatus.FAILED);
            transaction.setResponseAt(OffsetDateTime.now());
            if (transaction.getStatus() == Transaction.TransactionStatus.COMPLETED) {
                postProcessTransaction(transaction, pan, "pacs.008");
            }
        } catch (Exception e) {
            log.error("ISO 20022 credit transfer failed: {}", e.getMessage());
            transaction.setResponseCode("RJCT");
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setResponseAt(OffsetDateTime.now());
        }
        return transactionRepository.save(transaction);
    }

    private Transaction processIso20022Return(Document doc, Map<String, String> details,
                                               String xmlMessage, String sourceCode) {
        String msgId = UUID.randomUUID().toString().replace("-", "").substring(0, 30);
        String amount = details.getOrDefault("RtrdIntrBkSttlmAmt", "0");
        Participant source = participantService.findByCode(sourceCode);

        Transaction transaction = Transaction.builder()
                .transactionId(msgId)
                .messageType("pacs.004")
                .protocol(Transaction.Protocol.ISO20022)
                .amount(new BigDecimal(amount))
                .sourceParticipant(source)
                .status(Transaction.TransactionStatus.PENDING)
                .originalMessage(xmlMessage)
                .parsedMessage(details.toString())
                .build();
        transaction = transactionRepository.save(transaction);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setResponseCode("ACCP");
        transaction.setResponseAt(OffsetDateTime.now());
        log.info("Payment return processed: msgId={}, amount={}", msgId, amount);
        return transactionRepository.save(transaction);
    }

    private Transaction processIso20022Claim(Document doc, Map<String, String> details,
                                              String xmlMessage, String sourceCode) {
        String msgId = UUID.randomUUID().toString().replace("-", "").substring(0, 30);
        String amount = details.getOrDefault("ClmAmt", "0");
        Participant source = participantService.findByCode(sourceCode);

        Transaction transaction = Transaction.builder()
                .transactionId(msgId)
                .messageType("camt.056")
                .protocol(Transaction.Protocol.ISO20022)
                .amount(new BigDecimal(amount))
                .sourceParticipant(source)
                .status(Transaction.TransactionStatus.PENDING)
                .originalMessage(xmlMessage)
                .parsedMessage(details.toString())
                .build();
        transaction = transactionRepository.save(transaction);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setResponseCode("ACCP");
        transaction.setResponseAt(OffsetDateTime.now());
        log.info("Claim non-receipt processed: msgId={}, amount={}", msgId, amount);
        return transactionRepository.save(transaction);
    }

    private Transaction processIso20022StatusReport(Document doc, Map<String, String> details,
                                                     String xmlMessage, String sourceCode) {
        String msgId = UUID.randomUUID().toString().replace("-", "").substring(0, 30);
        Participant source = participantService.findByCode(sourceCode);

        Transaction transaction = Transaction.builder()
                .transactionId(msgId)
                .messageType("pacs.002")
                .protocol(Transaction.Protocol.ISO20022)
                .sourceParticipant(source)
                .status(Transaction.TransactionStatus.COMPLETED)
                .originalMessage(xmlMessage)
                .parsedMessage(details.toString())
                .responseCode("ACCP")
                .build();
        log.info("Payment status report received: msgId={}, status={}",
                details.get("OrgnlMsgId"), details.get("GrpSts"));
        return transactionRepository.save(transaction);
    }

    private void postProcessTransaction(Transaction transaction, String pan, String mti) {
        String panValue = pan;
        if (panValue == null && transaction.getParsedMessage() != null) {
            try {
                var parsed = objectMapper.readTree(transaction.getParsedMessage());
                var field2 = parsed.get("field_2");
                if (field2 != null && !field2.isNull()) panValue = field2.asText();
            } catch (Exception e) {
                log.warn("Failed to parse PAN from parsedMessage: {}", e.getMessage());
            }
        }

        String txType = transaction.getTransactionType();
        boolean shouldClear = txType == null || "PURC".equals(txType) || "REFD".equals(txType) || "COMP".equals(txType);

        if (transaction.getMerchantId() != null && transaction.getAmount() != null) {
            try {
                merchantService.getMerchantByCode(transaction.getMerchantId())
                        .ifPresent(merchant -> {
                            BigDecimal fee = merchantService.calculateMdr(
                                    merchant.getId(), transaction.getAmount(), "VISA", "DEBIT");
                            log.info("MDR calculated: merchant={}, amount={}, fee={}",
                                    merchant.getMerchantId(), transaction.getAmount(), fee);
                        });
            } catch (Exception e) {
                log.warn("MDR calculation failed for transaction {}: {}",
                        transaction.getTransactionId(), e.getMessage());
            }
        }

        if (transaction.getMerchantId() != null && transaction.getAmount() != null) {
            try {
                BigDecimal settleAmount = "REFD".equals(txType)
                        ? transaction.getAmount().negate()
                        : transaction.getAmount();
                settlementService.recordTransactionForSettlement(
                        transaction.getMerchantId(),
                        settleAmount,
                        "VISA",
                        "DEBIT");
            } catch (Exception e) {
                log.warn("Settlement recording failed for transaction {}: {}",
                        transaction.getTransactionId(), e.getMessage());
            }
        }

        if (transaction.getAmount() != null && transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                var interchangeResult = interchangeService.calculateInterchange(
                        "VISA", "DEBIT", "TN", "*",
                        transaction.getAmount(),
                        transaction.getCurrencyCode() != null ? transaction.getCurrencyCode() : "TND");
                log.info("Interchange calculated for transaction {}: fee={}, breakdown={}",
                        transaction.getTransactionId(), interchangeResult.totalFee(), interchangeResult.breakdown());
            } catch (Exception e) {
                log.warn("Interchange calculation failed for transaction {}: {}",
                        transaction.getTransactionId(), e.getMessage());
            }
        }

        if (shouldClear) {
            try {
                if (transaction.getAmount() != null
                        && transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {

                    String tradingName = null;
                    String merchantCategoryCode = null;
                    if (transaction.getMerchantId() != null) {
                        try {
                            Optional<Merchant> merchantOpt = merchantService.getMerchantByCode(transaction.getMerchantId());
                            if (merchantOpt.isPresent()) {
                                Merchant m = merchantOpt.get();
                                tradingName = m.getTradingName();
                                merchantCategoryCode = m.getMerchantCategoryCode();
                            }
                        } catch (Exception e) {
                            log.warn("Merchant lookup failed for {}: {}", transaction.getMerchantId(), e.getMessage());
                        }
                    }

                    String mcc = null;
                    if (transaction.getParsedMessage() != null) {
                        try {
                            var parsed = objectMapper.readTree(transaction.getParsedMessage());
                            var field18 = parsed.get("field_18");
                            if (field18 != null && !field18.isNull()) {
                                mcc = field18.asText();
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse MCC from parsedMessage: {}", e.getMessage());
                        }
                    }
                    if (mcc == null || mcc.isBlank()) {
                        mcc = merchantCategoryCode;
                    }

                    String cardBrand = null;
                    if (panValue != null && panValue.length() >= 6) {
                        try {
                            String bin = panValue.substring(0, 6);
                            Optional<BinTable> binOpt = binTableRepository.findByBinAndIsActiveTrue(bin);
                            if (binOpt.isPresent()) {
                                BinTable.CardBrand brand = binOpt.get().getCardBrand();
                                if (brand != null) cardBrand = brand.name();
                            }
                        } catch (Exception e) {
                            log.warn("BIN lookup failed for PAN prefix: {}", e.getMessage());
                        }
                    }

                    ClearingService.ClearingData clearingData = ClearingService.ClearingData.builder()
                            .transactionId(transaction.getTransactionId())
                            .acquiringParticipantId(transaction.getAcquiringParticipant() != null
                                    ? transaction.getAcquiringParticipant().getId() : null)
                            .issuingParticipantId(transaction.getIssuingParticipant() != null
                                    ? transaction.getIssuingParticipant().getId() : null)
                            .amount(transaction.getAmount())
                            .currencyCode(transaction.getCurrencyCode() != null
                                    ? transaction.getCurrencyCode() : "TND")
                            .messageType(mti)
                            .transactionDate(OffsetDateTime.now())
                            .merchantNumber(transaction.getMerchantId())
                            .cardNumber(panValue)
                            .merchantCategoryCode(mcc)
                            .cardBrand(cardBrand)
                            .tradingName(tradingName)
                            .slipNumber(null)
                            .representationFlag(false)
                            .build();
                    clearingService.processClearing(clearingData);
                    log.info("Clearing submitted for transaction {}", transaction.getTransactionId());
                }
            } catch (Exception e) {
                log.warn("Clearing submission failed for transaction {}: {}",
                        transaction.getTransactionId(), e.getMessage());
            }
        }
    }

    private Transaction processAdvice(Transaction transaction) {
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setResponseCode("00");
        transaction.setResponseAt(OffsetDateTime.now());
        transaction = transactionRepository.save(transaction);
        try {
            if ("COMP".equals(transaction.getTransactionType())) {
                if (transaction.getAmount() != null && transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    postProcessTransaction(transaction, null, "0220");
                }
            } else {
                ledgerPostingEngine.postReversal(transaction.getTransactionId(),
                        transaction.getAmount(), transaction.getCurrencyCode(), "Advice recorded");
            }
        } catch (Exception le) {
            log.warn("Advice processing failed for {}: {}", transaction.getTransactionId(), le.getMessage());
        }
        return transaction;
    }

    private Transaction processPreAuth(Transaction transaction, RoutingResult routing,
                                        byte[] rawMessage, String pan, String mti, String sourceCode) {
        transaction.setDestinationParticipant(routing.getDestinationParticipant());
        transaction.setRoutingRule(routingRuleFromResult(routing));
        transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);

        try {
            Participant destination = routing.getDestinationParticipant();
            byte[] responseData = messageHandlerService.sendAndReceive(destination, rawMessage);
            IsoMessage response = iso8583Engine.parse(responseData);
            String respCode = response.getField(39) != null ?
                    response.getField(39).toString() : "99";

            transaction.setResponseCode(respCode);
            transaction.setResponseMessage(bytesToHex(responseData));
            transaction.setStatus("00".equals(respCode) ?
                    Transaction.TransactionStatus.COMPLETED :
                    Transaction.TransactionStatus.FAILED);
            transaction.setResponseAt(OffsetDateTime.now());
            transaction.setProcessingTimeMs(calculateProcessingTime(transaction.getRequestAt()));
        } catch (Exception e) {
            log.error("Pre-auth {} failed: {}", transaction.getTransactionId(), e.getMessage());
            transaction.setResponseCode("99");
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setResponseAt(OffsetDateTime.now());
        }
        return transactionRepository.save(transaction);
    }

    private static String deriveChannel(String posEntryMode) {
        if (posEntryMode == null) return "POS";
        String m = posEntryMode.trim();
        if (m.length() >= 2) {
            String prefix = m.substring(0, 2);
            if ("06".equals(prefix)) return "ATM";
            int p = Integer.parseInt(prefix);
            if (p >= 10 && p <= 19) return "ATM";
            if (p >= 80 && p <= 99) return "ECOM";
        }
        return "POS";
    }

    private static String deriveTransactionType(String processingCode, String mti) {
        if ("0420".equals(mti)) return "REVS";
        if ("0220".equals(mti)) {
            if (processingCode != null && processingCode.length() >= 2
                    && "02".equals(processingCode.substring(0, 2))) {
                return "COMP";
            }
            return "REVS";
        }
        if ("0100".equals(mti)) return "PRAU";
        if (processingCode != null && processingCode.length() >= 2) {
            return switch (processingCode.substring(0, 2)) {
                case "00" -> "PURC";
                case "01" -> "PRAU";
                case "02" -> "COMP";
                case "20" -> "REFD";
                case "21" -> "VOID";
                default -> "OTHR";
            };
        }
        return "OTHR";
    }

    private RoutingRule routingRuleFromResult(RoutingResult result) {
        return routingRuleRepository.findById(result.getRuleId()).orElse(null);
    }

    private String hashPan(String pan) {
        if (pan == null) return null;
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec spec = new javax.crypto.spec.SecretKeySpec(
                    panHashKey.getBytes("UTF-8"), "HmacSHA256");
            mac.init(spec);
            byte[] hash = mac.doFinal(pan.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("PAN hash failed", e);
            return pan;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private int calculateProcessingTime(OffsetDateTime start) {
        if (start == null) return 0;
        return (int) java.time.Duration.between(start, OffsetDateTime.now()).toMillis();
    }
}
