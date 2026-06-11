package com.switchplatform.platform.service.simulator;

import com.switchplatform.platform.model.BinTable;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.repository.BinTableRepository;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.TransactionRepository;
import com.switchplatform.platform.repository.acquiring.MerchantRepository;
import com.switchplatform.platform.repository.ecommerce.EpgTransactionRepository;
import com.switchplatform.platform.service.clearing.ClearingService;
import com.switchplatform.platform.service.issuing.CardService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClearingBridgeService {

    private final ClearingService clearingService;
    private final EpgTransactionRepository epgTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final CardService cardService;
    private final BinTableRepository binTableRepository;
    private final ParticipantRepository participantRepository;

    @Data
    @Builder
    public static class ClearingItem {
        private UUID epgTransactionId;
        private UUID transactionId;
        private UUID cardId;
    }

    @Data
    @Builder
    public static class ClearingBatchResult {
        private int totalRequested;
        private int cleared;
        private int errors;
        private List<ClearingRecordSummary> records;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClearingRecordSummary {
        private UUID clearingId;
        private UUID epgTransactionId;
        private UUID posTransactionId;
        private String transactionId;
        private BigDecimal amount;
        private String currencyCode;
        private String cardBrand;
        private BigDecimal interchangeAmount;
        private BigDecimal netAmount;
        private String status;
        private String error;
    }

    @Transactional
    public ClearingBatchResult clearTransactions(List<ClearingItem> items) {
        List<ClearingRecordSummary> records = new ArrayList<>();
        int cleared = 0;
        int errors = 0;

        for (ClearingItem item : items) {
            try {
                com.switchplatform.platform.model.clearing.ClearingRecord record = clearSingleTransaction(item);
                cleared++;
                ClearingRecordSummary s = new ClearingRecordSummary();
                s.setClearingId(record.getId());
                s.setTransactionId(record.getTransactionId());
                s.setAmount(record.getAmount());
                s.setCurrencyCode(record.getCurrencyCode());
                s.setCardBrand(record.getCardBrand());
                s.setInterchangeAmount(record.getInterchangeAmount());
                s.setNetAmount(record.getNetAmount());
                s.setStatus(record.getStatus().name());
                if (item.getEpgTransactionId() != null) s.setEpgTransactionId(item.getEpgTransactionId());
                if (item.getTransactionId() != null) s.setPosTransactionId(item.getTransactionId());
                records.add(s);
            } catch (Exception e) {
                errors++;
                ClearingRecordSummary s = new ClearingRecordSummary();
                s.setError(e.getMessage());
                if (item.getEpgTransactionId() != null) s.setEpgTransactionId(item.getEpgTransactionId());
                if (item.getTransactionId() != null) s.setPosTransactionId(item.getTransactionId());
                records.add(s);
                String source = item.getEpgTransactionId() != null
                        ? "EPG " + item.getEpgTransactionId()
                        : "POS " + item.getTransactionId();
                log.error("Failed to clear transaction {}: {}", source, e.getMessage());
            }
        }

        return ClearingBatchResult.builder()
                .totalRequested(items.size())
                .cleared(cleared)
                .errors(errors)
                .records(records)
                .build();
    }

    private com.switchplatform.platform.model.clearing.ClearingRecord clearSingleTransaction(ClearingItem item) {
        if (item.getTransactionId() != null) {
            return clearPosTransaction(item);
        }
        return clearEcommerceTransaction(item);
    }

    private com.switchplatform.platform.model.clearing.ClearingRecord clearEcommerceTransaction(ClearingItem item) {
        EpgTransaction txn = epgTransactionRepository.findById(item.getEpgTransactionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "EPG transaction not found: " + item.getEpgTransactionId()));

        Card card = cardService.getCard(item.getCardId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Card not found: " + item.getCardId()));

        com.switchplatform.platform.model.acquiring.Merchant merchant =
                merchantRepository.findById(txn.getMerchantId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Merchant not found: " + txn.getMerchantId()));

        Participant acquiringParticipant = merchant.getAcquiringParticipant();
        if (acquiringParticipant == null) {
            throw new IllegalArgumentException(
                    "Merchant " + merchant.getId() + " has no acquiring participant");
        }

        Participant issuingParticipant = resolveIssuingParticipant(card);
        if (issuingParticipant == null) {
            throw new IllegalArgumentException(
                    "Could not resolve issuing participant for card " + item.getCardId());
        }

        String cardBrand = card.getCardBrand().name();
        String cardType = card.getCardType().name();

        ClearingService.ClearingData data = ClearingService.ClearingData.builder()
                .transactionId(txn.getId().toString())
                .acquiringParticipantId(acquiringParticipant.getId())
                .issuingParticipantId(issuingParticipant.getId())
                .amount(txn.getAmount())
                .currencyCode(txn.getCurrencyCode())
                .messageType("0100")
                .transactionDate(txn.getAuthorizedAt() != null
                        ? txn.getAuthorizedAt() : OffsetDateTime.now())
                .cardBrand(cardBrand)
                .cardType(cardType)
                .merchantCategoryCode(merchant.getMerchantCategoryCode())
                .region(merchant.getCountryCode() != null ? merchant.getCountryCode() : "TN")
                .merchantNumber(merchant.getMerchantId())
                .tradingName(merchant.getTradingName())
                .batchNumber("SIM-" + java.time.LocalDate.now())
                .slipNumber("000001")
                .build();

        return clearingService.processClearing(data);
    }

    private com.switchplatform.platform.model.clearing.ClearingRecord clearPosTransaction(ClearingItem item) {
        Transaction txn = transactionRepository.findById(item.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "POS transaction not found: " + item.getTransactionId()));
        log.info("Clearing POS transaction: id={}, amount={} {}",
                txn.getId(), txn.getAmount(), txn.getCurrencyCode());

        Card card = cardService.getCard(item.getCardId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Card not found: " + item.getCardId()));

        com.switchplatform.platform.model.acquiring.Merchant merchant =
                merchantRepository.findByMerchantId(txn.getMerchantId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Merchant not found by merchantId: " + txn.getMerchantId()));

        Participant acquiringParticipant = merchant.getAcquiringParticipant();
        if (acquiringParticipant == null) {
            throw new IllegalArgumentException(
                    "Merchant " + merchant.getId() + " has no acquiring participant");
        }

        Participant issuingParticipant = resolveIssuingParticipant(card);
        if (issuingParticipant == null) {
            throw new IllegalArgumentException(
                    "Could not resolve issuing participant for card " + item.getCardId());
        }

        String cardBrand = card.getCardBrand().name();
        String cardType = card.getCardType().name();

        ClearingService.ClearingData data = ClearingService.ClearingData.builder()
                .transactionId(txn.getId().toString())
                .acquiringParticipantId(acquiringParticipant.getId())
                .issuingParticipantId(issuingParticipant.getId())
                .amount(txn.getAmount())
                .currencyCode(txn.getCurrencyCode())
                .messageType("0100")
                .transactionDate(txn.getRequestAt() != null ? txn.getRequestAt() : OffsetDateTime.now())
                .cardBrand(cardBrand)
                .cardType(cardType)
                .merchantCategoryCode(merchant.getMerchantCategoryCode())
                .region(merchant.getCountryCode() != null ? merchant.getCountryCode() : "TN")
                .merchantNumber(merchant.getMerchantId())
                .tradingName(merchant.getTradingName())
                .batchNumber("SIM-POS-" + java.time.LocalDate.now())
                .slipNumber("000001")
                .build();

        return clearingService.processClearing(data);
    }

    private Participant resolveIssuingParticipant(Card card) {
        try {
            BinTable.CardBrand brand = BinTable.CardBrand.valueOf(card.getCardBrand().name());
            List<BinTable> matches = binTableRepository.findByCardBrand(brand);
            if (!matches.isEmpty()) {
                Participant participant = matches.get(0).getParticipant();
                if (participant != null) return participant;
            }
        } catch (Exception e) {
            log.warn("Could not resolve issuing participant via BIN table for card {}: {}",
                    card.getId(), e.getMessage());
        }

        List<Participant> issuers = participantRepository.findByType(Participant.ParticipantType.ISSUER);
        if (!issuers.isEmpty()) {
            return issuers.get(0);
        }

        List<Participant> any = participantRepository.findAll();
        if (!any.isEmpty()) {
            log.warn("No ISSUER participant found, falling back to first participant: {}", any.get(0).getName());
            return any.get(0);
        }

        return null;
    }
}
