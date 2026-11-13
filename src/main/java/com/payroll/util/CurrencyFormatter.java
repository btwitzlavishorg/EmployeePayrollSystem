package com.payroll.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {

    private static final Locale INDIA_LOCALE = new Locale("en", "IN");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(INDIA_LOCALE);

    public static String format(BigDecimal amount) {
        if (amount == null) return "₹ 0.00";
        return "₹ " + String.format("%,.2f", amount.doubleValue());
    }

    public static String formatNoSymbol(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount.doubleValue());
    }

    /**
     * Converts a number to words (for payslips: "Seventy-Four Thousand Five Hundred Rupees Only")
     */
    public static String convertToWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Zero Rupees Only";
        }
        long val = amount.longValue();
        return convertNumberToWords(val) + " Rupees Only";
    }

    private static final String[] UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static String convertNumberToWords(long n) {
        if (n < 0) return "Minus " + convertNumberToWords(-n);
        if (n < 20) return UNITS[(int) n];
        if (n < 100) return TENS[(int) (n / 10)] + ((n % 10 != 0) ? " " + UNITS[(int) (n % 10)] : "");
        if (n < 1000) return UNITS[(int) (n / 100)] + " Hundred" + ((n % 100 != 0) ? " and " + convertNumberToWords(n % 100) : "");
        if (n < 100000) return convertNumberToWords(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " + convertNumberToWords(n % 1000) : "");
        if (n < 10000000) return convertNumberToWords(n / 100000) + " Lakh" + ((n % 100000 != 0) ? " " + convertNumberToWords(n % 100000) : "");
        return convertNumberToWords(n / 10000000) + " Crore" + ((n % 10000000 != 0) ? " " + convertNumberToWords(n % 10000000) : "");
    }
}
