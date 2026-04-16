import { useState, useEffect, type FormEvent } from 'react'
import type { AxiosError } from 'axios'
import { Link } from 'react-router-dom'
import { Eye, EyeOff, MailCheck } from 'lucide-react'
import { register, type RegisterResult } from '@/api/auth.api'
import AuthLayout from '../components/AuthLayout'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const inputCls = [
  'w-full px-3 py-2.5 text-sm rounded-lg',
  'bg-[#F4F7F9] dark:bg-[#243447]',
  'border border-c-border',
  'focus:outline-none focus:border-[#29B6F6]/60',
  'text-[#0D1B2A] dark:text-white placeholder:text-c-muted',
  'transition-colors',
].join(' ')

const labelCls = 'block text-xs font-semibold text-c-muted uppercase tracking-wide mb-1.5'

// ---------------------------------------------------------------------------
// Password strength
// ---------------------------------------------------------------------------

function getStrength(pwd: string): { score: number; label: string; color: string } {
  if (!pwd) return { score: 0, label: '', color: '' }
  let score = 0
  if (pwd.length >= 8)          score++
  if (/[a-z]/.test(pwd))        score++
  if (/[A-Z]/.test(pwd))        score++
  if (/[0-9]/.test(pwd))        score++
  if (/[^a-zA-Z0-9]/.test(pwd)) score++

  if (score <= 2) return { score, label: 'Weak',   color: 'bg-red-500'    }
  if (score === 3) return { score, label: 'Fair',   color: 'bg-amber-400'  }
  if (score === 4) return { score, label: 'Good',   color: 'bg-yellow-400' }
  return               { score, label: 'Strong', color: 'bg-green-500'  }
}

function StrengthBar({ password }: { password: string }) {
  if (!password) return null
  const { score, label, color } = getStrength(password)
  const filled = Math.max(1, Math.ceil(score / 5 * 4))   // 1–4 bars

  return (
    <div className="mt-2 space-y-1">
      <div className="flex gap-1">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className={`h-1 flex-1 rounded-full transition-all duration-300 ${
              i <= filled ? color : 'bg-[#E2EAF0] dark:bg-white/10'
            }`}
          />
        ))}
      </div>
      <p className={`text-xs font-medium ${
        score <= 2 ? 'text-red-500' :
        score === 3 ? 'text-amber-500' :
        score === 4 ? 'text-yellow-500' :
        'text-green-500'
      }`}>
        {label}
      </p>
    </div>
  )
}

// Map ISO country codes → default billing currency
const COUNTRY_CURRENCY: Record<string, string> = {
  IN: 'INR',
  US: 'USD', CA: 'USD',
  GB: 'GBP',
  AU: 'AUD', NZ: 'AUD',
  SG: 'SGD',
  AE: 'AED', SA: 'AED', QA: 'AED', BH: 'AED', KW: 'AED', OM: 'AED',
  DE: 'EUR', FR: 'EUR', IT: 'EUR', ES: 'EUR', NL: 'EUR', PT: 'EUR',
  BE: 'EUR', AT: 'EUR', IE: 'EUR', FI: 'EUR', GR: 'EUR', PL: 'EUR',
}

function detectCurrency(): Promise<string> {
  return fetch('https://ipapi.co/json/')
    .then((r) => r.json())
    .then((d) => COUNTRY_CURRENCY[d.country_code as string] ?? 'USD')
    .catch(() => 'USD')
}

// ---------------------------------------------------------------------------
// Verification success state
// ---------------------------------------------------------------------------

function VerificationPending({ result }: { result: RegisterResult }) {
  return (
    <div className="text-center">
      <div className="flex items-center justify-center w-14 h-14 rounded-full bg-[#29B6F6]/10 mx-auto mb-4">
        <MailCheck size={26} className="text-[#29B6F6]" strokeWidth={1.5} />
      </div>
      <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white mb-2">Check your inbox</h1>
      <p className="text-sm text-c-muted mb-6 leading-relaxed">{result.message}</p>
      <Link
        to="/login"
        className="inline-block w-full py-2.5 rounded-lg text-sm font-semibold text-white text-center hover:opacity-90 transition-opacity"
        style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
      >
        Go to sign in →
      </Link>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function RegisterPage() {
  const [ownerName,        setOwnerName]        = useState('')
  const [organizationName, setOrganizationName] = useState('')
  const [email,            setEmail]            = useState('')
  const [password,         setPassword]         = useState('')
  const [currency,         setCurrency]         = useState('USD')
  const [showPwd,          setShowPwd]          = useState(false)
  const [loading,          setLoading]          = useState(false)
  const [error,            setError]            = useState<string | null>(null)
  const [result,           setResult]           = useState<RegisterResult | null>(null)

  useEffect(() => {
    detectCurrency().then(setCurrency)
  }, [])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }
    setLoading(true)
    try {
      const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
      const res = await register({ ownerName, organizationName, email, password, currency, timezone })
      setResult(res)
    } catch (err) {
      const ax = err as AxiosError<{ message?: string; errors?: { field: string; message: string }[] }>
      const fieldErrors = ax.response?.data?.errors
      if (fieldErrors?.length) {
        setError(fieldErrors.map((e) => `${e.field}: ${e.message}`).join('\n'))
      } else {
        const msg = ax.response?.data?.message
        setError(msg ?? `Error ${ax.response?.status ?? ''}: Could not create account.`)
      }
    } finally {
      setLoading(false)
    }
  }

  // Show verification pending screen after successful register
  if (result) return <AuthLayout><VerificationPending result={result} /></AuthLayout>

  return (
    <AuthLayout>
      {/* Heading */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-[#0D1B2A] dark:text-white">Start getting paid on time</h1>
        <p className="text-sm text-c-muted mt-1">Up and running in 2 minutes.</p>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-4">

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={labelCls}>Your name</label>
            <input
              type="text"
              placeholder="Jane Smith"
              required
              autoComplete="given-name"
              value={ownerName}
              onChange={(e) => setOwnerName(e.target.value)}
              className={inputCls}
            />
          </div>
          <div>
            <label className={labelCls}>Your name or business name</label>
            <input
              type="text"
              placeholder="Jane Smith or Jane Smith Design"
              required
              value={organizationName}
              onChange={(e) => setOrganizationName(e.target.value)}
              className={inputCls}
            />
          </div>
        </div>

        <div>
          <label className={labelCls}>Email</label>
          <input
            type="email"
            placeholder="you@example.com"
            required
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputCls}
          />
        </div>

        <div>
          <label className={labelCls}>Password</label>
          <div className="relative">
            <input
              type={showPwd ? 'text' : 'password'}
              placeholder="Mix of letters, numbers & symbols"
              required
              autoComplete="new-password"
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={inputCls + ' pr-10'}
            />
            <button
              type="button"
              tabIndex={-1}
              onClick={() => setShowPwd((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors"
            >
              {showPwd ? <EyeOff size={15} /> : <Eye size={15} />}
            </button>
          </div>
          <StrengthBar password={password} />
        </div>

        {error && (
          <p className="text-sm text-red-500 bg-red-50 dark:bg-red-500/10 px-3 py-2 rounded-lg whitespace-pre-line">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full py-2.5 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity disabled:opacity-50"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          {loading ? 'Setting up your account…' : 'Start Getting Paid →'}
        </button>

        <p className="text-xs text-c-muted text-center leading-relaxed">
          Free to start &middot; No credit card required
        </p>
      </form>

      {/* Login link */}
      <p className="mt-5 text-sm text-center text-c-muted">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-[#29B6F6] hover:underline">
          Sign in
        </Link>
      </p>
    </AuthLayout>
  )
}
