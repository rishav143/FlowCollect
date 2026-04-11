import { useState } from 'react'
import { createPortal } from 'react-dom'
import {
  Send, X, AlertCircle, Mail, Loader2,
} from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { useToast } from '@/store/toast.store'
import { consolidatedDispatch } from '@/api/followup.api'
import { formatCurrency } from '@/lib/format'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'

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
// ConsolidatedDispatchModal
// ---------------------------------------------------------------------------

export default function ConsolidatedDispatchModal({ group, customer, currency, onClose }: Props) {
  const orgId       = useAuthStore((s) => s.org?.id ?? '')
  const toast       = useToast()
  const queryClient = useQueryClient()

  const hasEmail = !!customer?.email
  const [sending,   setSending]   = useState(false)
  const [sendError, setSendError] = useState<string | null>(null)

  const canSend = !sending && hasEmail

  async function handleSend() {
    if (!canSend) return
    setSendError(null)
    setSending(true)

    const invoiceIds = group.invoices.map((inv) => inv.id)

    try {
      const results = await consolidatedDispatch(orgId, { invoiceIds, channels: ['EMAIL'] })
      const failed  = results.find((r) => r.status === 'FAILED')
      if (failed) {
        setSendError(failed.errorMessage ?? 'Could not deliver via Email.')
        setSending(false)
        return
      }
    } catch (err: unknown) {
      setSendError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Something went wrong. Please try again.',
      )
      setSending(false)
      return
    }

    setSending(false)

    for (const invoice of group.invoices) {
      queryClient.invalidateQueries({ queryKey: ['followups', orgId, invoice.id] })
    }
    queryClient.invalidateQueries({ queryKey: ['followup-invoices', orgId] })
    queryClient.invalidateQueries({ queryKey: ['dashboard-stats', orgId] })

    toast.success(
      `Consolidated follow-up sent via Email · ${group.invoices.length} invoice${group.invoices.length !== 1 ? 's' : ''}`,
    )
    onClose()
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

            {/* Email badge */}
            <div className="flex items-center gap-2">
              <span className={[
                'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border',
                hasEmail
                  ? 'bg-green-50 dark:bg-green-500/10 text-green-700 dark:text-green-400 border-green-200 dark:border-green-500/20'
                  : 'bg-[#F4F7F9] dark:bg-white/5 text-c-muted border-c-border',
              ].join(' ')}>
                <Mail size={11} strokeWidth={2} />
                {hasEmail ? customer!.email : 'No email'}
              </span>
            </div>

            {/* No email warning */}
            {!hasEmail && (
              <div className="flex items-start gap-2 rounded-xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/40 px-3.5 py-3">
                <AlertCircle size={14} className="text-amber-500 mt-0.5 shrink-0" />
                <p className="text-xs text-amber-700 dark:text-amber-400">
                  This client has no email on file. Add an email address before sending.
                </p>
              </div>
            )}

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
              disabled={!canSend}
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
