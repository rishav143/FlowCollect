import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useConfirmationView, useSubmitPaymentClaim } from '../hooks/useConfirmPayment'
import type { CustomerConfirmationView, PaymentDetails } from '@/api/publicConfirmation.api'
import { CheckCircle2, AlertCircle, Clock, Copy, Check, CreditCard, Wallet } from 'lucide-react'
import Select from '@/components/ui/Select'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const CURRENCY_LOCALE: Record<string, string> = {
  INR: 'en-IN', USD: 'en-US', EUR: 'de-DE', GBP: 'en-GB',
  AED: 'en-AE', SGD: 'en-SG', AUD: 'en-AU',
}

function fmt(amount: number, currency: string) {
  try {
    return new Intl.NumberFormat(CURRENCY_LOCALE[currency] ?? 'en-US', {
      style:                 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount)
  } catch {
    return `${currency} ${Math.round(amount)}`
  }
}

function daysDiff(dateStr: string): number {
  const due = new Date(dateStr)
  const now = new Date()
  due.setHours(0, 0, 0, 0)
  now.setHours(0, 0, 0, 0)
  return Math.floor((now.getTime() - due.getTime()) / 86_400_000)
}

function fmtDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  })
}

const PAYMENT_METHODS = [
  { value: 'Bank / Wire Transfer', label: 'Bank / Wire Transfer' },
  { value: 'Card',                 label: 'Card' },
  { value: 'UPI',                  label: 'UPI' },
  { value: 'Cash',                 label: 'Cash' },
  { value: 'Cheque',               label: 'Cheque' },
  { value: 'Other',                label: 'Other' },
]

// ---------------------------------------------------------------------------
// Shell — consistent wrapper for all states
// ---------------------------------------------------------------------------

function PageShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-[#F4F7F9] flex flex-col items-center justify-start sm:justify-center px-0 sm:px-4 py-0 sm:py-10">
      {children}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Copy field row
// ---------------------------------------------------------------------------

function CopyRow({ label, value }: { label: string; value: string }) {
  const [copied, setCopied] = useState(false)
  function handleCopy() {
    navigator.clipboard.writeText(value).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 1800)
    })
  }
  return (
    <div className="flex items-center justify-between gap-2 py-1.5">
      <span className="text-xs text-gray-500 shrink-0">{label}</span>
      <div className="flex items-center gap-0.5 min-w-0">
        <span className="text-xs font-semibold text-[#0D2137] tabular-nums truncate">{value}</span>
        <button
          type="button"
          onClick={handleCopy}
          title="Copy"
          className="shrink-0 p-1 rounded text-[#29B6F6] hover:text-[#0288D1] transition-colors"
        >
          {copied
            ? <Check size={12} className="text-green-500" />
            : <Copy size={12} />}
        </button>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Payment details panel — shown on confirmation page alongside the form
// ---------------------------------------------------------------------------

/**
 * Inline section rendered inside the confirmation card — between balance hero and form.
 * Shows the business's payment receiving details with one-tap copy buttons.
 */
function PaymentDetailsSection({ details }: { details: PaymentDetails }) {
  const hasBankWire = details.bankName || details.accountHolderName ||
                      details.accountNumber || details.iban ||
                      details.swiftCode || details.routingNumber || details.ifscCode
  const hasUpi     = !!details.upiId
  const hasWallets = details.paypalEmail || details.wiseEmail

  if (!hasBankWire && !hasUpi && !hasWallets) return null

  return (
    <div className="mx-6 mb-5 rounded-xl border border-[#29B6F6]/20 bg-[#F0FBFF] overflow-hidden">
      {/* Section label */}
      <div className="px-4 py-2.5 border-b border-[#29B6F6]/15">
        <p className="text-xs font-semibold text-[#0288D1]">Send your payment to</p>
      </div>

      <div className="px-4 py-3 space-y-3">
        {hasBankWire && (
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-wide text-gray-400 mb-1.5 flex items-center gap-1">
              <CreditCard size={10} strokeWidth={2} /> Bank / Wire Transfer
            </p>
            <div className="divide-y divide-[#29B6F6]/10">
              {details.bankName          && <CopyRow label="Bank"         value={details.bankName} />}
              {details.accountHolderName && <CopyRow label="Account name" value={details.accountHolderName} />}
              {details.accountNumber     && <CopyRow label="Account no."  value={details.accountNumber} />}
              {details.iban              && <CopyRow label="IBAN"         value={details.iban} />}
              {details.swiftCode         && <CopyRow label="SWIFT / BIC"  value={details.swiftCode} />}
              {details.routingNumber     && <CopyRow label="Routing no."  value={details.routingNumber} />}
              {details.ifscCode          && <CopyRow label="IFSC"         value={details.ifscCode} />}
            </div>
          </div>
        )}

        {hasUpi && (
          <div className={hasBankWire ? 'border-t border-[#29B6F6]/10 pt-2.5' : ''}>
            <p className="text-[10px] font-semibold uppercase tracking-wide text-gray-400 mb-1.5">UPI</p>
            <div className="divide-y divide-[#29B6F6]/10">
              <CopyRow label="UPI ID" value={details.upiId!} />
            </div>
          </div>
        )}

        {hasWallets && (
          <div className={(hasBankWire || hasUpi) ? 'border-t border-[#29B6F6]/10 pt-2.5' : ''}>
            <p className="text-[10px] font-semibold uppercase tracking-wide text-gray-400 mb-1.5 flex items-center gap-1">
              <Wallet size={10} strokeWidth={2} /> Digital Wallets
            </p>
            <div className="divide-y divide-[#29B6F6]/10">
              {details.paypalEmail && <CopyRow label="PayPal" value={details.paypalEmail} />}
              {details.wiseEmail   && <CopyRow label="Wise"   value={details.wiseEmail} />}
            </div>
          </div>
        )}

        {details.additionalNote && (
          <p className="text-xs text-gray-500 pt-2 border-t border-[#29B6F6]/10">
            {details.additionalNote}
          </p>
        )}
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Brand header strip inside the card
// ---------------------------------------------------------------------------

function BrandHeader({ subtitle }: { subtitle: string }) {
  return (
    <div
      className="px-6 py-5 text-white sm:rounded-t-2xl"
      style={{ background: 'linear-gradient(135deg, #29B6F6 0%, #0288D1 100%)' }}
    >
      <p className="text-lg font-bold tracking-tight">
        <span className="font-black">Paid</span>Peace
      </p>
      <p className="text-sm opacity-90 mt-0.5">{subtitle}</p>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Invoice summary rows
// ---------------------------------------------------------------------------

function Row({
  label,
  value,
  valueClass = 'text-gray-900 font-medium',
}: {
  label:       string
  value:       string
  valueClass?: string
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-sm text-gray-500">{label}</span>
      <span className={`text-sm ${valueClass}`}>{value}</span>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Loading skeleton
// ---------------------------------------------------------------------------

function LoadingSkeleton() {
  return (
    <PageShell>
      <div className="w-full sm:max-w-sm bg-white sm:rounded-2xl sm:shadow-sm sm:border sm:border-gray-100 overflow-hidden animate-pulse">
        <div className="h-20 bg-[#29B6F6]/20" />
        <div className="px-6 py-5 space-y-3">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="flex justify-between">
              <div className="h-4 w-20 rounded bg-gray-100" />
              <div className="h-4 w-24 rounded bg-gray-100" />
            </div>
          ))}
          <div className="h-14 w-40 mx-auto rounded-xl bg-gray-100 mt-4" />
          <div className="h-11 w-full rounded-xl bg-gray-100 mt-2" />
          <div className="h-11 w-full rounded-xl bg-gray-100" />
          <div className="h-12 w-full rounded-2xl bg-gray-100" />
        </div>
      </div>
    </PageShell>
  )
}

// ---------------------------------------------------------------------------
// Error page
// ---------------------------------------------------------------------------

function ErrorPage({ message }: { message: string }) {
  return (
    <PageShell>
      <div className="w-full sm:max-w-sm bg-white sm:rounded-2xl sm:shadow-sm sm:border sm:border-gray-100 overflow-hidden">
        <BrandHeader subtitle="Payment Confirmation" />
        <div className="px-6 py-10 text-center space-y-3">
          <AlertCircle size={40} className="text-red-400 mx-auto" />
          <p className="text-base font-semibold text-gray-800">Link unavailable</p>
          <p className="text-sm text-gray-500">{message}</p>
        </div>
      </div>
    </PageShell>
  )
}

// ---------------------------------------------------------------------------
// Already closed / settled
// ---------------------------------------------------------------------------

function AlreadyClosedPage({ view }: { view: CustomerConfirmationView }) {
  return (
    <PageShell>
      <div className="w-full sm:max-w-sm bg-white sm:rounded-2xl sm:shadow-sm sm:border sm:border-gray-100 overflow-hidden">
        <BrandHeader subtitle={`Invoice ${view.invoiceNumber} · ${view.organizationName}`} />
        <div className="px-6 py-10 text-center space-y-3">
          <CheckCircle2 size={44} className="text-[#29B6F6] mx-auto" />
          <p className="text-base font-semibold text-gray-800">Invoice fully settled</p>
          <p className="text-sm text-gray-500">
            This invoice has been paid and confirmed. No further action is needed.
          </p>
        </div>
        <div className="px-6 pb-6 text-center">
          <p className="text-xs text-gray-400">
            Sent by <strong className="text-gray-500">{view.organizationName}</strong> via FlowCollect
          </p>
        </div>
      </div>
    </PageShell>
  )
}

// ---------------------------------------------------------------------------
// Success page
// ---------------------------------------------------------------------------

function SuccessPage({ view, amountClaimed }: { view: CustomerConfirmationView; amountClaimed: number }) {
  return (
    <PageShell>
      <div className="w-full sm:max-w-sm bg-white sm:rounded-2xl sm:shadow-sm sm:border sm:border-gray-100 overflow-hidden">
        <BrandHeader subtitle={`Invoice ${view.invoiceNumber} · ${view.organizationName}`} />
        <div className="px-6 py-10 text-center space-y-3">
          <CheckCircle2 size={44} className="text-[#29B6F6] mx-auto" />
          <p className="text-base font-semibold text-gray-800">Claim submitted</p>
          <p className="text-sm text-gray-500">
            Your payment of <strong className="text-gray-700">{fmt(amountClaimed, view.currency)}</strong> has
            been submitted. <strong className="text-gray-700">{view.organizationName}</strong> will review and
            confirm it shortly.
          </p>
        </div>
        <div className="mx-6 mb-6 flex items-center gap-2 px-4 py-3 rounded-xl bg-[#F4F7F9] border border-gray-200">
          <Clock size={14} className="text-gray-400 shrink-0" />
          <p className="text-xs text-gray-500">You'll receive confirmation once the payment is verified.</p>
        </div>
        <div className="px-6 pb-6 text-center">
          <p className="text-xs text-gray-400">
            Sent by <strong className="text-gray-500">{view.organizationName}</strong> via FlowCollect
          </p>
        </div>
      </div>
    </PageShell>
  )
}

// ---------------------------------------------------------------------------
// Main payment form
// ---------------------------------------------------------------------------

function PaymentForm({ view, token }: { view: CustomerConfirmationView; token: string }) {
  const { mutate, isPending, isError, error } = useSubmitPaymentClaim(token)

  const [paymentMethod, setPaymentMethod] = useState<string>('Bank / Wire Transfer')
  const [reference,     setReference]     = useState('')
  const [amount,        setAmount]        = useState(Math.round(view.remainingAmount))
  const [submitted,     setSubmitted]     = useState(false)
  const [amountClaimed, setAmountClaimed] = useState(0)

  const overdue = daysDiff(view.dueDate)

  function buildNote(): string {
    const parts: string[] = [`Payment method: ${paymentMethod}`]
    if (reference.trim()) parts.push(`Reference: ${reference.trim()}`)
    return parts.join('. ') + '.'
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (amount <= 0) return
    const claimed = Math.round(amount)
    mutate(
      { amountClaimed: claimed, customerNote: buildNote() },
      {
        onSuccess: () => {
          setAmountClaimed(claimed)
          setSubmitted(true)
        },
      },
    )
  }

  if (submitted) {
    return <SuccessPage view={view} amountClaimed={amountClaimed} />
  }

  const errorMsg = isError
    ? ((error as any)?.response?.data?.message ?? 'Something went wrong. Please try again.')
    : null

  const currencySymbol = view.currency === 'INR' ? '₹'
    : view.currency === 'USD' ? '$'
    : view.currency === 'EUR' ? '€'
    : view.currency === 'GBP' ? '£'
    : view.currency

  return (
    <PageShell>
      <div className="w-full sm:max-w-sm">
        <div className="w-full bg-white sm:rounded-2xl sm:shadow-sm sm:border sm:border-gray-100 overflow-hidden">

          {/* Brand header */}
          <BrandHeader subtitle={`Invoice ${view.invoiceNumber} · ${view.organizationName}`} />

          {/* Invoice summary */}
          <div className="px-6 pt-5 pb-4 space-y-2.5">
            <Row label="Client"        value={view.customerName} />
            <Row label="Due date"      value={fmtDate(view.dueDate)} />
            <Row label="Invoice total" value={fmt(view.totalAmount, view.currency)} />
            {view.totalPaid > 0 && (
              <Row
                label="Already paid"
                value={fmt(view.totalPaid, view.currency)}
                valueClass="text-[#29B6F6] font-semibold"
              />
            )}
          </div>

          {/* Remaining balance hero */}
          <div className="mx-6 mb-5 rounded-xl bg-[#F4F7F9] border border-gray-200 px-5 py-4 text-center">
            <p className="text-3xl font-bold text-gray-900 tabular-nums">
              {fmt(view.remainingAmount, view.currency)}
            </p>
            <p className="text-xs text-gray-500 mt-1">
              Remaining balance
              {overdue > 0 && (
                <span className="text-red-500 font-medium"> · {overdue} day{overdue !== 1 ? 's' : ''} overdue</span>
              )}
            </p>
          </div>

          {/* Payment details — embedded inside the card, between hero and form */}
          {view.paymentDetails && (
            <PaymentDetailsSection details={view.paymentDetails} />
          )}

          {/* Divider */}
          <div className="border-t border-gray-100 mx-6" />

          {/* Form */}
          <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1.5">
                Payment method
              </label>
              <Select
                value={paymentMethod}
                onChange={setPaymentMethod}
                options={PAYMENT_METHODS}
                variant="light"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1.5">
                Reference number{' '}
                <span className="text-gray-400 font-normal normal-case">(optional)</span>
              </label>
              <input
                type="text"
                value={reference}
                onChange={(e) => setReference(e.target.value)}
                placeholder={paymentMethod === 'UPI' ? 'e.g. UPI ref 423891756234' : 'e.g. TXN123456'}
                className="w-full bg-white border border-gray-200 rounded-xl px-3.5 py-2.5 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:border-[#29B6F6] transition-colors"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1.5">
                Amount paid
              </label>
              <div className="relative">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500 text-sm font-medium select-none">
                  {currencySymbol}
                </span>
                <input
                  type="number"
                  min={1}
                  max={view.remainingAmount}
                  step={1}
                  value={amount}
                  onChange={(e) => setAmount(Math.round(Number(e.target.value)))}
                  required
                  className="w-full bg-white border border-gray-200 rounded-xl pl-8 pr-3.5 py-2.5 text-sm text-gray-900 focus:outline-none focus:border-[#29B6F6] transition-colors"
                />
              </div>
            </div>

            {errorMsg && (
              <div className="flex items-start gap-2 px-3 py-2.5 rounded-xl bg-red-50 border border-red-200 text-sm text-red-600">
                <AlertCircle size={14} className="shrink-0 mt-px" />
                <span>{errorMsg}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={isPending || amount <= 0}
              className="w-full py-3.5 rounded-2xl text-white text-sm font-semibold transition-opacity disabled:opacity-50"
              style={{ background: 'linear-gradient(135deg, #29B6F6 0%, #0288D1 100%)' }}
            >
              {isPending ? 'Submitting…' : 'Submit payment claim'}
            </button>

            <p className="text-center text-xs text-gray-400 pb-1">
              {view.organizationName} will be notified to verify your payment.
            </p>
          </form>

          {/* Footer */}
          <div className="px-6 pb-5 text-center border-t border-gray-100 pt-4">
            <p className="text-xs text-gray-400">
              Sent by <strong className="text-gray-500">{view.organizationName}</strong> via FlowCollect
            </p>
          </div>

        </div>
      </div>
    </PageShell>
  )
}

// ---------------------------------------------------------------------------
// Page entry point
// ---------------------------------------------------------------------------

export default function ConfirmPaymentPage() {
  const { token = '' } = useParams<{ token: string }>()
  const { data: view, isLoading, isError, error } = useConfirmationView(token)

  if (isLoading) return <LoadingSkeleton />

  if (isError) {
    const status  = (error as any)?.response?.status
    const message = status === 404
      ? 'This confirmation link is invalid or has expired.'
      : 'Unable to load the invoice. Please try again later.'
    return <ErrorPage message={message} />
  }

  if (!view) return <ErrorPage message="Invoice not found." />

  if (view.linkStatus === 'CLOSED') return <AlreadyClosedPage view={view} />

  return <PaymentForm view={view} token={token} />
}
