import { Zap } from 'lucide-react'

export default function HeroSection() {
  return (
    <section
      className="min-h-[88vh] flex flex-col items-center justify-center px-4 py-20 text-center"
      style={{ background: 'linear-gradient(160deg, #0D1B2A 0%, #122436 60%, #1B2838 100%)' }}
    >
      {/* Trust badge */}
      <div className="animate-fade-up inline-flex items-center gap-2 rounded-full border border-[#29B6F6]/30 bg-[#29B6F6]/10 px-3 py-1 text-xs text-[#4FC3F7] mb-6">
        <Zap size={12} />
        Built for freelancers & small businesses
      </div>

      {/* Headline */}
      <h1 className="animate-fade-up animate-fade-up-delay-1 text-5xl lg:text-6xl font-bold leading-tight tracking-tight max-w-3xl">
        <span className="text-white">We chase your clients</span>
        <br />
        <span className="text-[#29B6F6]">until you get paid.</span>
      </h1>

      {/* Sub-headline — outcome first, not feature list */}
      <p className="animate-fade-up animate-fade-up-delay-2 mt-6 text-lg text-[#8A9BAE] max-w-xl leading-relaxed">
        Your work is done. The follow-ups don't have to be.
        Automated email reminders - until you're paid.
      </p>

      {/* CTA buttons */}
      <div className="animate-fade-up animate-fade-up-delay-3 mt-8 flex flex-col sm:flex-row items-center gap-3">
        <a
          href={`${import.meta.env.VITE_APP_URL}/register`}
          className="px-8 py-3.5 rounded-xl text-white font-semibold text-base hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-[#29B6F6]/25"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          Get started free →
        </a>
        <a
          href="#how-it-works"
          className="px-8 py-3.5 rounded-xl border border-white/20 text-white/80 hover:text-white hover:border-white/40 transition-colors font-medium text-base"
        >
          See how it works
        </a>
      </div>

      {/* Micro trust line */}
      <p className="mt-4 text-sm text-[#4E6478]">
        No credit card · Cancel anytime · Live in 2 minutes
      </p>

      {/* App mockup */}
      <div className="mt-14 w-full max-w-2xl rounded-2xl border border-white/10 bg-[#1B2838] shadow-2xl shadow-black/40 overflow-hidden">
        {/* macOS title bar */}
        <div className="h-9 bg-[#243447] flex items-center gap-2 px-4 border-b border-white/5">
          <span className="w-3 h-3 rounded-full bg-red-400/70" />
          <span className="w-3 h-3 rounded-full bg-amber-400/70" />
          <span className="w-3 h-3 rounded-full bg-green-400/70" />
          <span className="ml-3 text-xs text-white/30 font-mono">app.flowcollect.io</span>
        </div>

        {/* Invoice rows */}
        <div className="px-4 pt-4 pb-2 space-y-0 divide-y divide-white/5">
          <InvoiceRow client="Acme Design Co."    amount="₹45,000" days={12} status="overdue" />
          <InvoiceRow client="Carter & Webb Ltd"   amount="£2,400"  days={7}  status="overdue" />
          <InvoiceRow client="Studio Bloom"       amount="₹28,500" days={0}  status="paid"    />
        </div>

        {/* Email notification bubble */}
        <div className="mx-4 mb-4 flex items-start gap-2.5 bg-[#29B6F6]/10 border border-[#29B6F6]/25 rounded-xl px-4 py-3">
          <div className="w-6 h-6 rounded-full bg-[#29B6F6] flex items-center justify-center shrink-0 mt-0.5">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="2" y="4" width="20" height="16" rx="2"/>
              <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
            </svg>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-semibold text-[#29B6F6] mb-0.5">FlowCollect · Email Reminder</p>
            <p className="text-xs text-white/70 leading-relaxed">
              "Hi James, Invoice #INV-042 for £2,400 is now 7 days overdue. Could you let me know when to expect payment?"
            </p>
          </div>
          <span className="text-[10px] text-[#4E6478] shrink-0 mt-0.5">Just now</span>
        </div>

        {/* Bottom bar */}
        <div className="px-4 py-3 bg-[#243447]/60 border-t border-white/5 flex items-center justify-between">
          <span className="text-xs text-[#4E6478]">FlowCollect is chasing 2 invoices for you</span>
          <span className="text-xs text-green-400 font-medium">● Active</span>
        </div>
      </div>
    </section>
  )
}

function InvoiceRow({
  client,
  amount,
  days,
  status,
}: {
  client: string
  amount: string
  days: number
  status: 'overdue' | 'paid'
}) {
  return (
    <div className="flex items-center justify-between py-3.5">
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded-full bg-[#2E7A8E] flex items-center justify-center text-xs font-bold text-white shrink-0">
          {client[0]}
        </div>
        <div className="text-left">
          <p className="text-sm font-medium text-white">{client}</p>
          <p className="text-xs text-[#4E6478] mt-0.5">{amount}</p>
        </div>
      </div>
      <div className="flex items-center gap-3 shrink-0">
        {status === 'paid' ? (
          <span className="text-xs font-semibold text-green-400">Paid</span>
        ) : (
          <>
            <span className="text-xs font-semibold text-red-400">{days}d overdue</span>
            <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-[#29B6F6]/15 text-[#4FC3F7]">
              Chasing ✓
            </span>
          </>
        )}
      </div>
    </div>
  )
}
