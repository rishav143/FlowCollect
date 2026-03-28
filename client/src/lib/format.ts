/**
 * format.ts — shared currency and date helpers.
 *
 * All formatting is driven by the org's currency / timezone from the auth
 * store.  Components import formatCurrency / formatDate and pass the values
 * they already have — no component needs to know about locales or time zones.
 */

// ---------------------------------------------------------------------------
// Currency → locale mapping
// ---------------------------------------------------------------------------

const CURRENCY_LOCALE: Record<string, string> = {
  INR: 'en-IN',
  USD: 'en-US',
  EUR: 'de-DE',
  GBP: 'en-GB',
  AED: 'en-AE',
  SGD: 'en-SG',
  AUD: 'en-AU',
}

function localeFor(currency: string): string {
  return CURRENCY_LOCALE[currency] ?? 'en-US'
}

// ---------------------------------------------------------------------------
// Currency formatting
// ---------------------------------------------------------------------------

export interface FormatCurrencyOptions {
  /** Show decimal places (default true) */
  decimals?: boolean
  /** Force compact notation e.g. 1.2K, 4.5M (default false) */
  compact?:  boolean
}

/**
 * Format a monetary amount using the correct locale for the given currency.
 *
 * @example
 *   formatCurrency(1234.5, 'INR')   // ₹1,234.50
 *   formatCurrency(1234.5, 'USD')   // $1,234.50
 *   formatCurrency(1234.5, 'AUD')   // A$1,234.50
 *   formatCurrency(99999, 'USD', { compact: true })  // $100K
 */
export function formatCurrency(
  amount:   number,
  currency: string,
  options:  FormatCurrencyOptions = {},
): string {
  const { decimals = true, compact = false } = options
  try {
    return new Intl.NumberFormat(localeFor(currency), {
      style:                 'currency',
      currency:              currency || 'USD',
      minimumFractionDigits: decimals ? 2 : 0,
      maximumFractionDigits: decimals ? 2 : 0,
      ...(compact ? { notation: 'compact' } : {}),
    }).format(amount)
  } catch {
    // Fallback: plain number with currency code
    return `${currency} ${amount.toFixed(decimals ? 2 : 0)}`
  }
}

// ---------------------------------------------------------------------------
// Date formatting
// ---------------------------------------------------------------------------

export interface FormatDateOptions {
  /** IANA timezone string, e.g. 'Asia/Kolkata', 'Australia/Sydney' */
  timezone?: string
  /** Date style: 'short' = "27 Mar", 'medium' = "27 Mar 2026", 'long' = "27 March 2026" */
  style?: 'short' | 'medium' | 'long'
}

/**
 * Format a date+time string — e.g. "27 Mar 2026, 10:30 AM"
 */
export function formatDateTime(
  value:    string | Date | null | undefined,
  timezone?: string,
): string {
  if (!value) return '—'
  try {
    const date = typeof value === 'string' ? new Date(value) : value
    if (isNaN(date.getTime())) return '—'
    return date.toLocaleString(undefined, {
      day: 'numeric', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
      ...(timezone ? { timeZone: timezone } : {}),
    })
  } catch {
    return String(value).slice(0, 16)
  }
}

/**
 * Format a date string/value in the org's timezone.
 *
 * @example
 *   formatDate('2026-03-27', { timezone: 'Australia/Sydney', style: 'medium' })
 *   // "27 Mar 2026"
 */
export function formatDate(
  value:   string | Date | null | undefined,
  options: FormatDateOptions = {},
): string {
  if (!value) return '—'
  const { timezone, style = 'medium' } = options
  try {
    const date = typeof value === 'string' ? new Date(value) : value
    if (isNaN(date.getTime())) return '—'

    const dayMonth: Intl.DateTimeFormatOptions =
      style === 'short'
        ? { day: 'numeric', month: 'short' }
        : style === 'medium'
          ? { day: 'numeric', month: 'short', year: 'numeric' }
          : { day: 'numeric', month: 'long',  year: 'numeric' }

    return date.toLocaleDateString(undefined, {
      ...dayMonth,
      ...(timezone ? { timeZone: timezone } : {}),
    })
  } catch {
    return String(value).slice(0, 10)
  }
}
