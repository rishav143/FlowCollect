import type { LifeCycleStatus, TimeStatus } from '@/types/invoice.types'

export interface InvoiceFilter {
  lifeCycleStatus?: LifeCycleStatus
  timeStatus?:      TimeStatus
}

interface Tab {
  label:  string
  filter: InvoiceFilter
}

const TABS: Tab[] = [
  { label: 'All',        filter: {} },
  { label: 'Draft',      filter: { lifeCycleStatus: 'DRAFT' } },
  { label: 'Issued',     filter: { lifeCycleStatus: 'ISSUED' } },
  { label: 'Overdue',    filter: { timeStatus: 'OVERDUE' } },
  { label: 'Partial',    filter: { lifeCycleStatus: 'PARTIALLY_PAID' } },
  { label: 'Paid',       filter: { lifeCycleStatus: 'PAID' } },
  { label: 'Cancelled',  filter: { lifeCycleStatus: 'CANCELLED' } },
]

interface Props {
  active:   InvoiceFilter
  onChange: (f: InvoiceFilter) => void
}

function filtersEqual(a: InvoiceFilter, b: InvoiceFilter) {
  return a.lifeCycleStatus === b.lifeCycleStatus && a.timeStatus === b.timeStatus
}

export default function InvoiceStatusTabs({ active, onChange }: Props) {
  return (
    <div className="flex gap-1 overflow-x-auto scrollbar-hide pb-0.5">
      {TABS.map((tab) => {
        const isActive = filtersEqual(active, tab.filter)
        return (
          <button
            key={tab.label}
            onClick={() => onChange(tab.filter)}
            className={[
              'shrink-0 px-3.5 py-1.5 rounded-lg text-sm font-medium transition-colors',
              isActive
                ? 'bg-[#2E7A8E] text-white'
                : 'text-[#8A9BAE] hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#8A9BAE]/10 dark:hover:bg-[#243447]',
            ].join(' ')}
          >
            {tab.label}
          </button>
        )
      })}
    </div>
  )
}
