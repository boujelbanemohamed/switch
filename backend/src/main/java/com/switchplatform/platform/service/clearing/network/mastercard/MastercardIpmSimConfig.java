package com.switchplatform.platform.service.clearing.network.mastercard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class MastercardIpmSimConfig {

    private MastercardIpmSimConfig() {}

    /** Record type: 1000 = Financial Presentment */
    public static final String RECORD_TYPE_1000 = "1";

    /** Transaction code: 00 = Purchase */
    public static final String TX_CODE_PURCHASE = "00";

    /** Clearing code: 00 = Normal presentment */
    public static final String CLEARING_CODE = "00";

    /** Activity code: 20 = Sale */
    public static final String ACTIVITY_CODE = "20";

    /** Currency code TND (ISO 4217 numeric) */
    public static final String CURRENCY_CODE_TND = "788";

    /** Country code Tunisia (ISO 3166 numeric) */
    public static final String COUNTRY_CODE_TUNISIA = "788";

    /** Default Acquirer ICA (Interbank Card Association) number — 8 digits */
    public static final String DEFAULT_ACQUIRER_ICA = "47691400";

    /** Default Issuer ICA — 8 digits */
    public static final String DEFAULT_ISSUER_ICA = "47691401";

    /** Merchant category code default */
    public static final String DEFAULT_MCC = "5411";

    /** Merchant country default */
    public static final String DEFAULT_MERCHANT_COUNTRY = "788";

    /** Merchant city default */
    public static final String DEFAULT_MERCHANT_CITY = "TUNIS";

    /** Default settlement amount sign: C = Credit (to acquirer) */
    public static final String SETTLEMENT_SIGN = "C";

    /** Default card acceptor name/merchant name (25 chars max) */
    public static final String DEFAULT_MERCHANT_NAME = "MARCHAND IPM";

    /** Default card acceptor city (13 chars max) */
    public static final String DEFAULT_CITY = "TUNIS";

    /** Default card acceptor country code ISO 3166 numeric */
    public static final String DEFAULT_COUNTRY = "788";

    /** Default card acceptor postal code (9 chars) */
    public static final String DEFAULT_POSTAL_CODE = "1002";

    /** Default card acceptor state (2 chars) */
    public static final String DEFAULT_STATE = "  ";

    /** Member message text area — placeholder spaces */
    public static final String MEMBER_MESSAGE_TEXT = "";

    /** Reserved field — spaces */
    public static final String RESERVED = "";

    public static String generateIcaNumber(String base, int seq) {
        String s = base.substring(0, Math.min(6, base.length())) + String.format("%02d", seq % 100);
        return s;
    }

    public static String buildArn(String transactionId, LocalDate date) {
        String clean = transactionId.replace("-", "").toUpperCase();
        String txnKey = clean.length() > 10 ? clean.substring(0, 10) : String.format("%-10s", clean);
        String mmdd = date.format(DateTimeFormatter.ofPattern("MMdd"));
        String withoutChk = "MC" + mmdd + txnKey;
        int chk = 0;
        for (char c : withoutChk.toCharArray()) {
            if (c >= '0' && c <= '9') chk += (c - '0');
        }
        return withoutChk + (chk % 10);
    }

    public static String extractTxnKey(String arn) {
        if (arn == null || arn.length() < 17) return null;
        return arn.substring(6, 16).trim();
    }

    public static boolean matchesTransaction(String txnKey, String transactionId) {
        if (txnKey == null || transactionId == null) return false;
        String clean = transactionId.replace("-", "").toUpperCase();
        return clean.startsWith(txnKey);
    }

    public static String generateAuthCode() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }

    public static final BigDecimal DISCREPANCY_DELTA = new BigDecimal("10.000");

    private static final Map<String, Integer> DECIMALS = Map.of(
            "TND", 3, "KWD", 3, "BHD", 3, "OMR", 3, "JOD", 3,
            "JPY", 0, "KRW", 0, "CLP", 0
    );

    public static int decimals(String currency) {
        return DECIMALS.getOrDefault(currency != null ? currency.toUpperCase() : "", 2);
    }
}
