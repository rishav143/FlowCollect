import { Link } from 'react-router-dom'
import ScrollReveal from './ScrollReveal'

export default function SocialProofSection() {
  return (
    <section id="social-proof" className="bg-white py-12 sm:py-24 px-4">
      <div className="max-w-2xl mx-auto">
        <ScrollReveal>
          <div className="rounded-2xl border border-[#29B6F6]/30 bg-[#29B6F6]/5 px-5 py-7 sm:px-8 sm:py-10 text-center">
            <p className="text-sm font-semibold text-[#29B6F6] uppercase tracking-widest mb-2">
              Early access
            </p>
            <h3 className="text-2xl font-bold text-[#0D1B2A] mb-3">
              We're onboarding our first users now
            </h3>
            <p className="text-sm text-c-muted leading-relaxed mb-6 max-w-md mx-auto">
              Sign up for free and get direct access to the founder. I personally respond to every
              piece of feedback, always.
            </p>
            <Link
              to="/register"
              className="inline-block px-7 py-3 rounded-xl text-white font-semibold text-sm hover:opacity-90 transition-opacity"
              style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
            >
              Join early access <span className="leading-none relative -top-px">→</span>
            </Link>
          </div>
        </ScrollReveal>
      </div>
    </section>
  )
}
