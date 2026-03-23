import { useState } from 'react'
import { Send, X, Bell } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listCustomers } from '@/api/customer.api'
import { useFollowupInvoices, useDispatchFollowup, type FollowupFilter } from '../hooks/useFollowups'
import FollowupFilterTabs from '../components/FollowupFilterTabs/FollowupFilterTabs'
import FollowupCard from '../components/FollowupCard/FollowupCard'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { FollowUpChannel, PaymentGateway, MultiChannelFollowUpRequest } from '@/types/followup.types'
import type { CustomerResponse } from '@/types/customer.types'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function fmt(n: number, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
}

function fmtDate(d: string | null) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

const TIME_CHIP: Record<string, string> = {
  OVERDUE:   'bg-red-100 text-red-600 dark:bg-red-500/20 dark:text-red-400',
  DUE_TODAY: 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400',
  NOT_DUE:   'bg-[#F4F7F9] text-[#8A9BAE] dark:bg-white/10 dark:text-[#8A9BAE]',
}

const TIME_LABEL: Record<string, string> = {
  OVERDUE:   'Overdue',
  DUE_TODAY: 'Due Today',
  NOT_DUE:   'Upcoming',
}

const LIFECYCLE_CHIP: Record<string, string> = {
  ISSUED:         'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400',
  PARTIALLY_PAID: 'bg-[#29B6F6]/10 text-[#29B6F6]',
}

// ---------------------------------------------------------------------------
// Dispatch Modal
// ---------------------------------------------------------------------------

const CHANNELS: { id: FollowUpChannel; label: string }[] = [
  { id: 'EMAIL',    label: 'Email'    },
  { id: 'SMS',      label: 'SMS'      },
  { id: 'WHATSAPP', label: 'WhatsApp' },
]

const GATEWAYS: { id: PaymentGateway; label: string }[] = [
  { id: 'RAZORPAY', label: 'Razorpay' },
  { id: 'STRIPE',   label: 'Stripe'   },
]

function DispatchModal({
  invoice,
  onClose,
}: {
  invoice:  InvoiceResponse
  onClose:  () => void
}) {
  const [channels,        setChannels]        = useState<FollowUpChannel[]>(['EMAIL'])
  const [attachPdf,       setAttachPdf]       = useState(false)
  const [includeLink,     setIncludeLink]     = useState(false)
  const [gateway,         setGateway]         = useState<PaymentGateway>('RAZORPAY')

  const dispatch = useDispatchFollowup(invoice.id)

  function toggleChannel(ch: FollowUpChannel) {
    setChannels((prev) =>
      prev.includes(ch) ? prev.filter((c) => c !== ch) : [...prev, ch],
    )
  }

  async function handleSend() {
    if (channels.length === 0) return
    const body: MultiChannelFollowUpRequest = {
      channels,
      attachPdf,
      includePaymentLink: includeLink,
      paymentGateway: includeLink ? gateway : undefined,
    }
    await dispatch.mutateAsync(body)
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6 space-y-5">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">
              Send Follow-up
            </h2>
            <p className="text-xs text-[#8A9BAE] mt-0.5">{invoice.invoiceNumber}</p>
          </div>
          <button
            onClick={onClose}
            className="text-[#8A9BAE] hover:text-[#0D1B2A] dark:hover:text-white transition-colors"
          >
            <X size={18} />
          </button>
        </div>

        {/* Channels */}
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-[#8A9BAE] mb-2">
            Send via
          </p>
          <div className="flex flex-wrap gap-2">
            {CHANNELS.map((ch) => {
              const active = channels.includes(ch.id)
              return (
                <button
                  key={ch.id}
                  onClick={() => toggleChannel(ch.id)}
                  className={[
                    'px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors',
                    active
                      ? 'border-[#2E7A8E] bg-[#2E7A8E]/10 text-[#2E7A8E] dark:border-[#29B6F6] dark:bg-[#29B6F6]/10 dark:text-[#29B6F6]'
                      : 'border-[#F4F7F9] dark:border-white/10 text-[#8A9BAE] hover:border-[#8A9BAE]/40',
                  ].join(' ')}
                >
                  {ch.label}
                </button>
              )
            })}
          </div>
          {channels.length === 0 && (
            <p className="text-xs text-red-500 mt-1.5">Select at least one channel</p>
          )}
        </div>

        {/* Options */}
        <div className="space-y-3">
          <label className="flex items-center justify-between gap-3 cursor-pointer">
            <span className="text-sm text-[#0D1B2A] dark:text-white">Attach invoice PDF</span>
            <button
              role="switch"
              aria-checked={attachPdf}
              onClick={() => setAttachPdf(!attachPdf)}
              className={[
                'relative w-9 h-5 rounded-full transition-colors',
                attachPdf ? 'bg-[#2E7A8E]' : 'bg-[#E2E8F0] dark:bg-white/20',
              ].join(' ')}
            >
              <span
                className={[
                  'absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform',
                  attachPdf ? 'translate-x-4' : 'translate-x-0',
                ].join(' ')}
              />
            </button>
          </label>

          <label className="flex items-center justify-between gap-3 cursor-pointer">
            <span className="text-sm text-[#0D1B2A] dark:text-white">Include payment link</span>
            <button
              role="switch"
              aria-checked={includeLink}
              onClick={() => setIncludeLink(!includeLink)}
              className={[
                'relative w-9 h-5 rounded-full transition-colors',
                includeLink ? 'bg-[#2E7A8E]' : 'bg-[#E2E8F0] dark:bg-white/20',
              ].join(' ')}
            >
              <span
                className={[
                  'absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform',
                  includeLink ? 'translate-x-4' : 'translate-x-0',
                ].join(' ')}
              />
            </button>
          </label>

          {includeLink && (
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-[#8A9BAE] mb-2">
                Payment gateway
              </p>
              <div className="flex gap-2">
                {GATEWAYS.map((g) => (
                  <button
                    key={g.id}
                    onClick={() => setGateway(g.id)}
                    className={[
                      'flex-1 py-1.5 rounded-lg text-sm font-medium border transition-colors',
                      gateway === g.id
                        ? 'border-[#2E7A8E] bg-[#2E7A8E]/10 text-[#2E7A8E] dark:border-[#29B6F6] dark:bg-[#29B6F6]/10 dark:text-[#29B6F6]'
                        : 'border-[#F4F7F9] dark:border-white/10 text-[#8A9BAE] hover:border-[#8A9BAE]/40',
                    ].join(' ')}
                  >
                    {g.label}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Actions */}
        <div className="flex gap-3 pt-1">
          <button
            onClick={onClose}
            className="flex-1 py-2 rounded-lg text-sm font-medium text-[#8A9BAE] hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSend}
            disabled={channels.length === 0 || dispatch.isPending}
            className="flex-1 flex items-center justify-center gap-1.5 py-2 rounded-lg text-sm font-semibold text-white disabled:opacity-50 transition-opacity hover:opacity-90"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            <Send size={13} strokeWidth={2.5} />
            {dispatch.isPending ? 'Sending…' : 'Send'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Desktop table row
// ---------------------------------------------------------------------------

function InvoiceRow({
  invoice,
  currency,
  customerName,
  onSend,
}: {
  invoice:      InvoiceResponse
  currency:     string
  customerName?: string
  onSend:       () => void
}) {
  return (
    <tr className="border-b border-[#F4F7F9] dark:border-white/10 last:border-0 hover:bg-[#F4F7F9]/50 dark:hover:bg-white/5 transition-colors">
      <td className="py-3 px-4">
        <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">
          {invoice.invoiceNumber}
        </p>
        {customerName && (
          <p className="text-xs text-[#8A9BAE] mt-0.5">{customerName}</p>
        )}
      </td>
      <td className="py-3 px-4">
        <div className="flex items-center gap-1.5">
          <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${LIFECYCLE_CHIP[invoice.lifeCycleStatus] ?? ''}`}>
            {invoice.lifeCycleStatus.replace('_', ' ')}
          </span>
          <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${TIME_CHIP[invoice.timeStatus] ?? ''}`}>
            {TIME_LABEL[invoice.timeStatus] ?? invoice.timeStatus}
          </span>
        </div>
      </td>
      <td className="py-3 px-4 text-sm text-[#0D1B2A] dark:text-white tabular-nums">
        {fmtDate(invoice.dueDate)}
      </td>
      <td className="py-3 px-4 text-right">
        <p className="text-sm font-bold text-red-500 tabular-nums">
          {fmt(invoice.remainingAmount, currency)}
        </p>
        <p className="text-xs text-[#8A9BAE] tabular-nums">
          of {fmt(invoice.totalAmount, currency)}
        </p>
      </td>
      <td className="py-3 px-4 text-right">
        <button
          onClick={onSend}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold text-white transition-opacity hover:opacity-90"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          <Send size={12} strokeWidth={2.5} />
          Send
        </button>
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

  const [filter,  setFilter]  = useState<FollowupFilter>('ALL')
  const [target,  setTarget]  = useState<InvoiceResponse | null>(null)

  const { data: invoices = [], isLoading } = useFollowupInvoices(filter)

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
        <div>
          <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Follow-ups</h1>
          {!isLoading && (
            <p className="text-sm text-[#8A9BAE] mt-0.5">
              {invoices.length} active invoice{invoices.length !== 1 ? 's' : ''} need attention
            </p>
          )}
        </div>

        {/* Filter tabs */}
        <FollowupFilterTabs active={filter} onChange={setFilter} />

        {/* Content */}
        {isLoading ? (
          <Skeleton />
        ) : invoices.length === 0 ? (
          <div className="flex flex-col items-center justify-center min-h-[40vh] gap-3 text-center">
            <div className="w-12 h-12 rounded-full bg-[#29B6F6]/10 flex items-center justify-center">
              <Bell size={20} className="text-[#29B6F6]" />
            </div>
            <div>
              <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">All caught up!</p>
              <p className="text-sm text-[#8A9BAE] mt-0.5">No active invoices need a follow-up right now.</p>
            </div>
          </div>
        ) : (
          <>
            {/* Mobile: card stack */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 md:hidden">
              {invoices.map((inv) => (
                <FollowupCard
                  key={inv.id}
                  invoice={inv}
                  currency={currency}
                  customerName={inv.customerId ? customerMap[inv.customerId]?.name : undefined}
                  onSend={() => setTarget(inv)}
                />
              ))}
            </div>

            {/* Desktop: table */}
            <div className="hidden md:block bg-white dark:bg-[#1B2838] rounded-xl border border-[#F4F7F9] dark:border-white/10 overflow-hidden">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-[#F4F7F9] dark:border-white/10">
                    <th className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wide text-[#8A9BAE]">Invoice</th>
                    <th className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wide text-[#8A9BAE]">Status</th>
                    <th className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wide text-[#8A9BAE]">Due Date</th>
                    <th className="py-3 px-4 text-right text-xs font-semibold uppercase tracking-wide text-[#8A9BAE]">Amount</th>
                    <th className="py-3 px-4 text-right text-xs font-semibold uppercase tracking-wide text-[#8A9BAE]">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {invoices.map((inv) => (
                    <InvoiceRow
                      key={inv.id}
                      invoice={inv}
                      currency={currency}
                      customerName={inv.customerId ? customerMap[inv.customerId]?.name : undefined}
                      onSend={() => setTarget(inv)}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
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
