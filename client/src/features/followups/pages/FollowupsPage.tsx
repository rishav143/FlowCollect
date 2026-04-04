import { useState, useMemo } from 'react'
import { Bell, Send, Zap } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listCustomers } from '@/api/customer.api'
import {
  useFollowupInvoices,
  useAllFollowupsByInvoices,
  type FollowupFilter,
} from '../hooks/useFollowups'
import FollowupFilterTabs from '../components/FollowupFilterTabs/FollowupFilterTabs'
import DispatchModal from '../components/DispatchModal/DispatchModal'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { FollowUpResponse } from '@/types/followup.types'
import type { CustomerResponse } from '@/types/customer.types'
import { formatCurrency } from '@/lib/format'
import { timeAgo, dueDateLabel } from '@/utils/date'

// -------------------------------------------------------------------------
// Types
// -------------------------------------------------------------------------

interface ClientGroupData {
  customerId:     string
  customerName:   string
  invoices:       InvoiceResponse[]
  totalRemaining: number
  hasOverdue:     boolean
}

// -------------------------------------------------------------------------
// ChannelDot
// -------------------------------------------------------------------------

function ChannelDot({ channel }: { channel: string }) {
  const style =
    channel === 'EMAIL'
      ? 'bg-blue-50 text-blue-600 dark:bg-blue-500/15 dark:text-blue-400'
      : 'bg-green-50 text-green-700 dark:bg-green-500/15 dark:text-green-400'

  const label =
    channel === 'EMAIL' ? '@' : channel === 'WHATSAPP' ? 'W' : 'S'

  return (
    <div
      className={`w-5 h-5 rounded-full flex items-center justify-center shrink-0 text-[8px] font-bold ${style}`}
    >
      {label}
    </div>
  )
}

// -------------------------------------------------------------------------
// InvoiceRow — single invoice inside a client group
// -------------------------------------------------------------------------

const SHOWN_PER_CLIENT = 5

function InvoiceRow({
  invoice,
  currency,
  lastFollowup,
  onSend,
  canSend,
}: {
  invoice:       InvoiceResponse
  currency:      string
  lastFollowup?: FollowUpResponse
  onSend:        () => void
  canSend:       boolean
}) {
  const overdue  = invoice.timeStatus === 'OVERDUE'
  const dueToday = invoice.timeStatus === 'DUE_TODAY'
  const opened   = !!lastFollowup?.openedAt

  return (
    <div className="flex items-center gap-2 px-4 py-2.5 border-t border-c-border hover:bg-[#F4F7F9]/50 dark:hover:bg-white/5 transition-colors group">
      {/* Invoice number */}
      <div className="text-xs font-semibold text-[#0D1B2A] dark:text-white min-w-[96px] truncate">
        {invoice.invoiceNumber}
      </div>

      {/* Amount */}
      <div className="text-xs font-bold tabular-nums text-[#0D1B2A] dark:text-white min-w-[60px]">
        {formatCurrency(invoice.remainingAmount, currency, { decimals: false })}
      </div>

      {/* Due */}
      <div
        className={[
          'text-[11px] min-w-[80px]',
          overdue  ? 'text-red-500 font-semibold' :
          dueToday ? 'text-amber-600 dark:text-amber-400 font-semibold' :
          'text-c-muted',
        ].join(' ')}
      >
        {dueDateLabel(invoice.dueDate)}
      </div>

      {/* Last contacted */}
      <div className="flex-1 flex items-center gap-1.5 min-w-0">
        {lastFollowup ? (
          <>
            <ChannelDot channel={lastFollowup.channel} />
            <span className="text-[11px] text-c-muted truncate">
              {lastFollowup.channel === 'EMAIL'
                ? 'Email'
                : lastFollowup.channel === 'SMS'
                ? 'SMS'
                : 'WhatsApp'}{' '}
              {timeAgo(lastFollowup.sentAt ?? lastFollowup.createdAt)}
            </span>
            {opened && (
              <span className="text-[9px] font-semibold px-1.5 py-0.5 rounded-full bg-green-50 text-green-700 dark:bg-green-500/15 dark:text-green-400 shrink-0">
                {lastFollowup?.clickedLinkType === 'PAYMENT_LINK' ? 'Payment link clicked' : lastFollowup?.clickedLinkType === 'CONFIRMATION_LINK' ? 'Confirmation clicked' : 'Clicked'}
              </span>
            )}
          </>
        ) : (
          <span className="text-[11px] text-c-muted italic">Never contacted</span>
        )}
      </div>

      {/* Send button */}
      {canSend ? (
        <button
          onClick={onSend}
          className="text-[11px] text-white px-3 py-1.5 rounded-md font-semibold hover:opacity-90 transition-opacity shrink-0 opacity-0 group-hover:opacity-100"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          Send
        </button>
      ) : (
        <span className="text-[11px] text-c-muted italic shrink-0">No client</span>
      )}
    </div>
  )
}

// -------------------------------------------------------------------------
// ActivityTimeline — vertical event log at the bottom of each client card
// -------------------------------------------------------------------------

function ActivityTimeline({
  invoices,
  allFollowupsMap,
}: {
  invoices:        InvoiceResponse[]
  allFollowupsMap: Record<string, FollowUpResponse[]>
}) {
  const events = useMemo(() => {
    const list: Array<{ f: FollowUpResponse; inv: InvoiceResponse }> = []
    for (const inv of invoices) {
      for (const f of allFollowupsMap[inv.id] ?? []) {
        if (f.status === 'SENT' || f.status === 'FAILED' || f.status === 'CANCELLED') {
          list.push({ f, inv })
        }
      }
    }
    return list
      .sort(
        (a, b) =>
          new Date(b.f.sentAt ?? b.f.updatedAt).getTime() -
          new Date(a.f.sentAt ?? a.f.updatedAt).getTime(),
      )
      .slice(0, 5)
  }, [invoices, allFollowupsMap])

  if (events.length === 0) return null

  return (
    <div className="px-4 pt-3 pb-4 border-t border-c-border">
      <div className="space-y-0">
        {events.map(({ f, inv }, i) => {
          const isLast = i === events.length - 1

          const dotColor =
            f.status === 'SENT'      ? 'bg-[#2E7A8E]' :
            f.status === 'FAILED'    ? 'bg-red-500'   :
            'bg-gray-400'

          const action =
            f.status === 'SENT'
              ? `${f.channel === 'EMAIL' ? 'Email' : f.channel === 'SMS' ? 'SMS' : 'WhatsApp'} sent`
              : f.status === 'FAILED'
              ? 'Reminder failed'
              : 'Reminder skipped'

          const autoSuffix = f.triggerType === 'AUTOMATED' ? ', auto-sent' : ''

          return (
            <div key={f.id} className="flex gap-2.5">
              {/* Dot + connector */}
              <div className="flex flex-col items-center w-3 shrink-0 mt-1">
                <div className={`w-2 h-2 rounded-full shrink-0 ${dotColor}`} />
                {!isLast && <div className="w-px flex-1 bg-c-border mt-1 min-h-[12px]" />}
              </div>

              {/* Content */}
              <div className="flex-1 flex items-start justify-between gap-3 pb-3">
                <p className="text-[11px] text-c-muted leading-snug">
                  <span className="font-medium text-[#0D1B2A] dark:text-white">{action}</span>
                  {autoSuffix}
                  {' — '}
                  {inv.invoiceNumber}
                  {f.openedAt && (
                    <span className="ml-1.5 text-[9px] font-semibold px-1.5 py-0.5 rounded-full bg-green-50 text-green-700 dark:bg-green-500/15 dark:text-green-400">
                      {f.clickedLinkType === 'PAYMENT_LINK' ? 'Payment link clicked' : f.clickedLinkType === 'CONFIRMATION_LINK' ? 'Confirmation clicked' : 'Clicked'}
                    </span>
                  )}
                </p>
                <span className="text-[10px] text-c-muted whitespace-nowrap shrink-0">
                  {timeAgo(f.sentAt ?? f.updatedAt)}
                </span>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

// -------------------------------------------------------------------------
// ClientGroupCard
// -------------------------------------------------------------------------

function ClientGroupCard({
  group,
  currency,
  autoRecoveryEnabled,
  lastFollowupMap,
  allFollowupsMap,
  onSend,
}: {
  group:                ClientGroupData
  currency:             string
  autoRecoveryEnabled:  boolean
  lastFollowupMap:      Record<string, FollowUpResponse>
  allFollowupsMap:      Record<string, FollowUpResponse[]>
  onSend:               (invoice: InvoiceResponse) => void
}) {
  const [showAll, setShowAll] = useState(false)

  const visibleInvoices = showAll ? group.invoices : group.invoices.slice(0, SHOWN_PER_CLIENT)
  const hiddenCount     = group.invoices.length - SHOWN_PER_CLIENT

  // Avatar color by urgency
  const overdueCount  = group.invoices.filter((i) => i.timeStatus === 'OVERDUE').length
  const dueTodayCount = group.invoices.filter((i) => i.timeStatus === 'DUE_TODAY').length
  const avatarStyle   =
    overdueCount  > 0 ? 'bg-red-100 text-red-600 dark:bg-red-500/15 dark:text-red-400' :
    dueTodayCount > 0 ? 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-400' :
    'bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-400'

  const initials = group.customerName
    .split(' ')
    .slice(0, 2)
    .map((w) => w[0] ?? '')
    .join('')
    .toUpperCase() || '?'

  // Most recent contact across all invoices for this client
  const latestContact = group.invoices
    .map((inv) => lastFollowupMap[inv.id])
    .filter(Boolean)
    .sort(
      (a, b) =>
        new Date(b!.sentAt ?? b!.updatedAt).getTime() -
        new Date(a!.sentAt ?? a!.updatedAt).getTime(),
    )[0]

  const metaParts = [
    overdueCount > 0
      ? `${overdueCount} invoice${overdueCount !== 1 ? 's' : ''} overdue`
      : null,
    latestContact
      ? `Last contacted ${timeAgo(latestContact.sentAt ?? latestContact.updatedAt)}`
      : 'Never contacted',
  ].filter(Boolean)

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">

      {/* Client header */}
      <div className="flex items-center gap-3 px-4 py-3.5 bg-[#F4F7F9]/60 dark:bg-white/[0.03] border-b border-c-border">
        {/* Avatar */}
        <div
          className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-semibold shrink-0 ${avatarStyle}`}
        >
          {initials}
        </div>

        {/* Name + meta */}
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white truncate">
            {group.customerName}
          </p>
          <p className="text-[11px] text-c-muted mt-0.5 truncate">
            {metaParts.join(' · ')}
          </p>
        </div>

        {/* Right side: auto pill + total + action */}
        <div className="flex items-center gap-3 shrink-0">
          {autoRecoveryEnabled && (
            <div className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-blue-50 dark:bg-blue-500/10 text-blue-600 dark:text-blue-400 text-[10px] font-medium">
              <Zap size={9} strokeWidth={2} />
              Auto
            </div>
          )}
          <div className="text-right">
            <p className="text-sm font-bold tabular-nums text-red-500">
              {formatCurrency(group.totalRemaining, currency, { decimals: false })}
            </p>
            <p className="text-[10px] text-c-muted">
              {group.invoices.length} invoice{group.invoices.length !== 1 ? 's' : ''}
            </p>
          </div>
          <button
            disabled
            title="Coming soon"
            className="text-xs px-3 py-1.5 rounded-lg font-semibold text-white opacity-40 cursor-not-allowed"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            Send consolidated
          </button>
        </div>
      </div>

      {/* Invoice rows */}
      {visibleInvoices.map((inv) => (
        <InvoiceRow
          key={inv.id}
          invoice={inv}
          currency={currency}
          lastFollowup={lastFollowupMap[inv.id]}
          onSend={() => onSend(inv)}
          canSend={!!inv.customerId}
        />
      ))}

      {/* Show more link */}
      {hiddenCount > 0 && !showAll && (
        <button
          onClick={() => setShowAll(true)}
          className="w-full text-left px-4 py-2.5 text-xs font-medium text-[#2E7A8E] dark:text-[#29B6F6] hover:bg-[#F4F7F9]/40 dark:hover:bg-white/5 border-t border-c-border transition-colors"
        >
          + {hiddenCount} more invoice{hiddenCount !== 1 ? 's' : ''} for this client…
        </button>
      )}

      {/* Activity timeline */}
      <ActivityTimeline invoices={group.invoices} allFollowupsMap={allFollowupsMap} />
    </div>
  )
}

// -------------------------------------------------------------------------
// MetricsStrip — 4 KPI cards
// -------------------------------------------------------------------------

function MetricsStrip({
  invoices,
  clientGroups,
  allFollowupsMap,
  currency,
}: {
  invoices:        InvoiceResponse[]
  clientGroups:    ClientGroupData[]
  allFollowupsMap: Record<string, FollowUpResponse[]>
  currency:        string
}) {
  const weekAgo = useMemo(() => {
    const d = new Date()
    d.setDate(d.getDate() - 7)
    return d
  }, [])

  const allFollowups = useMemo(() => Object.values(allFollowupsMap).flat(), [allFollowupsMap])

  const totalOutstanding = useMemo(
    () => invoices.reduce((s, i) => s + (i.remainingAmount ?? 0), 0),
    [invoices],
  )

  const sentThisWeek = useMemo(
    () =>
      allFollowups.filter(
        (f) => f.status === 'SENT' && f.sentAt && new Date(f.sentAt) >= weekAgo,
      ).length,
    [allFollowups, weekAgo],
  )

  const allSent = useMemo(
    () => allFollowups.filter((f) => f.status === 'SENT'),
    [allFollowups],
  )

  const clickRate =
    allSent.length > 0
      ? Math.round((allSent.filter((f) => f.openedAt != null).length / allSent.length) * 100)
      : null

  const cards = [
    {
      label:  'Total outstanding',
      value:  formatCurrency(totalOutstanding, currency, { decimals: false }),
      sub:    `across ${clientGroups.length} client${clientGroups.length !== 1 ? 's' : ''}`,
      color:  'text-red-500',
    },
    {
      label:  'Reminders sent',
      value:  sentThisWeek.toString(),
      sub:    'this week',
      color:  'text-[#0D1B2A] dark:text-white',
    },
    {
      label:  'Link click rate',
      value:  clickRate != null ? `${clickRate}%` : '—',
      sub:
        allSent.length > 0
          ? `${allSent.filter((f) => f.openedAt != null).length} of ${allSent.length} clicked`
          : 'No reminders sent yet',
      color:
        clickRate != null && clickRate >= 50
          ? 'text-[#2E7A8E] dark:text-[#29B6F6]'
          : 'text-[#0D1B2A] dark:text-white',
    },
    {
      label:  'Avg days to pay',
      value:  '—',
      sub:    'after reminder',
      color:  'text-[#0D1B2A] dark:text-white',
    },
  ]

  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
      {cards.map((c) => (
        <div
          key={c.label}
          className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border px-4 py-3"
        >
          <p className="text-[10px] uppercase tracking-wider text-c-muted font-semibold">
            {c.label}
          </p>
          <p className={`text-xl font-bold mt-1 tabular-nums ${c.color}`}>{c.value}</p>
          <p className="text-[10px] text-c-muted mt-0.5">{c.sub}</p>
        </div>
      ))}
    </div>
  )
}

// -------------------------------------------------------------------------
// Skeleton
// -------------------------------------------------------------------------

function Skeleton() {
  return (
    <div className="space-y-4 animate-pulse">
      {[...Array(2)].map((_, i) => (
        <div key={i} className="rounded-xl border border-c-border overflow-hidden bg-white dark:bg-[#1B2838]">
          <div className="h-14 bg-[#F4F7F9] dark:bg-white/10" />
          <div className="h-10 bg-[#F4F7F9]/60 dark:bg-white/5" />
          {[...Array(3)].map((__, j) => (
            <div key={j} className="h-10 border-t border-c-border" />
          ))}
        </div>
      ))}
    </div>
  )
}

// -------------------------------------------------------------------------
// Page
// -------------------------------------------------------------------------

export default function FollowupsPage() {
  const orgId               = useAuthStore((s) => s.org?.id ?? '')
  const currency            = useAuthStore((s) => s.org?.currency ?? 'INR')
  const autoRecoveryEnabled = useAuthStore((s) => s.org?.autoRecoveryEnabled ?? false)

  const [filter, setFilter] = useState<FollowupFilter>('ALL')
  const [target, setTarget] = useState<InvoiceResponse | null>(null)

  const { data: rawInvoices = [], isLoading, isFetching } = useFollowupInvoices(filter)
  const allFollowupsMap = useAllFollowupsByInvoices(rawInvoices.map((i) => i.id))

  // Derive last-sent map from allFollowupsMap — no second hook needed
  const lastFollowupMap = useMemo<Record<string, FollowUpResponse>>(() => {
    const map: Record<string, FollowUpResponse> = {}
    for (const [invoiceId, list] of Object.entries(allFollowupsMap)) {
      const sent = list.filter((f) => f.status === 'SENT')
      if (sent.length === 0) continue
      map[invoiceId] = sent.reduce((latest, f) =>
        new Date(f.sentAt ?? f.createdAt).getTime() >
        new Date(latest.sentAt ?? latest.createdAt).getTime()
          ? f
          : latest,
      )
    }
    return map
  }, [allFollowupsMap])

  const customersQuery = useQuery({
    queryKey: ['customers', orgId, 'all'],
    queryFn:  () => listCustomers(orgId, { size: 500 }),
    enabled:  !!orgId,
  })

  const customerMap = useMemo(() => {
    const m: Record<string, CustomerResponse> = {}
    for (const c of customersQuery.data?.content ?? []) m[c.id] = c
    return m
  }, [customersQuery.data])

  // Filter + sort invoices
  const invoices = useMemo(() => {
    let list = [...rawInvoices]
    if (filter === 'LEAST_CONTACTED') {
      list = list.filter((inv) => !lastFollowupMap[inv.id])
    }
    list.sort(
      (a, b) =>
        new Date(a.dueDate ?? '9999').getTime() - new Date(b.dueDate ?? '9999').getTime(),
    )
    return list
  }, [rawInvoices, lastFollowupMap, filter])

  // Group by customer, overdue-first
  const clientGroups = useMemo<ClientGroupData[]>(() => {
    const map: Record<string, ClientGroupData> = {}
    for (const inv of invoices) {
      const cid  = inv.customerId ?? '__unknown__'
      const name = inv.customerId
        ? (customerMap[inv.customerId]?.name ?? 'Unknown Client')
        : 'Unknown Client'
      if (!map[cid]) {
        map[cid] = {
          customerId:     cid,
          customerName:   name,
          invoices:       [],
          totalRemaining: 0,
          hasOverdue:     false,
        }
      }
      map[cid].invoices.push(inv)
      map[cid].totalRemaining += inv.remainingAmount ?? 0
      if (inv.timeStatus === 'OVERDUE') map[cid].hasOverdue = true
    }
    return Object.values(map).sort((a, b) => Number(b.hasOverdue) - Number(a.hasOverdue))
  }, [invoices, customerMap])

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
          <button
            disabled
            title="Coming soon — requires backend batch dispatch"
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-semibold text-white opacity-50 cursor-not-allowed"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            <Send size={14} strokeWidth={2.5} />
            Send all reminders
          </button>
        </div>

        {/* Filter tabs */}
        <FollowupFilterTabs active={filter} onChange={setFilter} />

        {/* Metrics strip */}
        {!isLoading && clientGroups.length > 0 && (
          <MetricsStrip
            invoices={invoices}
            clientGroups={clientGroups}
            allFollowupsMap={allFollowupsMap}
            currency={currency}
          />
        )}

        {/* Client groups */}
        <div className={`transition-opacity duration-200 ${isFetching && !isLoading ? 'opacity-60' : 'opacity-100'}`}>
          {isLoading ? (
            <Skeleton />
          ) : clientGroups.length === 0 ? (
            <div className="flex flex-col items-center justify-center min-h-[40vh] gap-3 text-center">
              <div className="w-12 h-12 rounded-full bg-[#29B6F6]/10 flex items-center justify-center">
                <Bell size={20} className="text-[#29B6F6]" />
              </div>
              <div>
                <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">All caught up!</p>
                <p className="text-sm text-c-muted mt-0.5">
                  No active invoices need a follow-up right now.
                </p>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              {clientGroups.map((group) => (
                <ClientGroupCard
                  key={group.customerId}
                  group={group}
                  currency={currency}
                  autoRecoveryEnabled={autoRecoveryEnabled}
                  lastFollowupMap={lastFollowupMap}
                  allFollowupsMap={allFollowupsMap}
                  onSend={setTarget}
                />
              ))}
            </div>
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
