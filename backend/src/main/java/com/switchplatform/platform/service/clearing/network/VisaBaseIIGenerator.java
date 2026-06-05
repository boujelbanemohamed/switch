/*
 * DRAFT — NOT VALIDATED against real Visa file or VTS certification.
 *
 * This is an exploratory implementation of TC 05 (Draft Data / purchase
 * presentment), limited to TCR 0 (basic financial record).  Field positions
 * are per the Visa "BASE II Clearing Interchange Formats TC 01-49"
 * specification (effective 14 Oct 2023).
 *
 * Placeholders / known issues pending external specs:
 *   - Clearing Data Codes manual (fields 145, 147, 150, 158)
 *   - Additional fields (positions 159-168)
 *   - ARN (pos 27-49) — structured format required (BIN + julian date + sequence + check digit)
 *   - Acquirer BID (pos 50-57) — Visa-assigned 8-digit BID, not internal bank code
 * DO NOT use this output for production or Visa submission without:
 *   1. Comparing against a known-good Visa reference file
 *   2. Passing Visa VTS (Visa Test System) certification
 *
 * Implemented TC/TCR:
 *   TC 05 TCR 0  — Draft Data, basic financial record  (168 chars)
 *
 * Pending (will throw UnsupportedOperationException):
 *   TC 05 TCR 1/2/3  — country-specific / industry-specific
 *   TC 06            — chargeback
 *   TC 07            — representment
 *   TC 25            — fee collection
 *   All other TC/TCR
 */
package com.switchplatform.platform.service.clearing.network;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.acquiring.MerchantRepository;
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
        throw new UnsupportedOperationException(
                "Visa BASE II ingestion pending phase 2 — TC 06/07/25 parsing not yet implemented.");
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

        String line = ""
                + f.tc05()                                                //  1-2   "05"
                + f.tcq("0")                                              //  3     "0"
                + f.tcr("0")                                              //  4     "0"
                + f.pan(record.getCardNumber())                           //  5-20   PAN (16)
                + f.panExt()                                              // 21-23   "000"
                + f.floorLimitIndicator()                                 // 24      space
                + f.crbExceptionFileIndicator()                           // 25      space
                + f.reserved26()                                          // 26      space
                + f.arn(record.getArchiveReference())                     // 27-49   ARN (23)
                + f.acquirerBid(acquirer)                                 // 50-57   BIN (8)
                + f.purchaseDate(record)                                  // 58-61   MMDD
                + f.destAmount(record.getAmount(), record.getCurrencyCode()) // 62-73   amount (12)
                + f.destCurrency(record.getCurrencyCode())                // 74-76   ISO code
                + f.sourceAmount(record.getAmount(), record.getCurrencyCode()) // 77-88   amount (12)
                + f.sourceCurrency(record.getCurrencyCode())              // 89-91   ISO code
                + f.merchantName(record.getTradingName())                 // 92-116  name (25)
                + f.merchantCity(merchant)                                // 117-129 city (13)
                + f.merchantCountry()                                     // 130-132 "788"
                + f.mcc(record.getMcc())                                  // 133-136 MCC (4)
                + f.merchantZip(merchant)                                 // 137-141 ZIP (5)
                + f.merchantState()                                       // 142-144 spaces
                + f.requestedPaymentService()                             // 145     space (TODO)
                + f.numPaymentForms()                                     // 146     "1"
                + f.usageCode()                                           // 147     space (TODO)
                + f.reasonCode()                                          // 148-149 "00"
                + f.settlementFlag()                                      // 150     space (TODO)
                + f.authCharIndicator()                                   // 151     space
                + f.authCode(record.getAuthorizationNumber())             // 152-157 auth (6)
                + f.posTerminalCapability()                               // 158     space (TODO)
                + f.additionalFields()                                    // 159-168 spaces (TODO)
                ;

        assert line.length() == 168 : "TC05 TCR0 must be exactly 168 chars, got " + line.length();
        return line;
    }

    // ---------------------------------------------------------------
    // Field helpers
    // ---------------------------------------------------------------

    private final Fields f = new Fields();

    private static class Fields {
        // Position  1-2 : Transaction Code
        String tc05() { return "05"; }

        // Position  3   : Transaction Code Qualifier
        String tcq(String v) { return v != null ? v : "0"; }

        // Position  4   : Transaction Component Sequence Number
        String tcr(String v) { return v != null ? v : "0"; }

        // Positions  5-20 : Account Number (PAN) — 16 chars
        String pan(String cardNumber) {
            // Use last 16 digits or pad to 16
            if (cardNumber == null) return "                ";
            String digits = cardNumber.replaceAll("\\D", "");
            if (digits.length() > 16) digits = digits.substring(digits.length() - 16);
            return VisaBaseIIFormatter.an(digits, 16);
        }

        // Positions 21-23 : Account Number Extension — 3 chars, default "000"
        String panExt() { return "000"; }

        // Position  24   : Floor Limit Indicator — space by default
        String floorLimitIndicator() { return " "; }

        // Position  25   : CRB/Exception File Indicator — space by default
        String crbExceptionFileIndicator() { return " "; }

        // Position  26   : Reserved — space
        String reserved26() { return " "; }

        // Positions 27-49 : Acquirer Reference Number (ARN) — 23 chars
        // TODO: ARN is a structured Visa field (acquirer BIN + julian date +
        //       sequence number + check digit), not free text.  Current
        //       implementation dumps archiveReference as-is (placeholder).
        String arn(String archiveReference) {
            return VisaBaseIIFormatter.an(archiveReference, 23);
        }

        // Positions 50-57 : Acquirer's Business ID — 8 chars
        // TODO: Acquirer BID is a Visa-assigned 8-digit identifier, not the
        //       internal bank code.  Current implementation uses bankCode as
        //       a placeholder until the Visa BID mapping table is available.
        String acquirerBid(Participant acquirer) {
            if (acquirer == null) return "        ";
            String code = acquirer.getBankCode() != null ? acquirer.getBankCode() : acquirer.getCode();
            return VisaBaseIIFormatter.an(code, 8);
        }

        // Positions 58-61 : Purchase Date — MMDD format, 4 chars
        String purchaseDate(ClearingRecord record) {
            if (record.getTransactionDate() != null) {
                return record.getTransactionDate().format(DateTimeFormatter.ofPattern("MMdd"));
            }
            if (record.getClearingDate() != null) {
                return record.getClearingDate().format(DateTimeFormatter.ofPattern("MMdd"));
            }
            return "0000";
        }

        // Positions 62-73 : Destination Amount — 12 chars
        String destAmount(java.math.BigDecimal amount, String currency) {
            return VisaBaseIIFormatter.amount(amount, currency, 12);
        }

        // Positions 74-76 : Destination Currency Code — 3 chars
        String destCurrency(String currency) {
            return VisaBaseIIFormatter.an(currency, 3);
        }

        // Positions 77-88 : Source Amount — 12 chars
        String sourceAmount(java.math.BigDecimal amount, String currency) {
            return VisaBaseIIFormatter.amount(amount, currency, 12);
        }

        // Positions 89-91 : Source Currency Code — 3 chars
        String sourceCurrency(String currency) {
            return VisaBaseIIFormatter.an(currency, 3);
        }

        // Positions 92-116 : Merchant Name — 25 chars
        String merchantName(String tradingName) {
            return VisaBaseIIFormatter.an(tradingName, 25);
        }

        // Positions 117-129 : Merchant City — 13 chars
        String merchantCity(Merchant merchant) {
            if (merchant == null) return "             ";
            return VisaBaseIIFormatter.an(merchant.getCity(), 13);
        }

        // Positions 130-132 : Merchant Country Code — "788" for Tunisia
        String merchantCountry() {
            return "788";
        }

        // Positions 133-136 : Merchant Category Code (MCC) — 4 chars
        String mcc(String value) {
            return VisaBaseIIFormatter.an(value, 4);
        }

        // Positions 137-141 : Merchant ZIP Code — 5 chars
        String merchantZip(Merchant merchant) {
            if (merchant == null) return "     ";
            return VisaBaseIIFormatter.an(merchant.getPostalCode(), 5);
        }

        // Positions 142-144 : Merchant State/Province Code — spaces (N/A for Tunisia)
        String merchantState() { return "   "; }

        // Position  145   : Requested Payment Service — TODO: confirm with Clearing Data Codes manual
        String requestedPaymentService() { return " "; }

        // Position  146   : Number of Payment Forms — default "1"
        String numPaymentForms() { return "1"; }

        // Position  147   : Usage Code — TODO: confirm with Clearing Data Codes manual
        String usageCode() { return " "; }

        // Positions 148-149 : Reason Code — "00" (not a chargeback)
        String reasonCode() { return "00"; }

        // Position  150   : Settlement Flag — TODO: confirm with Clearing Data Codes manual
        String settlementFlag() { return " "; }

        // Position  151   : Authorization Characteristics Indicator — space
        String authCharIndicator() { return " "; }

        // Positions 152-157 : Authorization Code — 6 chars
        String authCode(String authNumber) {
            return VisaBaseIIFormatter.an(authNumber, 6);
        }

        // Position  158   : POS Terminal Capability — TODO: confirm with Clearing Data Codes manual
        String posTerminalCapability() { return " "; }

        // Positions 159-168 : Additional fields — TODO: complete when spec available
        String additionalFields() { return "          "; }
    }

    // ---------------------------------------------------------------
    // Stub: TC 06 — Chargeback
    // ---------------------------------------------------------------
    public String generateTc06() {
        throw new UnsupportedOperationException("TC 06 (chargeback) pending phase 2");
    }

    // ---------------------------------------------------------------
    // Stub: TC 07 — Representment
    // ---------------------------------------------------------------
    public String generateTc07() {
        throw new UnsupportedOperationException("TC 07 (representment) pending phase 2");
    }

    // ---------------------------------------------------------------
    // Stub: TC 25 — Fee Collection
    // ---------------------------------------------------------------
    public String generateTc25() {
        throw new UnsupportedOperationException("TC 25 (fee collection) pending phase 2");
    }

    // ---------------------------------------------------------------
    // Stub: TC 05 TCR 1/2/3 — country-specific / industry-specific
    // ---------------------------------------------------------------
    public String generateTc05Tcr1() {
        throw new UnsupportedOperationException("TC 05 TCR 1 (country-specific) pending phase 2");
    }

    public String generateTc05Tcr2() {
        throw new UnsupportedOperationException("TC 05 TCR 2 (country-specific) pending phase 2");
    }

    public String generateTc05Tcr3() {
        throw new UnsupportedOperationException("TC 05 TCR 3 (industry-specific) pending phase 2");
    }
}
