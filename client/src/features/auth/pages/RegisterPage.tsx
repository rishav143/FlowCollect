import { useState, type FormEvent } from 'react'
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

const CURRENCIES = [
  { value: 'INR', label: 'INR — Indian Rupee' },
  { value: 'USD', label: 'USD — US Dollar' },
  { value: 'EUR', label: 'EUR — Euro' },
  { value: 'GBP', label: 'GBP — British Pound' },
  { value: 'AED', label: 'AED — UAE Dirham' },
  { value: 'SGD', label: 'SGD — Singapore Dollar' },
]

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
  const [currency,         setCurrency]         = useState('INR')
  const [showPwd,          setShowPwd]          = useState(false)
  const [loading,          setLoading]          = useState(false)
  const [error,            setError]            = useState<string | null>(null)
  const [result,           setResult]           = useState<RegisterResult | null>(null)

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
      const ax = err as AxiosError<{ message?: string }>
      const msg = ax.response?.data?.message
      setError(msg ?? `Error ${ax.response?.status ?? ''}: Could not create account.`)
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
        <h1 className="text-2xl font-bold text-[#0D1B2A] dark:text-white">Create account</h1>
        <p className="text-sm text-c-muted mt-1">Start collecting payments smarter</p>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-4">

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className={labelCls}>Your name</label>
            <input
              type="text"
              placeholder="Rishav"
              required
              autoComplete="given-name"
              value={ownerName}
              onChange={(e) => setOwnerName(e.target.value)}
              className={inputCls}
            />
          </div>
          <div>
            <label className={labelCls}>Business name</label>
            <input
              type="text"
              placeholder="My Agency"
              required
              value={organizationName}
              onChange={(e) => setOrganizationName(e.target.value)}
              className={inputCls}
            />
          </div>
        </div>

        <div>
          <label className={labelCls}>Work email</label>
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
              placeholder="Min. 8 characters"
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
        </div>

        <div>
          <label className={labelCls}>Currency</label>
          <select
            value={currency}
            onChange={(e) => setCurrency(e.target.value)}
            className={inputCls}
          >
            {CURRENCIES.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
        </div>

        {error && (
          <p className="text-sm text-red-500 bg-red-50 dark:bg-red-500/10 px-3 py-2 rounded-lg">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full py-2.5 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity disabled:opacity-50"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          {loading ? 'Creating account…' : 'Create account →'}
        </button>

        <p className="text-xs text-c-muted text-center leading-relaxed">
          By signing up you agree to our Terms of Service &amp; Privacy Policy.
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
