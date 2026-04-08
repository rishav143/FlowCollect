import { MessageSquare, UserX, CalendarClock } from 'lucide-react'
import ScrollReveal from './ScrollReveal'

export default function PainSection() {
  return (
    <section className="bg-white py-20 px-4">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <ScrollReveal>
          <div className="text-center mb-12">
            <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
              The problem
            </p>
            <h2 className="text-3xl font-bold text-[#0D1B2A]">Sound familiar?</h2>
            <p className="text-c-muted text-base mt-2">Every freelancer has been here.</p>
          </div>
        </ScrollReveal>

        {/* Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
          <ScrollReveal delay={0}>
            <PainCard
              icon={<MessageSquare size={22} className="text-amber-500" />}
              iconBg="bg-amber-50"
              title="The awkward follow-up"
              quote='"Hey… just following up on invoice #47. Again. Sorry to bother you."'
              body="You've done the work. You deserve to be paid. But chasing money feels embarrassing."
            />
          </ScrollReveal>
          <ScrollReveal delay={80}>
            <PainCard
              icon={<UserX size={22} className="text-red-500" />}
              iconBg="bg-red-50"
              title="The ghost client"
              quote="Seen ✓✓"
              quoteStyle="text-2xl font-bold text-[#0D1B2A] mb-2"
              body="They read it. Three days ago. Still no payment. No reply. Just silence."
            />
          </ScrollReveal>
          <ScrollReveal delay={160}>
            <PainCard
              icon={<CalendarClock size={22} className="text-violet-500" />}
              iconBg="bg-violet-50"
              title="The mental load"
              quote='"Who owes me? Did I follow up on that one? Was it 30 or 45 days?"'
              body="Tracking overdue invoices in your head while trying to actually do your work."
            />
          </ScrollReveal>
        </div>
      </div>
    </section>
  )
}

function PainCard({
  icon,
  iconBg,
  title,
  quote,
  quoteStyle,
  body,
}: {
  icon: React.ReactNode
  iconBg: string
  title: string
  quote: string
  quoteStyle?: string
  body: string
}) {
  return (
    <div className="h-full rounded-xl border border-c-border p-6 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
      <div className={`w-10 h-10 rounded-xl ${iconBg} flex items-center justify-center mb-4`}>
        {icon}
      </div>
      <h3 className="text-base font-bold text-[#0D1B2A] mb-3">{title}</h3>
      <p className={quoteStyle ?? 'text-sm text-[#0D1B2A] italic mb-2'}>{quote}</p>
      <p className="text-sm text-c-muted leading-relaxed">{body}</p>
    </div>
  )
}
