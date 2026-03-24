import { useState } from 'react'
import { createPortal } from 'react-dom'
import { Check, MessageSquare, MessageCircle, X, Zap } from 'lucide-react'
import { useBilling, usePurchaseCredits } from '../hooks/useBilling'

// ---------------------------------------------------------------------------
// Data
// ---------------------------------------------------------------------------

const PLANS = [
  {
    id:       'STARTER' as const,
    name:     'Starter',
    price:    null,
    subline:  'Free forever',
    features: [
      'Unlimited email reminders',
      '25 SMS / month',
      '10 WhatsApp / month',
      'Up to 25 clients',
      '1 team member',
    ],
  },
  {
    id:       'PRO' as const,
    name:     'Pro',
    price:    499,
    subline:  'per month · ₹4,499 / year',
    features: [
      'Unlimited email reminders',
      '200 SMS / month',
      '100 WhatsApp / month',
      'Unlimited clients',
      'Up to 5 team members',
      'Priority support',
    ],
    highlight: true,
  },
  {
    id:       'BUSINESS' as const,
    name:     'Business',
    price:    1299,
    subline:  'per month · ₹11,999 / year',
    features: [
      'Unlimited email reminders',
      '1,000 SMS / month',
      '500 WhatsApp / month',
      'Unlimited clients',
      'Unlimited team members',
      'Priority support + SLA',
    ],
  },
]

type CreditChannel = 'SMS' | 'WHATSAPP'

const CREDIT_PACKS: Record<CreditChannel, { credits: number; price: number }[]> = {
  SMS: [
    { credits: 100,  price:   199 },
    { credits: 500,  price:   799 },
    { credits: 1000, price: 1_299 },
  ],
  WHATSAPP: [
    { credits:  50, price:   299 },
    { credits: 200, price:   999 },
    { credits: 500, price: 1_999 },
  ],
}

function fmt(n: number) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n)
}

// ---------------------------------------------------------------------------
// Purchase modal
// ---------------------------------------------------------------------------

function PurchaseModal({
  channel,
  onClose,
}: {
  channel:  CreditChannel
  onClose:  () => void
}) {
  const [selected, setSelected] = useState<number | null>(null)
  const [success,  setSuccess]  = useState(false)
  const purchase = usePurchaseCredits()
  const packs = CREDIT_PACKS[channel]
  const label = channel === 'SMS' ? 'SMS' : 'WhatsApp'

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
                          {pack.credits.toLocaleString('en-IN')} messages
                        </p>
                        <p className="text-xs text-c-muted mt-0.5">
                          {fmt(Math.round(pack.price / pack.credits * 100) / 100 * 100)} per message
                        </p>
                      </div>
                      <div className="text-right shrink-0">
                        <p className={`text-sm font-bold tabular-nums ${selected === pack.credits ? 'text-[#29B6F6]' : 'text-[#0D1B2A] dark:text-white'}`}>
                          {fmt(pack.price)}
                        </p>
                      </div>
                    </button>
                  ))}
                </div>

                <button
                  onClick={handleBuy}
                  disabled={selected === null || purchase.isPending}
                  className="w-full py-2.5 text-sm font-semibold text-white rounded-lg disabled:opacity-40 hover:opacity-90 transition-opacity"
                  style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
                >
                  {purchase.isPending ? 'Processing…' : selected ? `Pay ${fmt(packs.find(p => p.credits === selected)!.price)}` : 'Select a pack'}
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

function PlanCard({
  plan,
  isCurrent,
}: {
  plan:      typeof PLANS[number]
  isCurrent: boolean
}) {
  return (
    <div className={[
      'rounded-xl border p-5 flex flex-col gap-4 relative',
      plan.highlight
        ? 'border-[#29B6F6] bg-[#29B6F6]/5 dark:bg-[#29B6F6]/5'
        : 'bg-white dark:bg-[#1B2838] border-c-border',
    ].join(' ')}>

      {plan.highlight && (
        <span className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-0.5 text-[10px] font-bold uppercase tracking-wider text-white rounded-full"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}>
          Most Popular
        </span>
      )}

      <div>
        <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">{plan.name}</p>
        <div className="mt-1 flex items-baseline gap-1">
          {plan.price ? (
            <>
              <span className="text-2xl font-bold text-[#0D1B2A] dark:text-white tabular-nums">
                {fmt(plan.price)}
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

  const currentPlan = billing?.plan ?? 'STARTER'

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
          <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white mb-3">Plans</h2>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {PLANS.map((plan) => (
              <PlanCard key={plan.id} plan={plan} isCurrent={currentPlan === plan.id} />
            ))}
          </div>
        </div>

      </div>

      {buyChannel && (
        <PurchaseModal channel={buyChannel} onClose={() => setBuyChannel(null)} />
      )}
    </>
  )
}
