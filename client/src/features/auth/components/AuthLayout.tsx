import type { ReactNode } from 'react'
import { CheckCircle2 } from 'lucide-react'
import Logo from '@/ui/components/Logo/Logo'

const VALUE_PROPS = [
  'Smart invoice management for freelancers',
  'Automated reminders — SMS, WhatsApp & email',
  'Real-time payment tracking & insights',
]

/**
 * AuthLayout
 *
 * Split-screen layout for Login and Register pages.
 *
 * Desktop (≥ lg)
 *  ┌────────────────────┬──────────────────────────┐
 *  │ Brand panel        │  Form content            │
 *  │ Dark gradient      │  Light/dark card         │
 *  └────────────────────┴──────────────────────────┘
 *
 * Mobile
 *  ┌──────────────────────────────────────────────┐
 *  │  Logo                                        │
 *  │  Form card (white / dark card centered)      │
 *  └──────────────────────────────────────────────┘
 */
export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen flex bg-[#F4F7F9] dark:bg-[#0D1B2A]">

      {/* ── Left brand panel — desktop only ─────────────────────────────── */}
      <div
        className="hidden lg:flex w-[480px] shrink-0 flex-col justify-between p-16"
        style={{ background: 'linear-gradient(160deg, #0D1B2A 0%, #122436 50%, #0D1B2A 100%)' }}
      >
        {/* Logo */}
        <Logo variant="light" size="lg" />

        {/* Headline + value props */}
        <div>
          <h2 className="text-4xl font-bold text-white leading-snug mb-4">
            Get paid faster.<br />
            <span className="text-[#29B6F6]">Stress less.</span>
          </h2>
          <p className="text-[#8A9BAE] text-base mb-8">
            The smart invoicing platform built for freelancers &amp; agencies worldwide.
          </p>
          <ul className="space-y-3.5">
            {VALUE_PROPS.map((p) => (
              <li key={p} className="flex items-start gap-3">
                <CheckCircle2 size={16} className="text-[#29B6F6] shrink-0 mt-0.5" strokeWidth={2.5} />
                <span className="text-[#8A9BAE] text-sm leading-relaxed">{p}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Footer tagline */}
        <p className="text-xs text-[#4E6478]">
          Trusted by freelancers &amp; small businesses around the world.
        </p>
      </div>

      {/* ── Right form area ──────────────────────────────────────────────── */}
      <div className="flex-1 flex flex-col items-center justify-center px-5 py-12">

        {/* Mobile logo — hidden on desktop since left panel shows it */}
        <div className="lg:hidden mb-8">
          <Logo size="lg" />
        </div>

        {/* Form card */}
        <div className="w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl border border-c-border shadow-sm p-8">
          {children}
        </div>
      </div>

    </div>
  )
}
