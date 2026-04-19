// ---------------------------------------------------------------------------
// Shared date formatting utilities
// ---------------------------------------------------------------------------

/**
 * Returns today as a YYYY-MM-DD string evaluated in the org's timezone.
 * Falls back to the browser timezone when not provided.
 * Uses en-CA locale because it produces ISO date format natively.
 */
export function todayInTz(timezone?: string): string {
  const tz = timezone ?? Intl.DateTimeFormat().resolvedOptions().timeZone
  return new Date().toLocaleDateString('en-CA', { timeZone: tz })
}

/**
 * Parses a YYYY-MM-DD string as a local (non-UTC) Date to avoid the UTC
 * midnight trap where new Date("2026-04-18") is midnight UTC, not local.
 */
function parseDateLocal(dateStr: string): Date {
  const [y, m, d] = dateStr.split('-').map(Number)
  return new Date(y, m - 1, d)
}

/**
 * Returns how many days a due date is past today (positive = overdue,
 * 0 = due today, negative = future). Evaluates "today" in the org timezone.
 */
export function daysPastDue(dueDateStr: string, timezone?: string): number {
  const today = parseDateLocal(todayInTz(timezone))
  const due   = parseDateLocal(dueDateStr.split('T')[0])
  return Math.round((today.getTime() - due.getTime()) / 86_400_000)
}

/**
 * timeAgo — day-granularity, for past-only timestamps (sentAt, createdAt).
 * "today" · "1 day ago" · "X days ago"
 */
export function timeAgo(dateStr: string | null | undefined): string {
  if (!dateStr) return '—'
  const diffDays = Math.floor((Date.now() - new Date(dateStr).getTime()) / 86_400_000)
  if (diffDays <= 0) return 'today'
  if (diffDays === 1) return '1 day ago'
  return `${diffDays} days ago`
}

/**
 * timeAgoFine — minute/hour/day granularity for activity feeds and dashboards.
 * "5m ago" · "2h ago" · "Yesterday" · "3d ago"
 */
export function timeAgoFine(dateStr: string | null | undefined): string {
  if (!dateStr) return '—'
  const mins = Math.floor((Date.now() - new Date(dateStr).getTime()) / 60_000)
  if (mins < 60)  return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24)   return `${hrs}h ago`
  const days = Math.floor(hrs / 24)
  if (days === 1) return 'Yesterday'
  return `${days}d ago`
}

/**
 * dueDateLabel — for invoice due dates which can be past OR future.
 * Past:   "1 day overdue" · "X days overdue"
 * Today:  "today"
 * Future: "tomorrow" · "in X days"
 *
 * orgTimezone must be passed (IANA string, e.g. "America/New_York") so that
 * "today" is evaluated in the org's timezone, not the browser's local clock.
 */
export function dueDateLabel(dateStr: string | null | undefined, orgTimezone?: string): string {
  if (!dateStr) return '—'
  const days = daysPastDue(dateStr, orgTimezone)
  if (days > 1)  return `${days} days overdue`
  if (days === 1) return '1 day overdue'
  if (days === 0) return 'today'
  const ahead = Math.abs(days)
  if (ahead === 1) return 'tomorrow'
  return `in ${ahead} days`
}
