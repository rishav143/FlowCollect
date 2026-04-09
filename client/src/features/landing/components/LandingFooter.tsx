import { Link } from 'react-router-dom'
import Logo from '@/ui/components/Logo/Logo'

export default function LandingFooter() {
  return (
    <footer className="bg-[#0D1B2A] border-t border-white/10 py-8 px-4 pb-24 md:pb-8">
      <div className="max-w-6xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
        {/* Left */}
        <div className="flex flex-col items-center sm:items-start gap-1">
          <Logo variant="light" size="sm" />
          <p className="text-xs text-[#4E6478]">© 2026 FlowCollect. All rights reserved.</p>
        </div>

        {/* Right */}
        <div className="flex items-center gap-5">
          {[
            { label: 'Privacy Policy', href: '#' },
            { label: 'Terms of Service', href: '#' },
            { label: 'Contact', href: '#' },
          ].map((link) => (
            <a
              key={link.label}
              href={link.href}
              className="text-sm text-[#4E6478] hover:text-white transition-colors"
            >
              {link.label}
            </a>
          ))}
        </div>
      </div>
    </footer>
  )
}
