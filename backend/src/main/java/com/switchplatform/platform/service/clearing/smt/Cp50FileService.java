package com.switchplatform.platform.service.clearing.smt;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cp50FileService {

    private static final int LINE_LENGTH = 500;

    private final ParticipantRepository participantRepository;

    public String generate(LocalDate date, String codeBanqueDestinataire, List<ClearingRecord> records) {
        StringBuilder sb = new StringBuilder();

        sb.append(generateHeader(date, codeBanqueDestinataire)).append("\n");

        int seq = 1;
        long totalDebit = 0;
        long totalCredit = 0;

        for (ClearingRecord r : records) {
            boolean debit = isDebit(r);
            long amount = amountValue(r);
            String partnerBank = resolvePartnerBank(r, codeBanqueDestinataire, debit);

            sb.append(generateType80(seq++, date, partnerBank, debit ? "50" : "10", amount, debit)).append("\n");

            if (debit) {
                totalDebit += amount;
            } else {
                totalCredit += amount;
            }
        }

        int totalRecs = 1 + records.size() + 1;
        sb.append(generateTrailer(date, codeBanqueDestinataire, totalRecs, totalDebit, totalCredit)).append("\n");

        return sb.toString();
    }

    String generateHeader(LocalDate date, String codeBanque) {
        StringBuilder sb = new StringBuilder(LINE_LENGTH);
        sb.append("01");                                                                    // 1-2
        sb.append(SmtFieldFormatter.numericRight("000001", 6));                            // 3-8
        sb.append("  ");                                                                     // 9-10
        sb.append(SmtFieldFormatter.dateJJMMAA(date));                                     // 11-16
        sb.append("222222");                                                                 // 17-22
        sb.append(SmtFieldFormatter.numericRight(codeBanque, 5));                          // 23-27
        sb.append(SmtFieldFormatter.spaces(472));                                            // 28-499
        sb.append("X");                                                                      // 500
        assert sb.length() == LINE_LENGTH : "CP50 header: " + sb.length();
        return sb.toString();
    }

    String generateType80(int seq, LocalDate date, String partnerBank, String codeOp, long amount, boolean isDebit) {
        StringBuilder sb = new StringBuilder(LINE_LENGTH);
        sb.append("80");                                                                    // 1-2
        sb.append(SmtFieldFormatter.numericRight(seq, 6));                                 // 3-8
        sb.append(codeOp);                                                                   // 9-10
        sb.append(SmtFieldFormatter.dateJJMMAA(date));                                     // 11-16
        sb.append(SmtFieldFormatter.numericRight(partnerBank, 5));                         // 17-21
        sb.append(SmtFieldFormatter.amount(amount, 12));                                    // 22-33
        sb.append(isDebit ? SmtFieldFormatter.amount(amount, 12)
                          : SmtFieldFormatter.amount(0L, 12));                              // 34-45
        sb.append(isDebit ? SmtFieldFormatter.amount(0L, 12)
                          : SmtFieldFormatter.amount(amount, 12));                          // 46-57
        sb.append(SmtFieldFormatter.spaces(442));                                            // 58-499
        sb.append("X");                                                                      // 500
        assert sb.length() == LINE_LENGTH : "CP50 type80: " + sb.length();
        return sb.toString();
    }

    String generateTrailer(LocalDate date, String codeBanque, int totalRecs, long totalDebit, long totalCredit) {
        StringBuilder sb = new StringBuilder(LINE_LENGTH);
        sb.append("99");                                                                    // 1-2
        sb.append(SmtFieldFormatter.numericRight(totalRecs, 6));                           // 3-8
        sb.append("  ");                                                                     // 9-10
        sb.append(SmtFieldFormatter.dateJJMMAA(date));                                     // 11-16
        sb.append(SmtFieldFormatter.numericRight(codeBanque, 5));                          // 17-21
        sb.append(SmtFieldFormatter.amount(totalDebit, 12));                                 // 22-33
        sb.append(SmtFieldFormatter.amount(totalCredit, 12));                                // 34-45
        sb.append(SmtFieldFormatter.spaces(454));                                            // 46-499
        sb.append("X");                                                                      // 500
        assert sb.length() == LINE_LENGTH : "CP50 trailer: " + sb.length();
        return sb.toString();
    }

    public List<String> parse(String content) {
        List<String> rawType40 = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.length() < 2) continue;
            String type = line.substring(0, 2);
            switch (type) {
                case "01", "99", "80" -> { /* logged or validated */ }
                case "40" -> rawType40.add(line);
                default -> log.warn("Unknown CP50 record type: {}", type);
            }
        }
        return rawType40;
    }

    private String resolvePartnerBank(ClearingRecord record, String codeBanqueDestinataire, boolean debit) {
        if (debit && record.getIssuingParticipantId() != null) {
            return participantRepository.findById(record.getIssuingParticipantId())
                    .map(Participant::getBankCode).orElse("00000");
        }
        if (!debit && record.getAcquiringParticipantId() != null) {
            return participantRepository.findById(record.getAcquiringParticipantId())
                    .map(Participant::getBankCode).orElse("00000");
        }
        return "99999";
    }

    private boolean isDebit(ClearingRecord r) {
        String mt = r.getMessageType();
        return mt != null && (mt.startsWith("02") || mt.startsWith("04"));
    }

    private long amountValue(ClearingRecord r) {
        if (r.getAmount() == null) return 0;
        return r.getAmount().setScale(3, RoundingMode.HALF_UP).movePointRight(3).abs().longValue();
    }

    public static String generateType40Record() {
        throw new UnsupportedOperationException("Type 40 layout pending SMT spec — see AGENTS.md");
    }
}
