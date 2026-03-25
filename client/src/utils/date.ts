// ---------------------------------------------------------------------------
// Shared date formatting utilities
// ---------------------------------------------------------------------------

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
 */
export function dueDateLabel(dateStr: string | null | undefined): string {
  if (!dateStr) return '—'
  const diffDays = Math.floor((Date.now() - new Date(dateStr).getTime()) / 86_400_000)
  if (diffDays > 1)  return `${diffDays} days overdue`
  if (diffDays === 1) return '1 day overdue'
  if (diffDays === 0) return 'today'
  const ahead = Math.abs(diffDays)
  if (ahead === 1) return 'tomorrow'
  return `in ${ahead} days`
}
