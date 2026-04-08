import { Link } from 'react-router-dom'
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
        Built for freelancers · India &amp; US
      </div>

      {/* Headline */}
      <h1 className="animate-fade-up animate-fade-up-delay-1 text-5xl lg:text-6xl font-bold leading-tight tracking-tight max-w-3xl">
        <span className="text-white">We chase your clients</span>
        <br />
        <span className="text-[#29B6F6]">until you get paid.</span>
      </h1>

      {/* Sub-headline */}
      <p className="animate-fade-up animate-fade-up-delay-2 mt-6 text-lg text-[#8A9BAE] max-w-xl leading-relaxed">
        Stop awkward follow-up messages. FlowCollect sends automated payment
        reminders via email &amp; SMS — so you don't have to.
      </p>

      {/* CTA buttons */}
      <div className="animate-fade-up animate-fade-up-delay-3 mt-8 flex flex-col sm:flex-row items-center gap-3">
        <Link
          to="/register"
          className="px-8 py-3.5 rounded-xl text-white font-semibold text-base hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-[#29B6F6]/25"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          Get started free →
        </Link>
        <a
          href="#how-it-works"
          className="px-8 py-3.5 rounded-xl border border-white/20 text-white/80 hover:text-white hover:border-white/40 transition-colors font-medium text-base"
        >
          See how it works
        </a>
      </div>

      {/* Micro trust line */}
      <p className="mt-4 text-sm text-[#4E6478]">
        No credit card required · Cancel anytime · Free to start
      </p>

      {/* Fake app mockup */}
      <div className="mt-14 w-full max-w-2xl rounded-2xl border border-white/10 bg-[#1B2838] shadow-2xl shadow-black/40 overflow-hidden">
        {/* macOS title bar */}
        <div className="h-9 bg-[#243447] flex items-center gap-2 px-4 border-b border-white/5">
          <span className="w-3 h-3 rounded-full bg-red-400/70" />
          <span className="w-3 h-3 rounded-full bg-amber-400/70" />
          <span className="w-3 h-3 rounded-full bg-green-400/70" />
          <span className="ml-3 text-xs text-white/30 font-mono">flowcollect.app</span>
        </div>

        {/* Invoice rows */}
        <div className="p-4 space-y-3">
          <InvoiceRow client="Acme Design Co." amount="₹45,000" days={12} status="overdue" />
          <InvoiceRow client="Rajesh Consulting" amount="$2,400" days={7} status="overdue" />
          <InvoiceRow client="Studio Bloom" amount="₹28,500" days={0} status="paid" />
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
    <div className="flex items-center justify-between bg-white/5 rounded-lg px-4 py-3">
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded-full bg-[#2E7A8E] flex items-center justify-center text-xs font-bold text-white">
          {client[0]}
        </div>
        <div className="text-left">
          <p className="text-sm font-medium text-white">{client}</p>
          <p className="text-xs text-[#4E6478]">{amount}</p>
        </div>
      </div>
      {status === 'paid' ? (
        <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-green-500/15 text-green-400">
          PAID
        </span>
      ) : (
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-red-500/15 text-red-400">
            {days}d overdue
          </span>
          <span className="text-xs px-2.5 py-1 rounded-full bg-[#2E7A8E]/20 text-[#4FC3F7] font-medium">
            Reminder sent ✓
          </span>
        </div>
      )}
    </div>
  )
}
