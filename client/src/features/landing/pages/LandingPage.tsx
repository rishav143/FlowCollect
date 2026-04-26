import LandingNavbar from '../components/LandingNavbar'
import HeroSection from '../components/HeroSection'
import FeaturesSection from '../components/FeaturesSection'
import HowItWorksSection from '../components/HowItWorksSection'
import SocialProofSection from '../components/SocialProofSection'
import PricingSection from '../components/PricingSection'
import FaqSection from '../components/FaqSection'
import FinalCtaSection from '../components/FinalCtaSection'
import LandingFooter from '../components/LandingFooter'
import { useGeoRegion } from '../hooks/useGeoRegion'

export default function LandingPage() {
  const region = useGeoRegion()

  return (
    <div className="page-enter">
      <LandingNavbar />
      <main>
        <HeroSection />
        <HowItWorksSection />
        <FeaturesSection />
        <SocialProofSection />
        <PricingSection region={region} />
        <FaqSection />
        <FinalCtaSection />
      </main>
      <LandingFooter />

    </div>
  )
}
