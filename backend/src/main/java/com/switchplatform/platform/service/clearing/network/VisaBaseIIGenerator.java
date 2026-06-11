/*
 * BASE II generator — TC 05 TCR 0 (Draft Data, purchase presentment).
 * Field positions per Visa "BASE II Clearing Interchange Formats TC 01-49"
 * (effective 14 Oct 2023).
 *
 * Simulation assumptions are centralized in VisaBaseIISimConfig — every
 * value that is an hypothesis is marked "hypothèse simulateur — à remplacer
 * par la spec Visa du client".
 *
 * Known limitations (no real spec available):
 *   - TC 05 TCR 1/2/3  — country-specific / industry-specific (not generated)
 *   - TC 06             — chargeback (not generated)
 *   - TC 07             — representment (not generated)
 *   - TC 25             — fee collection (not generated)
 */
package com.switchplatform.platform.service.clearing.network;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.acquiring.MerchantRepository;
import com.switchplatform.platform.repository.clearing.ClearingRecordRepository;
import com.switchplatform.platform.service.clearing.network.visa.VisaBaseIISimConfig;
import com.switchplatform.platform.service.clearing.network.visa.VisaBaseIIFormatter;
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
public class VisaBaseIIGenerator implements NetworkClearingGenerator {

    private final ParticipantRepository participantRepository;
    private final MerchantRepository merchantRepository;
    private final ClearingRecordRepository clearingRecordRepository;

    @Override
    public String scheme() {
        return "VISA";
    }

    @Override
    public String generate(LocalDate date, List<ClearingRecord> records) {
        if (records.isEmpty()) {
            log.info("Visa BASE II: no records to generate for {}", date);
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ClearingRecord record : records) {
            sb.append(generateTc05Tcr0(record)).append("\n");
        }
        String result = sb.toString();
        log.info("Visa BASE II: generated {} TC05/TCR0 lines for {}", records.size(), date);
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
            if (line.length() < 168) {
                log.warn("Visa BASE II ingest: skipping short line ({} chars)", line.length());
                unmatched++;
                continue;
            }
            total++;
            String arn = line.substring(26, 49); // positions 27-49 (0-indexed: 26-48)
            String txnKey = VisaBaseIISimConfig.extractTxnKey(arn);

            if (txnKey != null && !txnKey.isEmpty()) {
                List<ClearingRecord> records = clearingRecordRepository.findAll();
                boolean found = false;
                for (ClearingRecord r : records) {
                    String clean = r.getTransactionId().replace("-", "").toUpperCase();
                    if (clean.startsWith(txnKey)) {
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
        log.info("Visa BASE II: ingested {} lines, matched={}, unmatched={}", total, matched, unmatched);
        return new ReconciliationResult(total, matched, unmatched);
    }

    // ---------------------------------------------------------------
    // TC 05 TCR 0 — Draft Data, basic financial record (168 chars)
    // ---------------------------------------------------------------

    public String generateTc05Tcr0(ClearingRecord record) {
        Participant acquirer = record.getAcquiringParticipantId() != null
                ? participantRepository.findById(record.getAcquiringParticipantId()).orElse(null)
                : null;
        Merchant merchant = record.getMerchantNumber() != null
                ? merchantRepository.findByMerchantId(record.getMerchantNumber()).orElse(null)
                : null;

        String arnValue = record.getArchiveReference() != null
                ? record.getArchiveReference()
                : VisaBaseIISimConfig.buildArn(record.getTransactionId(),
                        record.getClearingDate() != null ? record.getClearingDate() : LocalDate.now());

        String authCode = record.getAuthorizationNumber() != null
                ? record.getAuthorizationNumber()
                : VisaBaseIISimConfig.generateAuthCode();

        String line = ""
                + f.tc05()                                                //  1-2   "05"
                + f.tcq()                                                 //  3     "0"
                + f.tcr()                                                 //  4     "0"
                + f.pan(record.getCardNumber())                           //  5-20   PAN (16)
                + f.panExt()                                              // 21-23   "000"
                + f.floorLimitIndicator()                                 // 24      space
                + f.crbExceptionFileIndicator()                           // 25      space
                + f.reserved26()                                          // 26      space
                + f.arn(arnValue)                                         // 27-49   ARN (23)
                + f.acquirerBid(acquirer)                                 // 50-57   BID (8)
                + f.purchaseDate(record)                                  // 58-61   MMDD
                + f.destAmount(record.getAmount(), record.getCurrencyCode()) // 62-73   amount (12)
                + f.destCurrency(record.getCurrencyCode())                // 74-76   ISO code
                + f.sourceAmount(record.getAmount(), record.getCurrencyCode()) // 77-88   amount (12)
                + f.sourceCurrency(record.getCurrencyCode())              // 89-91   ISO code
                + f.merchantName(record.getTradingName())                 // 92-116  name (25)
                + f.merchantCity(merchant)                                // 117-129 city (13)
                + f.merchantCountry()                                     // 130-132 country
                + f.mcc(record.getMcc())                                  // 133-136 MCC (4)
                + f.merchantZip(merchant)                                 // 137-141 ZIP (5)
                + f.merchantState()                                       // 142-144 spaces
                + f.requestedPaymentService()                             // 145     space
                + f.numPaymentForms()                                     // 146     "1"
                + f.usageCode()                                           // 147     space
                + f.reasonCode()                                          // 148-149 "00"
                + f.settlementFlag()                                      // 150     space
                + f.authCharIndicator()                                   // 151     space
                + f.authCode(authCode)                                    // 152-157 auth (6)
                + f.posTerminalCapability()                               // 158     space
                + f.additionalFields()                                    // 159-168 spaces
                ;

        assert line.length() == 168 : "TC05 TCR0 must be exactly 168 chars, got " + line.length();
        return line;
    }

    // ---------------------------------------------------------------
    // Field helpers
    // ---------------------------------------------------------------

    private final Fields f = new Fields();

    private static class Fields {
        String tc05() { return VisaBaseIISimConfig.TC_05; }

        String tcq() { return VisaBaseIISimConfig.TCR_Q; }

        String tcr() { return VisaBaseIISimConfig.TCR_0; }

        String pan(String cardNumber) {
            if (cardNumber == null) return "                ";
            String digits = cardNumber.replaceAll("\\D", "");
            if (digits.length() > 16) digits = digits.substring(digits.length() - 16);
            return VisaBaseIIFormatter.an(digits, 16);
        }

        String panExt() { return VisaBaseIISimConfig.PAN_EXT; }

        String floorLimitIndicator() { return String.valueOf(VisaBaseIISimConfig.FLOOR_LIMIT_INDICATOR); }

        String crbExceptionFileIndicator() { return String.valueOf(VisaBaseIISimConfig.CRB_EXCEPTION_INDICATOR); }

        String reserved26() { return " "; }

        String arn(String value) {
            return VisaBaseIIFormatter.an(value, 23);
        }

        String acquirerBid(Participant acquirer) {
            if (acquirer == null) return VisaBaseIISimConfig.DEFAULT_ACQUIRER_BID;
            String code = acquirer.getBankCode() != null ? acquirer.getBankCode() : acquirer.getCode();
            return VisaBaseIIFormatter.an(code, 8);
        }

        String purchaseDate(ClearingRecord record) {
            if (record.getTransactionDate() != null) {
                return record.getTransactionDate().format(DateTimeFormatter.ofPattern("MMdd"));
            }
            if (record.getClearingDate() != null) {
                return record.getClearingDate().format(DateTimeFormatter.ofPattern("MMdd"));
            }
            return VisaBaseIIFormatter.an("", 4);
        }

        String destAmount(java.math.BigDecimal amount, String currency) {
            return VisaBaseIIFormatter.amount(amount, currency, 12);
        }

        String destCurrency(String currency) {
            return VisaBaseIIFormatter.an(currency, 3);
        }

        String sourceAmount(java.math.BigDecimal amount, String currency) {
            return VisaBaseIIFormatter.amount(amount, currency, 12);
        }

        String sourceCurrency(String currency) {
            return VisaBaseIIFormatter.an(currency, 3);
        }

        String merchantName(String tradingName) {
            return VisaBaseIIFormatter.an(tradingName, 25);
        }

        String merchantCity(Merchant merchant) {
            if (merchant == null || merchant.getCity() == null) {
                return VisaBaseIIFormatter.an(VisaBaseIISimConfig.DEFAULT_MERCHANT_CITY, 13);
            }
            return VisaBaseIIFormatter.an(merchant.getCity(), 13);
        }

        String merchantCountry() {
            return VisaBaseIISimConfig.MERCHANT_COUNTRY_CODE;
        }

        String mcc(String value) {
            return VisaBaseIIFormatter.an(value, 4);
        }

        String merchantZip(Merchant merchant) {
            if (merchant == null || merchant.getPostalCode() == null) {
                return VisaBaseIIFormatter.an(VisaBaseIISimConfig.DEFAULT_MERCHANT_ZIP, 5);
            }
            return VisaBaseIIFormatter.an(merchant.getPostalCode(), 5);
        }

        String merchantState() {
            return VisaBaseIIFormatter.an(VisaBaseIISimConfig.MERCHANT_STATE_CODE, 3);
        }

        String requestedPaymentService() { return String.valueOf(VisaBaseIISimConfig.REQUESTED_PAYMENT_SERVICE); }

        String numPaymentForms() { return VisaBaseIISimConfig.NUM_PAYMENT_FORMS; }

        String usageCode() { return String.valueOf(VisaBaseIISimConfig.USAGE_CODE); }

        String reasonCode() { return VisaBaseIISimConfig.REASON_CODE; }

        String settlementFlag() { return String.valueOf(VisaBaseIISimConfig.SETTLEMENT_FLAG); }

        String authCharIndicator() { return String.valueOf(VisaBaseIISimConfig.AUTH_CHAR_INDICATOR); }

        String authCode(String authNumber) {
            return VisaBaseIIFormatter.an(authNumber, 6);
        }

        String posTerminalCapability() { return String.valueOf(VisaBaseIISimConfig.POS_TERMINAL_CAPABILITY); }

        String additionalFields() { return VisaBaseIISimConfig.ADDITIONAL_FIELDS; }
    }

    // ---------------------------------------------------------------
    // TC 06 — Chargeback (not needed for basic clearing cycle)
    // ---------------------------------------------------------------
    public String generateTc06() {
        log.warn("TC 06 (chargeback) not implemented — no real Visa spec available");
        return "";
    }

    // ---------------------------------------------------------------
    // TC 07 — Representment (not needed for basic clearing cycle)
    // ---------------------------------------------------------------
    public String generateTc07() {
        log.warn("TC 07 (representment) not implemented — no real Visa spec available");
        return "";
    }

    // ---------------------------------------------------------------
    // TC 25 — Fee Collection (not needed for basic clearing cycle)
    // ---------------------------------------------------------------
    public String generateTc25() {
        log.warn("TC 25 (fee collection) not implemented — no real Visa spec available");
        return "";
    }

    // ---------------------------------------------------------------
    // TC 05 TCR 1/2/3 — country-specific / industry-specific
    // ---------------------------------------------------------------
    public String generateTc05Tcr1() {
        log.warn("TC 05 TCR 1 (country-specific) not implemented — no real Visa spec available");
        return "";
    }

    public String generateTc05Tcr2() {
        log.warn("TC 05 TCR 2 (country-specific) not implemented — no real Visa spec available");
        return "";
    }

    public String generateTc05Tcr3() {
        log.warn("TC 05 TCR 3 (industry-specific) not implemented — no real Visa spec available");
        return "";
    }
}
