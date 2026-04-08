import { Bell, Sparkles, FileText, CheckSquare, Link2, Globe } from 'lucide-react'
import ScrollReveal from './ScrollReveal'

const features = [
  {
    icon: <Bell size={22} className="text-[#29B6F6]" />,
    title: 'Automated Reminders',
    body: 'Send email and SMS follow-ups on your schedule — Day 1, Day 7, Day 14. Set once and never think about it again.',
  },
  {
    icon: <Sparkles size={22} className="text-violet-400" />,
    title: 'AI Payment Insights',
    body: 'Know which clients are likely to delay before they do. FlowCollect learns your patterns and flags risky invoices early.',
  },
  {
    icon: <FileText size={22} className="text-amber-400" />,
    title: 'Follow-up Templates',
    body: 'Professional, tone-matched message templates. Add trackable links so you know the moment your client opens the message.',
  },
  {
    icon: <CheckSquare size={22} className="text-green-400" />,
    title: 'Approval Workflows',
    body: 'Let clients approve invoices digitally before the due date. Reduces disputes, speeds up payment.',
  },
  {
    icon: <Link2 size={22} className="text-[#4FC3F7]" />,
    title: 'One-click Payment Links',
    body: 'Clients pay directly from the reminder email — no account, no friction. Supports UPI, cards, and international transfers.',
  },
  {
    icon: <Globe size={22} className="text-rose-400" />,
    title: 'Multi-currency Support',
    body: 'Invoice in INR, USD, or any currency. FlowCollect handles the formatting — you just focus on the work.',
  },
]

export default function FeaturesSection() {
  return (
    <section id="features" className="bg-[#F4F7F9] py-20 px-4">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <ScrollReveal>
          <div className="text-center mb-14">
            <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
              Everything you need
            </p>
            <h2 className="text-3xl font-bold text-[#0D1B2A]">
              Built for freelancers who hate chasing money
            </h2>
            <p className="text-c-muted text-base mt-2 max-w-xl mx-auto">
              Every feature is designed to get you paid faster — without the awkward conversations.
            </p>
          </div>
        </ScrollReveal>

        {/* Grid — 5 cards, last row centered */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((f, i) => (
            <ScrollReveal key={f.title} delay={i * 60}>
              <div className="h-full rounded-xl border border-c-border bg-white p-6 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
                <div className="w-10 h-10 rounded-xl bg-[#F4F7F9] flex items-center justify-center mb-4">
                  {f.icon}
                </div>
                <h3 className="text-base font-bold text-[#0D1B2A] mb-2">{f.title}</h3>
                <p className="text-sm text-c-muted leading-relaxed">{f.body}</p>
              </div>
            </ScrollReveal>
          ))}
        </div>
      </div>
    </section>
  )
}
