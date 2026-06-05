package com.switchplatform.platform.service.clearing.smt;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompconfFileService {

    private static final int LINE_LENGTH = 168;
    private static final Set<String> CHARGEBACK_CODES = Set.of("15", "17", "18");
    private static final Set<String> FEE_GOODFAITH_CODES = Set.of("10", "20");
    private static final Set<String> PRESENTATION_CODES = Set.of("05", "07", "08");

    private final ParticipantRepository participantRepository;

    public String generate(LocalDate date, List<ClearingRecord> records) {
        StringBuilder sb = new StringBuilder();
        boolean warnedFallback = false;
        for (ClearingRecord r : records) {
            if (!warnedFallback && hasNullFrozenFields(r)) {
                log.warn("COMPCONF: old record {} has null frozen fields (pre-V050). Using fallback defaults.", r.getId());
                warnedFallback = true;
            }
            String line = generateLine(r, date);
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private boolean hasNullFrozenFields(ClearingRecord r) {
        return r.getMcc() == null || r.getCardBrand() == null || r.getTradingName() == null;
    }

    String generateLine(ClearingRecord record, LocalDate date) {
        String acqBank = lookupBankCode(record.getAcquiringParticipantId(), "00000");
        String issBank = lookupBankCode(record.getIssuingParticipantId(), "00000");

        String opCode = resolveOpCode(record);
        String nature = resolveNature(record);
        boolean invert = CHARGEBACK_CODES.contains(opCode) || FEE_GOODFAITH_CODES.contains(opCode);

        String banqueCommercant = invert ? issBank : acqBank;
        String banquePorteur = invert ? acqBank : issBank;

        StringBuilder line = new StringBuilder(LINE_LENGTH);

        line.append(SmtFieldFormatter.alphaLeft(coalesce(record.getMerchantNumber(), ""), 10));      // 1-10
        line.append(SmtFieldFormatter.numericRight(coalesce(record.getBatchNumber(), "000001"), 6));   // 11-16
        line.append(SmtFieldFormatter.numericRight(coalesce(record.getSlipNumber(), "000001"), 6));   // 17-22
        line.append(SmtFieldFormatter.alphaLeft(coalesce(record.getCardNumber(), ""), 19));           // 23-41
        line.append("N");                                                                              // 42
        line.append(SmtFieldFormatter.alphaLeft(coalesce(record.getOriginIdentifier(), "T"), 1));    // 43
        line.append(SmtFieldFormatter.alphaLeft(nature, 1));                                          // 44
        line.append(SmtFieldFormatter.numericRight(opCode, 2));                                       // 45-46
        line.append(SmtFieldFormatter.amount(coalesce(record.getAmount(), BigDecimal.ZERO), 9));     // 47-55
        line.append("0000");                                                                           // 56-59
        line.append(SmtFieldFormatter.dateJJMMAA(txDate(record, date)));                              // 60-65
        line.append(SmtFieldFormatter.dateJJMMAA(txDate(record, date)));                              // 66-71
        line.append(SmtFieldFormatter.alphaLeft(coalesce(record.getAuthorizationNumber(), ""), 6));  // 72-77
        line.append(SmtFieldFormatter.dateJJMMAA(date));                                              // 78-83
        line.append(SmtFieldFormatter.numericRight(coalesce(record.getMcc(), "0000"), 4));           // 84-87
        line.append("  ");                                                                             // 88-89
        line.append(SmtFieldFormatter.numericRight(banqueCommercant, 5));                              // 90-94
        line.append(resolveSystemCode(record));                                                          // 95
        line.append(SmtFieldFormatter.numericRight(banquePorteur, 5));                                 // 96-100
        line.append(SmtFieldFormatter.alphaLeft(coalesce(record.getArchiveReference(), ""), 23));    // 101-123
        line.append("00");                                                                             // 124-125

        line.append(buildRedefines(record, opCode, nature, date));                                    // 126-168

        if (line.length() != LINE_LENGTH) {
            log.error("COMPCONF line length mismatch: {} (expected {})", line.length(), LINE_LENGTH);
            while (line.length() < LINE_LENGTH) line.append(' ');
            line.setLength(LINE_LENGTH);
        }
        return line.toString();
    }

    private String buildRedefines(ClearingRecord record, String opCode, String nature, LocalDate date) {
        StringBuilder z = new StringBuilder(43);

        BigDecimal compensation = coalesce(record.getInterchangeAmount(), record.getAmount(), BigDecimal.ZERO);

        if (CHARGEBACK_CODES.contains(opCode) || FEE_GOODFAITH_CODES.contains(opCode)) {
            z.append("00");                                                                             // 126-127 CODE MOTIF
            z.append("1");                                                                              // 128 CYCLE
            z.append(SmtFieldFormatter.spaces(22));                                                     // 129-150 MESSAGE
            z.append(SmtFieldFormatter.amount(compensation, 9));                                       // 151-159 MONTANT COMPENSE
            z.append("0000");                                                                            // 160-163 HEURE
            z.append(SmtFieldFormatter.spaces(4));                                                      // 164-167 FILLER
            z.append("X");                                                                              // 168
        } else if ("D".equals(nature) && PRESENTATION_CODES.contains(opCode) && record.isRepresentationFlag()) {
            // ZONE3 — representation
            z.append("00");                                                                               // 126-127 CODE MOTIF
            z.append("2");                                                                                // 128 CYCLE
            z.append(SmtFieldFormatter.spaces(22));                                                       // 129-150 MESSAGE
            z.append(SmtFieldFormatter.amount(compensation, 9));                                         // 151-159 MONTANT COMPENSE
            z.append("0000");                                                                              // 160-163 HEURE
            z.append("R");                                                                                // 164 CODE REPRESENTATION
            z.append(SmtFieldFormatter.spaces(3));                                                        // 165-167 FILLER
            z.append("X");                                                                                // 168
        } else {
            z.append(SmtFieldFormatter.alphaLeft(coalesce(record.getTradingName(), ""), 25));            // 126-150 ENSEIGNE
            z.append(SmtFieldFormatter.amount(compensation, 9));                                       // 151-159 MONTANT COMPENSE
            z.append("0000");                                                                            // 160-163 HEURE
            z.append(SmtFieldFormatter.spaces(4));                                                      // 164-167 FILLER
            z.append("X");                                                                              // 168
        }

        return z.toString();
    }

    public List<ClearingRecord> parse(String content) {
        List<ClearingRecord> records = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.length() < LINE_LENGTH) continue;
            records.add(parseLine(line));
        }
        return records;
    }

    ClearingRecord parseLine(String line) {
        if (line.length() < LINE_LENGTH) return null;

        String merchantNumber = line.substring(0, 10).trim();
        String cardNumber = line.substring(22, 41).trim();
        String nature = line.substring(43, 44).trim();
        String opCode = line.substring(44, 46).trim();
        BigDecimal amount = SmtFieldFormatter.parseAmount(line.substring(46, 55));
        String authNumber = line.substring(71, 77).trim();
        String mcc = line.substring(83, 87).trim();
        String acqBank = line.substring(89, 94).trim();
        String issBank = line.substring(95, 100).trim();
        String archiveRef = line.substring(100, 123).trim();
        String ordre = line.substring(123, 125).trim();
        String redefHeader = line.substring(125, 168).trim();

        String rawTxDate = line.substring(59, 65).trim();
        LocalDate txDate = SmtFieldFormatter.parseDateJJMMAA(rawTxDate);

        ClearingRecord rec = ClearingRecord.builder()
                .transactionId("SMT-COMPCONF-" + rawTxDate + "-" + acqBank + "-" + issBank)
                .merchantNumber(merchantNumber.isEmpty() ? null : merchantNumber)
                .cardNumber(cardNumber.isEmpty() ? null : cardNumber)
                .amount(amount)
                .operationNature(nature.isEmpty() ? null : nature)
                .operationCode(opCode.isEmpty() ? null : opCode)
                .authorizationNumber(authNumber.isEmpty() ? null : authNumber)
                .mcc(mcc.isEmpty() ? null : mcc)
                .archiveReference(archiveRef.isEmpty() ? null : archiveRef)
                .build();

        if (txDate != null) {
            rec.setTransactionDate(txDate.atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime());
        }

        BigDecimal compAmount = SmtFieldFormatter.parseAmount(line.substring(150, 159));
        if (compAmount.compareTo(BigDecimal.ZERO) != 0) {
            rec.setInterchangeAmount(compAmount);
        }

        return rec;
    }

    private String resolveOpCode(ClearingRecord record) {
        if (record.getOperationCode() != null) return record.getOperationCode();
        String mt = record.getMessageType();
        if (mt == null) return "05";
        return switch (mt) {
            case "0100" -> "10";
            case "0400", "0420" -> "15";
            default -> "05";
        };
    }

    private String resolveNature(ClearingRecord record) {
        if (record.getOperationNature() != null) return record.getOperationNature();
        String mt = record.getMessageType();
        if (mt == null) return "C";
        return switch (mt) {
            case "0200", "0220", "0400", "0420" -> "D";
            default -> "C";
        };
    }

    private String lookupBankCode(UUID participantId, String fallback) {
        if (participantId == null) return fallback;
        return participantRepository.findById(participantId)
                .map(Participant::getBankCode)
                .filter(bc -> bc != null && !bc.isBlank())
                .orElse(fallback);
    }

    private String resolveSystemCode(ClearingRecord record) {
        String brand = record.getCardBrand();
        if (brand == null) return "2";
        return switch (brand.toUpperCase()) {
            case "CB", "CIB" -> "1";
            case "VISA" -> "2";
            case "MASTERCARD" -> "3";
            default -> "2";
        };
    }

    private LocalDate txDate(ClearingRecord record, LocalDate fallback) {
        if (record.getTransactionDate() != null) return record.getTransactionDate().toLocalDate();
        return fallback;
    }

    private static String coalesce(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return "";
    }

    private static BigDecimal coalesce(BigDecimal... values) {
        for (BigDecimal v : values) if (v != null && v.compareTo(BigDecimal.ZERO) != 0) return v;
        return BigDecimal.ZERO;
    }
}
