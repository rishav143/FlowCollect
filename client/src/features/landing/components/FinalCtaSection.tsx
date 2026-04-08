import { Link } from 'react-router-dom'
import { Zap, ShieldCheck, Lock } from 'lucide-react'

export default function FinalCtaSection() {
  return (
    <section
      className="py-24 px-4 text-center"
      style={{ background: 'linear-gradient(160deg, #0D1B2A 0%, #1B2838 100%)' }}
    >
      <div className="max-w-2xl mx-auto">
        <Zap size={32} className="text-[#29B6F6] mx-auto mb-6" strokeWidth={1.5} />

        <h2 className="text-4xl font-bold text-white">Start getting paid. Today.</h2>
        <p className="text-[#8A9BAE] text-lg mt-3 mb-8">
          Free forever for solo freelancers. No credit card. No contracts.
        </p>

        <Link
          to="/register"
          className="inline-block w-full max-w-xs mx-auto px-8 py-4 rounded-xl text-white font-semibold text-base hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-[#29B6F6]/25"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          Get started free →
        </Link>

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
