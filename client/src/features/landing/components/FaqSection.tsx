import { useState } from 'react'
import { ChevronDown } from 'lucide-react'
import ScrollReveal from './ScrollReveal'

const faqs = [
  {
    q: 'Will my client know the reminders are automated?',
    a: 'No. Reminders go out from your name and email address. To your client it looks like a personal message — because the tone and schedule are yours. FlowCollect stays invisible.',
  },
  {
    q: 'What if my client gets annoyed by the reminders?',
    a: 'You control the tone and timing entirely. Start with a gentle nudge, escalate only if the invoice stays overdue. Most clients simply forgot — a polite reminder is welcome, not annoying.',
  },
  {
    q: 'Does it work for international clients?',
    a: 'Yes. Email reminders work for any client anywhere in the world. SMS reminders work for India and US phone numbers. More regions coming soon.',
  },
  {
    q: 'I already use another invoicing tool. Do I need to switch?',
    a: "FlowCollect doesn't replace your invoicing tool — it handles the collection side. Add your invoice details and we take over from there. No migration needed.",
  },
  {
    q: 'Is my data safe?',
    a: 'We do not store any payment card details. Client data is encrypted in transit and at rest. We will never sell or share your data with third parties.',
  },
  {
    q: 'What happens after the free plan limit?',
    a: 'The Solo plan supports up to 10 active invoices at a time. Once you hit that, you can upgrade to Pro for unlimited invoices. No automatic charges — you choose when to upgrade.',
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
    <section className="bg-[#F4F7F9] py-20 px-4">
      <div className="max-w-2xl mx-auto">
        <ScrollReveal>
          <div className="text-center mb-12">
            <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
              FAQ
            </p>
            <h2 className="text-3xl font-bold text-[#0D1B2A]">Common questions</h2>
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
