import { Link } from 'react-router-dom'
import Logo from '@/ui/components/Logo/Logo'
import { useAuthStore } from '@/store/auth.store'

const NAV_LINKS = [
  { label: 'Features', href: '#features' },
  { label: 'How it works', href: '#how-it-works' },
  { label: 'Pricing', href: '#pricing' },
  { label: 'FAQ', href: '#faq' },
]

export default function LandingNavbar() {
  const token = useAuthStore((s) => s.token)

  return (
    <nav className="sticky top-0 z-40 bg-[#0D1B2A]/95 backdrop-blur-sm border-b border-white/10">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-14 flex items-center justify-between">
        <Link to="/" className="flex items-center shrink-0">
          <Logo variant="light" size="md" />
        </Link>

        {/* Anchor links — hidden on mobile */}
        {!token && (
          <div className="hidden md:flex items-center gap-6">
            {NAV_LINKS.map((link) => (
              <a
                key={link.label}
                href={link.href}
                className="text-sm text-white/60 hover:text-white transition-colors"
              >
                {link.label}
              </a>
            ))}
          </div>
        )}

        <div className="flex items-center gap-3">
          {token ? (
            <Link
              to="/dashboard"
              className="px-4 py-2 rounded-lg text-sm font-semibold text-white"
              style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
            >
              Go to Dashboard
            </Link>
          ) : (
            <>
              <a
                href={`${import.meta.env.VITE_APP_URL}/login`}
                className="hidden sm:block text-sm text-white/70 hover:text-white transition-colors"
              >
                Login
              </a>
              <a
                href={`${import.meta.env.VITE_APP_URL}/register`}
                className="px-4 py-2 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity"
                style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
              >
                Get started free
              </a>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
