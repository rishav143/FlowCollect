import { useState } from 'react'
import { CheckSquare, X, AlertCircle } from 'lucide-react'
import type { AxiosError } from 'axios'
import { useAuthStore } from '@/store/auth.store'
import ViewToggle, { useViewPreference, gridClass } from '@/ui/components/ViewToggle'
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
import DateRangePicker, { type DateRangeValue } from '@/ui/components/DateRangePicker/DateRangePicker'
import NoteModal from '@/ui/components/NoteModal/NoteModal'
import type { PaymentConfirmationResponse } from '@/types/confirmation.types'

// ---------------------------------------------------------------------------
// Skeleton
// ---------------------------------------------------------------------------

function Skeleton({ view }: { view: string }) {
  return (
    <div className={`${view === 'list' ? 'space-y-3' : `grid gap-4 ${view === 'grid3' ? 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3' : 'grid-cols-1 sm:grid-cols-2'}`} animate-pulse`}>
      {[...Array(view === 'list' ? 3 : 6)].map((_, i) => (
        <div key={i} className="h-28 rounded-xl bg-[#F4F7F9] dark:bg-white/10" />
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
  const [view,          setView]          = useViewPreference('approvals', 'list')
  const [actionError,   setActionError]   = useState<string | null>(null)
  const [dateRange,     setDateRange]     = useState<DateRangeValue>({})

  const { data, isLoading, isFetching } = useConfirmations(filter)
  const { data: pendingCount } = usePendingConfirmationCount()

  const approveMut          = useApproveConfirmation()
  const rejectMut           = useRejectConfirmation()
  const requestRemainingMut = useRequestRemainingConfirmation()

  // Client-side date filter — API already returns up to 100 records sorted desc
  const confirmations = (data?.content ?? []).filter((c) => {
    const ts = new Date(c.createdAt).getTime()
    if (dateRange.from && ts < new Date(dateRange.from + 'T00:00:00').getTime()) return false
    if (dateRange.to   && ts > new Date(dateRange.to   + 'T23:59:59').getTime()) return false
    return true
  })

  // Which card is currently being acted on
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
        onError: (err) => {
          const msg = (err as AxiosError<{ message?: string }>).response?.data?.message
          setActionError(msg ?? 'Failed to approve — please try again.')
        },
      },
    )
  }

  function handleRejectConfirm(note: string) {
    if (pendingAction?.type !== 'reject') return
    setActionError(null)
    rejectMut.mutate(
      { id: pendingAction.confirmation.id, businessNote: note || undefined },
      {
        onSuccess: () => setPendingAction(null),
        onError: (err) => {
          const msg = (err as AxiosError<{ message?: string }>).response?.data?.message
          setActionError(msg ?? 'Failed to reject — please try again.')
          setPendingAction(null)
        },
      },
    )
  }

  function handleRequestRemainingConfirm(note: string) {
    if (pendingAction?.type !== 'request-remaining') return
    setActionError(null)
    requestRemainingMut.mutate(
      { id: pendingAction.confirmation.id, businessNote: note || undefined },
      {
        onSuccess: () => setPendingAction(null),
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
      <div className="space-y-5">

        {/* Header */}
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Approvals</h1>
            {!isLoading && (
              <p className="text-sm text-c-muted mt-0.5">
                {pendingCount ?? 0} payment claim{(pendingCount ?? 0) !== 1 ? 's' : ''} awaiting review
              </p>
            )}
          </div>
          <ViewToggle value={view} onChange={setView} options={['list', 'grid2', 'grid3']} />
        </div>

        {/* Action error */}
        {actionError && (
          <div className="flex items-start gap-2 px-3 py-2.5 rounded-lg bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20 text-sm text-red-600 dark:text-red-400">
            <AlertCircle size={15} className="shrink-0 mt-0.5" />
            <span className="flex-1">{actionError}</span>
            <button onClick={() => setActionError(null)} className="shrink-0 hover:opacity-70 transition-opacity"><X size={14} /></button>
          </div>
        )}

        {/* Filter tabs + date range — stacked on mobile, inline on sm+ */}
        <div className="flex flex-col sm:flex-row sm:items-center gap-2">
          <div className="flex-1 min-w-0">
            <ConfirmationFilterTabs
              active={filter}
              onChange={setFilter}
              pendingCount={pendingCount}
            />
          </div>
          <div className="shrink-0">
            <DateRangePicker label="Date" value={dateRange} onChange={setDateRange} />
          </div>
        </div>

        {/* Content */}
        <div className={`transition-opacity duration-200 ${isFetching && !isLoading ? 'opacity-60' : 'opacity-100'}`}>
        {isLoading ? (
          <Skeleton view={view} />
        ) : confirmations.length === 0 ? (
          <div className="flex flex-col items-center justify-center min-h-[40vh] gap-3 text-center">
            <div className="w-12 h-12 rounded-full bg-green-50 dark:bg-green-500/10 flex items-center justify-center">
              <CheckSquare size={20} className="text-green-500" />
            </div>
            <div>
              <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">
                {filter === 'PENDING_APPROVAL' ? 'All caught up!' : 'Nothing here'}
              </p>
              <p className="text-sm text-c-muted mt-0.5">
                {filter === 'PENDING_APPROVAL'
                  ? 'No payment claims are waiting for your review.'
                  : 'No confirmations match this filter.'}
              </p>
            </div>
          </div>
        ) : (
          <div className={gridClass(view)}>
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
