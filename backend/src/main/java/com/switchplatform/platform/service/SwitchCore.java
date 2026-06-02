package com.switchplatform.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solab.iso8583.IsoMessage;
import com.switchplatform.platform.iso8583.Iso8583Engine;
import com.switchplatform.platform.iso20022.Iso20022Engine;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.RoutingRule;
import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.repository.RoutingRuleRepository;
import com.switchplatform.platform.repository.TransactionRepository;
import com.switchplatform.platform.router.RoutingContext;
import com.switchplatform.platform.router.RoutingResult;
import com.switchplatform.platform.service.acquiring.MerchantService;
import com.switchplatform.platform.service.acquiring.SettlementService;
import com.switchplatform.platform.service.clearing.ClearingService;
import com.switchplatform.platform.service.clearing.InterchangeService;
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
    private final ParticipantService participantService;
    private final MessageHandlerService messageHandlerService;
    private final ObjectMapper objectMapper;
    private final MerchantService merchantService;
    private final ClearingService clearingService;
    private final SettlementService settlementService;
    private final InterchangeService interchangeService;

    @Value("${switch.pan.hash-key:}")
    private String panHashKey = "test-key-not-for-production";

    public Transaction processIso8583Message(byte[] rawMessage, String sourceCode) {
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

        Map<String, Object> parsedFields = new HashMap<>();
        parsedFields.put("mti", mti);
        for (int i = 2; i <= 128; i++) {
            if (isoMsg.getField(i) != null) {
                parsedFields.put("field_" + i, isoMsg.getField(i).toString());
            }
        }

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
                .status(Transaction.TransactionStatus.PENDING)
                .originalMessage(bytesToHex(rawMessage))
                .build();
        try {
            transaction.setParsedMessage(objectMapper.writeValueAsString(parsedFields));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise parsed message: {}", e.getMessage());
        }

        transaction = transactionRepository.save(transaction);

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
            transaction.setResponseCode("99");
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setResponseAt(OffsetDateTime.now());
        }

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
        // Module B: Calculate MDR fee for merchant
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

        // Module B1: Record transaction for settlement
        if (transaction.getMerchantId() != null && transaction.getAmount() != null) {
            try {
                settlementService.recordTransactionForSettlement(
                        transaction.getMerchantId(),
                        transaction.getAmount(),
                        "VISA",
                        "DEBIT");
            } catch (Exception e) {
                log.warn("Settlement recording failed for transaction {}: {}",
                        transaction.getTransactionId(), e.getMessage());
            }
        }

        // Module E1: Auto-calculate interchange fee
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

        // Module E: Auto-submit to clearing
        try {
            if (transaction.getAmount() != null
                    && transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
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
                        .build();
                clearingService.processClearing(clearingData);
                log.info("Clearing submitted for transaction {}", transaction.getTransactionId());
            }
        } catch (Exception e) {
            log.warn("Clearing submission failed for transaction {}: {}",
                    transaction.getTransactionId(), e.getMessage());
        }
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
