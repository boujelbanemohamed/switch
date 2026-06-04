package com.switchplatform.platform.service.clearing;

import com.switchplatform.platform.iso20022.Iso20022Engine;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.clearing.ClearingRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementFileService {

    private final ClearingRecordRepository clearingRecordRepository;
    private final ParticipantRepository participantRepository;
    private final Iso20022Engine iso20022Engine;

    public String generateOutgoingClearingFile(LocalDate date, UUID participantId, String format) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + participantId));
        List<ClearingRecord> records = clearingRecordRepository
                .findByClearingDateAndAcquiringParticipantId(date, participantId);
        records.addAll(clearingRecordRepository
                .findByClearingDateAndAcquiringParticipantId(date, participantId));

        if ("CSV".equalsIgnoreCase(format)) {
            return generateCsv(records, participant, date);
        } else if ("ISO20022".equalsIgnoreCase(format)) {
            return generateIso20022(records, participant, date);
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
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
        BigDecimal totalAmount = records.stream()
                .map(ClearingRecord::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String msgId = "CLR-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8);
        String creditorBic = participant.getCode();
        String debtorBic = "SWITCH";
        String currency = records.isEmpty() ? "TND" : records.get(0).getCurrencyCode();
        String creditorAccount = participant.getCode() + "-CLR";
        String debtorAccount = "SWITCH-CLR";

        Document doc = iso20022Engine.createPaymentRequest(
                msgId, creditorBic, debtorBic, totalAmount, currency,
                creditorAccount, debtorAccount, "CLEARING-" + date);
        return iso20022Engine.toXml(doc);
    }

    public ReconciliationResult ingestIncomingClearingFile(String content, String format) {
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
        } else {
            throw new IllegalArgumentException("Unsupported format for ingestion: " + format);
        }

        log.info("Clearing file ingested: total={}, matched={}, unmatched={}", total, matched, unmatched);
        return new ReconciliationResult(total, matched, unmatched);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public record ReconciliationResult(int totalRecords, int matched, int unmatched) {}
}
