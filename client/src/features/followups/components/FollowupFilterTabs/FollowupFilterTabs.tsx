import type { FollowupFilter } from '../../hooks/useFollowups'

const TABS: { id: FollowupFilter; label: string }[] = [
  { id: 'ALL',       label: 'All Active'  },
  { id: 'OVERDUE',   label: 'Overdue'     },
  { id: 'DUE_TODAY', label: 'Due Today'   },
  { id: 'UPCOMING',  label: 'Upcoming'    },
]

interface Props {
  active:   FollowupFilter
  onChange: (f: FollowupFilter) => void
}

export default function FollowupFilterTabs({ active, onChange }: Props) {
  return (
    <div className="flex gap-1 border-b border-c-border -mb-px overflow-x-auto scrollbar-hide">
      {TABS.map((t) => (
        <button
          key={t.id}
          onClick={() => onChange(t.id)}
          className={[
            'px-4 py-2.5 text-sm font-medium border-b-2 transition-colors whitespace-nowrap shrink-0',
            active === t.id
              ? 'border-[#2E7A8E] text-[#2E7A8E] dark:text-[#29B6F6] dark:border-[#29B6F6]'
              : 'border-transparent text-c-muted hover:text-[#0D1B2A] dark:hover:text-white',
          ].join(' ')}
        >
          {t.label}
        </button>
      ))}
    </div>
  )
}
