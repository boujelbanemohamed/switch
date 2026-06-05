package com.switchplatform.platform.service.clearing.network.visa;

import java.math.BigDecimal;
import java.util.Map;

public class VisaBaseIIFormatter {

    private VisaBaseIIFormatter() {}

    private static final Map<String, Integer> DECIMALS = Map.of(
            "TND", 3, "KWD", 3, "BHD", 3, "OMR", 3, "JOD", 3,
            "JPY", 0, "KRW", 0, "CLP", 0
    );

    public static String un(long value, int len) {
        String s = Long.toString(value);
        if (s.length() > len) {
            s = s.substring(s.length() - len);
        }
        return padLeft(s, len, '0');
    }

    public static String an(String value, int len) {
        if (value == null) {
            return padRight("", len, ' ');
        }
        if (value.length() > len) {
            return value.substring(0, len);
        }
        return padRight(value, len, ' ');
    }

    public static String amount(BigDecimal amount, String currency, int len) {
        if (amount == null) {
            return padLeft("", len, '0');
        }
        int decimals = DECIMALS.getOrDefault(currency != null ? currency.toUpperCase() : "", 2);
        long value = amount.multiply(BigDecimal.TEN.pow(decimals)).longValue();
        return un(value, len);
    }

    private static String padLeft(String s, int len, char c) {
        if (s.length() >= len) return s;
        StringBuilder sb = new StringBuilder(len);
        for (int i = s.length(); i < len; i++) sb.append(c);
        sb.append(s);
        return sb.toString();
    }

    private static String padRight(String s, int len, char c) {
        if (s.length() >= len) return s;
        StringBuilder sb = new StringBuilder(len);
        sb.append(s);
        for (int i = s.length(); i < len; i++) sb.append(c);
        return sb.toString();
    }
}
