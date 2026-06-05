package com.switchplatform.platform.service.clearing.smt;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SmtFieldFormatterTest {

    @Test
    void alphaLeftPadsSpaces() {
        assertEquals("ABC  ", SmtFieldFormatter.alphaLeft("ABC", 5));
    }

    @Test
    void alphaLeftTruncates() {
        assertEquals("ABCDE", SmtFieldFormatter.alphaLeft("ABCDEFGH", 5));
    }

    @Test
    void alphaLeftNullReturnsSpaces() {
        assertEquals("     ", SmtFieldFormatter.alphaLeft(null, 5));
    }

    @Test
    void numericRightStringPadsZeros() {
        assertEquals("00123", SmtFieldFormatter.numericRight("123", 5));
    }

    @Test
    void numericRightStringTruncates() {
        assertEquals("789", SmtFieldFormatter.numericRight("123456789", 3));
    }

    @Test
    void numericRightLongPadsZeros() {
        assertEquals("000123", SmtFieldFormatter.numericRight(123L, 6));
    }

    @Test
    void numericRightLongNegativeUsesAbs() {
        assertEquals("000123", SmtFieldFormatter.numericRight(-123L, 6));
    }

    @Test
    void amount9v999FormatsCorrectly() {
        assertEquals("001234500", SmtFieldFormatter.amount(new BigDecimal("1234.500"), 9));
    }

    @Test
    void amount12Digits() {
        assertEquals("000000150000", SmtFieldFormatter.amount(new BigDecimal("150.000"), 12));
    }

    @Test
    void amountNullReturnsZeros() {
        assertEquals("000000000", SmtFieldFormatter.amount(null, 9));
    }

    @Test
    void amountLongValue() {
        assertEquals("000150000", SmtFieldFormatter.amount(150000L, 9));
    }

    @Test
    void dateJJMMAAFormats() {
        assertEquals("040625", SmtFieldFormatter.dateJJMMAA(LocalDate.of(2025, 6, 4)));
    }

    @Test
    void dateJJMMAANull() {
        assertEquals("      ", SmtFieldFormatter.dateJJMMAA(null));
    }

    @Test
    void spaces() {
        assertEquals("   ", SmtFieldFormatter.spaces(3));
        assertEquals("", SmtFieldFormatter.spaces(0));
    }

    @Test
    void parseAmountValid() {
        assertEquals(new BigDecimal("12.345"), SmtFieldFormatter.parseAmount("000012345"));
    }

    @Test
    void parseAmountZero() {
        assertEquals(BigDecimal.ZERO, SmtFieldFormatter.parseAmount("000000000"));
    }

    @Test
    void parseDateJJMMAA() {
        assertEquals(LocalDate.of(2025, 6, 4), SmtFieldFormatter.parseDateJJMMAA("040625"));
    }
}
