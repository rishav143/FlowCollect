import LandingNavbar from '../components/LandingNavbar'
import HeroSection from '../components/HeroSection'
import PainSection from '../components/PainSection'
import FeaturesSection from '../components/FeaturesSection'
import HowItWorksSection from '../components/HowItWorksSection'
import SocialProofSection from '../components/SocialProofSection'
import PricingSection from '../components/PricingSection'
import FaqSection from '../components/FaqSection'
import FinalCtaSection from '../components/FinalCtaSection'
import LandingFooter from '../components/LandingFooter'
import { useGeoRegion } from '../hooks/useGeoRegion'
import { useAuthStore } from '@/store/auth.store'

export default function LandingPage() {
  const region = useGeoRegion()
  const token = useAuthStore((s) => s.token)

  return (
    <div className="page-enter">
      <LandingNavbar />
      <main>
        <HeroSection />
        <PainSection />
        <HowItWorksSection />
        <FeaturesSection />
        <SocialProofSection />
        <PricingSection region={region} />
        <FaqSection />
        <FinalCtaSection />
      </main>
      <LandingFooter />

      {/* Mobile sticky CTA — visible only on small screens, hidden once logged in */}
      {!token && (
        <div className="fixed bottom-0 left-0 right-0 z-50 md:hidden border-t border-white/10 bg-[#0D1B2A]/95 backdrop-blur-sm px-4 py-3 flex items-center gap-3">
          <a
            href="https://app.flowcollect.io/register"
            className="flex-1 text-center py-3 rounded-xl text-white font-semibold text-sm hover:opacity-90 transition-opacity"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            Get started free →
          </a>
          <a
            href="https://app.flowcollect.io/login"
            className="px-5 py-3 rounded-xl border border-white/20 text-white/80 text-sm font-medium hover:border-white/40 transition-colors"
          >
            Login
          </a>
        </div>
      )}
    </div>
  )
}
