import type { LifeCycleStatus, TimeStatus } from '@/types/invoice.types'

// ---------------------------------------------------------------------------
// Data
// ---------------------------------------------------------------------------

const TIME_META: Partial<Record<TimeStatus, { label: string; cls: string }>> = {
  OVERDUE:   { label: 'Overdue',   cls: 'text-red-600 dark:text-red-300'     },
  DUE_TODAY: { label: 'Due Today', cls: 'text-amber-600 dark:text-amber-400' },
}

const LC_META: Record<LifeCycleStatus, { label: string; cls: string }> = {
  DRAFT:          { label: 'Draft',      cls: 'text-c-muted'                          },
  ISSUED:         { label: 'Issued',     cls: 'text-c-muted'                          },
  PARTIALLY_PAID: { label: 'Partial',    cls: 'text-c-muted'                          },
  PAID:           { label: 'Paid',       cls: 'text-green-600 dark:text-green-400'    },
  CANCELLED:      { label: 'Cancelled',  cls: 'text-c-muted'                          },
}

// Terminal lifecycle statuses where time label is never shown
const HIDE_TIME = new Set<LifeCycleStatus>(['DRAFT', 'PAID', 'CANCELLED'])

// ---------------------------------------------------------------------------
// Utility — use this when you only need the raw values (e.g. for dot colours)
// ---------------------------------------------------------------------------

export interface InvoiceStatusDisplay {
  timeMeta:  { label: string; cls: string } | null
  lcLabel:   string
  lcCls:     string
}

export function getInvoiceStatusDisplay(
  lc: LifeCycleStatus,
  ts: TimeStatus,
): InvoiceStatusDisplay {
  const timeMeta = HIDE_TIME.has(lc) ? null : (TIME_META[ts] ?? null)
  const { label: lcLabel, cls: lcCls } = LC_META[lc]
  return { timeMeta, lcLabel, lcCls }
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

interface Props {
  lifecycle: LifeCycleStatus
  time:      TimeStatus
  className?: string
}

export default function InvoiceStatusBadge({ lifecycle, time, className = '' }: Props) {
  const { timeMeta, lcLabel, lcCls } = getInvoiceStatusDisplay(lifecycle, time)

  if (timeMeta) {
    return (
      <span className={`inline-flex flex-col gap-0.5 ${className}`}>
        <span className={`font-semibold leading-tight ${timeMeta.cls}`}>{timeMeta.label}</span>
        <span className={`text-xs leading-tight ${lcCls}`}>{lcLabel}</span>
      </span>
    )
  }

  return (
    <span className={`inline-flex font-medium ${lcCls} ${className}`}>
      {lcLabel}
    </span>
  )
}
