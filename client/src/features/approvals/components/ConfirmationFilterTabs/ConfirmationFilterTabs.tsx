import type { ApprovalFilter } from '../../hooks/useConfirmations'

const TABS: { id: ApprovalFilter; label: string }[] = [
  { id: 'PENDING_APPROVAL',    label: 'Pending'             },
  { id: 'APPROVED',            label: 'Approved'            },
  { id: 'REMAINING_REQUESTED', label: 'Remaining Requested' },
  { id: 'REJECTED',            label: 'Rejected'            },
  { id: 'ALL',                 label: 'All'                 },
]

interface Props {
  active:        ApprovalFilter
  onChange:      (f: ApprovalFilter) => void
  pendingCount?: number
}

export default function ConfirmationFilterTabs({ active, onChange, pendingCount }: Props) {
  return (
    <div className="flex gap-1 border-b border-c-border -mb-px overflow-x-auto scrollbar-hide">
      {TABS.map((t) => (
        <button
          key={t.id}
          onClick={() => onChange(t.id)}
          className={[
            'inline-flex items-center gap-1.5 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors whitespace-nowrap shrink-0',
            active === t.id
              ? 'border-[#2E7A8E] text-[#2E7A8E] dark:text-[#29B6F6] dark:border-[#29B6F6]'
              : 'border-transparent text-c-muted hover:text-[#0D1B2A] dark:hover:text-white',
          ].join(' ')}
        >
          {t.label}
          {t.id === 'PENDING_APPROVAL' && !!pendingCount && pendingCount > 0 && (
            <span className="text-[11px] font-semibold tabular-nums opacity-70">
              {pendingCount > 99 ? '99+' : pendingCount}
            </span>
          )}
        </button>
      ))}
    </div>
  )
}
