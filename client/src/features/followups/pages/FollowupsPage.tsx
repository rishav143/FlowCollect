import { useState } from 'react'
import { Send, X, Bell, AlertCircle } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listCustomers } from '@/api/customer.api'
import { useFollowupInvoices, useFollowupsByInvoices, useDispatchFollowup, type FollowupFilter } from '../hooks/useFollowups'
import FollowupFilterTabs from '../components/FollowupFilterTabs/FollowupFilterTabs'
import FollowupCard from '../components/FollowupCard/FollowupCard'
import ViewToggle, { useViewPreference, gridClass } from '@/ui/components/ViewToggle'
import { useTemplates } from '@/features/templates/hooks/useTemplates'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { FollowUpChannel } from '@/types/followup.types'
import type { CustomerResponse } from '@/types/customer.types'
import type { TemplateResponse } from '@/types/template.types'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function fmt(n: number, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
}

// ---------------------------------------------------------------------------
// Dispatch Modal
// ---------------------------------------------------------------------------

const CHANNELS: { id: FollowUpChannel; label: string }[] = [
  { id: 'EMAIL',    label: 'Email'    },
  { id: 'SMS',      label: 'SMS'      },
  { id: 'WHATSAPP', label: 'WhatsApp' },
]

// Per-channel template selector shown when that channel is active
function ChannelTemplateSelect({
  label,
  templates,
  value,
  loading,
  onChange,
}: {
  label:     string
  templates: TemplateResponse[]
  value:     string
  loading:   boolean
  onChange:  (id: string) => void
}) {
  if (loading) {
    return (
      <div className="space-y-1">
        <p className="text-xs text-c-muted">{label} template</p>
        <div className="h-9 rounded-lg bg-[#F4F7F9] dark:bg-white/10 animate-pulse" />
      </div>
    )
  }

  const active = templates.filter((t) => t.active)

  if (active.length === 0) {
    return (
      <div className="flex items-start gap-2 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/40 px-3 py-2">
        <AlertCircle size={14} className="text-amber-500 mt-0.5 shrink-0" />
        <p className="text-xs text-amber-700 dark:text-amber-400">
          No {label} template found.{' '}
          <a href="/templates" className="underline font-medium">Create one in Templates.</a>
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-1">
      <p className="text-xs text-c-muted">{label} template</p>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-lg border border-c-border bg-white dark:bg-[#243447] text-sm text-[#0D1B2A] dark:text-white px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#29B6F6]/40"
      >
        <option value="">— Select template —</option>
        {active.map((t) => (
          <option key={t.id} value={t.id}>{t.name}</option>
        ))}
      </select>
    </div>
  )
}

function DispatchModal({
  invoice,
  onClose,
}: {
  invoice: InvoiceResponse
  onClose: () => void
}) {
  const [channels,         setChannels]         = useState<FollowUpChannel[]>(['EMAIL'])
  const [attachPdf,        setAttachPdf]        = useState(false)
  const [channelTemplates, setChannelTemplates] = useState<Partial<Record<FollowUpChannel, string>>>({})
  const [sendError,        setSendError]        = useState<string | null>(null)

  const dispatch = useDispatchFollowup(invoice.id)

  // Load all active templates once — filter by channel in the selector.
  // Use isFetching (not just isLoading) so skeleton shows during background
  // refetches triggered by useDefaultTemplates seeding.
  const { data: templatesData, isLoading, isFetching } = useTemplates({ size: 100 })
  const templatesLoading = isLoading || isFetching
  const allTemplates = templatesData?.content ?? []

  function templatesFor(ch: FollowUpChannel) {
    return allTemplates.filter((t) => t.channel === ch)
  }

  function toggleChannel(ch: FollowUpChannel) {
    setChannels((prev) =>
      prev.includes(ch) ? prev.filter((c) => c !== ch) : [...prev, ch],
    )
  }

  function setTemplate(ch: FollowUpChannel, id: string) {
    setChannelTemplates((prev) => ({ ...prev, [ch]: id }))
  }

  // Channels with no active templates (can't be sent even if selected)
  const channelsWithNoTemplates = channels.filter(
    (ch) => templatesFor(ch).filter((t) => t.active).length === 0,
  )
  // Channels that have templates available but none selected yet
  const channelsWithoutSelection = channels.filter(
    (ch) => templatesFor(ch).filter((t) => t.active).length > 0 && !channelTemplates[ch],
  )

  const canSend =
    channels.length > 0 &&
    channelsWithNoTemplates.length === 0 &&
    channelsWithoutSelection.length === 0

  async function handleSend() {
    if (!canSend) {
      if (channelsWithoutSelection.length > 0) {
        setSendError(
          `Please select a template for: ${channelsWithoutSelection.map((c) => CHANNELS.find((ch) => ch.id === c)?.label).join(', ')}`,
        )
      }
      return
    }
    setSendError(null)
    try {
      // Send separately per channel so each gets its own channel-specific template
      for (const ch of channels) {
        await dispatch.mutateAsync({
          channels: [ch],
          templateId: channelTemplates[ch],
          attachPdf,
        })
      }
      onClose()
    } catch {
      setSendError('Something went wrong. Please try again.')
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6 space-y-5 max-h-[90vh] overflow-y-auto">

        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Send Follow-up</h2>
            <p className="text-xs text-c-muted mt-0.5">{invoice.invoiceNumber}</p>
          </div>
          <button onClick={onClose} className="text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors">
            <X size={18} />
          </button>
        </div>

        {/* Channel toggles */}
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-c-muted mb-2">Send via</p>
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
                      : 'border-c-border text-c-muted hover:border-[#8A9BAE]/40',
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

        {/* Per-channel template selectors */}
        {channels.length > 0 && (
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-c-muted">Templates</p>
            {channels.map((ch) => {
              const meta = CHANNELS.find((c) => c.id === ch)!
              return (
                <ChannelTemplateSelect
                  key={ch}
                  label={meta.label}
                  templates={templatesFor(ch)}
                  value={channelTemplates[ch] ?? ''}
                  loading={templatesLoading}
                  onChange={(id) => setTemplate(ch, id)}
                />
              )
            })}
          </div>
        )}

        {/* Attach PDF toggle */}
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
            <span className={[
              'absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform',
              attachPdf ? 'translate-x-4' : 'translate-x-0',
            ].join(' ')} />
          </button>
        </label>

        {/* Inline error */}
        {sendError && (
          <div className="flex items-start gap-2 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700/40 px-3 py-2">
            <AlertCircle size={14} className="text-red-500 mt-0.5 shrink-0" />
            <p className="text-xs text-red-600 dark:text-red-400">{sendError}</p>
          </div>
        )}

        {/* Actions */}
        <div className="flex gap-3 pt-1">
          <button
            onClick={onClose}
            className="flex-1 py-2 rounded-lg text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
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

function timeAgo(dateStr: string | null): string {
  if (!dateStr) return '—'
  const diffDays = Math.floor((Date.now() - new Date(dateStr).getTime()) / 86_400_000)
  if (diffDays === 0) return 'today'
  if (diffDays === 1) return '1 day ago'
  return `${diffDays} days ago`
}

const CHANNEL_LABEL: Record<string, string> = {
  EMAIL: 'Email', SMS: 'SMS', WHATSAPP: 'WhatsApp',
}

function InvoiceRow({
  invoice,
  currency,
  customerName,
  lastFollowup,
  onSend,
}: {
  invoice:       InvoiceResponse
  currency:      string
  customerName?: string
  lastFollowup?: import('@/types/followup.types').FollowUpResponse
  onSend:        () => void
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
        {fmt(invoice.remainingAmount, currency)}
      </td>
      {/* Due */}
      <td className="py-3 px-4 text-sm">
        <span className={
          invoice.timeStatus === 'OVERDUE'   ? 'text-red-500 font-semibold' :
          invoice.timeStatus === 'DUE_TODAY' ? 'text-amber-600 dark:text-amber-400 font-semibold' :
          'text-c-muted'
        }>
          {invoice.timeStatus === 'DUE_TODAY' ? 'Today' : timeAgo(invoice.dueDate)}
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
  const [view,    setView]    = useViewPreference('followups', 'list')

  const { data: invoices = [], isLoading, isFetching } = useFollowupInvoices(filter)
  const lastFollowupMap = useFollowupsByInvoices(invoices.map((i) => i.id))

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
