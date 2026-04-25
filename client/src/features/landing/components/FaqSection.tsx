import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import ScrollReveal from './ScrollReveal'

const faqs = [
  {
    q: 'Will my client know the reminders are automated?',
    a: 'Reminders are sent professionally on your behalf via FlowCollect. The tone, schedule, and content are entirely yours, so every message still sounds like you, not a robot.',
  },
  {
    q: 'What if my client gets annoyed by the reminders?',
    a: 'You control the tone and timing. Start with a gentle nudge and escalate only if the invoice remains overdue. Most clients simply forget - a polite reminder is appreciated, not annoying.',
  },
  {
    q: 'Does it work for international clients?',
    a: 'Yes. Email reminders work for any client, anywhere in the world.',
  },
  {
    q: 'I already use another invoicing tool. Do I need to switch?',
    a: "FlowCollect handles the collection side: reminders, follow-ups, and payment confirmations. Create your invoice here or just use it alongside your existing tool. No migration needed.",
  },
  {
    q: 'Is my data safe?',
    a: 'Yes. Your data is encrypted in transit and at rest. We never store payment card details or sell your data. AI features process data through trusted providers under strict confidentiality agreements.',
  },
  {
    q: 'What happens when I reach the free plan limit?',
    a: 'The Free plan supports up to 3 active invoices. Once you reach that limit, you can upgrade to Pro for unlimited invoices. There are no automatic charges - you choose when to upgrade.',
  },
]

function FaqItem({ q, a }: { q: string; a: string }) {
  const [open, setOpen] = useState(false)

  return (
    <div className="border-b border-c-border last:border-0">
      <button
        className="w-full flex items-center justify-between gap-4 py-5 text-left"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
      >
        <span className="text-sm font-semibold text-[#0D1B2A]">{q}</span>
        <ChevronDown
          size={16}
          className={`shrink-0 text-c-muted transition-transform duration-200 ${open ? 'rotate-180' : ''}`}
        />
      </button>
      {open && (
        <p className="text-sm text-c-muted leading-relaxed pb-5">{a}</p>
      )}
    </div>
  )
}

export default function FaqSection() {
  return (
    <section id="faq" className="bg-[#F4F7F9] py-12 sm:py-20 px-4">
      <div className="max-w-2xl mx-auto">
        <ScrollReveal>
          <div className="text-center mb-12">
            <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
              FAQ
            </p>
            <h2 className="text-2xl sm:text-3xl font-bold text-[#0D1B2A]">Everything you're wondering about</h2>
          </div>
        </ScrollReveal>

        <ScrollReveal delay={60}>
          <div className="rounded-2xl border border-c-border bg-white px-6">
            {faqs.map((faq) => (
              <FaqItem key={faq.q} q={faq.q} a={faq.a} />
            ))}
          </div>
        </ScrollReveal>
      </div>
    </section>
  )
}
