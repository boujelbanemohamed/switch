package com.switchplatform.platform.service.clearing.network.visa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Centralized simulation assumptions for Visa BASE II format generation.
 *
 * EVERY value here is an hypothesis for the simulator — pending the client's
 * actual Visa spec / VTS certification. Replace each field with the real
 * value obtained from the client's Visa Clearing Data Codes manual, BID
 * mapping table, and TC/TCR specifications before going to production.
 */
public class VisaBaseIISimConfig {

    private VisaBaseIISimConfig() {}

    /** Hypothèse simulateur — ARN BIN (6 digits). À remplacer par le BIN Visa réel du client. */
    public static final String ARN_BIN = "476914";

    /** Hypothèse simulateur — zone indicator pour ARN (2 chars). Valeur réelle : '20' ou 'SZ'. */
    public static final String ARN_ZONE = "SZ";

    /** Hypothèse simulateur — Acquirer BID par défaut (8 digits). À remplacer par le BID Visa assigné au client. */
    public static final String DEFAULT_ACQUIRER_BID = "47691400";

    /** Hypothèse simulateur — Tunisie. Code pays ISO 3166-1 alpha-3. */
    public static final String MERCHANT_COUNTRY_CODE = "788";

    /** Hypothèse simulateur — ville par défaut quand le merchant n'a pas de city. */
    public static final String DEFAULT_MERCHANT_CITY = "TUNIS";

    /** Hypothèse simulateur — code postal par défaut. */
    public static final String DEFAULT_MERCHANT_ZIP = "1000";

    /** Hypothèse simulateur — état/province (N/A pour Tunisie). */
    public static final String MERCHANT_STATE_CODE = "";

    /**
     * ARN format: ZONE(2) + BIN(6) + MMDD(4) + TXN_KEY(10) + CHK(1) = 23 chars.
     * TXN_KEY = first 10 hex chars of UUID (sans tirets), unique pour le simulateur.
     * CHK = simple Luhn-like checksum : sum of digits mod 10.
     *
     * Hypothèse simulateur — le vrai format Visa ARN est documenté dans le
     * "Visa Acquirer Reference Number (ARN) Format" spec. À remplacer par la
     * vraie règle de construction.
     */
    public static String buildArn(String transactionId, LocalDate date) {
        String clean = transactionId.replace("-", "").toUpperCase();
        String txnKey = clean.length() > 10 ? clean.substring(0, 10) : String.format("%-10s", clean);
        String mmdd = date.format(DateTimeFormatter.ofPattern("MMdd"));
        String withoutChk = ARN_ZONE + ARN_BIN + mmdd + txnKey;
        int chk = 0;
        for (char c : withoutChk.toCharArray()) {
            if (c >= '0' && c <= '9') chk += (c - '0');
        }
        return withoutChk + (chk % 10);
    }

    /**
     * Extract transaction key from simulated ARN (chars 13-22, 10 chars).
     * Hypothèse simulateur — le format de l'ARN réel sera différent.
     */
    public static String extractTxnKey(String arn) {
        if (arn == null || arn.length() < 22) return null;
        return arn.substring(12, 22).trim();
    }

    /**
     * Match extracted transaction key against a transaction ID (UUID string).
     */
    public static boolean matchesTransaction(String txnKey, String transactionId) {
        if (txnKey == null || transactionId == null) return false;
        String clean = transactionId.replace("-", "").toUpperCase();
        return clean.startsWith(txnKey);
    }

    /** Hypothèse simulateur — transaction code pour TC 05 (Purchase Presentment). */
    public static final String TC_05 = "05";

    /** Hypothèse simulateur — TCQ pour TCR 0. */
    public static final String TCR_Q = "0";

    /** Hypothèse simulateur — TCR sequence number pour basic financial record. */
    public static final String TCR_0 = "0";

    /** Hypothèse simulateur — PAN extension par défaut. */
    public static final String PAN_EXT = "000";

    /** Hypothèse simulateur — Floor Limit Indicator (espace = pas de restriction). */
    public static final char FLOOR_LIMIT_INDICATOR = ' ';

    /** Hypothèse simulateur — CRB/Exception File Indicator (espace = pas d'exception). */
    public static final char CRB_EXCEPTION_INDICATOR = ' ';

    /** Hypothèse simulateur — Requested Payment Service (espace = non spécifié). Clearing Data Codes manual field 145. */
    public static final char REQUESTED_PAYMENT_SERVICE = ' ';

    /** Hypothèse simulateur — Number of Payment Forms (1 = single form of payment). */
    public static final String NUM_PAYMENT_FORMS = "1";

    /** Hypothèse simulateur — Usage Code (espace = non spécifié). Clearing Data Codes manual field 147. */
    public static final char USAGE_CODE = ' ';

    /** Hypothèse simulateur — Reason Code (00 = pas un chargeback/représentment). */
    public static final String REASON_CODE = "00";

    /** Hypothèse simulateur — Settlement Flag (espace = non spécifié). Clearing Data Codes manual field 150. */
    public static final char SETTLEMENT_FLAG = ' ';

    /** Hypothèse simulateur — Authorization Characteristics Indicator (espace = non spécifié). */
    public static final char AUTH_CHAR_INDICATOR = ' ';

    /** Hypothèse simulateur — POS Terminal Capability (espace = non spécifié). Clearing Data Codes manual field 158. */
    public static final char POS_TERMINAL_CAPABILITY = ' ';

    /** Hypothèse simulateur — Additional fields 159-168 (10 espaces). À compléter quand la spec complète est disponible. */
    public static final String ADDITIONAL_FIELDS = "          ";

    /** Hypothèse simulateur — format du numéro d'autorisation (6 digits aléatoires). */
    public static String generateAuthCode() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }

    /** Hypothèse simulateur — montant minimum pour la réponse simulée (+10 TND écart). */
    public static final BigDecimal DISCREPANCY_DELTA = new BigDecimal("10.000");
}
