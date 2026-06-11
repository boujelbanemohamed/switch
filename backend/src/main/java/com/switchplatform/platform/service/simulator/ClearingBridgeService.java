package com.switchplatform.platform.service.simulator;

import com.switchplatform.platform.model.BinTable;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.ecommerce.EpgTransaction;
import com.switchplatform.platform.model.issuing.Card;
import com.switchplatform.platform.repository.BinTableRepository;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.acquiring.MerchantRepository;
import com.switchplatform.platform.repository.ecommerce.EpgTransactionRepository;
import com.switchplatform.platform.service.clearing.ClearingService;
import com.switchplatform.platform.service.issuing.CardService;
import lombok.Builder;
import lombok.Data;
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
    private final MerchantRepository merchantRepository;
    private final CardService cardService;
    private final BinTableRepository binTableRepository;
    private final ParticipantRepository participantRepository;

    @Data
    @Builder
    public static class ClearingItem {
        private UUID epgTransactionId;
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
    public static class ClearingRecordSummary {
        private UUID clearingId;
        private UUID epgTransactionId;
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
                records.add(ClearingRecordSummary.builder()
                        .clearingId(record.getId())
                        .epgTransactionId(item.getEpgTransactionId())
                        .transactionId(record.getTransactionId())
                        .amount(record.getAmount())
                        .currencyCode(record.getCurrencyCode())
                        .cardBrand(record.getCardBrand())
                        .interchangeAmount(record.getInterchangeAmount())
                        .netAmount(record.getNetAmount())
                        .status(record.getStatus().name())
                        .build());
            } catch (Exception e) {
                errors++;
                records.add(ClearingRecordSummary.builder()
                        .epgTransactionId(item.getEpgTransactionId())
                        .error(e.getMessage())
                        .build());
                log.error("Failed to clear EPG transaction {}: {}", item.getEpgTransactionId(), e.getMessage());
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
