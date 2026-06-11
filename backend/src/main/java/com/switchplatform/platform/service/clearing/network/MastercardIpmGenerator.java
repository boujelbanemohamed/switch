/*
 * Mastercard IPM generator — Type 1000 (Financial Presentment).
 * Field positions per Mastercard "Integrated Product Messages (IPM) Format"
 * specifications (publicly available reference).
 *
 * Simulation assumptions are centralized in MastercardIpmSimConfig — every
 * value that is an hypothesis is marked "hypothèse simulateur — à remplacer
 * par la spec Mastercard du client".
 *
 * Known limitations (no real spec available):
 *   - Type 1100 — Fee record (not generated)
 *   - Type 1200 — Chargeback record (not generated)
 *   - Type 1300 — Representment record (not generated)
 *   - Reconciliation fields (not generated)
 */
package com.switchplatform.platform.service.clearing.network;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.acquiring.MerchantRepository;
import com.switchplatform.platform.repository.clearing.ClearingRecordRepository;
import com.switchplatform.platform.service.clearing.network.mastercard.MastercardIpmFormatter;
import com.switchplatform.platform.service.clearing.network.mastercard.MastercardIpmSimConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MastercardIpmGenerator implements NetworkClearingGenerator {

    private final ParticipantRepository participantRepository;
    private final MerchantRepository merchantRepository;
    private final ClearingRecordRepository clearingRecordRepository;

    @Override
    public String scheme() {
        return "MASTERCARD";
    }

    @Override
    public String generate(LocalDate date, List<ClearingRecord> records) {
        if (records.isEmpty()) {
            log.info("Mastercard IPM: no records to generate for {}", date);
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ClearingRecord record : records) {
            sb.append(generateType1000(record)).append("\n");
        }
        String result = sb.toString();
        log.info("Mastercard IPM: generated {} Type 1000 lines for {}", records.size(), date);
        return result;
    }

    @Override
    public ReconciliationResult ingest(String content) {
        if (content == null || content.isBlank()) {
            return new ReconciliationResult(0, 0, 0);
        }
        String[] lines = content.split("\n");
        int total = 0;
        int matched = 0;
        int unmatched = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.length() < 200) {
                log.warn("Mastercard IPM ingest: skipping short line ({} chars)", line.length());
                unmatched++;
                continue;
            }
            total++;
            String arn = line.substring(31, 55).trim();
            if (!arn.isEmpty()) {
                List<ClearingRecord> records = clearingRecordRepository.findAll();
                boolean found = false;
                for (ClearingRecord r : records) {
                    String clean = r.getTransactionId().replace("-", "").toUpperCase();
                    if (clean.startsWith(arn)) {
                        if (r.getStatus() == ClearingRecord.Status.PENDING) {
                            r.setStatus(ClearingRecord.Status.SETTLED);
                            clearingRecordRepository.save(r);
                        }
                        found = true;
                        break;
                    }
                }
                if (found) {
                    matched++;
                } else {
                    unmatched++;
                }
            } else {
                unmatched++;
            }
        }
        log.info("Mastercard IPM: ingested {} lines, matched={}, unmatched={}", total, matched, unmatched);
        return new ReconciliationResult(total, matched, unmatched);
    }

    // ---------------------------------------------------------------
    // Type 1000 — Financial Presentment (200 chars)
    // ---------------------------------------------------------------

    public String generateType1000(ClearingRecord record) {
        Participant acquirer = record.getAcquiringParticipantId() != null
                ? participantRepository.findById(record.getAcquiringParticipantId()).orElse(null)
                : null;
        Merchant merchant = record.getMerchantNumber() != null
                ? merchantRepository.findByMerchantId(record.getMerchantNumber()).orElse(null)
                : null;

        String arnValue = record.getArchiveReference() != null
                ? record.getArchiveReference()
                : MastercardIpmSimConfig.buildArn(record.getTransactionId(),
                        record.getClearingDate() != null ? record.getClearingDate() : LocalDate.now());

        String authCode = record.getAuthorizationNumber() != null
                ? record.getAuthorizationNumber()
                : MastercardIpmSimConfig.generateAuthCode();

        String currency = record.getCurrencyCode() != null ? record.getCurrencyCode() : "TND";
        String mcc = record.getMcc() != null ? record.getMcc() : MastercardIpmSimConfig.DEFAULT_MCC;

        String line = ""
                + f.recordType()                                            //  1-4   "1000"
                + f.txCode()                                                //  5-6   "00"
                + f.clearingCode()                                          //  7-8   "00"
                + f.activityCode()                                          //  9-10  "20"
                + f.processingDate(record)                                  // 11-12  MMDD
                + f.pan(record.getCardNumber())                             // 13-28  PAN (16)
                + f.panExt()                                                // 29-31  "000"
                + f.senderReference(arnValue)                               // 32-55  Sender Ref (24)
                + f.acquiringIca(acquirer)                                  // 56-63  ICA (8)
                + f.issuingIca()                                            // 64-71  ICA (8)
                + f.settlementSign()                                        // 72-73  "C "
                + f.txnAmount(record.getAmount(), currency)                 // 74-85  amount (12)
                + f.txnCurrency(currency)                                   // 86-88  ISO numeric
                + f.settlementAmount(record.getAmount(), currency)          // 89-100 settle amt (12)
                + f.settlementCurrency(currency)                            // 101-103 ISO numeric
                + f.cardAcceptorCountry()                                   // 104-106 country
                + f.cardAcceptorName(record.getTradingName())               // 107-131 name (25)
                + f.cardAcceptorCity(merchant)                              // 132-144 city (13)
                + f.countryCode()                                           // 145-147 country
                + f.postalCode(merchant)                                    // 148-152 ZIP (5)
                + f.stateCode()                                             // 155-156 state (2)
                + f.mcc(mcc)                                                // 157-160 MCC (4)
                + f.reserved161()                                           // 161-168 spaces (8)
                + f.authCode(authCode)                                      // 169-174 auth (6)
                + f.rrn(record)                                             // 175-180 RRN (6)
                + f.reserved181()                                           // 181-190 spaces (10)
                + f.recordLevelIndicator()                                  // 191    "1"
                + f.reserved192()                                           // 192-200 spaces (9)
                ;

        assert line.length() == 200 : "Mastercard IPM Type 1000 must be exactly 200 chars, got " + line.length();
        return line;
    }

    // ---------------------------------------------------------------
    // Field helpers
    // ---------------------------------------------------------------

    private final Fields f = new Fields();

    private static class Fields {
        private final MastercardIpmFormatter fmt = new MastercardIpmFormatter("TND");

        String recordType() { return MastercardIpmSimConfig.RECORD_TYPE_1000 + "000"; }

        String txCode() { return MastercardIpmSimConfig.TX_CODE_PURCHASE; }

        String clearingCode() { return MastercardIpmSimConfig.CLEARING_CODE; }

        String activityCode() { return MastercardIpmSimConfig.ACTIVITY_CODE; }

        String processingDate(ClearingRecord record) {
            if (record.getTransactionDate() != null) {
                return record.getTransactionDate().format(DateTimeFormatter.ofPattern("MMdd"));
            }
            if (record.getClearingDate() != null) {
                return record.getClearingDate().format(DateTimeFormatter.ofPattern("MMdd"));
            }
            return "    ";
        }

        String pan(String cardNumber) {
            if (cardNumber == null) return "                ";
            String digits = cardNumber.replaceAll("\\D", "");
            if (digits.length() > 16) digits = digits.substring(digits.length() - 16);
            return fmt.an(digits, 16);
        }

        String panExt() { return "000"; }

        String senderReference(String value) {
            return fmt.an(value, 24);
        }

        String acquiringIca(Participant acquirer) {
            if (acquirer == null) return MastercardIpmSimConfig.DEFAULT_ACQUIRER_ICA;
            String code = acquirer.getBankCode() != null ? acquirer.getBankCode() : acquirer.getCode();
            return fmt.an(code, 8);
        }

        String issuingIca() {
            return MastercardIpmSimConfig.DEFAULT_ISSUER_ICA;
        }

        String settlementSign() {
            return MastercardIpmSimConfig.SETTLEMENT_SIGN + " ";
        }

        String txnAmount(java.math.BigDecimal amount, String currency) {
            return fmt.amount(amount, currency);
        }

        String txnCurrency(String currency) {
            return fmt.currencyCode();
        }

        String settlementAmount(java.math.BigDecimal amount, String currency) {
            return fmt.amount(amount, currency);
        }

        String settlementCurrency(String currency) {
            return fmt.currencyCode();
        }

        String cardAcceptorCountry() {
            return MastercardIpmSimConfig.COUNTRY_CODE_TUNISIA;
        }

        String cardAcceptorName(String tradingName) {
            return fmt.an(tradingName, 25);
        }

        String cardAcceptorCity(Merchant merchant) {
            if (merchant == null || merchant.getCity() == null) {
                return fmt.an(MastercardIpmSimConfig.DEFAULT_MERCHANT_CITY, 13);
            }
            return fmt.an(merchant.getCity(), 13);
        }

        String countryCode() {
            return MastercardIpmSimConfig.COUNTRY_CODE_TUNISIA;
        }

        String postalCode(Merchant merchant) {
            if (merchant == null || merchant.getPostalCode() == null) {
                return fmt.an(MastercardIpmSimConfig.DEFAULT_POSTAL_CODE, 5);
            }
            return fmt.an(merchant.getPostalCode(), 5);
        }

        String stateCode() {
            return fmt.an(MastercardIpmSimConfig.DEFAULT_STATE, 2);
        }

        String mcc(String value) {
            return fmt.nn(value, 4);
        }

        String reserved161() {
            return MastercardIpmFormatter.spaces(8);
        }

        String authCode(String authNumber) {
            return fmt.an(authNumber, 6);
        }

        String rrn(ClearingRecord record) {
            String id = record.getTransactionId();
            if (id == null) return MastercardIpmFormatter.spaces(6);
            String clean = id.replaceAll("[^A-Za-z0-9]", "");
            if (clean.length() > 6) clean = clean.substring(clean.length() - 6);
            return fmt.an(clean, 6);
        }

        String reserved181() {
            return MastercardIpmFormatter.spaces(10);
        }

        String recordLevelIndicator() {
            return "1";
        }

        String reserved192() {
            return MastercardIpmFormatter.spaces(9);
        }
    }

    // ---------------------------------------------------------------
    // Unsupported record types (not needed for basic cycle)
    // ---------------------------------------------------------------

    public String generateType1100() {
        log.warn("Type 1100 (fee) not implemented — no real Mastercard spec available");
        return "";
    }

    public String generateType1200() {
        log.warn("Type 1200 (chargeback) not implemented — no real Mastercard spec available");
        return "";
    }

    public String generateType1300() {
        log.warn("Type 1300 (representment) not implemented — no real Mastercard spec available");
        return "";
    }
}
