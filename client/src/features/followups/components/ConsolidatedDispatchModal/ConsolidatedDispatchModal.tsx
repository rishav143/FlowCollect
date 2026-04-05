import { useState } from 'react'
import { createPortal } from 'react-dom'
import {
  Send, X, AlertCircle, Mail, MessageSquare,
  MessageCircle, CheckCircle2, Loader2,
} from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { useToast } from '@/store/toast.store'
import { useTemplates } from '@/features/templates/hooks/useTemplates'
import { dispatchFollowup } from '@/api/followup.api'
import { formatCurrency } from '@/lib/format'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'
import type { TemplateResponse } from '@/types/template.types'
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
  group:    ClientGroupData
  customer: CustomerResponse | undefined
  currency: string
  onClose:  () => void
}

// ---------------------------------------------------------------------------
// Channel config
// ---------------------------------------------------------------------------

const CHANNEL_CONFIG: {
  id:       FollowUpChannel
  label:    string
  icon:     React.ReactNode
  requires: 'email' | 'phone'
}[] = [
  {
    id:       'EMAIL',
    label:    'Email',
    icon:     <Mail size={14} strokeWidth={1.8} />,
    requires: 'email',
  },
  {
    id:       'SMS',
    label:    'SMS',
    icon:     <MessageSquare size={14} strokeWidth={1.8} />,
    requires: 'phone',
  },
  {
    id:       'WHATSAPP',
    label:    'WhatsApp',
    icon:     <MessageCircle size={14} strokeWidth={1.8} />,
    requires: 'phone',
  },
]

// ---------------------------------------------------------------------------
// ChannelPill
// ---------------------------------------------------------------------------

function ChannelPill({
  id, label, icon, disabled, disabledReason, selected, onToggle,
}: {
  id:             FollowUpChannel
  label:          string
  icon:           React.ReactNode
  disabled:       boolean
  disabledReason: string
  selected:       boolean
  onToggle:       () => void
}) {
  return (
    <div className="relative group">
      <button
        type="button"
        onClick={onToggle}
        disabled={disabled}
        className={[
          'flex items-center gap-2 px-3.5 py-2.5 rounded-xl border text-sm font-medium transition-all',
          disabled
            ? 'border-c-border text-c-muted/40 cursor-not-allowed bg-[#F4F7F9]/60 dark:bg-white/[0.02]'
            : selected
              ? 'border-[#29B6F6] bg-[#29B6F6]/8 text-[#29B6F6] dark:bg-[#29B6F6]/10 shadow-sm'
              : 'border-c-border text-c-muted hover:border-[#8A9BAE]/60 hover:text-[#0D1B2A] dark:hover:text-white',
        ].join(' ')}
      >
        <span className={disabled ? 'opacity-40' : ''}>{icon}</span>
        <span>{label}</span>
        {disabled && (
          <span className="w-3.5 h-3.5 rounded-full border border-c-border/60 flex items-center justify-center text-[8px] text-c-muted/60">
            ✕
          </span>
        )}
        {selected && !disabled && (
          <CheckCircle2 size={13} className="text-[#29B6F6]" strokeWidth={2.5} />
        )}
      </button>
      {/* Tooltip */}
      {disabled && (
        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-2.5 py-1.5 rounded-lg bg-[#0D1B2A] dark:bg-[#243447] text-white text-[11px] font-medium whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-10 shadow-lg">
          {disabledReason}
          <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-[#0D1B2A] dark:border-t-[#243447]" />
        </div>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// TemplateSelect — per channel
// ---------------------------------------------------------------------------

function TemplateSelect({
  channel, label, icon, templates, value, loading, onChange,
}: {
  channel:   FollowUpChannel
  label:     string
  icon:      React.ReactNode
  templates: TemplateResponse[]
  value:     string
  loading:   boolean
  onChange:  (id: string) => void
}) {
  const active = templates.filter((t) => t.active)

  return (
    <div className="flex items-start gap-3">
      <div className="w-7 h-7 rounded-full bg-[#F4F7F9] dark:bg-white/10 flex items-center justify-center text-c-muted shrink-0 mt-0.5">
        {icon}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-medium text-c-muted mb-1.5">{label} template</p>
        {loading ? (
          <div className="h-9 rounded-lg bg-[#F4F7F9] dark:bg-white/10 animate-pulse" />
        ) : active.length === 0 ? (
          <div className="flex items-start gap-2 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/40 px-3 py-2">
            <AlertCircle size={13} className="text-amber-500 mt-0.5 shrink-0" />
            <p className="text-xs text-amber-700 dark:text-amber-400">
              No {label} template.{' '}
              <a href="/templates" className="underline font-medium">Create one.</a>
            </p>
          </div>
        ) : (
          <select
            value={value}
            onChange={(e) => onChange(e.target.value)}
            className="w-full rounded-lg border border-c-border bg-white dark:bg-[#243447] text-sm text-[#0D1B2A] dark:text-white px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#29B6F6]/30 transition-shadow"
          >
            <option value="">— Select template —</option>
            {active.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
        )}
      </div>
    </div>
  )
  void channel // used for key only
}

// ---------------------------------------------------------------------------
// ConsolidatedDispatchModal
// ---------------------------------------------------------------------------

export default function ConsolidatedDispatchModal({ group, customer, currency, onClose }: Props) {
  const orgId       = useAuthStore((s) => s.org?.id ?? '')
  const toast       = useToast()
  const queryClient = useQueryClient()

  const hasEmail = !!customer?.email
  const hasPhone = !!customer?.phone

  // Default to first available channel
  const defaultChannels: FollowUpChannel[] = hasEmail ? ['EMAIL'] : hasPhone ? ['SMS'] : []
  const [selectedChannels,  setSelectedChannels]  = useState<FollowUpChannel[]>(defaultChannels)
  const [channelTemplates,  setChannelTemplates]  = useState<Partial<Record<FollowUpChannel, string>>>({})
  const [attachPdf,         setAttachPdf]         = useState(false)
  const [sending,           setSending]           = useState(false)
  const [progress,          setProgress]          = useState<{ done: number; total: number } | null>(null)
  const [sendError,         setSendError]         = useState<string | null>(null)

  const { data: templatesData, isLoading: templatesLoading } = useTemplates({ size: 100, mode: 'MANUAL' })
  const allTemplates = templatesData?.content ?? []

  function templatesFor(ch: FollowUpChannel) {
    return allTemplates.filter((t) => t.channel === ch)
  }

  function toggleChannel(ch: FollowUpChannel) {
    setSelectedChannels((prev) =>
      prev.includes(ch) ? prev.filter((c) => c !== ch) : [...prev, ch],
    )
  }

  function isChannelDisabled(ch: FollowUpChannel): boolean {
    const cfg = CHANNEL_CONFIG.find((c) => c.id === ch)!
    return cfg.requires === 'email' ? !hasEmail : !hasPhone
  }

  function disabledReason(ch: FollowUpChannel): string {
    const cfg = CHANNEL_CONFIG.find((c) => c.id === ch)!
    return cfg.requires === 'email' ? 'No email on file' : 'No phone on file'
  }

  // Channels that are selected but have no template chosen
  const channelsWithoutTemplate = selectedChannels.filter(
    (ch) => {
      const active = templatesFor(ch).filter((t) => t.active)
      return active.length > 0 && !channelTemplates[ch]
    },
  )
  const channelsWithNoTemplates = selectedChannels.filter(
    (ch) => templatesFor(ch).filter((t) => t.active).length === 0,
  )
  const canSend =
    !sending &&
    selectedChannels.length > 0 &&
    channelsWithNoTemplates.length === 0 &&
    channelsWithoutTemplate.length === 0

  async function handleSend() {
    if (!canSend) {
      if (channelsWithoutTemplate.length > 0) {
        setSendError(
          `Please select a template for: ${channelsWithoutTemplate
            .map((ch) => CHANNEL_CONFIG.find((c) => c.id === ch)?.label)
            .join(', ')}`,
        )
      }
      return
    }
    setSendError(null)
    setSending(true)

    const total = group.invoices.length * selectedChannels.length
    setProgress({ done: 0, total })

    let done = 0
    let firstError: string | null = null

    for (const invoice of group.invoices) {
      for (const ch of selectedChannels) {
        try {
          const result = await dispatchFollowup(orgId, invoice.id, {
            channels:   [ch],
            templateId: channelTemplates[ch],
            attachPdf,
          })
          const anyFailed = result.some((f) => f.status === 'FAILED')
          if (anyFailed && !firstError) {
            firstError = `Some messages via ${CHANNEL_CONFIG.find((c) => c.id === ch)?.label} could not be delivered. Check client contact details.`
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

    // Invalidate all invoices in the group
    for (const invoice of group.invoices) {
      queryClient.invalidateQueries({ queryKey: ['followups', orgId, invoice.id] })
    }
    queryClient.invalidateQueries({ queryKey: ['followup-invoices', orgId] })
    queryClient.invalidateQueries({ queryKey: ['dashboard-stats', orgId] })

    if (firstError) {
      setSendError(firstError)
    } else {
      const labels = selectedChannels
        .map((ch) => CHANNEL_CONFIG.find((c) => c.id === ch)?.label)
        .join(', ')
      toast.success(`Consolidated follow-up sent via ${labels} for ${group.invoices.length} invoice${group.invoices.length !== 1 ? 's' : ''}`)
      onClose()
    }
  }

  return createPortal(
    <>
      <div
        className="fixed inset-0 z-[200] bg-black/50 backdrop-blur-sm"
        onClick={!sending ? onClose : undefined}
        aria-hidden="true"
      />

      <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 pointer-events-none">
        <div
          className="pointer-events-auto w-full max-w-md bg-white dark:bg-[#1B2838] rounded-2xl shadow-2xl flex flex-col max-h-[90vh]"
          onClick={(e) => e.stopPropagation()}
        >

          {/* ── Header ─────────────────────────────────────────── */}
          <div className="flex items-start justify-between px-6 pt-5 pb-4 border-b border-c-border shrink-0">
            <div>
              <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">
                Send Consolidated Follow-up
              </h2>
              <p className="text-xs text-c-muted mt-0.5">
                {group.customerName}
                <span className="mx-1.5 text-c-border">·</span>
                {group.invoices.length} invoice{group.invoices.length !== 1 ? 's' : ''}
                <span className="mx-1.5 text-c-border">·</span>
                <span className="text-red-500 font-semibold">
                  {formatCurrency(group.totalRemaining, currency, { decimals: false })}
                </span>
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

          {/* ── Scrollable body ─────────────────────────────────── */}
          <div className="px-6 py-5 space-y-5 overflow-y-auto flex-1">

            {/* Contact status */}
            <div className="flex items-center gap-2 flex-wrap">
              <span className={[
                'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border',
                hasEmail
                  ? 'bg-green-50 dark:bg-green-500/10 text-green-700 dark:text-green-400 border-green-200 dark:border-green-500/20'
                  : 'bg-[#F4F7F9] dark:bg-white/5 text-c-muted border-c-border',
              ].join(' ')}>
                <Mail size={11} strokeWidth={2} />
                {hasEmail ? customer!.email : 'No email'}
              </span>
              <span className={[
                'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border',
                hasPhone
                  ? 'bg-green-50 dark:bg-green-500/10 text-green-700 dark:text-green-400 border-green-200 dark:border-green-500/20'
                  : 'bg-[#F4F7F9] dark:bg-white/5 text-c-muted border-c-border',
              ].join(' ')}>
                <MessageSquare size={11} strokeWidth={2} />
                {hasPhone ? customer!.phone : 'No phone'}
              </span>
            </div>

            {/* No contact at all */}
            {!hasEmail && !hasPhone && (
              <div className="flex items-start gap-2 rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/40 px-3.5 py-3">
                <AlertCircle size={14} className="text-amber-500 mt-0.5 shrink-0" />
                <p className="text-xs text-amber-700 dark:text-amber-400">
                  This client has no email or phone on file. Add contact details before sending.
                </p>
              </div>
            )}

            {/* Channel selection */}
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-c-muted mb-2.5">
                Send via
              </p>
              <div className="flex flex-wrap gap-2">
                {CHANNEL_CONFIG.map((ch) => (
                  <ChannelPill
                    key={ch.id}
                    id={ch.id}
                    label={ch.label}
                    icon={ch.icon}
                    disabled={isChannelDisabled(ch.id)}
                    disabledReason={disabledReason(ch.id)}
                    selected={selectedChannels.includes(ch.id)}
                    onToggle={() => toggleChannel(ch.id)}
                  />
                ))}
              </div>
              {selectedChannels.length === 0 && (hasEmail || hasPhone) && (
                <p className="text-xs text-red-500 mt-1.5">Select at least one channel.</p>
              )}
            </div>

            {/* Template selectors */}
            {selectedChannels.length > 0 && (
              <div className="space-y-4">
                <p className="text-xs font-semibold uppercase tracking-wider text-c-muted">
                  Templates
                </p>
                {selectedChannels.map((ch) => {
                  const cfg = CHANNEL_CONFIG.find((c) => c.id === ch)!
                  return (
                    <TemplateSelect
                      key={ch}
                      channel={ch}
                      label={cfg.label}
                      icon={cfg.icon}
                      templates={templatesFor(ch)}
                      value={channelTemplates[ch] ?? ''}
                      loading={templatesLoading}
                      onChange={(id) => setChannelTemplates((prev) => ({ ...prev, [ch]: id }))}
                    />
                  )
                })}
              </div>
            )}

            {/* Attach PDF */}
            <div className="flex items-center justify-between">
              <span className="text-sm text-[#0D1B2A] dark:text-white">Attach invoice PDF</span>
              <button
                role="switch"
                aria-checked={attachPdf}
                onClick={() => setAttachPdf((v) => !v)}
                className={[
                  'relative w-9 h-5 rounded-full transition-colors shrink-0',
                  attachPdf ? 'bg-[#2E7A8E]' : 'bg-[#E2E8F0] dark:bg-white/20',
                ].join(' ')}
              >
                <span className={[
                  'absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform',
                  attachPdf ? 'translate-x-4' : 'translate-x-0',
                ].join(' ')} />
              </button>
            </div>

            {/* Invoice list summary */}
            <div className="rounded-xl border border-c-border overflow-hidden">
              <div className="px-4 py-2.5 bg-[#F4F7F9]/60 dark:bg-white/[0.02] border-b border-c-border">
                <p className="text-xs font-semibold text-c-muted uppercase tracking-wider">
                  Invoices included
                </p>
              </div>
              <div className="divide-y divide-c-border max-h-36 overflow-y-auto">
                {group.invoices.map((inv) => (
                  <div key={inv.id} className="flex items-center justify-between px-4 py-2 gap-3">
                    <span className="text-xs font-medium text-[#0D1B2A] dark:text-white truncate">
                      {inv.invoiceNumber}
                    </span>
                    <span className={[
                      'text-xs font-semibold tabular-nums shrink-0',
                      inv.timeStatus === 'OVERDUE' ? 'text-red-500' : 'text-c-muted',
                    ].join(' ')}>
                      {formatCurrency(inv.remainingAmount, currency, { decimals: false })}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {/* Send progress */}
            {progress && (
              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-xs text-c-muted">
                  <span className="flex items-center gap-1.5">
                    <Loader2 size={12} className="animate-spin" />
                    Sending…
                  </span>
                  <span>{progress.done}/{progress.total}</span>
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

          {/* ── Footer actions ───────────────────────────────────── */}
          <div className="px-6 pb-5 pt-3 border-t border-c-border shrink-0 flex gap-3">
            <button
              onClick={!sending ? onClose : undefined}
              disabled={sending}
              className="flex-1 py-2.5 rounded-xl text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors disabled:opacity-40"
            >
              Cancel
            </button>
            <button
              onClick={handleSend}
              disabled={!canSend || (!hasEmail && !hasPhone)}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl text-sm font-semibold text-white disabled:opacity-50 hover:opacity-90 transition-opacity"
              style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
            >
              {sending ? (
                <><Loader2 size={14} className="animate-spin" /> Sending…</>
              ) : (
                <><Send size={14} strokeWidth={2.5} /> Send to All</>
              )}
            </button>
          </div>

        </div>
      </div>
    </>,
    document.body,
  )
}
