import { Link } from 'react-router-dom'
import { Check } from 'lucide-react'
import { type Region } from '../hooks/useGeoRegion'
import ScrollReveal from './ScrollReveal'

interface Plan {
  name: string
  price: string
  period: string
  description: string
  cta: string
  ctaTo: string
  highlight: boolean
  features: string[]
}

function getPlans(region: Region): Plan[] {
  const isIndia = region === 'IN'

  return [
    {
      name: 'Solo',
      price: isIndia ? '₹0' : '$0',
      period: '/mo',
      description: 'Perfect for solo freelancers just getting started.',
      cta: 'Get started free',
      ctaTo: '/register',
      highlight: false,
      features: [
        'Up to 10 active invoices',
        'Automated email reminders',
        'Client payment portal',
        'Basic follow-up templates',
        'Invoice approval workflows',
      ],
    },
    {
      name: 'Pro',
      price: isIndia ? '₹599' : '$14',
      period: '/mo',
      description: 'For freelancers serious about getting paid on time.',
      cta: 'Start free trial',
      ctaTo: '/register',
      highlight: true,
      features: [
        'Unlimited invoices',
        'Email + SMS reminders',
        'AI payment insights',
        'Trackable follow-up links',
        'Custom reminder schedules',
        'Priority support',
      ],
    },
  ]
}

export default function PricingSection({ region }: { region: Region }) {
  const plans = getPlans(region)
  const isLoading = region === 'loading'

  return (
    <section id="pricing" className="bg-white py-20 px-4">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <ScrollReveal>
          <div className="text-center mb-14">
            <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
              Pricing
            </p>
            <h2 className="text-3xl font-bold text-[#0D1B2A]">Simple, honest pricing</h2>
            <p className="text-c-muted text-base mt-2">
              {isLoading
                ? 'Loading pricing for your region…'
                : region === 'IN'
                  ? 'Prices in Indian Rupees (₹) · Billed monthly'
                  : 'Prices in USD ($) · Billed monthly'}
            </p>
          </div>
        </ScrollReveal>

        {/* Plans — 2 cards, centered */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-2xl mx-auto">
          {plans.map((plan, i) => (
            <ScrollReveal key={plan.name} delay={i * 80} className="h-full">
              <div
                className={`h-full flex flex-col rounded-2xl border p-7 transition-all duration-200 ${
                  plan.highlight
                    ? 'border-[#29B6F6] shadow-lg shadow-[#29B6F6]/10 bg-white relative'
                    : 'border-c-border bg-[#F4F7F9] shadow-sm hover:shadow-md hover:-translate-y-0.5'
                }`}
              >
                {plan.highlight && (
                  <div className="absolute -top-3.5 left-1/2 -translate-x-1/2">
                    <span
                      className="px-3 py-1 rounded-full text-xs font-semibold text-white whitespace-nowrap"
                      style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
                    >
                      Most popular
                    </span>
                  </div>
                )}

                <div className="flex-1">
                  <p className="text-sm font-semibold text-c-muted uppercase tracking-wide mb-1">
                    {plan.name}
                  </p>
                  <div className="flex items-end gap-0.5 mb-1">
                    <span
                      className={`text-4xl font-black text-[#0D1B2A] transition-all ${isLoading ? 'opacity-30 blur-sm' : ''}`}
                    >
                      {plan.price}
                    </span>
                    <span className="text-sm text-c-muted mb-1.5">{plan.period}</span>
                  </div>
                  <p className="text-sm text-c-muted mb-6">{plan.description}</p>

                  <ul className="space-y-2.5 mb-8">
                    {plan.features.map((f) => (
                      <li key={f} className="flex items-start gap-2 text-sm text-[#0D1B2A]">
                        <Check size={15} className="text-[#29B6F6] shrink-0 mt-0.5" />
                        {f}
                      </li>
                    ))}
                  </ul>
                </div>

                <Link
                  to={plan.ctaTo}
                  className={`block w-full text-center py-3 rounded-xl font-semibold text-sm transition-all active:scale-95 ${
                    plan.highlight
                      ? 'text-white hover:opacity-90 shadow-md shadow-[#29B6F6]/20'
                      : 'border border-c-border text-[#0D1B2A] hover:border-[#29B6F6] hover:text-[#29B6F6] bg-white'
                  }`}
                  style={
                    plan.highlight
                      ? { background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }
                      : undefined
                  }
                >
                  {plan.cta}
                </Link>
              </div>
            </ScrollReveal>
          ))}
        </div>

        {/* Footer note */}
        <ScrollReveal delay={160}>
          <p className="text-center text-xs text-c-muted mt-8">
            Pro plan includes a 14-day free trial. No credit card required to start.
          </p>
        </ScrollReveal>
      </div>
    </section>
  )
}
