package com.switchplatform.platform.service.clearing.network.mastercard;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MastercardIpmFormatter {

    private final String currencyCode;
    private final String currencyAlpha;

    public MastercardIpmFormatter(String currencyAlpha) {
        this.currencyAlpha = currencyAlpha != null ? currencyAlpha.toUpperCase() : "TND";
        this.currencyCode = MastercardIpmSimConfig.CURRENCY_CODE_TND;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public String an(String value, int length) {
        if (value == null) return spaces(length);
        String v = value.trim();
        if (v.length() > length) return v.substring(0, length);
        return String.format("%-" + length + "s", v);
    }

    public String nn(String value, int length) {
        if (value == null) return spaces(length);
        String v = value.trim();
        if (v.length() > length) return v.substring(0, length);
        return String.format("%" + length + "s", v);
    }

    public String amount(BigDecimal value) {
        return amount(value, currencyAlpha);
    }

    public String amount(BigDecimal value, String currency) {
        if (value == null) value = BigDecimal.ZERO;
        int d = MastercardIpmSimConfig.decimals(currency);
        long l = value.setScale(d, RoundingMode.HALF_UP)
                .multiply(BigDecimal.TEN.pow(d))
                .longValue();
        return String.format("%012d", l);
    }

    public String dateMMDD(java.time.LocalDate date) {
        if (date == null) return "    ";
        return date.format(java.time.format.DateTimeFormatter.ofPattern("MMdd"));
    }

    public static String spaces(int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(' ');
        return sb.toString();
    }
}
