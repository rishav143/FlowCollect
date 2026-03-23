import { useState } from 'react'
import { CheckSquare, X } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import {
  useConfirmations,
  usePendingConfirmationCount,
  useApproveConfirmation,
  useRejectConfirmation,
  useRequestRemainingConfirmation,
  type ApprovalFilter,
} from '../hooks/useConfirmations'
import ConfirmationFilterTabs from '../components/ConfirmationFilterTabs/ConfirmationFilterTabs'
import ConfirmationCard from '../components/ConfirmationCard/ConfirmationCard'
import type { PaymentConfirmationResponse } from '@/types/confirmation.types'

// ---------------------------------------------------------------------------
// Note modal — shared by Reject and Request Remaining actions
// ---------------------------------------------------------------------------

function NoteModal({
  title,
  subtitle,
  confirmLabel,
  confirmStyle,
  onConfirm,
  onCancel,
  isLoading,
}: {
  title:        string
  subtitle:     string
  confirmLabel: string
  confirmStyle: 'danger' | 'primary'
  onConfirm:    (note: string) => void
  onCancel:     () => void
  isLoading:    boolean
}) {
  const [note, setNote] = useState('')

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onCancel} />
      <div className="relative w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6 space-y-4">
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0 pr-3">
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">{title}</h2>
            <p className="text-xs text-[#8A9BAE] mt-0.5 leading-relaxed">{subtitle}</p>
          </div>
          <button onClick={onCancel} className="text-[#8A9BAE] hover:text-[#0D1B2A] dark:hover:text-white transition-colors shrink-0">
            <X size={18} />
          </button>
        </div>

        <div>
          <label className="text-xs font-semibold uppercase tracking-wide text-[#8A9BAE]">
            Note <span className="normal-case font-normal text-[#8A9BAE]">(optional)</span>
          </label>
          <textarea
            value={note}
            onChange={(e) => setNote(e.target.value)}
            rows={3}
            placeholder="Add a note for the customer..."
            className="mt-1.5 w-full text-sm rounded-lg border border-[#F4F7F9] dark:border-white/10 bg-transparent text-[#0D1B2A] dark:text-white placeholder:text-[#8A9BAE] p-2.5 focus:outline-none focus:border-[#8A9BAE]/40 resize-none"
          />
        </div>

        <div className="flex gap-3">
          <button
            onClick={onCancel}
            className="flex-1 py-2 rounded-lg text-sm font-medium text-[#8A9BAE] hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={() => onConfirm(note)}
            disabled={isLoading}
            className={[
              'flex-1 py-2 rounded-lg text-sm font-semibold text-white disabled:opacity-50 transition-opacity hover:opacity-90',
              confirmStyle === 'danger' ? 'bg-red-500' : '',
            ].join(' ')}
            style={confirmStyle === 'primary' ? { background: 'linear-gradient(90deg, #2E7A8E 0%, #29B6F6 100%)' } : undefined}
          >
            {isLoading ? 'Processing…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Skeleton
// ---------------------------------------------------------------------------

function Skeleton() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4 animate-pulse">
      {[...Array(3)].map((_, i) => (
        <div key={i} className="h-56 rounded-xl bg-[#F4F7F9] dark:bg-white/10" />
      ))}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

type PendingAction =
  | { type: 'reject';            confirmation: PaymentConfirmationResponse }
  | { type: 'request-remaining'; confirmation: PaymentConfirmationResponse }

export default function ApprovalsPage() {
  const currency = useAuthStore((s) => s.org?.currency ?? 'INR')

  const [filter,        setFilter]        = useState<ApprovalFilter>('PENDING_APPROVAL')
  const [pendingAction, setPendingAction] = useState<PendingAction | null>(null)

  const { data, isLoading }    = useConfirmations(filter)
  const { data: pendingCount } = usePendingConfirmationCount()

  const approveMut          = useApproveConfirmation()
  const rejectMut           = useRejectConfirmation()
  const requestRemainingMut = useRequestRemainingConfirmation()

  const confirmations = data?.content ?? []

  // Which card is currently being acted on
  function isActingOn(id: string) {
    return (
      (approveMut.isPending          && (approveMut.variables          as { id: string })?.id === id) ||
      (rejectMut.isPending           && (rejectMut.variables           as { id: string })?.id === id) ||
      (requestRemainingMut.isPending && (requestRemainingMut.variables as { id: string })?.id === id)
    )
  }

  function handleApprove(c: PaymentConfirmationResponse) {
    approveMut.mutate({ id: c.id })
  }

  function handleRejectConfirm(note: string) {
    if (pendingAction?.type !== 'reject') return
    rejectMut.mutate(
      { id: pendingAction.confirmation.id, businessNote: note || undefined },
      { onSuccess: () => setPendingAction(null) },
    )
  }

  function handleRequestRemainingConfirm(note: string) {
    if (pendingAction?.type !== 'request-remaining') return
    requestRemainingMut.mutate(
      { id: pendingAction.confirmation.id, businessNote: note || undefined },
      { onSuccess: () => setPendingAction(null) },
    )
  }

  return (
    <>
      <div className="space-y-5">

        {/* Header */}
        <div>
          <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Approvals</h1>
          {!isLoading && (
            <p className="text-sm text-[#8A9BAE] mt-0.5">
              {pendingCount ?? 0} payment claim{(pendingCount ?? 0) !== 1 ? 's' : ''} awaiting review
            </p>
          )}
        </div>

        {/* Filter tabs */}
        <ConfirmationFilterTabs
          active={filter}
          onChange={setFilter}
          pendingCount={pendingCount}
        />

        {/* Content */}
        {isLoading ? (
          <Skeleton />
        ) : confirmations.length === 0 ? (
          <div className="flex flex-col items-center justify-center min-h-[40vh] gap-3 text-center">
            <div className="w-12 h-12 rounded-full bg-green-50 dark:bg-green-500/10 flex items-center justify-center">
              <CheckSquare size={20} className="text-green-500" />
            </div>
            <div>
              <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">
                {filter === 'PENDING_APPROVAL' ? 'All caught up!' : 'Nothing here'}
              </p>
              <p className="text-sm text-[#8A9BAE] mt-0.5">
                {filter === 'PENDING_APPROVAL'
                  ? 'No payment claims are waiting for your review.'
                  : 'No confirmations match this filter.'}
              </p>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
            {confirmations.map((c) => (
              <ConfirmationCard
                key={c.id}
                confirmation={c}
                currency={currency}
                isActing={isActingOn(c.id)}
                onApprove={() => handleApprove(c)}
                onRequestRemaining={() => setPendingAction({ type: 'request-remaining', confirmation: c })}
                onReject={() => setPendingAction({ type: 'reject', confirmation: c })}
              />
            ))}
          </div>
        )}
      </div>

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
        <NoteModal
          title="Approve & Request Remaining"
          subtitle={`Invoice ${pendingAction.confirmation.invoiceNumber} — partial payment will be recorded and the customer will receive a request for the balance.`}
          confirmLabel="Approve & Request"
          confirmStyle="primary"
          isLoading={requestRemainingMut.isPending}
          onConfirm={handleRequestRemainingConfirm}
          onCancel={() => setPendingAction(null)}
        />
      )}
    </>
  )
}
