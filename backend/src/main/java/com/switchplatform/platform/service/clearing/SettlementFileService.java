package com.switchplatform.platform.service.clearing;

import com.switchplatform.platform.event.ClearingFileGeneratedEvent;
import com.switchplatform.platform.event.EventPublisher;
import com.switchplatform.platform.iso20022.Iso20022Engine;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.model.clearing.ReconciliationRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.clearing.ClearingRecordRepository;
import com.switchplatform.platform.repository.clearing.ReconciliationRecordRepository;
import com.switchplatform.platform.service.clearing.network.Iso20022ClearingGenerator;
import com.switchplatform.platform.service.clearing.network.MastercardIpmGenerator;
import com.switchplatform.platform.service.clearing.network.NetworkClearingGenerator;
import com.switchplatform.platform.service.clearing.network.VisaBaseIIGenerator;
import com.switchplatform.platform.service.clearing.smt.CompconfFileService;
import com.switchplatform.platform.service.clearing.smt.Cp50FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementFileService {

    private final ClearingRecordRepository clearingRecordRepository;
    private final ParticipantRepository participantRepository;
    private final ReconciliationRecordRepository reconciliationRecordRepository;
    private final Iso20022Engine iso20022Engine;
    private final CompconfFileService compconfFileService;
    private final Cp50FileService cp50FileService;
    private final EventPublisher eventPublisher;
    private final Iso20022ClearingGenerator iso20022ClearingGenerator;
    private final VisaBaseIIGenerator visaBaseIIGenerator;
    private final MastercardIpmGenerator mastercardIpmGenerator;

    public String generateOutgoingClearingFile(LocalDate date, UUID participantId, String format) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + participantId));
        List<ClearingRecord> records = clearingRecordRepository
                .findByClearingDateAndAcquiringParticipantId(date, participantId);
        records.addAll(clearingRecordRepository
                .findByClearingDateAndIssuingParticipantId(date, participantId));

        String content;
        if ("CSV".equalsIgnoreCase(format)) {
            content = generateCsv(records, participant, date);
        } else if ("ISO20022".equalsIgnoreCase(format)) {
            content = generateIso20022(records, participant, date);
        } else if ("COMPCONF".equalsIgnoreCase(format)) {
            content = compconfFileService.generate(date, records);
        } else if ("CP50".equalsIgnoreCase(format)) {
            content = cp50FileService.generate(date, participant, records);
        } else if ("VISA".equalsIgnoreCase(format)) {
            content = visaBaseIIGenerator.generate(date, records);
        } else if ("MASTERCARD".equalsIgnoreCase(format)) {
            content = mastercardIpmGenerator.generate(date, records);
        } else {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }

        eventPublisher.publishClearingFileGenerated(new ClearingFileGeneratedEvent(
                null, date.toString(), format, participant.getCode(), content.length(), OffsetDateTime.now()));

        return content;
    }

    private String generateCsv(List<ClearingRecord> records, Participant participant, LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("clearing_date,transaction_id,acquiring_participant,issuing_participant,")
          .append("amount,currency,interchange_amount,fee_amount,net_amount,message_type,status\n");
        for (ClearingRecord r : records) {
            sb.append(date).append(",")
              .append(escapeCsv(r.getTransactionId())).append(",")
              .append(r.getAcquiringParticipantId()).append(",")
              .append(r.getIssuingParticipantId()).append(",")
              .append(r.getAmount()).append(",")
              .append(r.getCurrencyCode()).append(",")
              .append(r.getInterchangeAmount() != null ? r.getInterchangeAmount() : "0").append(",")
              .append(r.getFeeAmount() != null ? r.getFeeAmount() : "0").append(",")
              .append(r.getNetAmount() != null ? r.getNetAmount() : "0").append(",")
              .append(escapeCsv(r.getMessageType())).append(",")
              .append(r.getStatus()).append("\n");
        }
        return sb.toString();
    }

    private String generateIso20022(List<ClearingRecord> records, Participant participant, LocalDate date) {
        return iso20022ClearingGenerator.generate(date, records);
    }

    public ReconciliationResult ingestIncomingClearingFile(String content, String format, UUID participantId) {
        int matched = 0;
        int unmatched = 0;
        int total = 0;

        if ("CSV".equalsIgnoreCase(format)) {
            String[] lines = content.split("\n");
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                total++;
                String[] fields = line.split(",");
                if (fields.length >= 2) {
                    String txId = fields[1].replace("\"", "");
                    var opt = clearingRecordRepository.findByTransactionId(txId);
                    if (opt.isPresent()) {
                        ClearingRecord record = opt.get();
                        record.setStatus(ClearingRecord.Status.SETTLED);
                        clearingRecordRepository.save(record);
                        matched++;
                    } else {
                        unmatched++;
                    }
                }
            }
        } else if ("ISO20022".equalsIgnoreCase(format)) {
            try {
                Document doc = iso20022Engine.parse(content);
                var details = iso20022Engine.extractPaymentDetails(doc);
                String ref = details.get("EndToEndId");
                total = 1;
                if (ref != null) {
                    var opt = clearingRecordRepository.findByTransactionId(ref);
                    if (opt.isPresent()) {
                        ClearingRecord record = opt.get();
                        record.setStatus(ClearingRecord.Status.SETTLED);
                        clearingRecordRepository.save(record);
                        matched = 1;
                    } else {
                        unmatched = 1;
                    }
                } else {
                    unmatched = 1;
                }
            } catch (Exception e) {
                log.warn("ISO 20022 ingestion parse failed: {}", e.getMessage());
                unmatched = 1;
            }
        } else if ("COMPCONF".equalsIgnoreCase(format)) {
            List<ClearingRecord> parsed = compconfFileService.parse(content);
            total = parsed.size();
            for (ClearingRecord r : parsed) {
                if (r.getTransactionId() != null) {
                    var opt = clearingRecordRepository.findByTransactionId(r.getTransactionId());
                    if (opt.isPresent()) {
                        ClearingRecord record = opt.get();
                        record.setStatus(ClearingRecord.Status.SETTLED);
                        clearingRecordRepository.save(record);
                        matched++;
                    } else {
                        unmatched++;
                    }
                } else {
                    unmatched++;
                }
            }
        } else if ("CP50".equalsIgnoreCase(format)) {
            List<String> rawType40 = cp50FileService.parse(content);
            total = rawType40.size();
            log.info("CP50 ingestion: {} type40 lines (raw, waiting for SMT spec)", total);
            unmatched = total;
        } else if ("VISA".equalsIgnoreCase(format)) {
            com.switchplatform.platform.service.clearing.network.NetworkClearingGenerator.ReconciliationResult result
                    = visaBaseIIGenerator.ingest(content);
            total = result.totalRecords();
            matched = result.matched();
            unmatched = result.unmatched();
        } else if ("MASTERCARD".equalsIgnoreCase(format)) {
            com.switchplatform.platform.service.clearing.network.NetworkClearingGenerator.ReconciliationResult result
                    = mastercardIpmGenerator.ingest(content);
            total = result.totalRecords();
            matched = result.matched();
            unmatched = result.unmatched();
        } else {
            throw new IllegalArgumentException("Unsupported format for ingestion: " + format);
        }

        BigDecimal amountDifference = BigDecimal.ZERO;
        persistReconciliationRecord(LocalDate.now(), participantId, format, total, matched, unmatched, amountDifference);

        log.info("Clearing file ingested: total={}, matched={}, unmatched={}", total, matched, unmatched);
        return new ReconciliationResult(total, matched, unmatched, amountDifference);
    }

    private void persistReconciliationRecord(LocalDate date, UUID participantId, String format,
                                              int total, int matched, int unmatched,
                                              BigDecimal amountDifference) {
        try {
            ReconciliationRecord record = ReconciliationRecord.builder()
                    .reconciliationDate(date)
                    .participantId(participantId)
                    .source(ReconciliationRecord.Source.SCHEME)
                    .totalTransactions(total)
                    .totalAmount(amountDifference != null ? amountDifference : BigDecimal.ZERO)
                    .matchedCount(matched)
                    .unmatchedCount(unmatched)
                    .discrepancyCount(unmatched)
                    .status(unmatched == 0 ? ReconciliationRecord.Status.MATCHED : ReconciliationRecord.Status.PARTIALLY_MATCHED)
                    .notes("Ingested " + format + " file: " + matched + "/" + total + " matched")
                    .build();
            reconciliationRecordRepository.save(record);
        } catch (Exception e) {
            log.warn("Failed to persist reconciliation record: {}", e.getMessage());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public record ReconciliationResult(int totalRecords, int matched, int unmatched, BigDecimal amountDifference) {}
}
