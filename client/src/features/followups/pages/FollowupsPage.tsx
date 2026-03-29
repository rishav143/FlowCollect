import { useState, useMemo } from 'react'
import { Send, Bell } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listCustomers } from '@/api/customer.api'
import { useFollowupInvoices, useFollowupsByInvoices, type FollowupFilter } from '../hooks/useFollowups'
import FollowupFilterTabs from '../components/FollowupFilterTabs/FollowupFilterTabs'
import FollowupCard from '../components/FollowupCard/FollowupCard'
import DispatchModal from '../components/DispatchModal/DispatchModal'
import ViewToggle, { useViewPreference, gridClass } from '@/ui/components/ViewToggle'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'

import { formatCurrency } from '@/lib/format'

// ---------------------------------------------------------------------------
// Desktop table row
// ---------------------------------------------------------------------------

import { timeAgo, dueDateLabel } from '@/utils/date'

const CHANNEL_LABEL: Record<string, string> = {
  EMAIL: 'Email', SMS: 'SMS', WHATSAPP: 'WhatsApp',
}

function InvoiceRow({
  invoice,
  currency,
  customerName,
  lastFollowup,
  onSend,
  canSend = true,
}: {
  invoice:       InvoiceResponse
  currency:      string
  customerName?: string
  lastFollowup?: import('@/types/followup.types').FollowUpResponse
  onSend:        () => void
  canSend?:      boolean
}) {
  return (
    <tr className="border-b border-c-border last:border-0 hover:bg-[#F4F7F9]/50 dark:hover:bg-white/5 transition-colors">
      {/* Client + invoice # */}
      <td className="py-3 px-4">
        <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">
          {customerName ?? '—'}
        </p>
        <p className="text-xs text-c-muted mt-0.5">{invoice.invoiceNumber}</p>
      </td>
      {/* Amount owed */}
      <td className="py-3 px-4 text-sm font-bold tabular-nums text-[#0D1B2A] dark:text-white">
        {formatCurrency(invoice.remainingAmount, currency, { decimals: false })}
      </td>
      {/* Due */}
      <td className="py-3 px-4 text-sm">
        <span className={
          invoice.timeStatus === 'OVERDUE'   ? 'text-red-500 font-semibold' :
          invoice.timeStatus === 'DUE_TODAY' ? 'text-amber-600 dark:text-amber-400 font-semibold' :
          'text-c-muted'
        }>
          {dueDateLabel(invoice.dueDate)}
        </span>
      </td>
      {/* Last contacted */}
      <td className="py-3 px-4 text-sm text-c-muted">
        {lastFollowup
          ? `${CHANNEL_LABEL[lastFollowup.channel] ?? lastFollowup.channel} · ${timeAgo(lastFollowup.sentAt ?? lastFollowup.createdAt)}`
          : <span className="italic">Never</span>
        }
      </td>
      {/* Action */}
      <td className="py-3 px-4 text-right">
        {canSend ? (
          <button
            onClick={onSend}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold text-white transition-opacity hover:opacity-90"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            <Send size={12} strokeWidth={2.5} />
            Send
          </button>
        ) : (
          <span className="text-xs text-c-muted italic">No client</span>
        )}
      </td>
    </tr>
  )
}

// ---------------------------------------------------------------------------
// Skeleton
// ---------------------------------------------------------------------------

function Skeleton() {
  return (
    <div className="space-y-3 animate-pulse">
      {[...Array(4)].map((_, i) => (
        <div key={i} className="h-20 rounded-xl bg-[#F4F7F9] dark:bg-white/10 md:h-12" />
      ))}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function FollowupsPage() {
  const orgId    = useAuthStore((s) => s.org?.id ?? '')
  const currency = useAuthStore((s) => s.org?.currency ?? 'INR')

  const [filter,    setFilter]    = useState<FollowupFilter>('ALL')
  const [target,    setTarget]    = useState<InvoiceResponse | null>(null)
  const [view,      setView]      = useViewPreference('followups', 'list')


  const { data: rawInvoices = [], isLoading, isFetching } = useFollowupInvoices(filter)
  const lastFollowupMap = useFollowupsByInvoices(rawInvoices.map((i) => i.id))

  const invoices = useMemo(() => {
    let list = [...rawInvoices]

    if (filter === 'LEAST_CONTACTED') {
      // Only show invoices that have never been contacted
      list = list.filter((inv) => !lastFollowupMap[inv.id])
      // Sort by due date so most urgent appear first
      list.sort((a, b) =>
        new Date(a.dueDate ?? '9999').getTime() - new Date(b.dueDate ?? '9999').getTime()
      )
    } else {
      list.sort((a, b) =>
        new Date(a.dueDate ?? '9999').getTime() - new Date(b.dueDate ?? '9999').getTime()
      )
    }

    return list
  }, [rawInvoices, lastFollowupMap, filter])

  const customersQuery = useQuery({
    queryKey: ['customers', orgId, 'all'],
    queryFn:  () => listCustomers(orgId, { size: 500 }),
    enabled:  !!orgId,
  })

  const customerMap: Record<string, CustomerResponse> = {}
  for (const c of customersQuery.data?.content ?? []) {
    customerMap[c.id] = c
  }

  return (
    <>
      <div className="space-y-5">

        {/* Header */}
        <div className="flex items-center justify-between gap-3 flex-wrap">
          <div>
            <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Follow-ups</h1>
            {!isLoading && (
              <p className="text-sm text-c-muted mt-0.5">
                {invoices.length} active invoice{invoices.length !== 1 ? 's' : ''} need attention
              </p>
            )}
          </div>
          <ViewToggle value={view} onChange={setView} />
        </div>

        {/* Filter tabs */}
        <FollowupFilterTabs active={filter} onChange={setFilter} />

        {/* Content */}
        <div className={`transition-opacity duration-200 ${isFetching && !isLoading ? 'opacity-60' : 'opacity-100'}`}>
        {isLoading ? (
          <Skeleton />
        ) : invoices.length === 0 ? (
          <div className="flex flex-col items-center justify-center min-h-[40vh] gap-3 text-center">
            <div className="w-12 h-12 rounded-full bg-[#29B6F6]/10 flex items-center justify-center">
              <Bell size={20} className="text-[#29B6F6]" />
            </div>
            <div>
              <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">All caught up!</p>
              <p className="text-sm text-c-muted mt-0.5">No active invoices need a follow-up right now.</p>
            </div>
          </div>
        ) : (
          <>
            {/* Card grid — shown when view is grid2/grid3, or on mobile in list view */}
            <div className={view !== 'list' ? gridClass(view) : 'grid grid-cols-1 sm:grid-cols-2 gap-3 md:hidden'}>
              {invoices.map((inv) => (
                <FollowupCard
                  key={inv.id}
                  invoice={inv}
                  currency={currency}
                  customerName={inv.customerId ? customerMap[inv.customerId]?.name : undefined}
                  lastFollowup={lastFollowupMap[inv.id]}
                  onSend={() => setTarget(inv)}
                  canSend={!!inv.customerId}
                />
              ))}
            </div>

            {/* Table — shown only in list view on md+ */}
            <div className={view !== 'list' ? 'hidden' : 'hidden md:block bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden'}>
              <table className="w-full">
                <thead>
                  <tr className="border-b border-c-border">
                    <th className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wide text-c-muted">Client</th>
                    <th className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wide text-c-muted">Remaining</th>
                    <th className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wide text-c-muted">Due</th>
                    <th className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wide text-c-muted">Last Contacted</th>
                    <th className="py-3 px-4 text-right text-xs font-semibold uppercase tracking-wide text-c-muted"></th>
                  </tr>
                </thead>
                <tbody>
                  {invoices.map((inv) => (
                    <InvoiceRow
                      key={inv.id}
                      invoice={inv}
                      currency={currency}
                      customerName={inv.customerId ? customerMap[inv.customerId]?.name : undefined}
                      lastFollowup={lastFollowupMap[inv.id]}
                      onSend={() => setTarget(inv)}
                      canSend={!!inv.customerId}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
        </div>
      </div>

      {/* Dispatch modal */}
      {target && (
        <DispatchModal
          invoice={target}
          onClose={() => setTarget(null)}
        />
      )}
    </>
  )
}
