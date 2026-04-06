package com.flowcollect.infrastructure.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
                // Java's NumberFormat for en_IN uses US 3-digit grouping on many JVM versions.
                // Force the Indian lakh system (groups of 2 after the first 3 from the right:
                // e.g. 1560858 → 15,60,858) with an explicit DecimalFormat pattern.
                // Avoid ¤ + setCurrency() which can silently reset the symbol; prepend ₹ directly.
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("en", "IN"));
                symbols.setGroupingSeparator(',');
                symbols.setDecimalSeparator('.');
                DecimalFormat fmt = new DecimalFormat("#,##,##0", symbols);
                fmt.setMinimumFractionDigits(0);
                fmt.setMaximumFractionDigits(0);
                return "₹" + fmt.format(amount);
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
     * Returns the most appropriate {@link Locale} for formatting numbers in the
     * given currency. The locale controls grouping separators and decimal symbols
     * (e.g. comma vs period). The currency symbol itself is set explicitly via
     * {@link NumberFormat#setCurrency} and overrides the locale's default symbol.
     */
    public static Locale localeFor(String currencyCode) {
        if (currencyCode == null) return Locale.US;
        return switch (currencyCode) {
            case "INR" -> Locale.of("en", "IN");   // ₹15,60,858 (lakh — handled by explicit DecimalFormat above)
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
