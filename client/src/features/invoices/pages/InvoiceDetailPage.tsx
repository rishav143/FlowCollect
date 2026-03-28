import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { useInvoiceDetail } from '../hooks/useInvoiceDetail'
import { useDeleteInvoice, useIssueInvoice, useDownloadPdf, useCancelInvoice } from '../hooks/useInvoiceMutations'
import { getCustomer } from '@/api/customer.api'
import InvoiceActionBar from '../components/InvoiceActionBar/InvoiceActionBar'
import InvoiceStatusBadge from '../components/InvoiceStatusBadge/InvoiceStatusBadge'
import PaymentsTab from '../components/PaymentsTab/PaymentsTab'
import FollowupsTab from '../components/FollowupsTab/FollowupsTab'
import DispatchModal from '@/features/followups/components/DispatchModal/DispatchModal'
import { formatCurrency, formatDate } from '@/lib/format'

// ---------------------------------------------------------------------------
// Info row
// ---------------------------------------------------------------------------

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between items-start py-2 border-b border-c-border last:border-0">
      <span className="text-sm text-c-muted">{label}</span>
      <span className="text-sm font-medium text-[#0D1B2A] dark:text-white text-right">{value}</span>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Tab bar
// ---------------------------------------------------------------------------

type Tab = 'details' | 'payments' | 'followups'

function TabBar({ active, onChange }: { active: Tab; onChange: (t: Tab) => void }) {
  const tabs: { id: Tab; label: string }[] = [
    { id: 'details',  label: 'Details' },
    { id: 'payments', label: 'Payments' },
    { id: 'followups', label: 'Follow-ups' },
  ]
  return (
    <div className="flex gap-1 border-b border-c-border -mb-px">
      {tabs.map((t) => (
        <button
          key={t.id}
          onClick={() => onChange(t.id)}
          className={[
            'px-4 py-2.5 text-sm font-medium border-b-2 transition-colors',
            active === t.id
              ? 'border-[#2E7A8E] text-[#2E7A8E] dark:text-[#29B6F6] dark:border-[#29B6F6]'
              : 'border-transparent text-c-muted hover:text-[#0D1B2A] dark:hover:text-white',
          ].join(' ')}
        >
          {t.label}
        </button>
      ))}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function InvoiceDetailPage() {
  const { id }   = useParams<{ id: string }>()
  const navigate = useNavigate()
  const orgId    = useAuthStore((s) => s.org?.id ?? '')
  const currency = useAuthStore((s) => s.org?.currency ?? 'INR')

  const [activeTab,     setActiveTab]     = useState<Tab>('details')
  const [showFollowup,  setShowFollowup]  = useState(false)
  const [showDeleteDlg, setShowDeleteDlg] = useState(false)
  const [showCancelDlg, setShowCancelDlg] = useState(false)

  const { data: invoice, isLoading } = useInvoiceDetail(id!)
  const issueMut   = useIssueInvoice(id!)
  const deleteMut  = useDeleteInvoice()
  const cancelMut  = useCancelInvoice(id!)
  const downloadMut = useDownloadPdf()

  const customerQuery = useQuery({
    queryKey: ['customer', orgId, invoice?.customerId],
    queryFn:  () => getCustomer(orgId, invoice!.customerId!),
    enabled:  !!orgId && !!invoice?.customerId,
  })
  const customer = customerQuery.data

  async function handleDelete() {
    await deleteMut.mutateAsync(id!)
    navigate('/invoices')
  }

  async function handleCancel() {
    await cancelMut.mutateAsync()
    setShowCancelDlg(false)
  }

  // ── Loading ────────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="space-y-5 animate-pulse">
        <div className="h-7 w-48 rounded bg-[#F4F7F9] dark:bg-white/10" />
        <div className="h-24 rounded-xl bg-[#F4F7F9] dark:bg-white/10" />
        <div className="h-64 rounded-xl bg-[#F4F7F9] dark:bg-white/10" />
      </div>
    )
  }

  if (!invoice) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[40vh] gap-3">
        <p className="text-sm text-c-muted">Invoice not found.</p>
        <button onClick={() => navigate('/invoices')} className="text-sm text-[#29B6F6] hover:underline">
          ← Back to invoices
        </button>
      </div>
    )
  }

  return (
    <>
      <div className="space-y-5">

        {/* ── Back ──────────────────────────────────────────────────────── */}
        <button
          onClick={() => navigate('/invoices')}
          className="flex items-center gap-1.5 text-sm text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors"
        >
          <ArrowLeft size={15} strokeWidth={2} />
          Invoices
        </button>

        {/* ── Header card ───────────────────────────────────────────────── */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-5 space-y-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h1 className="text-2xl font-bold text-[#0D1B2A] dark:text-white">{invoice.invoiceNumber}</h1>
              <p className="text-sm text-c-muted mt-0.5">
                Created {formatDate(invoice.createdAt)}
              </p>
            </div>
            <InvoiceStatusBadge lifecycle={invoice.lifeCycleStatus} time={invoice.timeStatus} className="text-sm" />
          </div>

          {/* Action bar */}
          <InvoiceActionBar
            lifeCycleStatus={invoice.lifeCycleStatus}
            onIssue={() => issueMut.mutate(undefined)}
            onDelete={() => setShowDeleteDlg(true)}
            onDownloadPdf={() => downloadMut.mutate({ id: id!, invoiceNumber: invoice.invoiceNumber })}
            onFollowup={() => setShowFollowup(true)}
            onCancel={() => setShowCancelDlg(true)}
            isIssuing={issueMut.isPending}
            isDownloading={downloadMut.isPending}
          />
        </div>

        {/* ── Main content ──────────────────────────────────────────────── */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">

          {/* Left — details + tabs */}
          <div className="lg:col-span-2 space-y-5">

            {/* Tab bar */}
            <TabBar active={activeTab} onChange={setActiveTab} />

            {/* Tab content */}
            <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-5">
              {/* Details — always in DOM */}
              <div className={activeTab !== 'details' ? 'hidden' : 'space-y-5'}>
                <div>
                  <InfoRow label="Customer"   value={customer ? `${customer.name}${customer.companyName ? ` — ${customer.companyName}` : ''}` : '—'} />
                  <InfoRow label="Issue Date" value={formatDate(invoice.issueDate)} />
                  <InfoRow label="Due Date"   value={formatDate(invoice.dueDate)} />
                </div>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wide text-c-muted mb-3">Line Items</p>
                  <div className="space-y-2">
                    {invoice.items.length === 0 ? (
                      <p className="text-sm text-c-muted">No line items</p>
                    ) : (
                      invoice.items.map((item) => (
                        <div key={item.id} className="flex justify-between items-start gap-4 py-2 border-b border-c-border last:border-0">
                          <div className="min-w-0">
                            <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">{item.description}</p>
                            <p className="text-xs text-c-muted">{item.quantity} × {formatCurrency(item.unitPrice, currency, { decimals: false })}</p>
                          </div>
                          <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white tabular-nums shrink-0">
                            {formatCurrency(item.amount, currency, { decimals: false })}
                          </span>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </div>

              {/* Payments — always in DOM */}
              <div className={activeTab !== 'payments' ? 'hidden' : ''}>
                <PaymentsTab
                  invoiceId={id!}
                  remainingAmount={invoice.remainingAmount}
                  currency={currency}
                  lifeCycleStatus={invoice.lifeCycleStatus}
                />
              </div>

              {/* Follow-ups — always in DOM */}
              <div className={activeTab !== 'followups' ? 'hidden' : ''}>
                <FollowupsTab />
              </div>
            </div>
          </div>

          {/* Right — payment summary */}
          <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-5 self-start">
            <p className="text-xs font-semibold uppercase tracking-wide text-c-muted mb-4">Payment Summary</p>
            <div className="space-y-1">
              <InfoRow label="Subtotal"  value={formatCurrency(invoice.subtotal, currency, { decimals: false })} />
              {invoice.taxPercentage > 0 && (
                <InfoRow label={`Tax (${invoice.taxPercentage}%)`} value={formatCurrency(invoice.totalAmount - invoice.subtotal, currency, { decimals: false })} />
              )}
              <div className="py-2 border-b border-c-border flex justify-between">
                <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Total</span>
                <span className="text-sm font-bold text-[#0D1B2A] dark:text-white tabular-nums">{formatCurrency(invoice.totalAmount, currency, { decimals: false })}</span>
              </div>
              <InfoRow label="Paid"      value={<span className="text-green-600 dark:text-green-400">{formatCurrency(invoice.totalPaid, currency, { decimals: false })}</span>} />
              <div className="py-2 flex justify-between">
                <span className="text-sm text-c-muted">Remaining</span>
                <span className={`text-sm font-bold tabular-nums ${invoice.remainingAmount > 0 ? 'text-red-500' : 'text-green-600 dark:text-green-400'}`}>
                  {formatCurrency(invoice.remainingAmount, currency, { decimals: false })}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Follow-up modal */}
      {showFollowup && (
        <DispatchModal invoice={invoice} onClose={() => setShowFollowup(false)} />
      )}

      {/* Cancel confirm */}
      {showCancelDlg && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={() => setShowCancelDlg(false)} />
          <div className="relative w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6">
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white mb-2">Cancel Invoice</h2>
            <p className="text-sm text-c-muted mb-1">
              Cancel <span className="font-semibold text-[#0D1B2A] dark:text-white">{invoice.invoiceNumber}</span>?
            </p>
            <p className="text-sm text-c-muted mb-5">
              This will stop all pending follow-ups and close any active payment links. This action cannot be undone.
            </p>
            <div className="flex gap-3 justify-end">
              <button onClick={() => setShowCancelDlg(false)} className="px-4 py-2 rounded-lg text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">
                Go Back
              </button>
              <button
                onClick={handleCancel}
                disabled={cancelMut.isPending}
                className="px-4 py-2 rounded-lg text-sm font-semibold text-white bg-red-500 hover:bg-red-600 disabled:opacity-50 transition-colors"
              >
                {cancelMut.isPending ? 'Cancelling…' : 'Cancel Invoice'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete confirm */}
      {showDeleteDlg && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={() => setShowDeleteDlg(false)} />
          <div className="relative w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6">
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white mb-2">Delete Invoice</h2>
            <p className="text-sm text-c-muted mb-5">
              Delete <span className="font-semibold text-[#0D1B2A] dark:text-white">{invoice.invoiceNumber}</span>? This cannot be undone.
            </p>
            <div className="flex gap-3 justify-end">
              <button onClick={() => setShowDeleteDlg(false)} className="px-4 py-2 rounded-lg text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">
                Cancel
              </button>
              <button
                onClick={handleDelete}
                disabled={deleteMut.isPending}
                className="px-4 py-2 rounded-lg text-sm font-semibold text-white bg-red-500 hover:bg-red-600 disabled:opacity-50 transition-colors"
              >
                {deleteMut.isPending ? 'Deleting…' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
