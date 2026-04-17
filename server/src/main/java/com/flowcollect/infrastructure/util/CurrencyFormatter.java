package com.flowcollect.infrastructure.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Central utility for locale-aware currency formatting.
 *
 * <p>Single source of truth for amount rendering across system emails, user-template
 * rendering, PDF generation, and AI insight prompts. All produce consistent output
 * (e.g. "₹77,143", "$1,200", "€1.500") regardless of where they are called.
 *
 * <p>Always formats with <strong>zero decimal places</strong> (whole numbers only),
 * matching the rounding applied to invoice amounts in the domain layer.
 */
public final class CurrencyFormatter {

    private CurrencyFormatter() {}

    /**
     * Formats {@code amount} with the locale-appropriate symbol and grouping separators
     * for the given ISO 4217 {@code currencyCode}.
     *
     * <p>Examples:
     * <ul>
     *   <li>INR, 77143    → "₹77,143"</li>
     *   <li>INR, 443000   → "₹4,43,000"   (Indian lakh system)</li>
     *   <li>INR, 1560858  → "₹15,60,858"  (Indian lakh system)</li>
     *   <li>USD, 1200     → "$1,200"</li>
     *   <li>EUR, 1500     → "€1.500"</li>
     *   <li>GBP, 950      → "£950"</li>
     * </ul>
     *
     * @param amount       amount to format; {@code null} returns empty string
     * @param currencyCode ISO 4217 code (e.g. "INR", "USD"); {@code null} returns empty string
     * @return formatted string, never {@code null}
     */
    public static String format(BigDecimal amount, String currencyCode) {
        if (amount == null || currencyCode == null || currencyCode.isBlank()) return "";
        try {
            if ("INR".equals(currencyCode)) {
                // Java's NumberFormat for en_IN silently falls back to 3-digit grouping on many
                // JVM builds, producing ₹443,000 instead of the correct ₹4,43,000 (lakh system).
                // Use a hand-rolled algorithm that is guaranteed correct on every JVM:
                //   1,000 → ₹1,000 | 1,00,000 → ₹1,00,000 | 15,60,858 → ₹15,60,858
                return formatIndianRupees(amount);
            }
            Currency currency = Currency.getInstance(currencyCode);
            NumberFormat fmt  = NumberFormat.getCurrencyInstance(localeFor(currencyCode));
            fmt.setCurrency(currency);
            fmt.setMinimumFractionDigits(0);
            fmt.setMaximumFractionDigits(0);
            return fmt.format(amount);
        } catch (Exception e) {
            // Fallback: plain number + code, still readable
            return amount.toPlainString() + " " + currencyCode;
        }
    }

    /**
     * Overload accepting a Java {@link Currency} object directly.
     */
    public static String format(BigDecimal amount, Currency currency) {
        if (currency == null) return amount != null ? amount.toPlainString() : "";
        return format(amount, currency.getCurrencyCode());
    }

    /**
     * Formats a rupee amount using the Indian lakh/crore grouping system.
     *
     * <p>Groups: last 3 digits, then groups of 2 going left.
     * <pre>
     *   443000   → ₹4,43,000
     *   1560858  → ₹15,60,858
     *   100000   → ₹1,00,000
     *   10000000 → ₹1,00,00,000
     * </pre>
     */
    private static String formatIndianRupees(BigDecimal amount) {
        long value = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        boolean negative = value < 0;
        String digits = String.valueOf(Math.abs(value));
        int len = digits.length();

        if (len <= 3) {
            return (negative ? "-₹" : "₹") + digits;
        }

        StringBuilder sb = new StringBuilder();
        // Rightmost group: 3 digits
        sb.append(digits, len - 3, len);
        int pos = len - 3;

        // Remaining groups: 2 digits each
        while (pos > 0) {
            int start = Math.max(0, pos - 2);
            sb.insert(0, ',');                 // comma separator first
            sb.insert(0, digits, start, pos);  // then the digit group in front of it
            pos = start;
        }

        return (negative ? "-₹" : "₹") + sb;
    }

    /**
     * Returns the most appropriate {@link Locale} for formatting numbers in the
     * given currency. The locale controls grouping separators and decimal symbols
     * (e.g. comma vs period). The currency symbol itself is set explicitly via
     * {@link NumberFormat#setCurrency} and overrides the locale's default symbol.
     */
    public static Locale localeFor(String currencyCode) {
        if (currencyCode == null) return Locale.US;
        return switch (currencyCode) {
            case "INR" -> Locale.of("en", "IN");   // ₹15,60,858 (lakh — handled by formatIndianRupees, not this locale)
            case "USD" -> Locale.US;                // $1,000
            case "EUR" -> Locale.GERMANY;           // €1.000
            case "GBP" -> Locale.UK;                // £1,000
            case "AUD" -> Locale.of("en", "AU");    // A$1,000
            case "CAD" -> Locale.CANADA;            // CA$1,000
            case "SGD" -> Locale.of("en", "SG");    // S$1,000
            case "AED" -> Locale.of("en", "AE");    // AED 1,000
            case "JPY" -> Locale.JAPAN;             // ¥1,000
            case "CNY" -> Locale.CHINA;             // ¥1,000
            case "NZD" -> Locale.of("en", "NZ");    // NZ$1,000
            case "CHF" -> Locale.of("de", "CH");    // CHF 1'000
            case "MYR" -> Locale.of("en", "MY");    // RM1,000
            case "PHP" -> Locale.of("en", "PH");    // ₱1,000
            case "THB" -> Locale.of("th", "TH");    // ฿1,000
            case "IDR" -> Locale.of("id", "ID");    // Rp1.000
            case "BRL" -> Locale.of("pt", "BR");    // R$1.000
            case "ZAR" -> Locale.of("en", "ZA");    // R1,000
            default    -> Locale.US;
        };
    }
}
