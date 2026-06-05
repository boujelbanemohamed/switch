package com.switchplatform.platform.service.clearing.smt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class SmtFieldFormatter {

    private SmtFieldFormatter() {}

    public static String alphaLeft(String value, int len) {
        if (value == null) value = "";
        if (value.length() > len) return value.substring(0, len);
        return value + " ".repeat(len - value.length());
    }

    public static String numericRight(long value, int len) {
        return numericRight(String.valueOf(Math.abs(value)), len);
    }

    public static String numericRight(String value, int len) {
        if (value == null || value.isEmpty()) return "0".repeat(len);
        if (value.length() > len) return value.substring(value.length() - len);
        return "0".repeat(len - value.length()) + value;
    }

    public static String amount(BigDecimal montant, int len) {
        if (montant == null) return "0".repeat(len);
        long cents = montant.setScale(3, RoundingMode.HALF_UP).movePointRight(3).abs().longValue();
        return numericRight(cents, len);
    }

    public static String amount(long value, int len) {
        return numericRight(Math.abs(value), len);
    }

    public static String dateJJMMAA(LocalDate date) {
        if (date == null) return "      ";
        return date.format(DateTimeFormatter.ofPattern("ddMMyy"));
    }

    public static LocalDate parseDateJJMMAA(String raw) {
        if (raw == null || raw.trim().length() != 6) return null;
        try {
            return LocalDate.parse(raw.trim(), DateTimeFormatter.ofPattern("ddMMyy"));
        } catch (Exception e) {
            return null;
        }
    }

    public static String spaces(int n) {
        return " ".repeat(Math.max(0, n));
    }

    public static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.length() == 0 || raw.trim().isEmpty()) return BigDecimal.ZERO;
        String trimmed = raw.trim();
        if (trimmed.chars().allMatch(c -> c == '0')) return BigDecimal.ZERO;
        try {
            return new BigDecimal(trimmed).divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
