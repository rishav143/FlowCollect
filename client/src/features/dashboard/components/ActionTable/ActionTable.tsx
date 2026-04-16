import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowRight, CheckCircle2, Clock, Bell, Check, X, AlertCircle, RotateCcw } from 'lucide-react'
import type { NeedsAttentionItem } from '@/api/dashboard.api'


import type { AxiosError } from 'axios'

import type { PaymentConfirmationResponse } from '@/types/confirmation.types'
import { formatCurrency } from '@/lib/format'
import {
  useApproveConfirmation,
  useRejectConfirmation,
  useRequestRemainingConfirmation,
} from '@/features/approvals/hooks/useConfirmations'
import { useToast } from '@/store/toast.store'
import NoteModal from '@/ui/components/NoteModal/NoteModal'
import RequestRemainingModal from '@/ui/components/RequestRemainingModal/RequestRemainingModal'

function daysOverdue(dueDate: string): number {
  const due   = new Date(dueDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.max(0, Math.floor((today.getTime() - due.getTime()) / 86_400_000))
}

import { timeAgoFine as timeAgo } from '@/utils/date'

// ---------------------------------------------------------------------------
// Section wrapper
// ---------------------------------------------------------------------------

function Section({
  title,
  icon,
  count,
  iconColor,
  children,
}: {
  title:     string
  icon:      React.ReactNode
  count:     number
  iconColor: string
  children:  React.ReactNode
}) {
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
      <div className="flex items-center gap-2 px-5 py-4 border-b border-c-border">
        <span className={iconColor}>{icon}</span>
        <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white">{title}</h2>
        {count > 0 && (
          <span className="ml-auto text-xs font-medium text-c-muted">{count} item{count !== 1 ? 's' : ''}</span>
        )}
      </div>
      {children}
    </div>
  )
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex items-center justify-center py-10">
      <p className="text-sm text-c-muted">{message}</p>
    </div>
  )
}

function RowSkeleton() {
  return (
    <div className="flex items-center gap-3 px-5 py-3.5 animate-pulse">
      <div className="h-4 w-20 rounded bg-[#F4F7F9] dark:bg-white/10" />
      <div className="flex-1 h-4 w-24 rounded bg-[#F4F7F9] dark:bg-white/10" />
      <div className="h-4 w-16 rounded bg-[#F4F7F9] dark:bg-white/10" />
    </div>
  )
}

// ---------------------------------------------------------------------------
// Overdue invoices table
// ---------------------------------------------------------------------------

function OverdueSection({
  invoices,
  currency,
  isLoading,
  onFollowup,
}: {
  invoices:    NeedsAttentionItem[]
  currency:    string
  isLoading:   boolean
  onFollowup?: (item: NeedsAttentionItem) => void
}) {
  const navigate = useNavigate()

  return (
    <Section
      title="Needs Attention"
      icon={<Clock size={15} strokeWidth={2} />}
      count={invoices.length}
      iconColor="text-red-500"
    >
      {isLoading ? (
        <>{[...Array(3)].map((_, i) => <RowSkeleton key={i} />)}</>
      ) : invoices.length === 0 ? (
        <EmptyState message="No overdue invoices — you're all caught up!" />
      ) : (
        <>
          <ul>
            {invoices.map((inv, idx) => {
              const days      = inv.daysPastDue
              const borderCls = idx < invoices.length - 1 ? 'border-b border-c-border' : ''
              return (
                <li
                  key={inv.invoiceId}
                  className={`cursor-pointer hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors ${borderCls}`}
                  onClick={() => navigate(`/invoices/${inv.invoiceId}`)}
                >
                  {/* ── Mobile layout (< sm) — two rows ── */}
                  <div className="sm:hidden px-5 py-3">
                    <div className="flex items-center gap-3">
                      <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white flex-1">{inv.invoiceNumber}</p>
                      <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums shrink-0">
                        {formatCurrency(inv.remainingAmount, currency, { decimals: false })}
                      </span>
                    </div>
                    <div className="flex items-center justify-between mt-1.5">
                      <div className="flex items-center gap-1.5 min-w-0">
                        {inv.customerName && (
                          <span className="text-xs text-c-muted truncate">{inv.customerName}</span>
                        )}
                        <span className="text-xs font-medium text-red-500 shrink-0">{days}d overdue</span>
                      </div>
                      {onFollowup && (
                        <div onClick={(e) => e.stopPropagation()}>
                          <button
                            onClick={() => onFollowup(inv)}
                            title="Send reminder"
                            className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-[#29B6F6] hover:bg-[#29B6F6]/10 transition-colors"
                          >
                            <Bell size={12} strokeWidth={2.5} />
                          </button>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* ── Desktop/tablet layout (sm+) — original single row ── */}
                  <div className="hidden sm:flex items-center gap-3 px-5 py-3">
                    <div className="min-w-0 flex-1 flex items-center gap-1.5">
                      <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white shrink-0">{inv.invoiceNumber}</p>
                      {inv.customerName && (
                        <span className="text-xs text-c-muted truncate">{inv.customerName}</span>
                      )}
                    </div>
                    <div className="flex items-center gap-2 shrink-0" onClick={(e) => e.stopPropagation()}>
                      <span className="text-xs font-medium text-red-500">{days}d overdue</span>
                      <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums">
                        {formatCurrency(inv.remainingAmount, currency, { decimals: false })}
                      </span>
                      {onFollowup && (
                        <button
                          onClick={() => onFollowup(inv)}
                          title="Send reminder"
                          className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-[#29B6F6] hover:bg-[#29B6F6]/10 transition-colors"
                        >
                          <Bell size={12} strokeWidth={2.5} />
                          <span>Remind</span>
                        </button>
                      )}
                    </div>
                  </div>
                </li>
              )
            })}
          </ul>
        </>
      )}
    </Section>
  )
}

// ---------------------------------------------------------------------------
// Claim type helper
// ---------------------------------------------------------------------------

type ClaimType = 'FULL' | 'PARTIAL' | 'OVER'

function getClaimType(c: PaymentConfirmationResponse): ClaimType {
  if (c.amountClaimed > c.invoiceRemainingAmount) return 'OVER'
  if (c.amountClaimed < c.invoiceRemainingAmount) return 'PARTIAL'
  return 'FULL'
}

// ---------------------------------------------------------------------------
// Pending approvals table (with inline actions)
// ---------------------------------------------------------------------------

type PendingAction =
  | { type: 'reject';            confirmation: PaymentConfirmationResponse }
  | { type: 'request-remaining'; confirmation: PaymentConfirmationResponse }

function ApprovalsSection({
  confirmations,
  currency,
  isLoading,
}: {
  confirmations: PaymentConfirmationResponse[]
  currency:      string
  isLoading:     boolean
}) {
  const navigate = useNavigate()
  const toast    = useToast()

  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null)
  const [actionError,   setActionError]   = useState<string | null>(null)

  const approveMut          = useApproveConfirmation()
  const rejectMut           = useRejectConfirmation()
  const requestRemainingMut = useRequestRemainingConfirmation()

  function isActingOn(id: string) {
    return (
      (approveMut.isPending          && (approveMut.variables          as { id: string })?.id === id) ||
      (rejectMut.isPending           && (rejectMut.variables           as { id: string })?.id === id) ||
      (requestRemainingMut.isPending && (requestRemainingMut.variables as { id: string })?.id === id)
    )
  }

  function handleApprove(c: PaymentConfirmationResponse) {
    setActionError(null)
    approveMut.mutate(
      { id: c.id },
      {
        onSuccess: () => toast.success(`Payment claim for ${c.invoiceNumber} approved`),
        onError: (err) => {
          const msg = (err as AxiosError<{ message?: string }>).response?.data?.message
          setActionError(msg ?? 'Failed to approve — please try again.')
        },
      },
    )
  }

  function handleRejectConfirm(note: string) {
    if (pendingAction?.type !== 'reject') return
    const { confirmation: c } = pendingAction
    setActionError(null)
    rejectMut.mutate(
      { id: c.id, businessNote: note || undefined },
      {
        onSuccess: () => {
          setPendingAction(null)
          toast.success(`Payment claim for ${c.invoiceNumber} rejected`)
        },
        onError: (err) => {
          const msg = (err as AxiosError<{ message?: string }>).response?.data?.message
          setActionError(msg ?? 'Failed to reject — please try again.')
          setPendingAction(null)
        },
      },
    )
  }

  function handleRequestRemainingConfirm(note: string, newDueDate: string | undefined) {
    if (pendingAction?.type !== 'request-remaining') return
    const { confirmation: c } = pendingAction
    setActionError(null)
    requestRemainingMut.mutate(
      { id: c.id, businessNote: note || undefined, newDueDate },
      {
        onSuccess: () => {
          setPendingAction(null)
          toast.success(`Partial payment recorded for ${c.invoiceNumber} — remaining requested`)
        },
        onError: (err) => {
          const msg = (err as AxiosError<{ message?: string }>).response?.data?.message
          setActionError(msg ?? 'Failed to process — please try again.')
          setPendingAction(null)
        },
      },
    )
  }

  return (
    <>
      <Section
        title="Pending Approvals"
        icon={<CheckCircle2 size={15} strokeWidth={2} />}
        count={confirmations.length}
        iconColor="text-amber-500"
      >
        {/* Inline action error */}
        {actionError && (
          <div className="flex items-start gap-2 mx-4 mt-3 px-3 py-2 rounded-lg bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20 text-xs text-red-600 dark:text-red-400">
            <AlertCircle size={13} className="shrink-0 mt-0.5" />
            <span className="flex-1">{actionError}</span>
            <button onClick={() => setActionError(null)} className="shrink-0 hover:opacity-70 transition-opacity">
              <X size={12} />
            </button>
          </div>
        )}

        {isLoading ? (
          <>{[...Array(3)].map((_, i) => <RowSkeleton key={i} />)}</>
        ) : confirmations.length === 0 ? (
          <EmptyState message="No pending payment claims" />
        ) : (
          <ul>
            {confirmations.map((conf, idx) => {
              const claimType = getClaimType(conf)
              const acting    = isActingOn(conf.id)
              const borderCls = idx < confirmations.length - 1 ? 'border-b border-c-border' : ''

              const actionButtons = acting ? (
                <span className="text-xs text-c-muted">Processing…</span>
              ) : claimType === 'PARTIAL' ? (
                <>
                  <button onClick={() => setPendingAction({ type: 'request-remaining', confirmation: conf })} title="Approve partial & request remaining" className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-[#29B6F6] hover:bg-[#29B6F6]/10 transition-colors"><RotateCcw size={11} strokeWidth={2.5} /></button>
                  <button onClick={() => handleApprove(conf)} title="Approve as final" className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-500/10 transition-colors"><Check size={11} strokeWidth={2.5} /></button>
                  <button onClick={() => setPendingAction({ type: 'reject', confirmation: conf })} title="Reject claim" className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"><X size={11} strokeWidth={2.5} /></button>
                </>
              ) : (
                <>
                  <button onClick={() => handleApprove(conf)} title="Approve claim" className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-500/10 transition-colors"><Check size={11} strokeWidth={2.5} /></button>
                  <button onClick={() => setPendingAction({ type: 'reject', confirmation: conf })} title="Reject claim" className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"><X size={11} strokeWidth={2.5} /></button>
                </>
              )

              return (
                <li
                  key={conf.id}
                  className={`group cursor-pointer hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors ${borderCls}`}
                  onClick={() => navigate('/approvals')}
                >
                  {/* ── Mobile layout (< sm) — two rows ── */}
                  <div className="sm:hidden px-5 py-3">
                    <div className="flex items-center gap-3">
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">{conf.invoiceNumber}</p>
                      </div>
                      <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums shrink-0">
                        {formatCurrency(conf.amountClaimed, currency, { decimals: false })}
                      </span>
                      <ArrowRight size={14} className="text-c-muted shrink-0" />
                    </div>
                    <div className="flex items-center justify-between mt-1.5">
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs text-c-muted">{timeAgo(conf.createdAt)}</span>
                        {claimType === 'PARTIAL' && <span className="text-xs font-medium text-amber-500">· Partial</span>}
                        {claimType === 'OVER'    && <span className="text-xs font-medium text-red-500">· Over</span>}
                      </div>
                      <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                        {actionButtons}
                      </div>
                    </div>
                  </div>

                  {/* ── Desktop/tablet layout (sm+) — original single row ── */}
                  <div className="hidden sm:flex items-center gap-3 px-5 py-3">
                    <div className="w-28 shrink-0 min-w-0">
                      <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">{conf.invoiceNumber}</p>
                      {claimType === 'PARTIAL' && <p className="text-xs font-medium text-amber-500">Partial claim</p>}
                      {claimType === 'OVER'    && <p className="text-xs font-medium text-red-500">Over claim</p>}
                    </div>
                    <span className="text-xs text-c-muted shrink-0">{timeAgo(conf.createdAt)}</span>
                    <span className="flex-1" />
                    <div
                      className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity shrink-0"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {acting ? (
                        <span className="text-xs text-c-muted px-2">Processing…</span>
                      ) : claimType === 'PARTIAL' ? (
                        <>
                          <button onClick={() => setPendingAction({ type: 'request-remaining', confirmation: conf })} title="Approve partial & request remaining" className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-[#29B6F6] hover:bg-[#29B6F6]/10 transition-colors"><RotateCcw size={11} strokeWidth={2.5} />Req. Remaining</button>
                          <button onClick={() => handleApprove(conf)} title="Approve as final payment" className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-500/10 transition-colors"><Check size={11} strokeWidth={2.5} />Approve</button>
                          <button onClick={() => setPendingAction({ type: 'reject', confirmation: conf })} title="Reject claim" className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"><X size={11} strokeWidth={2.5} />Reject</button>
                        </>
                      ) : (
                        <>
                          <button onClick={() => handleApprove(conf)} title="Approve claim" className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-green-600 dark:text-green-400 hover:bg-green-50 dark:hover:bg-green-500/10 transition-colors"><Check size={11} strokeWidth={2.5} />Approve</button>
                          <button onClick={() => setPendingAction({ type: 'reject', confirmation: conf })} title="Reject claim" className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"><X size={11} strokeWidth={2.5} />Reject</button>
                        </>
                      )}
                    </div>
                    <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums">
                      {formatCurrency(conf.amountClaimed, currency, { decimals: false })}
                    </span>
                    <ArrowRight size={14} className="text-c-muted shrink-0 group-hover:opacity-0 transition-opacity" />
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </Section>

      {/* Reject modal */}
      {pendingAction?.type === 'reject' && (
        <NoteModal
          title="Reject Claim"
          subtitle={`Invoice ${pendingAction.confirmation.invoiceNumber} — the customer will be notified and can resubmit.`}
          confirmLabel="Reject"
          confirmStyle="danger"
          isLoading={rejectMut.isPending}
          onConfirm={handleRejectConfirm}
          onCancel={() => setPendingAction(null)}
        />
      )}

      {/* Request Remaining modal */}
      {pendingAction?.type === 'request-remaining' && (
        <RequestRemainingModal
          invoiceNumber={pendingAction.confirmation.invoiceNumber}
          isLoading={requestRemainingMut.isPending}
          onConfirm={handleRequestRemainingConfirm}
          onCancel={() => setPendingAction(null)}
        />
      )}
    </>
  )
}

// ---------------------------------------------------------------------------
// Exported component
// ---------------------------------------------------------------------------

// ApprovalsSection is exported for use in PendingApprovalsPanel
export { ApprovalsSection }

interface Props {
  invoices:    NeedsAttentionItem[]
  currency:    string
  isLoading:   boolean
  onFollowup?: (item: NeedsAttentionItem) => void
}

export default function ActionTable({ invoices, currency, isLoading, onFollowup }: Props) {
  return (
    <OverdueSection
      invoices={invoices}
      currency={currency}
      isLoading={isLoading}
      onFollowup={onFollowup}
    />
  )
}
