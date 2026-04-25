import { Check, AlertCircle, Clock, RefreshCw } from 'lucide-react'
import { useEffect, useState }       from 'react'
import { useSearchParams }           from 'react-router-dom'
import { useQueryClient }            from '@tanstack/react-query'
import { useBilling }                from '../hooks/useBilling'
import { useOrgProfile }             from '../hooks/useOrgSettings'
import { useAuthStore }              from '@/store/auth.store'
import { createCheckoutSession, cancelSubscription } from '@/api/organization.api'
import { useToast }                  from '@/store/toast.store'

// ---------------------------------------------------------------------------
// Pricing — two regions: India (INR) and Global (USD)
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

function fmtDate(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric', month: 'long', day: 'numeric',
  })
}

// ---------------------------------------------------------------------------
// Plans definition
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
        'Send professional invoices',
        'Automatic follow-ups until you get paid',
        'Track when clients open messages',
        'Client payment confirmations',
        'Basic follow-up templates',
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
        'Everything in Free',
        'Unlimited invoices',
        'Automatic follow-ups until you get paid',
        'Smart escalation from polite to firm reminders',
        'Real-time recovery dashboard',
        'Professional follow-ups that sound human',
        'Smart insights to spot late payers early',
        'Priority support',
      ],
    },
  ]
}

// ---------------------------------------------------------------------------
// Return-URL banner (shown after redirect back from Dodo checkout)
// ---------------------------------------------------------------------------

function CheckoutBanner({ status, planConfirmed, onDismiss, onRefresh }: {
  status:        'upgraded' | 'cancelled'
  planConfirmed: boolean   // true once billing data loads and plan === PRO
  onDismiss:     () => void
  onRefresh:     () => void
}) {
  if (status === 'upgraded' && planConfirmed) {
    return (
      <div className="flex items-start gap-3 rounded-lg border border-green-200 bg-green-50 dark:border-green-800 dark:bg-green-900/20 p-4">
        <Check size={16} className="text-green-600 dark:text-green-400 shrink-0 mt-0.5" strokeWidth={2.5} />
        <div className="flex-1">
          <p className="text-sm font-semibold text-green-800 dark:text-green-300">
            Payment received. Welcome to Pro.
          </p>
        </div>
        <button onClick={onDismiss} className="text-green-600 dark:text-green-400 hover:opacity-70 text-xs">✕</button>
      </div>
    )
  }

  if (status === 'upgraded' && !planConfirmed) {
    return (
      <div className="flex items-start gap-3 rounded-lg border border-c-border bg-white dark:bg-[#1B2838] p-4">
        <RefreshCw size={16} className="text-c-muted shrink-0 mt-0.5" />
        <div className="flex-1">
          <p className="text-sm text-[#0D1B2A] dark:text-white">Verifying payment status…</p>
          <p className="text-xs text-c-muted mt-0.5">
            If your plan has not updated after a few seconds, click refresh.
          </p>
          <button
            onClick={onRefresh}
            className="mt-1.5 flex items-center gap-1 text-xs font-medium text-c-muted hover:opacity-70"
          >
            <RefreshCw size={11} />
            Refresh plan status
          </button>
        </div>
        <button onClick={onDismiss} className="text-c-muted hover:opacity-70 text-xs">✕</button>
      </div>
    )
  }

  return (
    <div className="flex items-start gap-3 rounded-lg border border-c-border bg-white dark:bg-[#1B2838] p-4">
      <AlertCircle size={16} className="text-c-muted shrink-0 mt-0.5" />
      <div className="flex-1">
        <p className="text-sm text-[#0D1B2A] dark:text-white">Checkout was not completed.</p>
        <p className="text-xs text-c-muted mt-0.5">You can upgrade any time below.</p>
      </div>
      <button onClick={onDismiss} className="text-c-muted hover:opacity-70 text-xs">✕</button>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Subscription status banner (shown when org is suspended or subscription cancelled)
// ---------------------------------------------------------------------------

function SubscriptionStatusBanner({
  orgStatus,
  planExpiresAt,
}: {
  orgStatus:     string
  planExpiresAt: string | null
}) {
  if (orgStatus === 'SUSPENDED') {
    return (
      <div className="flex items-start gap-3 rounded-lg border border-red-200 bg-red-50 dark:border-red-800 dark:bg-red-900/20 p-4">
        <AlertCircle size={16} className="text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
        <p className="text-sm text-red-800 dark:text-red-300">
          Your subscription is on hold due to a failed payment. Please update your billing details
          through the Dodo customer portal, or contact support.
        </p>
      </div>
    )
  }

  // PRO plan but subscription cancelled — show expiry notice
  if (planExpiresAt) {
    return (
      <div className="flex items-start gap-3 rounded-lg border border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-900/20 p-4">
        <Clock size={16} className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
        <p className="text-sm text-amber-800 dark:text-amber-300">
          Your PRO subscription was cancelled and will expire on{' '}
          <span className="font-semibold">{fmtDate(planExpiresAt)}</span>.
          Resubscribe below to keep your access.
        </p>
      </div>
    )
  }

  return null
}

// ---------------------------------------------------------------------------
// Plan card
// ---------------------------------------------------------------------------

type PlanCardProps = {
  plan:              ReturnType<typeof getPlans>[number]
  isCurrent:         boolean
  isLoading:         boolean
  canUpgrade:        boolean    // false when billing is disabled or org is suspended
  subscriptionActive: boolean
  onUpgrade:         () => void
  currency:          Currency
}

function PlanCard({ plan, isCurrent, isLoading, canUpgrade, subscriptionActive, onUpgrade, currency }: PlanCardProps) {
  // Determine button label
  const buttonLabel = (() => {
    if (isCurrent && plan.id === 'PRO' && !subscriptionActive) return 'Resubscribe'
    if (isCurrent)                                              return 'Current plan'
    if (plan.id === 'STARTER')                                  return 'Free plan'
    return `Upgrade to ${plan.name}`
  })()

  const isActionable = plan.id !== 'STARTER' && (!isCurrent || (isCurrent && !subscriptionActive))
  const disabled     = !isActionable || !canUpgrade || isLoading

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
        disabled={disabled}
        onClick={isActionable && canUpgrade ? onUpgrade : undefined}
        className={[
          'w-full py-2 text-sm font-semibold rounded-lg transition-opacity',
          disabled
            ? 'border border-c-border text-c-muted cursor-default'
            : 'text-white hover:opacity-90',
          isLoading ? 'opacity-60 cursor-wait' : '',
        ].join(' ')}
        style={!disabled ? { background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' } : undefined}
      >
        {isLoading && isActionable ? 'Redirecting…' : buttonLabel}
      </button>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function BillingPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryClient                     = useQueryClient()
  const orgId                           = useAuthStore((s) => s.org?.id ?? '')

  const { data: billing, isLoading: billingLoading } = useBilling()
  const currency    = useCurrency()
  const plans       = getPlans(currency)
  const currentPlan = billing?.plan ?? 'STARTER'

  // Return-URL detection: Dodo redirects back with ?upgraded=true or ?cancelled=true
  const returnStatus = searchParams.get('upgraded') === 'true'   ? 'upgraded'
                     : searchParams.get('cancelled') === 'true'  ? 'cancelled'
                     : null

  const toast = useToast()
  const [banner, setBanner]                   = useState<'upgraded' | 'cancelled' | null>(null)
  const [checkoutLoading, setCheckoutLoading] = useState(false)
  const [showCancelConfirm, setShowCancelConfirm] = useState(false)
  const [cancelLoading,     setCancelLoading]     = useState(false)

  // On redirect back from Dodo checkout
  useEffect(() => {
    if (returnStatus) {
      setBanner(returnStatus)
      setSearchParams({}, { replace: true })
      if (returnStatus === 'upgraded') {
        queryClient.invalidateQueries({ queryKey: ['billing', orgId] })
      }
    }
  }, [returnStatus]) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleCancel() {
    if (!orgId || cancelLoading) return
    setCancelLoading(true)
    try {
      await cancelSubscription(orgId)
      setShowCancelConfirm(false)
      queryClient.invalidateQueries({ queryKey: ['billing', orgId] })
      toast.success('Subscription cancelled. You keep Pro access until the end of your billing period.')
    } catch {
      toast.error('Could not cancel subscription. Please try again.')
    } finally {
      setCancelLoading(false)
    }
  }

  async function handleUpgrade() {
    if (!orgId || checkoutLoading) return
    setCheckoutLoading(true)
    try {
      const { checkoutUrl } = await createCheckoutSession(orgId, 'PRO')
      // Full page redirect — Dodo will redirect back to return_url after payment
      window.location.href = checkoutUrl
    } catch {
      setCheckoutLoading(false)
      toast.error('Could not start checkout. Please try again.')
    }
  }

  const orgStatus         = billing?.orgStatus ?? 'ACTIVE'
  const isSuspended       = orgStatus === 'SUSPENDED'
  const hasCancelledPro   = currentPlan === 'PRO' && billing != null && !billing.subscriptionActive
  const canUpgrade        = !billingLoading && !isSuspended && !checkoutLoading
  const activeInvoices    = billing?.activeInvoiceCount ?? 0
  const atInvoiceLimit    = currentPlan === 'STARTER' && activeInvoices >= 3

  return (
    <div className="space-y-5">

      {/* Return-URL banner */}
      {banner && (
        <CheckoutBanner
          status={banner}
          planConfirmed={currentPlan === 'PRO'}
          onDismiss={() => setBanner(null)}
          onRefresh={() => queryClient.invalidateQueries({ queryKey: ['billing', orgId] })}
        />
      )}

      {/* Subscription status banner (on_hold or cancelled-but-active) */}
      {billing && (isSuspended || hasCancelledPro) && (
        <SubscriptionStatusBanner
          orgStatus={orgStatus}
          planExpiresAt={billing.planExpiresAt ?? null}
        />
      )}

      {/* STARTER invoice usage bar */}
      {currentPlan === 'STARTER' && !billingLoading && (
        <div className="rounded-lg border border-c-border bg-white dark:bg-[#1B2838] p-4">
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs font-medium text-[#0D1B2A] dark:text-white">Active invoices</p>
            <p className={`text-xs font-semibold ${atInvoiceLimit ? 'text-amber-500 dark:text-amber-400' : 'text-c-muted'}`}>
              {activeInvoices} / 3
            </p>
          </div>
          <div className="h-1.5 rounded-full bg-[#F4F7F9] dark:bg-white/10 overflow-hidden">
            <div
              className={`h-full rounded-full transition-all ${atInvoiceLimit ? 'bg-amber-400' : 'bg-[#29B6F6]'}`}
              style={{ width: `${Math.min((activeInvoices / 3) * 100, 100)}%` }}
            />
          </div>
          {atInvoiceLimit && (
            <p className="text-xs text-amber-600 dark:text-amber-400 mt-1.5">
              Limit reached. Upgrade to Pro for unlimited invoices.
            </p>
          )}
        </div>
      )}

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
            <PlanCard
              key={plan.id}
              plan={plan}
              isCurrent={currentPlan === plan.id}
              isLoading={checkoutLoading}
              canUpgrade={canUpgrade}
              subscriptionActive={billing?.subscriptionActive ?? false}
              onUpgrade={handleUpgrade}
              currency={currency}
            />
          ))}
        </div>

        <p className="text-xs text-c-muted mt-4 text-center max-w-2xl mx-auto">
          Founding member pricing, locked forever. Raises after the first 200 users.
        </p>
      </div>

      {/* Cancel subscription — only shown when PRO and actively renewing */}
      {currentPlan === 'PRO' && billing?.subscriptionActive && !isSuspended && (
        <div className="pt-1">
          {!showCancelConfirm ? (
            <button
              onClick={() => setShowCancelConfirm(true)}
              className="text-xs text-c-muted hover:text-red-500 transition-colors"
            >
              Cancel subscription
            </button>
          ) : (
            <div className="rounded-xl border border-c-border bg-white dark:bg-[#1B2838] p-4 space-y-3 max-w-2xl">
              <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Cancel your subscription?</p>
              <p className="text-xs text-c-muted leading-relaxed">
                You'll keep Pro access until{' '}
                <span className="font-medium text-[#0D1B2A] dark:text-white">
                  {billing.planExpiresAt ? fmtDate(billing.planExpiresAt) : 'the end of your billing period'}
                </span>
                . After that your account moves to the free plan. You can resubscribe any time.
              </p>
              <div className="flex items-center gap-3">
                <button
                  onClick={handleCancel}
                  disabled={cancelLoading}
                  className="px-3 py-1.5 rounded-lg text-xs font-semibold text-white bg-red-500 hover:bg-red-600 disabled:opacity-50 transition-colors"
                >
                  {cancelLoading ? 'Cancelling…' : 'Yes, cancel'}
                </button>
                <button
                  onClick={() => setShowCancelConfirm(false)}
                  disabled={cancelLoading}
                  className="text-xs text-c-muted hover:opacity-70 transition-opacity disabled:opacity-30"
                >
                  Keep subscription
                </button>
              </div>
            </div>
          )}
        </div>
      )}

    </div>
  )
}
