import { FileText, Bell, BadgeCheck } from 'lucide-react'

export default function HowItWorksSection() {
  return (
    <section id="how-it-works" className="bg-[#F4F7F9] py-20 px-4">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="text-center mb-14">
          <p className="text-xs font-semibold uppercase tracking-widest text-c-muted mb-3">
            How it works
          </p>
          <h2 className="text-3xl font-bold text-[#0D1B2A]">
            Three steps to stop chasing clients
          </h2>
        </div>

        {/* Steps */}
        <div className="flex flex-col sm:flex-row gap-8 sm:gap-4 items-start">
          <Step
            number="1"
            icon={<FileText size={22} className="text-[#29B6F6]" />}
            title="Add your invoice"
            body="Enter your client's details and invoice amount. Takes 30 seconds - no complex setup needed."
          />

          {/* Connector */}
          <div className="hidden sm:block h-px flex-1 bg-c-border mt-10 self-start" />

          <Step
            number="2"
            icon={<Bell size={22} className="text-[#29B6F6]" />}
            title="Set your rules"
            body="Tell FlowCollect when to follow up - Day 1, Day 7, Day 14 - via email. Your tone, your schedule."
          />

          {/* Connector */}
          <div className="hidden sm:block h-px flex-1 bg-c-border mt-10 self-start" />

          <Step
            number="3"
            icon={<BadgeCheck size={22} className="text-[#29B6F6]" />}
            title="Collect your money"
            body="We send the reminders. Clients pay. You get notified. No more awkward messages - ever."
          />
        </div>
      </div>
    </section>
  )
}

function Step({
  number,
  icon,
  title,
  body,
}: {
  number: string
  icon: React.ReactNode
  title: string
  body: string
}) {
  return (
    <div className="flex-1 relative">
      <span
        className="absolute top-0 right-0 text-7xl font-black text-[#29B6F6]/10 select-none leading-none"
        aria-hidden
      >
        {number}
      </span>
      <div className="w-12 h-12 rounded-xl bg-[#29B6F6]/10 flex items-center justify-center">
        {icon}
      </div>
      <h3 className="text-lg font-bold text-[#0D1B2A] mt-4 mb-2">{title}</h3>
      <p className="text-sm text-c-muted leading-relaxed">{body}</p>
    </div>
  )
}
