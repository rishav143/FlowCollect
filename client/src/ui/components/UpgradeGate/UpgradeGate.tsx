import { Lock } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

interface UpgradeGateProps {
  /** Title shown on the lock screen */
  title?: string
  /** Short description of what the user is missing */
  description?: string
  children: React.ReactNode
  /** When true, renders children instead of the gate */
  allowed: boolean
}

/**
 * Wraps a feature with a PRO upgrade prompt when `allowed` is false.
 * Renders children transparently when the user has access.
 */
export default function UpgradeGate({ title, description, children, allowed }: UpgradeGateProps) {
  const navigate = useNavigate()

  if (allowed) return <>{children}</>

  return (
    <div className="flex flex-col items-center justify-center min-h-[320px] gap-6 py-12">
      <div className="flex items-center justify-center w-14 h-14 rounded-2xl bg-[#29B6F6]/10">
        <Lock size={24} className="text-[#29B6F6]" strokeWidth={1.8} />
      </div>

      <div className="text-center max-w-sm">
        <p className="text-base font-semibold text-[#0D1B2A] dark:text-white">
          {title ?? 'PRO feature'}
        </p>
        <p className="text-sm text-c-muted mt-1.5">
          {description ?? 'Upgrade to PRO to unlock this feature.'}
        </p>
      </div>

      <button
        onClick={() => navigate('/settings/billing')}
        className="px-5 py-2 text-sm font-semibold text-white rounded-lg hover:opacity-90 transition-opacity"
        style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
      >
        Upgrade to PRO
      </button>
    </div>
  )
}
