import { Check } from 'lucide-react'
import { useBilling } from '../hooks/useBilling'
import { useOrgProfile } from '../hooks/useOrgSettings'

// ---------------------------------------------------------------------------
// Pricing — two regions only: India (INR) and Global (USD)
// ---------------------------------------------------------------------------

type Currency = 'INR' | 'USD'

const PLAN_PRICES: Record<Currency, { pro: number; proAnnual: number }> = {
  INR: { pro: 499, proAnnual: 4_490 },
  USD: { pro: 12,  proAnnual: 108   },
}

function useCurrency(): Currency {
  const { data } = useOrgProfile()
  return data?.currency === 'INR' ? 'INR' : 'USD'
}

function fmt(amount: number, currency: Currency) {
  return new Intl.NumberFormat(currency === 'INR' ? 'en-IN' : 'en-US', {
    style:                 'currency',
    currency,
    maximumFractionDigits: 0,
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
      features: [
        'Unlimited invoices',
        'Automated email reminders',
        'AI payment insights',
        'Trackable follow-up links',
        'Custom reminder schedules',
        'Priority support',
      ],
    },
  ]
}

// ---------------------------------------------------------------------------
// Plan card
// ---------------------------------------------------------------------------

function PlanCard({ plan, isCurrent, currency }: {
  plan:      ReturnType<typeof getPlans>[number]
  isCurrent: boolean
  currency:  Currency
}) {
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
            <>
              <span className="text-2xl font-bold text-[#0D1B2A] dark:text-white tabular-nums">
                {fmt(0, currency)}
              </span>
              <span className="text-xs text-c-muted">{plan.subline}</span>
            </>
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
  const { data: billing } = useBilling()
  const currency    = useCurrency()
  const currentPlan = billing?.plan ?? 'STARTER'
  const plans       = getPlans(currency)

  return (
    <div className="space-y-6">

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
  )
}
