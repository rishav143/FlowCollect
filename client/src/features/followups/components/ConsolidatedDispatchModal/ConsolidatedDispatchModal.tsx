import { useState } from 'react'
import { createPortal } from 'react-dom'
import {
  Send, X, AlertCircle, Mail, MessageSquare,
  MessageCircle, CheckCircle2, Loader2,
} from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { useToast } from '@/store/toast.store'
import { consolidatedDispatch } from '@/api/followup.api'
import { formatCurrency } from '@/lib/format'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'
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
  { id: 'EMAIL',    label: 'Email',    icon: <Mail          size={14} strokeWidth={1.8} />, requires: 'email' },
  { id: 'SMS',      label: 'SMS',      icon: <MessageSquare size={14} strokeWidth={1.8} />, requires: 'phone' },
  { id: 'WHATSAPP', label: 'WhatsApp', icon: <MessageCircle size={14} strokeWidth={1.8} />, requires: 'phone' },
]

// ---------------------------------------------------------------------------
// ChannelPill
// ---------------------------------------------------------------------------

function ChannelPill({
  label, icon, disabled, disabledReason, selected, onToggle,
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
// ConsolidatedDispatchModal
// ---------------------------------------------------------------------------

export default function ConsolidatedDispatchModal({ group, customer, currency, onClose }: Props) {
  const orgId       = useAuthStore((s) => s.org?.id ?? '')
  const toast       = useToast()
  const queryClient = useQueryClient()

  const hasEmail = !!customer?.email
  const hasPhone = !!customer?.phone

  const defaultChannels: FollowUpChannel[] = hasEmail ? ['EMAIL'] : hasPhone ? ['SMS'] : []
  const [selectedChannels, setSelectedChannels] = useState<FollowUpChannel[]>(defaultChannels)
  const [sending,          setSending]          = useState(false)
  const [progress,         setProgress]         = useState<{ done: number; total: number } | null>(null)
  const [sendError,        setSendError]        = useState<string | null>(null)

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

  const canSend = !sending && selectedChannels.length > 0

  async function handleSend() {
    if (!canSend) return
    setSendError(null)
    setSending(true)

    const total      = selectedChannels.length
    const invoiceIds = group.invoices.map((inv) => inv.id)
    setProgress({ done: 0, total })

    let done = 0
    let firstError: string | null = null

    for (const ch of selectedChannels) {
      try {
        const results = await consolidatedDispatch(orgId, {
          invoiceIds,
          channels: [ch],
        })
        const failed = results.find((r) => r.status === 'FAILED')
        if (failed && !firstError) {
          firstError = failed.errorMessage
            ?? `Could not deliver via ${CHANNEL_CONFIG.find((c) => c.id === ch)?.label}.`
        }
      } catch (err: unknown) {
        if (!firstError) {
          firstError =
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message
            ?? 'Something went wrong. Please try again.'
        }
      }
      done += 1
      setProgress({ done, total })
    }

    setSending(false)
    setProgress(null)

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
      toast.success(
        `Consolidated follow-up sent via ${labels} · ${group.invoices.length} invoice${group.invoices.length !== 1 ? 's' : ''}`,
      )
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

          {/* ── Body ────────────────────────────────────────────── */}
          <div className="px-6 py-5 space-y-5 overflow-y-auto flex-1">

            {/* Contact badges */}
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

            {/* No contact */}
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

            {/* Invoice summary */}
            <div className="rounded-xl border border-c-border overflow-hidden">
              <div className="px-4 py-2.5 bg-[#F4F7F9]/60 dark:bg-white/[0.02] border-b border-c-border">
                <p className="text-xs font-semibold text-c-muted uppercase tracking-wider">
                  Invoices included
                </p>
              </div>
              <div className="divide-y divide-c-border max-h-40 overflow-y-auto">
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

            {/* Progress */}
            {progress && (
              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-xs text-c-muted">
                  <span className="flex items-center gap-1.5">
                    <Loader2 size={12} className="animate-spin" />
                    Sending…
                  </span>
                  <span>{progress.done} / {progress.total}</span>
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
                <><Send size={14} strokeWidth={2.5} /> Send</>
              )}
            </button>
          </div>

        </div>
      </div>
    </>,
    document.body,
  )
}
