import ScrollReveal from './ScrollReveal'

const stats = [
  {
    value: '87%',
    label: 'of freelancers experience late payments every year',
    sub:   'Federation of Small Businesses',
  },
  {
    value: '20 days',
    label: 'spent annually chasing invoices — instead of doing real work',
    sub:   'Freelancer.com Research',
  },
  {
    value: '8+ days',
    label: 'is how long the average invoice sits unpaid past its due date',
    sub:   'Xero Global Small Business Insights',
  },
]

export default function TestimonialsSection() {
  return (
    <section id="testimonials" className="bg-[#F4F7F9] py-12 sm:py-20 px-4">
      <div className="max-w-6xl mx-auto">
        <ScrollReveal>
          <div className="text-center mb-12">
            <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
              The reality
            </p>
            <h2 className="text-3xl font-bold text-[#0D1B2A]">
              Late payments are a freelancer epidemic
            </h2>
            <p className="text-c-muted text-base mt-2 max-w-xl mx-auto">
              You're not alone. And you shouldn't have to keep chasing.
            </p>
          </div>
        </ScrollReveal>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {stats.map((s, i) => (
            <ScrollReveal key={s.value} delay={i * 80}>
              <div className="h-full rounded-xl border border-c-border bg-white p-5 sm:p-8 shadow-sm text-center">
                <p className="text-4xl sm:text-5xl font-black text-[#29B6F6] mb-3">{s.value}</p>
                <p className="text-sm font-medium text-[#0D1B2A] leading-relaxed mb-3">{s.label}</p>
                <p className="text-xs text-c-muted">{s.sub}</p>
              </div>
            </ScrollReveal>
          ))}
        </div>
      </div>
    </section>
  )
}
