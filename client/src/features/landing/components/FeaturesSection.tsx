import { Bell, Sparkles, FileText, CheckSquare, Globe, ReceiptText } from 'lucide-react'

const features = [
  {
    icon: <ReceiptText size={22} className="text-[#29B6F6]" />,
    title: 'Invoice Management',
    body: 'Create and send professional invoices in minutes - then let FlowCollect handle the rest.',
  },
  {
    icon: <Bell size={22} className="text-[#29B6F6]" />,
    title: 'Automated Reminders',
    body: 'Automated email follow-ups on your schedule. Set once and never think about it again.',
  },
  {
    icon: <Sparkles size={22} className="text-violet-400" />,
    title: 'AI Payment Insights',
    body: 'Spots payment patterns across your invoices and flags risks before they become problems.',
  },
  {
    icon: <FileText size={22} className="text-amber-400" />,
    title: 'Follow-up Templates',
    body: 'Your tone, your words. Craft the exact message your clients receive — professional every time.',
  },
  {
    icon: <CheckSquare size={22} className="text-green-400" />,
    title: 'Approval Workflows',
    body: 'Clients confirm payment with one tap. No disputes. No "I already sent it" surprises.',
  },
  {
    icon: <Globe size={22} className="text-rose-400" />,
    title: 'Multi-currency Support',
    body: 'USD, EUR, GBP, AUD, SGD, and more. We handle the formatting - you focus on the work.',
  },
]

export default function FeaturesSection() {
  return (
    <section id="features" className="bg-[#F4F7F9] py-20 px-4">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-14">
          <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
            What you get
          </p>
          <h2 className="text-3xl font-bold text-[#0D1B2A]">
            Built for freelancers who hate chasing money
          </h2>
          <p className="text-c-muted text-base mt-2 max-w-xl mx-auto">
            Every feature is designed to get you paid faster - without the awkward conversations.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((f) => (
            <div
              key={f.title}
              className="h-full rounded-xl border border-c-border bg-white p-6 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200"
            >
              <div className="w-10 h-10 rounded-xl bg-[#F4F7F9] flex items-center justify-center mb-4">
                {f.icon}
              </div>
              <h3 className="text-base font-bold text-[#0D1B2A] mb-2">{f.title}</h3>
              <p className="text-sm text-c-muted leading-relaxed">{f.body}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
