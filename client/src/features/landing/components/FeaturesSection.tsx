import { Mail, Phone, MessageSquare, Sparkles, ExternalLink, BadgeCheck } from 'lucide-react'

// ─── ReminderMockup — matches FollowupsTab exactly ───────────────────────────
function ReminderMockup() {
  const rows = [
    { channel: 'Email',     icon: <Mail size={14} />,           day: 'Day 1',  status: 'Sent',      statusCls: 'text-green-600' },
    { channel: 'SMS',       icon: <Phone size={14} />,          day: 'Day 7',  status: 'Sent',      statusCls: 'text-green-600' },
    { channel: 'WhatsApp',  icon: <MessageSquare size={14} />,  day: 'Day 14', status: 'Scheduled', statusCls: 'text-amber-600' },
  ]
  return (
    <div className="mt-4 rounded-xl border border-c-border bg-white overflow-hidden">
      {rows.map((r, i) => (
        <div
          key={r.channel}
          className={`flex items-center gap-3 px-4 py-3 ${i < rows.length - 1 ? 'border-b border-c-border' : ''}`}
        >
          <div className="w-8 h-8 rounded-full bg-[#29B6F6]/10 flex items-center justify-center text-[#29B6F6] shrink-0">
            {r.icon}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-[#0D1B2A]">
              {r.channel}
              <span className="ml-1.5 text-[10px] font-semibold text-c-muted uppercase tracking-wide">Auto</span>
            </p>
            <p className="text-xs text-c-muted mt-0.5">{r.day}</p>
          </div>
          <span className={`text-xs font-medium shrink-0 ${r.statusCls}`}>{r.status}</span>
        </div>
      ))}
    </div>
  )
}

// ─── AiMockup — matches AiInsightBanner exactly ──────────────────────────────
function AiMockup() {
  const bullets = [
    'Rajesh Consulting has delayed 3 of 4 past invoices — send WhatsApp today.',
    'Studio Bloom pays within 5 days on average. Low risk.',
    '₹73,000 overdue across 2 clients. Escalate reminders.',
  ]
  return (
    <div className="mt-4 rounded-xl border border-c-border bg-white overflow-hidden">
      <div className="flex items-center gap-2 px-4 py-3 border-b border-c-border">
        <Sparkles size={13} className="text-[#29B6F6] shrink-0" />
        <span className="text-xs font-semibold text-[#0D1B2A]">AI Insights</span>
        <span className="text-xs text-c-muted ml-auto">just now</span>
      </div>
      <ul className="px-4 py-3 space-y-2.5">
        {bullets.map((b, i) => (
          <li key={i} className="flex items-start gap-2.5">
            <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-[#29B6F6] shrink-0" />
            <span className="text-xs text-[#0D1B2A] leading-snug">{b}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

// ─── TemplateMockup ───────────────────────────────────────────────────────────
function TemplateMockup() {
  return (
    <div className="mt-4 space-y-2">
      <div className="rounded-xl border border-c-border bg-white overflow-hidden">
        <div className="flex items-center gap-2 px-4 py-2.5 border-b border-c-border bg-[#F9FAFB]">
          <div className="w-6 h-6 rounded-full bg-[#29B6F6]/10 flex items-center justify-center text-[#29B6F6]">
            <Mail size={11} />
          </div>
          <span className="text-xs font-medium text-[#0D1B2A]">Email · Day 7 reminder</span>
        </div>
        <div className="px-4 py-3">
          <p className="text-xs text-[#0D1B2A] leading-relaxed">
            "Hi Priya, just a friendly reminder that Invoice #47 for{' '}
            <span className="font-semibold">₹45,000</span> is now 7 days overdue…"
          </p>
          <div className="mt-2.5 inline-flex items-center gap-1 text-[10px] font-semibold text-[#29B6F6] bg-[#29B6F6]/10 rounded-md px-2 py-1">
            <ExternalLink size={9} />
            View &amp; pay invoice →
          </div>
        </div>
      </div>
      <div className="flex items-center gap-2 px-1">
        <span className="h-1.5 w-1.5 rounded-full bg-green-500 shrink-0" />
        <span className="text-xs font-medium text-[#0D1B2A]">Link opened by client</span>
        <span className="text-xs text-c-muted ml-auto">2 min ago</span>
      </div>
    </div>
  )
}

// ─── ConfirmMockup — matches public confirmation card style ───────────────────
function ConfirmMockup() {
  return (
    <div className="mt-4 space-y-2">
      <div className="rounded-xl border border-c-border bg-white overflow-hidden">
        <div className="px-4 py-3 border-b border-c-border bg-[#F9FAFB]">
          <p className="text-xs text-c-muted">Invoice #47 · ₹45,000</p>
          <p className="text-sm font-semibold text-[#0D1B2A] mt-0.5">Have you sent the payment?</p>
        </div>
        <div className="px-4 py-3">
          <button className="w-full py-2 rounded-lg text-white text-xs font-semibold" style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}>
            Yes, I've paid →
          </button>
        </div>
      </div>
      <div className="flex items-center gap-2.5 bg-green-50 border border-green-200 rounded-xl px-4 py-2.5">
        <BadgeCheck size={14} className="text-green-600 shrink-0" />
        <span className="text-xs font-semibold text-green-700">Payment confirmed · Invoice closed</span>
      </div>
    </div>
  )
}

// ─── Feature cards ────────────────────────────────────────────────────────────
const features = [
  {
    tag: 'Automated Reminders',
    tagColor: 'text-[#29B6F6] bg-[#29B6F6]/10',
    headline: 'Stop sending "just following up"',
    mockup: <ReminderMockup />,
  },
  {
    tag: 'AI Insights',
    tagColor: 'text-violet-500 bg-violet-400/10',
    headline: 'Know which invoice will go late — before it does',
    mockup: <AiMockup />,
  },
  {
    tag: 'Smart Templates',
    tagColor: 'text-amber-500 bg-amber-400/10',
    headline: 'Your tone. Your words. Track every open and click.',
    mockup: <TemplateMockup />,
  },
  {
    tag: 'Payment Confirmation',
    tagColor: 'text-green-600 bg-green-400/10',
    headline: 'Clients confirm. You get paid. No disputes.',
    mockup: <ConfirmMockup />,
  },
]

// ─── Section ──────────────────────────────────────────────────────────────────
export default function FeaturesSection() {
  return (
    <section id="features" className="bg-[#F4F7F9] py-24 px-4">
      <div className="max-w-5xl mx-auto">
        <div className="text-center mb-16">
          <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
            What you get
          </p>
          <h2 className="text-3xl font-bold text-[#0D1B2A]">Everything you need to get paid — nothing you don't</h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          {features.map((f) => (
            <div key={f.tag} className="h-full rounded-2xl border border-c-border bg-white p-6 hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200">
              <span className={`text-[10px] font-bold uppercase tracking-widest px-2.5 py-1 rounded-full ${f.tagColor}`}>
                {f.tag}
              </span>
              <h3 className="text-base font-bold text-[#0D1B2A] mt-3 leading-snug">
                {f.headline}
              </h3>
              {f.mockup}
            </div>
          ))}
        </div>

        <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-6 text-sm text-c-muted">
          <span>
            Also supports{' '}
            <span className="text-[#0D1B2A] font-medium">multi-currency</span> — INR, USD, EUR, GBP, AUD, SGD, and more.
          </span>
          <span className="hidden sm:block text-c-border">·</span>
          <span className="text-[#0D1B2A] font-medium">
            No switching required. Works alongside your existing invoicing software.
          </span>
        </div>

        {/* Comparison table */}
        <div className="mt-20">
          <div className="text-center mb-10">
            <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
              Why FlowCollect
            </p>
            <h2 className="text-3xl font-bold text-[#0D1B2A]">How we compare</h2>
            <p className="text-c-muted text-sm mt-2">
              Most tools send invoices. We make sure you actually get paid.
            </p>
          </div>

          <div className="overflow-x-auto rounded-2xl border border-c-border">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-c-border bg-[#F4F7F9]">
                  <th className="text-left px-5 py-4 font-semibold text-[#0D1B2A] w-[32%]">Feature</th>
                  <th className="px-4 py-4 text-center font-bold text-[#29B6F6] w-[17%]">
                    <span className="block text-xs uppercase tracking-widest">FlowCollect</span>
                  </th>
                  <th className="px-4 py-4 text-center font-medium text-c-muted w-[17%]">
                    <span className="block text-xs uppercase tracking-widest">Manual / WhatsApp</span>
                  </th>
                  <th className="px-4 py-4 text-center font-medium text-c-muted w-[17%]">
                    <span className="block text-xs uppercase tracking-widest">FreshBooks</span>
                  </th>
                  <th className="px-4 py-4 text-center font-medium text-c-muted w-[17%]">
                    <span className="block text-xs uppercase tracking-widest">QuickBooks</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, i) => (
                  <tr
                    key={row.feature}
                    className={`border-b border-c-border last:border-0 ${i % 2 === 0 ? 'bg-white' : 'bg-[#F9FAFB]'}`}
                  >
                    <td className="px-5 py-4 font-medium text-[#0D1B2A]">{row.feature}</td>
                    <td className="px-4 py-4 text-center"><Cell value={row.us} /></td>
                    <td className="px-4 py-4 text-center"><Cell value={row.manual} /></td>
                    <td className="px-4 py-4 text-center"><Cell value={row.freshbooks} /></td>
                    <td className="px-4 py-4 text-center"><Cell value={row.quickbooks} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>
  )
}

type CellValue = true | false | string

const rows: { feature: string; us: CellValue; manual: CellValue; freshbooks: CellValue; quickbooks: CellValue }[] = [
  { feature: 'Automated follow-up reminders', us: true, manual: false,    freshbooks: 'Email only', quickbooks: 'Email only'   },
  { feature: 'SMS reminders',                 us: true, manual: false,    freshbooks: false,        quickbooks: false          },
  { feature: 'WhatsApp reminders',            us: true, manual: 'Manual', freshbooks: false,        quickbooks: false          },
  { feature: 'AI payment insights',           us: true, manual: false,    freshbooks: 'Basic',      quickbooks: 'Higher plans' },
  { feature: 'Free plan available',           us: true, manual: true,     freshbooks: false,        quickbooks: false          },
]

function Cell({ value }: { value: CellValue }) {
  if (value === true)   return <span className="text-[#29B6F6] font-bold text-base">✓</span>
  if (value === false)  return <span className="text-c-muted/40 text-base">✕</span>
  if (value === 'Soon') return <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-amber-400/10 text-amber-500">Soon</span>
  return <span className="text-xs text-c-muted">{value}</span>
}
