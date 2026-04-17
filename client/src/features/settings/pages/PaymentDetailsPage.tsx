import { useState, useEffect } from 'react'
import { Copy, Check, CreditCard, Wallet, Lock } from 'lucide-react'
import { usePaymentDetails, useSavePaymentDetails, useOrgProfile } from '../hooks/useOrgSettings'

// ---------------------------------------------------------------------------
// Shared UI
// ---------------------------------------------------------------------------

function Field({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: React.ReactNode
}) {
  return (
    <div>
      <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-1.5">
        {label}
        {hint && <span className="ml-1.5 font-normal normal-case text-[#8A9BAE]">· {hint}</span>}
      </label>
      {children}
    </div>
  )
}

const inputCls =
  'w-full text-sm rounded-lg border border-c-border bg-transparent text-[#0D1B2A] dark:text-white placeholder:text-c-muted px-3 py-2.5 focus:outline-none focus:border-[#8A9BAE]/40 transition-colors'

// ---------------------------------------------------------------------------
// Copy button (used inside preview)
// ---------------------------------------------------------------------------

function CopyButton({ value }: { value: string }) {
  const [copied, setCopied] = useState(false)
  function handle() {
    navigator.clipboard.writeText(value).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 1800)
    })
  }
  return (
    <button type="button" onClick={handle} title="Copy"
      className="shrink-0 p-0.5 rounded text-gray-400 hover:text-gray-700 transition-colors">
      {copied ? <Check size={11} className="text-green-500" /> : <Copy size={11} />}
    </button>
  )
}

function CopyRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-2 py-1">
      <span className="text-[11px] text-gray-400 shrink-0">{label}</span>
      <div className="flex items-center gap-0.5 min-w-0">
        <span className="text-[11px] font-medium text-gray-800 tabular-nums truncate">{value}</span>
        <CopyButton value={value} />
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Full confirmation page preview
// ---------------------------------------------------------------------------

interface PreviewState {
  orgName: string
  currency: string
  bankName: string; accountHolderName: string; accountNumber: string
  iban: string; swiftCode: string; routingNumber: string
  ifscCode: string; upiId: string
  paypalEmail: string; wiseEmail: string
  additionalNote: string
}

function fmtSample(currency: string): string {
  return new Intl.NumberFormat(currency === 'INR' ? 'en-IN' : 'en-US', {
    style: 'currency',
    currency: currency || 'USD',
    maximumFractionDigits: 0,
  }).format(25000)
}

function ConfirmationPagePreview(s: PreviewState) {
  const sampleAmount = fmtSample(s.currency)
  const hasBankWire = s.bankName || s.accountHolderName || s.accountNumber ||
                      s.iban || s.swiftCode || s.routingNumber || s.ifscCode
  const hasUpi     = !!s.upiId
  const hasWallets = s.paypalEmail || s.wiseEmail
  const hasDetails = hasBankWire || hasUpi || hasWallets

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
      {/* Panel header */}
      <div className="px-5 py-3 border-b border-c-border">
        <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Customer Preview</p>
        <p className="text-xs text-c-muted mt-0.5">Exactly what your client sees on their phone</p>
      </div>

      {/* Phone frame — outer bg adapts to dark mode, screen content is always light */}
      <div className="flex justify-center py-6 px-4 bg-[#E8EDF2] dark:bg-[#131E2B]">
        <div className="relative w-[260px]">
          {/* Phone shell — always renders light-mode screen regardless of app theme */}
          <div className="rounded-[28px] border-[6px] border-[#1C1C1E] shadow-2xl overflow-hidden bg-[#F4F7F9]">
            {/* Status bar */}
            <div className="bg-[#1C1C1E] px-4 py-1 flex items-center justify-between">
              <span className="text-[8px] text-white font-medium">9:41</span>
              <div className="flex items-center gap-1">
                <div className="w-3 h-1.5 rounded-sm bg-white opacity-80" />
                <div className="w-1 h-1.5 rounded-sm bg-white opacity-80" />
              </div>
            </div>

            {/* Scrollable page content — force light color scheme so no dark-mode bleed */}
            <div className="overflow-y-auto max-h-[520px] bg-[#F4F7F9]" style={{ colorScheme: 'light' }}>
              {/* Confirmation card */}
              <div className="bg-white mx-0 shadow-sm">

                {/* Brand header */}
                <div
                  className="px-4 py-3.5 text-white"
                  style={{ background: 'linear-gradient(135deg, #29B6F6 0%, #0288D1 100%)' }}
                >
                  <p className="text-[11px] font-black tracking-tight">FlowCollect</p>
                  <p className="text-[10px] opacity-85 mt-0.5">INV-0042 · {s.orgName || 'Your Business'}</p>
                </div>

                {/* Invoice summary */}
                <div className="px-4 pt-3 pb-3 space-y-1.5">
                  {([
                    ['Client',        'Sample Client'      ],
                    ['Due date',      '15 Jan 2025'        ],
                    ['Invoice total', sampleAmount          ],
                  ] as const).map(([lbl, val]) => (
                    <div key={lbl} className="flex items-center justify-between">
                      <span className="text-[10px] text-gray-400">{lbl}</span>
                      <span className="text-[10px] font-medium text-gray-700">{val}</span>
                    </div>
                  ))}
                </div>

                {/* Balance hero */}
                <div className="mx-4 mb-4 rounded-xl bg-[#F4F7F9] border border-gray-200 px-3 py-2.5 text-center">
                  <p className="text-lg font-bold text-gray-900 tabular-nums">{sampleAmount}</p>
                  <p className="text-[9px] text-gray-500 mt-0.5">
                    Remaining balance <span className="text-red-500 font-medium">· 3 days overdue</span>
                  </p>
                </div>

                {/* Payment details — embedded inside card */}
                {hasDetails && (
                  <div className="mx-4 mb-4 rounded-xl border border-[#29B6F6]/20 bg-[#F0FBFF] overflow-hidden">
                    <div className="px-3 py-2 border-b border-[#29B6F6]/15">
                      <p className="text-[9px] font-semibold text-[#0288D1]">Send your payment to</p>
                    </div>
                    <div className="px-3 py-2.5 space-y-2">
                      {hasBankWire && (
                        <div>
                          <p className="text-[8px] font-semibold uppercase tracking-wide text-gray-400 mb-1 flex items-center gap-0.5">
                            <CreditCard size={8} /> Bank / Wire
                          </p>
                          <div className="space-y-0.5">
                            {s.bankName          && <MiniCopyRow label="Bank"         value={s.bankName} />}
                            {s.accountHolderName && <MiniCopyRow label="Account name" value={s.accountHolderName} />}
                            {s.accountNumber     && <MiniCopyRow label="Account no."  value={s.accountNumber} />}
                            {s.iban              && <MiniCopyRow label="IBAN"         value={s.iban} />}
                            {s.swiftCode         && <MiniCopyRow label="SWIFT / BIC"  value={s.swiftCode} />}
                            {s.routingNumber     && <MiniCopyRow label="Routing no."  value={s.routingNumber} />}
                            {s.ifscCode          && <MiniCopyRow label="IFSC"         value={s.ifscCode} />}
                          </div>
                        </div>
                      )}
                      {hasUpi && (
                        <div className={hasBankWire ? 'border-t border-[#29B6F6]/10 pt-1.5' : ''}>
                          <p className="text-[8px] font-semibold uppercase tracking-wide text-gray-400 mb-1">UPI</p>
                          <MiniCopyRow label="UPI ID" value={s.upiId} />
                        </div>
                      )}
                      {hasWallets && (
                        <div className={(hasBankWire || hasUpi) ? 'border-t border-[#29B6F6]/10 pt-1.5' : ''}>
                          <p className="text-[8px] font-semibold uppercase tracking-wide text-gray-400 mb-1 flex items-center gap-0.5">
                            <Wallet size={8} /> Wallets
                          </p>
                          {s.paypalEmail && <MiniCopyRow label="PayPal" value={s.paypalEmail} />}
                          {s.wiseEmail   && <MiniCopyRow label="Wise"   value={s.wiseEmail} />}
                        </div>
                      )}
                      {s.additionalNote && (
                        <p className="text-[9px] text-gray-400 pt-1.5 border-t border-[#29B6F6]/10">
                          {s.additionalNote}
                        </p>
                      )}
                    </div>
                  </div>
                )}

                {/* Divider */}
                <div className="border-t border-gray-100 mx-4 mb-3" />

                {/* Form — visual only */}
                <div className="px-4 pb-4 space-y-2.5">
                  <div>
                    <p className="text-[8px] font-semibold uppercase tracking-wide text-gray-400 mb-1">Payment method</p>
                    <div className="w-full border border-gray-200 rounded-lg px-2.5 py-1.5 text-[10px] text-gray-700 bg-white flex items-center justify-between">
                      <span>Bank Transfer</span><span className="text-[8px] text-gray-400">▼</span>
                    </div>
                  </div>
                  <div>
                    <p className="text-[8px] font-semibold uppercase tracking-wide text-gray-400 mb-1">Reference number</p>
                    <div className="w-full border border-gray-200 rounded-lg px-2.5 py-1.5 text-[10px] text-gray-400 bg-white">
                      e.g. TXN123456
                    </div>
                  </div>
                  <div>
                    <p className="text-[8px] font-semibold uppercase tracking-wide text-gray-400 mb-1">Amount paid</p>
                    <div className="w-full border border-gray-200 rounded-lg px-2.5 py-1.5 text-[10px] text-gray-700 bg-white">
                      {sampleAmount}
                    </div>
                  </div>
                  <div
                    className="w-full py-2 rounded-xl text-white text-[10px] font-semibold text-center"
                    style={{ background: 'linear-gradient(135deg, #29B6F6 0%, #0288D1 100%)' }}
                  >
                    Submit payment claim
                  </div>
                  <p className="text-center text-[8px] text-gray-400">
                    {s.orgName || 'Your business'} will verify your payment.
                  </p>
                </div>

                {/* Footer */}
                <div className="px-4 pb-4 text-center border-t border-gray-100 pt-3">
                  <p className="text-[8px] text-gray-400">
                    Sent by <strong className="text-gray-500">{s.orgName || 'Your Business'}</strong> via FlowCollect
                  </p>
                </div>

              </div>
            </div>
          </div>

          {/* Home indicator */}
          <div className="mt-2 flex justify-center">
            <div className="w-16 h-1 rounded-full bg-[#4B5563]" />
          </div>
        </div>
      </div>

      {!hasDetails && (
        <p className="text-center text-xs text-c-muted pb-4 -mt-2">
          Fill in payment details on the left — they'll appear inside this preview
        </p>
      )}
    </div>
  )
}

/** Tiny copy row used only inside the phone preview */
function MiniCopyRow({ label, value }: { label: string; value: string }) {
  const [copied, setCopied] = useState(false)
  return (
    <div className="flex items-center justify-between gap-1 py-0.5">
      <span className="text-[9px] text-gray-400 shrink-0">{label}</span>
      <div className="flex items-center gap-0.5 min-w-0">
        <span className="text-[9px] font-semibold text-[#0D2137] tabular-nums truncate">{value}</span>
        <button
          type="button"
          onClick={() => {
            navigator.clipboard.writeText(value).then(() => {
              setCopied(true)
              setTimeout(() => setCopied(false), 1500)
            })
          }}
          className="shrink-0 text-[#29B6F6] hover:text-[#0288D1] transition-colors"
        >
          {copied ? <Check size={9} className="text-green-500" /> : <Copy size={9} />}
        </button>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function PaymentDetailsPage() {
  const { data, isLoading }   = usePaymentDetails()
  const { data: org }         = useOrgProfile()
  const save = useSavePaymentDetails()

  const [bankName,          setBankName]          = useState('')
  const [accountHolderName, setAccountHolderName] = useState('')
  const [accountNumber,     setAccountNumber]     = useState('')
  const [iban,              setIban]              = useState('')
  const [swiftCode,         setSwiftCode]         = useState('')
  const [routingNumber,     setRoutingNumber]     = useState('')
  const [ifscCode,          setIfscCode]          = useState('')
  const [upiId,             setUpiId]             = useState('')
  const [paypalEmail,       setPaypalEmail]       = useState('')
  const [wiseEmail,         setWiseEmail]         = useState('')
  const [additionalNote,    setAdditionalNote]    = useState('')
  const [saved,             setSaved]             = useState(false)

  useEffect(() => {
    if (data) {
      setBankName(data.bankName ?? '')
      setAccountHolderName(data.accountHolderName ?? '')
      setAccountNumber(data.accountNumber ?? '')
      setIban(data.iban ?? '')
      setSwiftCode(data.swiftCode ?? '')
      setRoutingNumber(data.routingNumber ?? '')
      setIfscCode(data.ifscCode ?? '')
      setUpiId(data.upiId ?? '')
      setPaypalEmail(data.paypalEmail ?? '')
      setWiseEmail(data.wiseEmail ?? '')
      setAdditionalNote(data.additionalNote ?? '')
    }
  }, [data])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    await save.mutateAsync({
      bankName:          bankName          || undefined,
      accountHolderName: accountHolderName || undefined,
      accountNumber:     accountNumber     || undefined,
      iban:              iban              || undefined,
      swiftCode:         swiftCode         || undefined,
      routingNumber:     routingNumber     || undefined,
      ifscCode:          ifscCode          || undefined,
      upiId:             upiId             || undefined,
      paypalEmail:       paypalEmail       || undefined,
      wiseEmail:         wiseEmail         || undefined,
      additionalNote:    additionalNote    || undefined,
    })
    setSaved(true)
    setTimeout(() => setSaved(false), 2500)
  }

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-5 space-y-4 animate-pulse">
          {[...Array(6)].map((_, i) => <div key={i} className="h-10 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />)}
        </div>
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border h-64 animate-pulse" />
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 items-start">

      {/* ── Form ─────────────────────────────────────────────────── */}
      <form onSubmit={handleSubmit} className="space-y-4">

        {/* Bank / Wire Transfer */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
          <div className="px-5 py-4 border-b border-c-border flex items-center gap-2">
            <CreditCard size={15} className="text-c-muted" />
            <div>
              <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Bank / Wire Transfer</p>
              <p className="text-xs text-c-muted">Fill only what applies to your country</p>
            </div>
          </div>
          <div className="p-5 space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Field label="Bank Name">
                <input type="text" value={bankName} onChange={(e) => setBankName(e.target.value)}
                  placeholder="e.g. HDFC, Chase, Barclays" className={inputCls} />
              </Field>
              <Field label="Account Holder Name">
                <input type="text" value={accountHolderName} onChange={(e) => setAccountHolderName(e.target.value)}
                  placeholder="Name on the account" className={inputCls} />
              </Field>
            </div>

            <Field label="Account Number" hint="domestic — US, India, AU, etc.">
              <input type="text" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)}
                placeholder="e.g. 50100123456789" className={inputCls} />
            </Field>

            <Field label="IBAN" hint="Europe, UK, Middle East">
              <input type="text" value={iban} onChange={(e) => setIban(e.target.value.toUpperCase())}
                placeholder="e.g. GB29 NWBK 6016 1331 9268 19" className={inputCls} />
            </Field>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Field label="SWIFT / BIC" hint="international wire">
                <input type="text" value={swiftCode} onChange={(e) => setSwiftCode(e.target.value.toUpperCase())}
                  placeholder="e.g. HDFCINBB" className={inputCls} />
              </Field>
              <Field label="Routing Number" hint="US ACH">
                <input type="text" value={routingNumber} onChange={(e) => setRoutingNumber(e.target.value)}
                  placeholder="e.g. 021000021" className={inputCls} />
              </Field>
            </div>

            <Field label="IFSC Code" hint="India NEFT / RTGS">
              <input type="text" value={ifscCode} onChange={(e) => setIfscCode(e.target.value.toUpperCase())}
                placeholder="e.g. HDFC0001234" className={inputCls} />
            </Field>
          </div>
        </div>

        {/* UPI */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
          <div className="px-5 py-4 border-b border-c-border">
            <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">UPI <span className="text-xs font-normal text-c-muted">· India</span></p>
          </div>
          <div className="p-5">
            <Field label="UPI ID">
              <input type="text" value={upiId} onChange={(e) => setUpiId(e.target.value)}
                placeholder="e.g. yourname@hdfc" className={inputCls} />
            </Field>
          </div>
        </div>

        {/* Digital Wallets */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
          <div className="px-5 py-4 border-b border-c-border flex items-center gap-2">
            <Wallet size={15} className="text-c-muted" />
            <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Digital Wallets</p>
          </div>
          <div className="p-5 space-y-4">
            <Field label="PayPal" hint="global">
              <input type="text" value={paypalEmail} onChange={(e) => setPaypalEmail(e.target.value)}
                placeholder="e.g. yourname@gmail.com" className={inputCls} />
            </Field>
            <Field label="Wise" hint="global — popular with freelancers">
              <input type="text" value={wiseEmail} onChange={(e) => setWiseEmail(e.target.value)}
                placeholder="e.g. yourname@gmail.com" className={inputCls} />
            </Field>
          </div>
        </div>

        {/* Note */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
          <div className="p-5">
            <Field label="Note for customer" hint="optional">
              <textarea value={additionalNote} onChange={(e) => setAdditionalNote(e.target.value)}
                placeholder="e.g. Please mention the invoice number in your transfer remarks"
                rows={2} className={`${inputCls} resize-none`} />
            </Field>
          </div>
        </div>

        {/* Payment Link — coming soon */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden opacity-55">
          <div className="px-5 py-4 border-b border-c-border flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Lock size={15} className="text-c-muted" />
              <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Payment Link</p>
            </div>
            <span className="text-[10px] font-semibold uppercase tracking-wide px-2 py-0.5 rounded-full bg-[#29B6F6]/10 text-[#29B6F6]">
              Coming Soon
            </span>
          </div>
          <div className="p-5">
            <p className="text-xs text-c-muted">
              Let customers pay directly via Stripe or Razorpay — no manual approval needed.
            </p>
          </div>
        </div>

        {/* Save */}
        <div className="flex items-center justify-end gap-3">
          {saved && <span className="text-sm text-green-600 dark:text-green-400">Saved</span>}
          <button
            type="submit"
            disabled={save.isPending}
            className="px-5 py-2 text-sm font-semibold text-white rounded-lg disabled:opacity-50 hover:opacity-90 transition-opacity"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            {save.isPending ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </form>

      {/* ── Full confirmation page preview ───────────────────────── */}
      <div className="lg:sticky lg:top-6">
        <ConfirmationPagePreview
          orgName={org?.name ?? ''}
          currency={org?.currency ?? 'USD'}
          bankName={bankName} accountHolderName={accountHolderName}
          accountNumber={accountNumber} iban={iban} swiftCode={swiftCode}
          routingNumber={routingNumber} ifscCode={ifscCode} upiId={upiId}
          paypalEmail={paypalEmail} wiseEmail={wiseEmail}
          additionalNote={additionalNote}
        />
      </div>

    </div>
  )
}
