package com.switch.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solab.iso8583.IsoMessage;
import com.switch.platform.iso8583.Iso8583Engine;
import com.switch.platform.iso20022.Iso20022Engine;
import com.switch.platform.model.Participant;
import com.switch.platform.model.Transaction;
import com.switch.platform.repository.TransactionRepository;
import com.switch.platform.router.RoutingContext;
import com.switch.platform.router.RoutingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ParticipantService participantService;
    private final MessageHandlerService messageHandlerService;
    private final ObjectMapper objectMapper;

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
        Map<String, String> details = iso20022Engine.extractPaymentDetails(doc);

        String msgId = UUID.randomUUID().toString().replace("-", "").substring(0, 30);
        String pan = details.getOrDefault("IBAN", "UNKNOWN");
        String amount = details.getOrDefault("IntrBkSttlmAmt", "0");
        String currency = "EUR";
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

            String responseXml = messageHandlerService.sendAndReceiveXml(
                    destination, xmlMessage);

            Document responseDoc = iso20022Engine.parse(responseXml);
            String status = "ACCP";

            transaction.setResponseCode(status);
            transaction.setResponseMessage(responseXml);
            transaction.setStatus("ACCP".equals(status) ?
                    Transaction.TransactionStatus.COMPLETED :
                    Transaction.TransactionStatus.FAILED);
            transaction.setResponseAt(OffsetDateTime.now());

        } catch (Exception e) {
            log.error("ISO 20022 transaction failed: {}", e.getMessage());
            transaction.setResponseCode("RJCT");
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setResponseAt(OffsetDateTime.now());
        }

        return transactionRepository.save(transaction);
    }

    private RoutingRule routingRuleFromResult(RoutingResult result) {
        return routingRuleRepository.findById(result.getRuleId()).orElse(null);
    }

    private String hashPan(String pan) {
        if (pan == null) return null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(pan.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
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
