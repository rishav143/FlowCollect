import { useState, useMemo } from 'react'
import { createPortal } from 'react-dom'
import {
  X, Send, AlertCircle, Mail, MessageSquare,
  MessageCircle, Loader2, Users, CheckSquare, Square,
} from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { useToast } from '@/store/toast.store'
import { useTemplates } from '@/features/templates/hooks/useTemplates'
import { dispatchFollowup } from '@/api/followup.api'
import { formatCurrency } from '@/lib/format'
import type { CustomerResponse } from '@/types/customer.types'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { FollowUpChannel } from '@/types/followup.types'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface ClientGroupData {
  customerId:     string
  customerName:   string
  invoices:       InvoiceResponse[]
  totalRemaining: number
}

interface Props {
  clientGroups: ClientGroupData[]
  customerMap:  Record<string, CustomerResponse>
  currency:     string
  onClose:      () => void
}

// ---------------------------------------------------------------------------
// Channel config
// ---------------------------------------------------------------------------

type ChannelCfg = {
  id:       FollowUpChannel
  label:    string
  icon:     React.ReactNode
  requires: 'email' | 'phone'
}

const CHANNELS: ChannelCfg[] = [
  { id: 'EMAIL',    label: 'Email',    icon: <Mail          size={15} strokeWidth={1.8} />, requires: 'email' },
  { id: 'SMS',      label: 'SMS',      icon: <MessageSquare size={15} strokeWidth={1.8} />, requires: 'phone' },
  { id: 'WHATSAPP', label: 'WhatsApp', icon: <MessageCircle size={15} strokeWidth={1.8} />, requires: 'phone' },
]

function isEligible(customer: CustomerResponse | undefined, ch: ChannelCfg): boolean {
  if (!customer) return false
  return ch.requires === 'email' ? !!customer.email : !!customer.phone
}

// ---------------------------------------------------------------------------
// CustomerRow
// ---------------------------------------------------------------------------

function CustomerRow({
  group,
  currency,
  selected,
  onToggle,
}: {
  group:    ClientGroupData
  currency: string
  selected: boolean
  onToggle: () => void
}) {
  const initials = group.customerName
    .split(' ').slice(0, 2).map((w) => w[0] ?? '').join('').toUpperCase() || '?'

  return (
    <button
      type="button"
      onClick={onToggle}
      className={[
        'w-full flex items-center gap-3 px-4 py-3 text-left transition-colors',
        selected
          ? 'bg-[#29B6F6]/5 dark:bg-[#29B6F6]/8'
          : 'hover:bg-[#F4F7F9]/60 dark:hover:bg-white/[0.03]',
      ].join(' ')}
    >
      {/* Checkbox */}
      <span className={selected ? 'text-[#29B6F6]' : 'text-c-muted/40'}>
        {selected
          ? <CheckSquare size={15} strokeWidth={2} />
          : <Square      size={15} strokeWidth={1.5} />
        }
      </span>

      {/* Avatar */}
      <div className="w-7 h-7 rounded-full bg-[#29B6F6]/10 text-[#29B6F6] flex items-center justify-center text-[10px] font-bold shrink-0">
        {initials}
      </div>

      {/* Name + invoices */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-[#0D1B2A] dark:text-white truncate">
          {group.customerName}
        </p>
        <p className="text-[11px] text-c-muted">
          {group.invoices.length} invoice{group.invoices.length !== 1 ? 's' : ''}
        </p>
      </div>

      {/* Amount */}
      <span className="text-xs font-semibold tabular-nums text-red-500 shrink-0">
        {formatCurrency(group.totalRemaining, currency, { decimals: false })}
      </span>
    </button>
  )
}

// ---------------------------------------------------------------------------
// SendAllModal
// ---------------------------------------------------------------------------

export default function SendAllModal({ clientGroups, customerMap, currency, onClose }: Props) {
  const orgId       = useAuthStore((s) => s.org?.id ?? '')
  const toast       = useToast()
  const queryClient = useQueryClient()

  // Active channel tab
  const [activeChannel, setActiveChannel] = useState<FollowUpChannel>('EMAIL')

  // Per-channel template selection
  const [channelTemplate, setChannelTemplate] = useState<Partial<Record<FollowUpChannel, string>>>({})

  // Send state
  const [sending,   setSending]   = useState(false)
  const [progress,  setProgress]  = useState<{ done: number; total: number } | null>(null)
  const [sendError, setSendError] = useState<string | null>(null)

  const { data: templatesData, isLoading: templatesLoading } = useTemplates({ size: 100, mode: 'MANUAL' })
  const allTemplates = templatesData?.content ?? []

  // Eligible groups per channel (exclude unknown customers)
  const eligibleGroups = useMemo(() => {
    const result: Record<FollowUpChannel, ClientGroupData[]> = {
      EMAIL: [], SMS: [], WHATSAPP: [],
    }
    for (const group of clientGroups) {
      if (group.customerId === '__unknown__') continue
      const customer = customerMap[group.customerId]
      for (const ch of CHANNELS) {
        if (isEligible(customer, ch)) result[ch.id].push(group)
      }
    }
    return result
  }, [clientGroups, customerMap])

  // Selected customers per channel (default: all eligible selected)
  const [selectedIds, setSelectedIds] = useState<Partial<Record<FollowUpChannel, Set<string>>>>({})

  function getSelected(ch: FollowUpChannel): Set<string> {
    if (selectedIds[ch]) return selectedIds[ch]!
    // Default: all eligible
    return new Set(eligibleGroups[ch].map((g) => g.customerId))
  }

  function toggleCustomer(ch: FollowUpChannel, customerId: string) {
    const current = new Set(getSelected(ch))
    if (current.has(customerId)) current.delete(customerId)
    else current.add(customerId)
    setSelectedIds((prev) => ({ ...prev, [ch]: current }))
  }

  function toggleAll(ch: FollowUpChannel) {
    const eligible = eligibleGroups[ch]
    const current  = getSelected(ch)
    const allSelected = eligible.every((g) => current.has(g.customerId))
    if (allSelected) {
      setSelectedIds((prev) => ({ ...prev, [ch]: new Set() }))
    } else {
      setSelectedIds((prev) => ({ ...prev, [ch]: new Set(eligible.map((g) => g.customerId)) }))
    }
  }

  const activeCfg      = CHANNELS.find((c) => c.id === activeChannel)!
  const activeEligible = eligibleGroups[activeChannel]
  const activeSelected = getSelected(activeChannel)
  const selectedGroups = activeEligible.filter((g) => activeSelected.has(g.customerId))

  const activeTemplates = allTemplates.filter((t) => t.channel === activeChannel && t.active)
  const templateId      = channelTemplate[activeChannel] ?? ''

  const totalInvoices = selectedGroups.reduce((s, g) => s + g.invoices.length, 0)

  const canSend =
    !sending &&
    activeEligible.length > 0 &&
    selectedGroups.length > 0 &&
    !!templateId

  async function handleSend() {
    if (!canSend) return
    setSendError(null)
    setSending(true)

    const total = totalInvoices
    setProgress({ done: 0, total })

    let done       = 0
    let firstError: string | null = null

    for (const group of selectedGroups) {
      for (const invoice of group.invoices) {
        try {
          const result = await dispatchFollowup(orgId, invoice.id, {
            channels:   [activeChannel],
            templateId: templateId,
            attachPdf:  false,
          })
          const anyFailed = result.some((f) => f.status === 'FAILED')
          if (anyFailed && !firstError) {
            firstError = `Some messages could not be delivered. Check client contact details.`
          }
        } catch (err: unknown) {
          if (!firstError) {
            firstError =
              (err as { response?: { data?: { message?: string } } })?.response?.data?.message
              ?? 'Something went wrong sending some messages.'
          }
        }
        done += 1
        setProgress({ done, total })
      }
    }

    setSending(false)
    setProgress(null)

    // Invalidate affected queries
    for (const group of selectedGroups) {
      for (const invoice of group.invoices) {
        queryClient.invalidateQueries({ queryKey: ['followups', orgId, invoice.id] })
      }
    }
    queryClient.invalidateQueries({ queryKey: ['followup-invoices', orgId] })
    queryClient.invalidateQueries({ queryKey: ['dashboard-stats', orgId] })

    if (firstError) {
      setSendError(firstError)
    } else {
      toast.success(
        `Sent ${activeCfg.label} reminders to ${selectedGroups.length} client${selectedGroups.length !== 1 ? 's' : ''} · ${totalInvoices} invoice${totalInvoices !== 1 ? 's' : ''}`,
      )
      onClose()
    }
  }

  const allSelected = activeEligible.length > 0 && activeEligible.every((g) => activeSelected.has(g.customerId))

  return createPortal(
    <>
      <div
        className="fixed inset-0 z-[200] bg-black/50 backdrop-blur-sm"
        onClick={!sending ? onClose : undefined}
        aria-hidden="true"
      />

      <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 pointer-events-none">
        <div
          className="pointer-events-auto w-full max-w-lg bg-white dark:bg-[#1B2838] rounded-2xl shadow-2xl flex flex-col max-h-[88vh]"
          onClick={(e) => e.stopPropagation()}
        >

          {/* ── Header ─────────────────────────────────────────── */}
          <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-c-border shrink-0">
            <div>
              <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">
                Send All Reminders
              </h2>
              <p className="text-xs text-c-muted mt-0.5">
                Choose a channel, pick a template, and send to all eligible clients
              </p>
            </div>
            <button
              onClick={!sending ? onClose : undefined}
              disabled={sending}
              className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors disabled:opacity-40"
            >
              <X size={18} />
            </button>
          </div>

          {/* ── Channel tabs ────────────────────────────────────── */}
          <div className="flex px-6 pt-4 gap-2 shrink-0">
            {CHANNELS.map((ch) => {
              const count     = eligibleGroups[ch.id].length
              const isActive  = activeChannel === ch.id
              const isEmpty   = count === 0

              return (
                <button
                  key={ch.id}
                  type="button"
                  onClick={() => !isEmpty && setActiveChannel(ch.id)}
                  disabled={isEmpty}
                  className={[
                    'flex-1 flex flex-col items-center gap-1 py-3 rounded-xl border text-xs font-medium transition-all',
                    isEmpty
                      ? 'border-c-border text-c-muted/40 cursor-not-allowed bg-[#F4F7F9]/40 dark:bg-white/[0.02]'
                      : isActive
                        ? 'border-[#29B6F6] bg-[#29B6F6]/8 dark:bg-[#29B6F6]/10 text-[#29B6F6] shadow-sm'
                        : 'border-c-border text-c-muted hover:border-[#8A9BAE]/60 hover:text-[#0D1B2A] dark:hover:text-white',
                  ].join(' ')}
                >
                  <span className={isEmpty ? 'opacity-30' : ''}>{ch.icon}</span>
                  <span>{ch.label}</span>
                  <span className={[
                    'px-2 py-0.5 rounded-full text-[10px] font-semibold',
                    isEmpty
                      ? 'bg-[#F4F7F9] dark:bg-white/5 text-c-muted/40'
                      : isActive
                        ? 'bg-[#29B6F6]/15 text-[#29B6F6]'
                        : 'bg-[#F4F7F9] dark:bg-white/10 text-c-muted',
                  ].join(' ')}>
                    {isEmpty ? 'No clients' : `${count} client${count !== 1 ? 's' : ''}`}
                  </span>
                </button>
              )
            })}
          </div>

          {/* ── Scrollable body ─────────────────────────────────── */}
          <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">

            {/* No eligible clients */}
            {activeEligible.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 gap-3 text-center">
                <div className="w-11 h-11 rounded-full bg-[#F4F7F9] dark:bg-white/5 flex items-center justify-center">
                  <Users size={20} className="text-c-muted/50" strokeWidth={1.5} />
                </div>
                <div>
                  <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">
                    No eligible clients
                  </p>
                  <p className="text-xs text-c-muted mt-0.5">
                    {activeCfg.requires === 'email'
                      ? 'No clients have an email address on file.'
                      : 'No clients have a phone number on file.'}
                  </p>
                </div>
              </div>
            ) : (
              <>
                {/* Template selector */}
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-c-muted mb-2">
                    Template
                  </p>
                  {templatesLoading ? (
                    <div className="h-9 rounded-lg bg-[#F4F7F9] dark:bg-white/10 animate-pulse" />
                  ) : activeTemplates.length === 0 ? (
                    <div className="flex items-start gap-2 rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/40 px-3.5 py-3">
                      <AlertCircle size={13} className="text-amber-500 mt-0.5 shrink-0" />
                      <p className="text-xs text-amber-700 dark:text-amber-400">
                        No active {activeCfg.label} template found.{' '}
                        <a href="/templates" className="underline font-medium">Create one.</a>
                      </p>
                    </div>
                  ) : (
                    <select
                      value={templateId}
                      onChange={(e) =>
                        setChannelTemplate((prev) => ({ ...prev, [activeChannel]: e.target.value }))
                      }
                      className="w-full rounded-xl border border-c-border bg-white dark:bg-[#243447] text-sm text-[#0D1B2A] dark:text-white px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-[#29B6F6]/30 transition-shadow"
                    >
                      <option value="">— Select a template —</option>
                      {activeTemplates.map((t) => (
                        <option key={t.id} value={t.id}>{t.name}</option>
                      ))}
                    </select>
                  )}
                </div>

                {/* Client list */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <p className="text-xs font-semibold uppercase tracking-wider text-c-muted">
                      Eligible clients
                    </p>
                    <button
                      type="button"
                      onClick={() => toggleAll(activeChannel)}
                      className="text-xs font-medium text-[#29B6F6] hover:opacity-80 transition-opacity"
                    >
                      {allSelected ? 'Deselect all' : 'Select all'}
                    </button>
                  </div>

                  <div className="rounded-xl border border-c-border overflow-hidden divide-y divide-c-border">
                    {activeEligible.map((group) => (
                      <CustomerRow
                        key={group.customerId}
                        group={group}
                        currency={currency}
                        selected={activeSelected.has(group.customerId)}
                        onToggle={() => toggleCustomer(activeChannel, group.customerId)}
                      />
                    ))}
                  </div>

                  {activeSelected.size === 0 && (
                    <p className="text-xs text-red-500 mt-1.5">Select at least one client.</p>
                  )}
                </div>
              </>
            )}

            {/* Progress */}
            {progress && (
              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-xs text-c-muted">
                  <span className="flex items-center gap-1.5">
                    <Loader2 size={12} className="animate-spin" />
                    Sending reminders…
                  </span>
                  <span>{progress.done} / {progress.total} invoices</span>
                </div>
                <div className="w-full h-1.5 bg-[#F4F7F9] dark:bg-white/10 rounded-full overflow-hidden">
                  <div
                    className="h-full rounded-full transition-all duration-300"
                    style={{
                      width:      `${(progress.done / progress.total) * 100}%`,
                      background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)',
                    }}
                  />
                </div>
              </div>
            )}

            {/* Error */}
            {sendError && (
              <div className="flex items-start gap-2 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700/40 px-3.5 py-3">
                <AlertCircle size={14} className="text-red-500 mt-0.5 shrink-0" />
                <p className="text-xs text-red-600 dark:text-red-400">{sendError}</p>
              </div>
            )}
          </div>

          {/* ── Footer ──────────────────────────────────────────── */}
          <div className="px-6 pb-5 pt-3 border-t border-c-border shrink-0 flex items-center gap-3">
            <button
              onClick={!sending ? onClose : undefined}
              disabled={sending}
              className="flex-1 py-2.5 rounded-xl text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors disabled:opacity-40"
            >
              Cancel
            </button>
            <button
              onClick={handleSend}
              disabled={!canSend}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl text-sm font-semibold text-white disabled:opacity-50 hover:opacity-90 transition-opacity"
              style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
            >
              {sending ? (
                <><Loader2 size={14} className="animate-spin" /> Sending…</>
              ) : (
                <>
                  <Send size={14} strokeWidth={2.5} />
                  {selectedGroups.length > 0 && totalInvoices > 0
                    ? `Send to ${selectedGroups.length} client${selectedGroups.length !== 1 ? 's' : ''} · ${totalInvoices} invoice${totalInvoices !== 1 ? 's' : ''}`
                    : 'Send'}
                </>
              )}
            </button>
          </div>

        </div>
      </div>
    </>,
    document.body,
  )
}
