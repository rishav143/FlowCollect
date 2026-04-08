import ScrollReveal from './ScrollReveal'

const stats = [
  {
    figure: '14%',
    label: 'of freelancer revenue is lost to late or unpaid invoices every year',
    source: 'FreshBooks, 2023',
  },
  {
    figure: '29 days',
    label: 'is the average time freelancers wait past the due date to get paid',
    source: 'AND.CO Freelance Report',
  },
  {
    figure: '3 in 4',
    label: 'freelancers have had a client go silent after work was delivered',
    source: 'Payoneer Global Survey',
  },
]

export default function SocialProofSection() {
  return (
    <section id="testimonials" className="bg-[#F4F7F9] py-20 px-4">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <ScrollReveal>
          <div className="text-center mb-14">
            <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
              The reality
            </p>
            <h2 className="text-3xl font-bold text-[#0D1B2A]">
              The late payment problem is massive — and mostly ignored
            </h2>
            <p className="text-c-muted text-base mt-2 max-w-xl mx-auto">
              These aren't edge cases. This is what freelancing actually looks like.
            </p>
          </div>
        </ScrollReveal>

        {/* Stat cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-14">
          {stats.map((s, i) => (
            <ScrollReveal key={s.figure} delay={i * 80}>
              <div className="h-full rounded-xl border border-c-border bg-white p-7 shadow-sm text-center">
                <p
                  className="text-5xl font-black mb-3"
                  style={{
                    background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)',
                    WebkitBackgroundClip: 'text',
                    WebkitTextFillColor: 'transparent',
                  }}
                >
                  {s.figure}
                </p>
                <p className="text-sm text-[#0D1B2A] leading-relaxed mb-3">{s.label}</p>
                <p className="text-xs text-c-muted">{s.source}</p>
              </div>
            </ScrollReveal>
          ))}
        </div>

        {/* Early access callout */}
        <ScrollReveal delay={240}>
          <div className="rounded-2xl border border-[#29B6F6]/30 bg-[#29B6F6]/5 px-8 py-8 text-center max-w-2xl mx-auto">
            <p className="text-sm font-semibold text-[#29B6F6] uppercase tracking-widest mb-2">
              Early access
            </p>
            <h3 className="text-xl font-bold text-[#0D1B2A] mb-2">
              We're onboarding our first users now
            </h3>
            <p className="text-sm text-c-muted leading-relaxed">
              Sign up free and get direct access to the founders. We personally respond to every
              piece of feedback in the first month.
            </p>
          </div>
        </ScrollReveal>
      </div>
    </section>
  )
}
