import { useState } from 'react'
import { createPortal } from 'react-dom'
import { Check, MessageSquare, MessageCircle, X, Zap } from 'lucide-react'
import { useBilling, usePurchaseCredits } from '../hooks/useBilling'
import { useOrgProfile } from '../hooks/useOrgSettings'

// ---------------------------------------------------------------------------
// Pricing — two regions only: India (INR) and Global (USD)
// ---------------------------------------------------------------------------

type Currency = 'INR' | 'USD'

const PLAN_PRICES: Record<Currency, { pro: number; proAnnual: number }> = {
  INR: { pro: 499, proAnnual: 4_490 },
  USD: { pro: 12,  proAnnual: 108   },
}

// Credit pack pricing
// Cost basis: SMS US ~$0.012/msg, SMS India ~$0.003/msg, WhatsApp utility ~$0.016/msg globally
// Target: 5–7x markup for sustainable margin after infra + support
const CREDIT_PRICES: Record<Currency, {
  sms:      { credits: number; price: number }[]
  whatsapp: { credits: number; price: number }[]
}> = {
  INR: {
    sms:      [{ credits: 100, price: 299  }, { credits: 500, price: 999   }, { credits: 1000, price: 1_799 }],
    whatsapp: [{ credits: 50,  price: 399  }, { credits: 200, price: 1_299 }, { credits: 500,  price: 2_799 }],
  },
  USD: {
    sms:      [{ credits: 100, price: 8  }, { credits: 500, price: 30 }, { credits: 1000, price: 50 }],
    whatsapp: [{ credits: 50,  price: 5  }, { credits: 200, price: 18 }, { credits: 500,  price: 40 }],
  },
}

function useCurrency(): Currency {
  const { data } = useOrgProfile()
  return data?.currency === 'INR' ? 'INR' : 'USD'
}

// For pack totals — whole numbers
function fmt(amount: number, currency: Currency) {
  return new Intl.NumberFormat(currency === 'INR' ? 'en-IN' : 'en-US', {
    style:                 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(amount)
}

// For per-message rates — show decimals when amount < 1
function fmtUnit(amount: number, currency: Currency) {
  const decimals = amount < 10 && currency === 'USD' ? 2 : 0
  return new Intl.NumberFormat(currency === 'INR' ? 'en-IN' : 'en-US', {
    style:                 'currency',
    currency,
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(amount)
}

// ---------------------------------------------------------------------------
// Plans
// ---------------------------------------------------------------------------

function getPlans(currency: Currency) {
  const p = PLAN_PRICES[currency]
  return [
    {
      id:       'STARTER' as const,
      name:     'Free',
      price:    null,
      subline:  'Free forever',
      features: [
        'Up to 3 active invoices',
        'Automated email reminders',
        'Client payment confirmation',
        'Basic follow-up templates',
        'Invoice approval workflows',
      ],
    },
    {
      id:            'PRO' as const,
      name:          'Pro',
      price:         p.pro,
      originalPrice: currency === 'INR' ? 799 : 19,
      subline:       `per month · ${fmt(p.proAnnual, currency)} billed yearly`,
      highlight:     true,
      features:  [
        'Unlimited invoices',
        'Email + SMS + WhatsApp reminders',
        'AI payment insights',
        'Trackable follow-up links',
        'Custom reminder schedules',
        'Priority support',
      ],
    },
  ]
}

// ---------------------------------------------------------------------------
// Purchase modal
// ---------------------------------------------------------------------------

type CreditChannel = 'SMS' | 'WHATSAPP'

function PurchaseModal({ channel, currency, onClose }: { channel: CreditChannel; currency: Currency; onClose: () => void }) {
  const [selected, setSelected] = useState<number | null>(null)
  const [success,  setSuccess]  = useState(false)
  const purchase = usePurchaseCredits()
  const packs    = channel === 'SMS' ? CREDIT_PRICES[currency].sms : CREDIT_PRICES[currency].whatsapp
  const label    = channel === 'SMS' ? 'SMS' : 'WhatsApp'

  async function handleBuy() {
    if (selected === null) return
    await purchase.mutateAsync({ channel, pack: selected })
    setSuccess(true)
  }

  return createPortal(
    <>
      <div className="fixed inset-0 z-[200] bg-black/60" onClick={onClose} aria-hidden="true" />
      <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 pointer-events-none">
        <div
          className="pointer-events-auto w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-2xl"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-c-border">
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Buy {label} Credits</h2>
            <button onClick={onClose} className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors">
              <X size={18} />
            </button>
          </div>

          <div className="px-6 py-5">
            {success ? (
              <div className="flex flex-col items-center gap-3 py-6 text-center">
                <div className="w-12 h-12 rounded-full bg-green-500/10 flex items-center justify-center">
                  <Check size={22} className="text-green-500" />
                </div>
                <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Credits added!</p>
                <p className="text-sm text-c-muted">{selected} {label} credits have been added to your account.</p>
                <button
                  onClick={onClose}
                  className="mt-1 px-5 py-2 text-sm font-semibold text-white rounded-lg hover:opacity-90 transition-opacity"
                  style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
                >
                  Done
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                <p className="text-sm text-c-muted">Choose a credit pack:</p>
                <div className="space-y-2">
                  {packs.map((pack) => (
                    <button
                      key={pack.credits}
                      type="button"
                      onClick={() => setSelected(pack.credits)}
                      className={[
                        'w-full flex items-center justify-between px-4 py-3 rounded-lg border transition-colors text-left',
                        selected === pack.credits
                          ? 'border-[#29B6F6] bg-[#29B6F6]/5'
                          : 'border-c-border hover:border-[#8A9BAE]/50',
                      ].join(' ')}
                    >
                      <div>
                        <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">
                          {pack.credits.toLocaleString()} messages
                        </p>
                        <p className="text-xs text-c-muted mt-0.5">
                          {fmtUnit(pack.price / pack.credits, currency)} per message
                        </p>
                      </div>
                      <p className={`text-sm font-bold tabular-nums shrink-0 ${selected === pack.credits ? 'text-[#29B6F6]' : 'text-[#0D1B2A] dark:text-white'}`}>
                        {fmt(pack.price, currency)}
                      </p>
                    </button>
                  ))}
                </div>
                <button
                  onClick={handleBuy}
                  disabled={selected === null || purchase.isPending}
                  className="w-full py-2.5 text-sm font-semibold text-white rounded-lg disabled:opacity-40 hover:opacity-90 transition-opacity"
                  style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
                >
                  {purchase.isPending
                    ? 'Processing…'
                    : selected
                      ? `Pay ${fmt(packs.find((p) => p.credits === selected)!.price, currency)}`
                      : 'Select a pack'}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </>,
    document.body,
  )
}

// ---------------------------------------------------------------------------
// Plan card
// ---------------------------------------------------------------------------

function PlanCard({ plan, isCurrent, currency }: { plan: ReturnType<typeof getPlans>[number]; isCurrent: boolean; currency: Currency }) {
  return (
    <div className={[
      'rounded-xl border p-5 flex flex-col gap-4 relative',
      plan.highlight
        ? 'border-[#29B6F6] bg-[#29B6F6]/5 dark:bg-[#29B6F6]/5'
        : 'bg-white dark:bg-[#1B2838] border-c-border',
    ].join(' ')}>

      {plan.highlight && (
        <span
          className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-0.5 text-[10px] font-bold uppercase tracking-wider text-white rounded-full whitespace-nowrap"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          Founding Member Price
        </span>
      )}

      <div>
        <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">{plan.name}</p>
        <div className="mt-1 flex items-baseline gap-1.5 flex-wrap">
          {plan.price ? (
            <>
              {'originalPrice' in plan && plan.originalPrice && (
                <span className="text-sm font-semibold text-c-muted line-through">
                  {fmt(plan.originalPrice, currency)}
                </span>
              )}
              <span className="text-2xl font-bold text-[#0D1B2A] dark:text-white tabular-nums">
                {fmt(plan.price, currency)}
              </span>
              <span className="text-xs text-c-muted">{plan.subline}</span>
            </>
          ) : (
            <span className="text-2xl font-bold text-[#0D1B2A] dark:text-white">Free</span>
          )}
        </div>
      </div>

      <ul className="flex-1 space-y-2">
        {plan.features.map((f) => (
          <li key={f} className="flex items-start gap-2">
            <Check size={14} className="text-[#29B6F6] shrink-0 mt-0.5" strokeWidth={2.5} />
            <span className="text-xs text-[#0D1B2A]/80 dark:text-white/70">{f}</span>
          </li>
        ))}
      </ul>

      <button
        disabled={isCurrent}
        className={[
          'w-full py-2 text-sm font-semibold rounded-lg transition-opacity',
          isCurrent
            ? 'border border-c-border text-c-muted cursor-default'
            : 'text-white hover:opacity-90',
        ].join(' ')}
        style={!isCurrent ? { background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' } : undefined}
      >
        {isCurrent ? 'Current plan' : `Upgrade to ${plan.name}`}
      </button>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function BillingPage() {
  const [buyChannel, setBuyChannel] = useState<CreditChannel | null>(null)
  const { data: billing, isLoading } = useBilling()
  const currency    = useCurrency()
  const currentPlan = billing?.plan ?? 'STARTER'
  const plans       = getPlans(currency)

  return (
    <>
      <div className="space-y-6">

        {/* Credit balance */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
          <div className="px-5 py-4 border-b border-c-border flex items-center gap-2">
            <Zap size={15} className="text-amber-500" />
            <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Message Credits</h2>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 divide-y sm:divide-y-0 sm:divide-x divide-c-border">

            {/* SMS */}
            <div className="flex items-center justify-between gap-4 px-5 py-4">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-full bg-amber-500/10 text-amber-500 flex items-center justify-center shrink-0">
                  <MessageSquare size={15} strokeWidth={1.8} />
                </div>
                <div>
                  <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">SMS</p>
                  {isLoading ? (
                    <div className="h-3 w-16 rounded bg-[#F4F7F9] dark:bg-white/10 mt-1 animate-pulse" />
                  ) : (
                    <p className="text-xs text-c-muted">{billing?.smsCredits ?? 0} credits remaining</p>
                  )}
                </div>
              </div>
              <button
                onClick={() => setBuyChannel('SMS')}
                className="px-3 py-1.5 text-xs font-semibold text-white rounded-lg hover:opacity-90 transition-opacity shrink-0"
                style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
              >
                Buy Credits
              </button>
            </div>

            {/* WhatsApp */}
            <div className="flex items-center justify-between gap-4 px-5 py-4">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-full bg-green-500/10 text-green-500 flex items-center justify-center shrink-0">
                  <MessageCircle size={15} strokeWidth={1.8} />
                </div>
                <div>
                  <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">WhatsApp</p>
                  {isLoading ? (
                    <div className="h-3 w-16 rounded bg-[#F4F7F9] dark:bg-white/10 mt-1 animate-pulse" />
                  ) : (
                    <p className="text-xs text-c-muted">{billing?.waCredits ?? 0} credits remaining</p>
                  )}
                </div>
              </div>
              <button
                onClick={() => setBuyChannel('WHATSAPP')}
                className="px-3 py-1.5 text-xs font-semibold text-white rounded-lg hover:opacity-90 transition-opacity shrink-0"
                style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
              >
                Buy Credits
              </button>
            </div>
          </div>
          <div className="px-5 py-3 border-t border-c-border bg-[#F4F7F9]/50 dark:bg-white/5">
            <p className="text-xs text-c-muted">
              Email reminders are always free. Credits are used for SMS and WhatsApp messages only.
            </p>
          </div>
        </div>

        {/* Plans */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Plans</h2>
            <p className="text-xs text-c-muted">
              {currency === 'INR' ? 'Prices in INR · India' : 'Prices in USD · Global'}
            </p>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-2xl mx-auto">
            {plans.map((plan) => (
              <PlanCard key={plan.id} plan={plan} isCurrent={currentPlan === plan.id} currency={currency} />
            ))}
          </div>
          <p className="text-xs text-c-muted mt-4 text-center max-w-2xl mx-auto">
            Founding member pricing — locked forever. Raises after the first 200 users.
          </p>
        </div>

      </div>

      {buyChannel && (
        <PurchaseModal channel={buyChannel} currency={currency} onClose={() => setBuyChannel(null)} />
      )}
    </>
  )
}
