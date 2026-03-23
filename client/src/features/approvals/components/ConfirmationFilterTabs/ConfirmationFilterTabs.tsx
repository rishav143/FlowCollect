import type { ApprovalFilter } from '../../hooks/useConfirmations'

const TABS: { id: ApprovalFilter; label: string }[] = [
  { id: 'PENDING_APPROVAL',    label: 'Pending'             },
  { id: 'APPROVED',            label: 'Approved'            },
  { id: 'REMAINING_REQUESTED', label: 'Remaining Requested' },
  { id: 'REJECTED',            label: 'Rejected'            },
  { id: 'ALL',                 label: 'All'                 },
]

interface Props {
  active:   ApprovalFilter
  onChange: (f: ApprovalFilter) => void
  pendingCount?: number
}

export default function ConfirmationFilterTabs({ active, onChange, pendingCount }: Props) {
  return (
    <div className="flex gap-1 border-b border-[#F4F7F9] dark:border-white/10 -mb-px overflow-x-auto scrollbar-none">
      {TABS.map((t) => (
        <button
          key={t.id}
          onClick={() => onChange(t.id)}
          className={[
            'flex items-center gap-1.5 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors whitespace-nowrap shrink-0',
            active === t.id
              ? 'border-[#2E7A8E] text-[#2E7A8E] dark:text-[#29B6F6] dark:border-[#29B6F6]'
              : 'border-transparent text-[#8A9BAE] hover:text-[#0D1B2A] dark:hover:text-white',
          ].join(' ')}
        >
          {t.label}
          {t.id === 'PENDING_APPROVAL' && !!pendingCount && pendingCount > 0 && (
            <span className="min-w-[18px] text-center text-[10px] font-bold tabular-nums px-1.5 py-0.5 rounded-full bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400">
              {pendingCount > 99 ? '99+' : pendingCount}
            </span>
          )}
        </button>
      ))}
    </div>
  )
}
