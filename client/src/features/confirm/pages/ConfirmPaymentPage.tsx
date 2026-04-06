import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useConfirmationView, useSubmitPaymentClaim } from '../hooks/useConfirmPayment'
import type { CustomerConfirmationView } from '@/api/publicConfirmation.api'
import { CheckCircle2, AlertCircle } from 'lucide-react'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function fmt(amount: number, currency: string) {
  try {
    return new Intl.NumberFormat('en-IN', {
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
  const due  = new Date(dateStr)
  const now  = new Date()
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
  'Bank Transfer',
  'UPI',
  'Cash',
  'Cheque',
  'NEFT / RTGS',
  'Other',
] as const

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

function Header({ view }: { view: CustomerConfirmationView }) {
  return (
    <div style={{ background: '#2a9d8f' }} className="px-5 py-5 text-white">
      <p className="text-lg font-bold tracking-tight">
        <span className="font-black">Flow</span>Collect
      </p>
      <p className="text-sm opacity-90 mt-0.5">
        Invoice {view.invoiceNumber} from {view.organizationName}
      </p>
    </div>
  )
}

function InvoiceSummary({ view }: { view: CustomerConfirmationView }) {
  const overdue = daysDiff(view.dueDate)
  return (
    <div className="px-5 pt-5 pb-4 space-y-5">
      {/* Info rows */}
      <div className="space-y-2 text-sm">
        <Row label="Client"          value={view.customerName} />
        <Row label="Due date"        value={fmtDate(view.dueDate)} />
        <Row label="Original amount" value={fmt(view.totalAmount, view.currency)} />
        {view.totalPaid > 0 && (
          <Row
            label="Already paid"
            value={fmt(view.totalPaid, view.currency)}
            valueClass="text-[#2a9d8f] font-semibold"
          />
        )}
      </div>

      {/* Remaining balance hero */}
      <div className="text-center py-2">
        <p className="text-4xl font-bold text-gray-900">
          {fmt(view.remainingAmount, view.currency)}
        </p>
        <p className="text-sm text-gray-500 mt-1">
          Remaining balance
          {overdue > 0 && (
            <span className="text-red-500"> · {overdue} day{overdue !== 1 ? 's' : ''} overdue</span>
          )}
        </p>
      </div>
    </div>
  )
}

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
      <span className="text-gray-500">{label}</span>
      <span className={valueClass}>{value}</span>
    </div>
  )
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

function LoadingSkeleton() {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <div className="h-20 bg-[#2a9d8f]" />
      <div className="px-5 pt-5 space-y-3 animate-pulse">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="flex justify-between">
            <div className="h-4 w-24 rounded bg-gray-200" />
            <div className="h-4 w-20 rounded bg-gray-200" />
          </div>
        ))}
        <div className="h-12 w-36 mx-auto rounded bg-gray-200 mt-4" />
      </div>
    </div>
  )
}

function ErrorPage({ message }: { message: string }) {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <div className="text-center space-y-3 max-w-sm">
        <AlertCircle size={40} className="text-red-400 mx-auto" />
        <p className="text-base font-semibold text-gray-800">Link unavailable</p>
        <p className="text-sm text-gray-500">{message}</p>
      </div>
    </div>
  )
}

function AlreadyClosedPage({ view }: { view: CustomerConfirmationView }) {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header view={view} />
      <div className="flex-1 flex items-center justify-center p-6">
        <div className="text-center space-y-3 max-w-sm">
          <CheckCircle2 size={44} className="text-[#2a9d8f] mx-auto" />
          <p className="text-base font-semibold text-gray-800">Payment already confirmed</p>
          <p className="text-sm text-gray-500">
            This invoice has been fully settled. No further action needed.
          </p>
        </div>
      </div>
    </div>
  )
}

function SuccessPage({ view, amountClaimed }: { view: CustomerConfirmationView; amountClaimed: number }) {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header view={view} />
      <div className="flex-1 flex items-center justify-center p-6">
        <div className="text-center space-y-3 max-w-sm">
          <CheckCircle2 size={44} className="text-[#2a9d8f] mx-auto" />
          <p className="text-base font-semibold text-gray-800">Payment claim submitted</p>
          <p className="text-sm text-gray-500">
            {fmt(amountClaimed, view.currency)} claimed.{' '}
            <strong>{view.organizationName}</strong> will review and confirm your payment.
          </p>
        </div>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Main form
// ---------------------------------------------------------------------------

function PaymentForm({ view, token }: { view: CustomerConfirmationView; token: string }) {
  const { mutate, isPending, isError, error } = useSubmitPaymentClaim(token)

  const [paymentMethod, setPaymentMethod] = useState<string>('Bank Transfer')
  const [reference,     setReference]     = useState('')
  const [amount,        setAmount]        = useState(Math.round(view.remainingAmount))
  const [submitted,     setSubmitted]     = useState(false)
  const [amountClaimed, setAmountClaimed] = useState(0)

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

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header view={view} />
      <InvoiceSummary view={view} />

      <div className="border-t border-gray-100" />

      <form onSubmit={handleSubmit} className="px-5 py-5 space-y-5 flex-1">

        {/* Payment method */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            Payment method
          </label>
          <div className="relative">
            <select
              value={paymentMethod}
              onChange={(e) => setPaymentMethod(e.target.value)}
              className="w-full appearance-none bg-white border border-gray-200 rounded-xl px-4 py-3 text-sm text-gray-900 focus:outline-none focus:border-[#2a9d8f] transition-colors pr-9"
            >
              {PAYMENT_METHODS.map((m) => (
                <option key={m} value={m}>{m}</option>
              ))}
            </select>
            <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-xs">▼</span>
          </div>
        </div>

        {/* Transaction reference */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            Transaction / reference number{' '}
            <span className="text-gray-400 font-normal">(optional)</span>
          </label>
          <input
            type="text"
            value={reference}
            onChange={(e) => setReference(e.target.value)}
            placeholder={`e.g. ${paymentMethod === 'UPI' ? 'UPI ref 423891756234' : 'TXN123456'}`}
            className="w-full bg-white border border-gray-200 rounded-xl px-4 py-3 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:border-[#2a9d8f] transition-colors"
          />
        </div>

        {/* Amount */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1.5">
            Amount paid
          </label>
          <div className="relative">
            <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 text-sm font-medium">
              {view.currency === 'INR' ? '₹' : view.currency}
            </span>
            <input
              type="number"
              min={1}
              max={view.remainingAmount}
              step={1}
              value={amount}
              onChange={(e) => setAmount(Math.round(Number(e.target.value)))}
              required
              className="w-full bg-white border border-gray-200 rounded-xl pl-8 pr-4 py-3 text-sm text-gray-900 focus:outline-none focus:border-[#2a9d8f] transition-colors"
            />
          </div>
        </div>

        {/* Error */}
        {errorMsg && (
          <div className="flex items-start gap-2 px-3 py-2.5 rounded-xl bg-red-50 border border-red-200 text-sm text-red-600">
            <AlertCircle size={15} className="shrink-0 mt-px" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Submit */}
        <button
          type="submit"
          disabled={isPending || amount <= 0}
          className="w-full py-3.5 rounded-2xl text-white text-base font-semibold transition-opacity disabled:opacity-50"
          style={{ background: '#2a9d8f' }}
        >
          {isPending ? 'Submitting…' : 'Submit payment claim'}
        </button>

        <p className="text-center text-xs text-gray-400 pb-4">
          {view.organizationName} will be notified to verify your payment
        </p>
      </form>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
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
