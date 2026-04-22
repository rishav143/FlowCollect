import { useState, type FormEvent } from 'react'
import type { AxiosError } from 'axios'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { Eye, EyeOff } from 'lucide-react'
import { login, resendVerificationEmail } from '@/api/auth.api'
import { useAuthStore } from '@/store/auth.store'
import AuthLayout from '../components/AuthLayout'

// ---------------------------------------------------------------------------
// Shared input styles — same tokens used across modals and forms
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
// Page
// ---------------------------------------------------------------------------

export default function LoginPage() {
  const navigate  = useNavigate()
  const location  = useLocation()
  const setAuth   = useAuthStore((s) => s.setAuth)

  const successMessage = (location.state as { message?: string } | null)?.message ?? null

  const [email,        setEmail]        = useState('')
  const [password,     setPassword]     = useState('')
  const [showPwd,      setShowPwd]      = useState(false)
  const [loading,      setLoading]      = useState(false)
  const [error,        setError]        = useState<string | null>(null)
  const [unverified,   setUnverified]   = useState(false)
  const [resendState,  setResendState]  = useState<'idle' | 'sending' | 'sent'>('idle')

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setUnverified(false)
    setLoading(true)
    try {
      const { token, user, org } = await login(email, password)
      setAuth(token, user, org)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      const ax = err as AxiosError<{ message?: string }>
      const msg = ax.response?.data?.message ?? `Error ${ax.response?.status ?? ''}: Invalid email or password.`
      if (msg.toLowerCase().includes('verify your email')) {
        setUnverified(true)
      } else {
        setError(msg)
      }
    } finally {
      setLoading(false)
    }
  }

  async function handleResend() {
    if (resendState !== 'idle') return
    setResendState('sending')
    try {
      await resendVerificationEmail(email)
      setResendState('sent')
    } catch {
      setResendState('idle')
    }
  }

  return (
    <AuthLayout>
      {/* Heading */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-[#0D1B2A] dark:text-white">Welcome back</h1>
        <p className="text-sm text-c-muted mt-1">Sign in to your FlowCollect account</p>
      </div>

      {successMessage && (
        <p className="text-sm text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-500/10 px-3 py-2 rounded-lg mb-4">
          {successMessage}
        </p>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit} className="space-y-4">

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
          <div className="flex items-center justify-between mb-1.5">
            <span className={labelCls} style={{ marginBottom: 0 }}>Password</span>
            <Link
              to="/forgot-password"
              className="text-xs text-[#29B6F6] hover:underline"
              tabIndex={-1}
            >
              Forgot password?
            </Link>
          </div>
          <div className="relative">
            <input
              type={showPwd ? 'text' : 'password'}
              placeholder="••••••••"
              required
              autoComplete="current-password"
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

        {error && (
          <p className="text-sm text-red-500 bg-red-50 dark:bg-red-500/10 px-3 py-2 rounded-lg">
            {error}
          </p>
        )}

        {unverified && (
          <div className="text-sm bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20 px-3 py-2.5 rounded-lg space-y-1.5">
            <p className="text-amber-800 dark:text-amber-300 font-medium">Email not verified</p>
            <p className="text-amber-700 dark:text-amber-400 text-xs">
              Check your inbox for the verification link.
            </p>
            {resendState === 'sent' ? (
              <p className="text-xs text-green-600 dark:text-green-400">Verification email sent. Check your inbox.</p>
            ) : (
              <button
                type="button"
                onClick={handleResend}
                disabled={resendState === 'sending'}
                className="text-xs font-medium text-amber-800 dark:text-amber-300 underline hover:opacity-70 disabled:opacity-50"
              >
                {resendState === 'sending' ? 'Sending…' : 'Resend verification email'}
              </button>
            )}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full py-2.5 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity disabled:opacity-50"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          {loading ? 'Signing in…' : 'Sign in →'}
        </button>
      </form>

      {/* Register link */}
      <p className="mt-6 text-sm text-center text-c-muted">
        Don't have an account?{' '}
        <Link to="/register" className="font-medium text-[#29B6F6] hover:underline">
          Create one free
        </Link>
      </p>
    </AuthLayout>
  )
}
