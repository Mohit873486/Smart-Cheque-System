package com.chequeprint.util;

/**
 * Converts a numeric amount to Indian-English words.
 * Example: 25000.50 → "Rupees Twenty Five Thousand and Paise Fifty Only"
 */
public class AmountToWords {

    private static final String[] ONES = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
        "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty",
        "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String convert(double amount) {
        long rupees = (long) amount;
        long paise  = Math.round((amount - rupees) * 100);

        StringBuilder sb = new StringBuilder("Rupees ");
        sb.append(inWords(rupees));
        if (paise > 0) {
            sb.append(" and Paise ").append(inWords(paise));
        }
        sb.append(" Only");
        return sb.toString();
    }

    private static String inWords(long n) {
        if (n == 0) return "Zero";
        StringBuilder sb = new StringBuilder();
        if (n >= 10_00_00_000L) {
            sb.append(inWords(n / 10_00_00_000L)).append(" Arab ");
            n %= 10_00_00_000L;
        }
        if (n >= 1_00_00_000L) {
            sb.append(inWords(n / 1_00_00_000L)).append(" Crore ");
            n %= 1_00_00_000L;
        }
        if (n >= 1_00_000L) {
            sb.append(inWords(n / 1_00_000L)).append(" Lakh ");
            n %= 1_00_000L;
        }
        if (n >= 1000) {
            sb.append(inWords(n / 1000)).append(" Thousand ");
            n %= 1000;
        }
        if (n >= 100) {
            sb.append(ONES[(int)(n / 100)]).append(" Hundred ");
            n %= 100;
        }
        if (n >= 20) {
            sb.append(TENS[(int)(n / 10)]);
            if (n % 10 != 0) sb.append(" ").append(ONES[(int)(n % 10)]);
            sb.append(" ");
        } else if (n > 0) {
            sb.append(ONES[(int) n]).append(" ");
        }
        return sb.toString().trim();
    }
}