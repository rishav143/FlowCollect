import { ShieldCheck, Lock, Zap } from 'lucide-react'

export default function FinalCtaSection() {
  return (
    <section
      className="py-14 sm:py-28 px-4 text-center"
      style={{ background: 'linear-gradient(160deg, #0D1B2A 0%, #1B2838 100%)' }}
    >
      <div className="max-w-2xl mx-auto">
        {/* Eyebrow */}
        <p className="text-xs font-semibold uppercase tracking-widest text-[#29B6F6] mb-4">
          Stop waiting. Start collecting.
        </p>

        <h2 className="text-3xl sm:text-4xl font-bold text-white leading-tight">
          Your next invoice<br />won't be late.
        </h2>
        <p className="text-[#8A9BAE] text-base sm:text-lg mt-4 mb-8 leading-relaxed">
          Stop sending awkward follow-ups.<br className="hidden sm:block" />
          Start getting paid on time - automatically.
        </p>

        <a
          href={`${import.meta.env.VITE_APP_URL}/register`}
          className="inline-flex items-center justify-center gap-1.5 w-full max-w-xs mx-auto px-8 py-4 rounded-xl text-white font-semibold text-base hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-[#29B6F6]/25"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          Get started free <span className="leading-none relative -top-px">→</span>
        </a>

        {/* Trust icons */}
        <div className="flex justify-center gap-6 mt-6 text-xs text-[#4E6478]">
          <span className="flex items-center gap-1.5">
            <ShieldCheck size={14} /> No credit card
          </span>
          <span className="flex items-center gap-1.5">
            <Lock size={14} /> Secure
          </span>
          <span className="flex items-center gap-1.5">
            <Zap size={14} /> Live in 2 minutes
          </span>
        </div>
      </div>
    </section>
  )
}
