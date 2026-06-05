package com.switchplatform.platform.service.clearing.bct;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.clearing.NettingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.clearing.NettingRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BctSettlementService {

    private final NettingRecordRepository nettingRecordRepository;
    private final ParticipantRepository participantRepository;

    /**
     * Generates a BCT settlement file (FCOMPSMT format) as CSV.
     * Aggregates net positions by domestic institution only.
     * Foreign bank transactions are excluded per FSD specification.
     *
     * LAYOUT NOTE: The exact FCOMPSMT.BCT fixed-width format is not publicly available.
     * This implementation produces a documented CSV as an interim format.
     * When the official BCT layout is obtained, replace this implementation
     * with the proper fixed-width record generation and mark the old CSV as deprecated.
     *
     * CSV columns:
     *   settlement_date,institution_code,institution_name,net_debit,net_credit,net_position,currency,record_count
     */
    public String generateBctSettlementFile(LocalDate date) {
        List<NettingRecord> allNetting = nettingRecordRepository.findByNettingDate(date);

        Set<UUID> foreignParticipantIds = participantRepository.findByIsDomesticFalse().stream()
                .map(Participant::getId)
                .collect(Collectors.toSet());

        StringBuilder sb = new StringBuilder();
        sb.append("settlement_date,institution_code,institution_name,net_debit,net_credit,net_position,currency,record_count\n");

        Map<UUID, NettingTotals> byParticipant = new HashMap<>();

        for (NettingRecord n : allNetting) {
            BigDecimal sent = n.getTotalSent() != null ? n.getTotalSent() : BigDecimal.ZERO;
            BigDecimal recv = n.getTotalReceived() != null ? n.getTotalReceived() : BigDecimal.ZERO;
            int txns = n.getTransactionCount() != null ? n.getTransactionCount() : 0;

            if (n.getCounterpartyId() == null) {
                if (!foreignParticipantIds.contains(n.getParticipantId())) {
                    byParticipant.computeIfAbsent(n.getParticipantId(), k -> new NettingTotals())
                            .addDebit(sent).addCredit(recv).addCount(txns);
                }
            } else {
                boolean partForeign = foreignParticipantIds.contains(n.getParticipantId());
                boolean cpForeign = foreignParticipantIds.contains(n.getCounterpartyId());
                if (partForeign || cpForeign) continue;

                byParticipant.computeIfAbsent(n.getParticipantId(), k -> new NettingTotals())
                        .addDebit(sent).addCredit(recv).addCount(txns);
                byParticipant.computeIfAbsent(n.getCounterpartyId(), k -> new NettingTotals())
                        .addDebit(recv).addCredit(sent).addCount(txns);
            }
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (var entry : byParticipant.entrySet()) {
            UUID pid = entry.getKey();
            NettingTotals totals = entry.getValue();

            BigDecimal grossDebit = totals.debit;
            BigDecimal grossCredit = totals.credit;

            BigDecimal netPosition = grossCredit.subtract(grossDebit);
            String currency = "TND";
            long count = totals.count;

            Participant p = participantRepository.findById(pid).orElse(null);
            String code = p != null ? p.getCode() : pid.toString().substring(0, 8);
            String name = p != null ? p.getName() : "UNKNOWN";

            sb.append(date.format(DateTimeFormatter.ISO_LOCAL_DATE)).append(",")
                    .append(escapeCsv(code)).append(",")
                    .append(escapeCsv(name)).append(",")
                    .append(grossDebit).append(",")
                    .append(grossCredit).append(",")
                    .append(netPosition).append(",")
                    .append(currency).append(",")
                    .append(count).append("\n");

            totalDebit = totalDebit.add(grossDebit);
            totalCredit = totalCredit.add(grossCredit);
        }

        sb.append("\nTOTALS,,,").append(totalDebit).append(",")
                .append(totalCredit).append(",")
                .append(totalCredit.subtract(totalDebit)).append(",TND,")
                .append(byParticipant.size()).append("\n");

        log.info("BCT settlement file generated for {}: {} domestic institutions, totalDebit={}, totalCredit={}",
                date, byParticipant.size(), totalDebit, totalCredit);
        return sb.toString();
    }

    private static class NettingTotals {
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        long count = 0;

        NettingTotals addDebit(BigDecimal v) { debit = debit.add(v); return this; }
        NettingTotals addCredit(BigDecimal v) { credit = credit.add(v); return this; }
        NettingTotals addCount(int v) { count += v; return this; }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
