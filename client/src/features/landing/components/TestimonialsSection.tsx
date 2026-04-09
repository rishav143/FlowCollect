import { Star } from 'lucide-react'
import ScrollReveal from './ScrollReveal'

/* TODO: Replace with real user quotes before public launch */
const testimonials = [
  {
    quote:
      'I used to feel embarrassed chasing clients for money. Now FlowCollect does it for me — professionally and automatically. ₹2L recovered in my first month.',
    name: 'Priya S.',
    role: 'Freelance Designer · Bangalore',
    initials: 'PS',
  },
  {
    quote:
      'Our average collection time dropped from 45 days to 12 days. The ROI on a free plan is insane. Every agency needs this.',
    name: 'Marcus T.',
    role: 'Agency Owner · Austin, TX',
    initials: 'MT',
  },
  {
    quote:
      'The reminder feature is a game-changer for Indian clients — they actually respond. I stopped dreading payment week.',
    name: 'Riya M.',
    role: 'Independent Consultant · Mumbai',
    initials: 'RM',
  },
]

export default function TestimonialsSection() {
  return (
    <section id="testimonials" className="bg-[#F4F7F9] py-20 px-4">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <ScrollReveal>
          <div className="text-center mb-12">
            <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
              Early users
            </p>
            <h2 className="text-3xl font-bold text-[#0D1B2A]">What early users say</h2>
          </div>
        </ScrollReveal>

        {/* Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {testimonials.map((t, i) => (
            <ScrollReveal key={t.name} delay={i * 80}>
              <div className="h-full rounded-xl border border-c-border bg-white p-6 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200">
                {/* Stars */}
                <div className="flex gap-0.5 mb-4">
                  {Array.from({ length: 5 }).map((_, j) => (
                    <Star key={j} size={14} className="text-amber-400 fill-amber-400" />
                  ))}
                </div>

                {/* Quote */}
                <p className="text-sm text-[#0D1B2A] leading-relaxed italic mb-5">"{t.quote}"</p>

                {/* Author */}
                <div className="flex items-center gap-3 mt-auto">
                  <div className="w-9 h-9 rounded-full bg-[#2E7A8E] flex items-center justify-center text-xs font-bold text-white shrink-0">
                    {t.initials}
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-[#0D1B2A]">{t.name}</p>
                    <p className="text-xs text-c-muted">{t.role}</p>
                  </div>
                </div>
              </div>
            </ScrollReveal>
          ))}
        </div>
      </div>
    </section>
  )
}
