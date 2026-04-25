import { useState, type FormEvent } from 'react'
import type { AxiosError } from 'axios'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Eye, EyeOff, XCircle } from 'lucide-react'
import { resetPassword } from '@/api/auth.api'
import AuthLayout from '../components/AuthLayout'

const inputCls = [
  'w-full px-3 py-2.5 text-sm rounded-lg',
  'bg-[#F4F7F9] dark:bg-[#243447]',
  'border border-c-border',
  'focus:outline-none focus:border-[#29B6F6]/60',
  'text-[#0D1B2A] dark:text-white placeholder:text-c-muted',
  'transition-colors',
].join(' ')

const labelCls = 'block text-xs font-semibold text-c-muted uppercase tracking-wide mb-1.5'

export default function ResetPasswordPage() {
  const navigate           = useNavigate()
  const [searchParams]     = useSearchParams()
  const token              = searchParams.get('token')

  const [password,  setPassword]  = useState('')
  const [confirm,   setConfirm]   = useState('')
  const [showPwd,   setShowPwd]   = useState(false)
  const [showConf,  setShowConf]  = useState(false)
  const [loading,   setLoading]   = useState(false)
  const [error,     setError]     = useState<string | null>(null)

  // No token in URL — invalid entry point
  if (!token) {
    return (
      <AuthLayout>
        <div className="flex flex-col items-center gap-4 py-4 text-center">
          <XCircle size={40} className="text-red-500" />
          <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Invalid link</h1>
          <p className="text-sm text-c-muted">
            This reset link is missing a token. Please request a new one.
          </p>
          <Link
            to="/forgot-password"
            className="w-full py-2.5 rounded-lg text-sm font-semibold text-white text-center hover:opacity-90 transition-opacity"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            Request new link
          </Link>
        </div>
      </AuthLayout>
    )
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (password !== confirm) {
      setError('Passwords do not match.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      await resetPassword(token!, password)
      navigate('/login', { state: { message: 'Password reset successfully. Sign in with your new password.' }, replace: true })
    } catch (err) {
      const ax = err as AxiosError<{ message?: string }>
      setError(ax.response?.data?.message ?? 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-[#0D1B2A] dark:text-white">Set new password</h1>
        <p className="text-sm text-c-muted mt-1">Must be at least 8 characters.</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">

        <div>
          <label className={labelCls}>New password</label>
          <div className="relative">
            <input
              type={showPwd ? 'text' : 'password'}
              placeholder="••••••••"
              required
              minLength={8}
              maxLength={100}
              autoComplete="new-password"
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
          <label className={labelCls}>Confirm password</label>
          <div className="relative">
            <input
              type={showConf ? 'text' : 'password'}
              placeholder="••••••••"
              required
              autoComplete="new-password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              className={inputCls + ' pr-10'}
            />
            <button
              type="button"
              tabIndex={-1}
              onClick={() => setShowConf((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors"
            >
              {showConf ? <EyeOff size={15} /> : <Eye size={15} />}
            </button>
          </div>
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
          {loading ? 'Saving…' : <span className="inline-flex items-center gap-1.5">Reset password <span className="leading-none relative -top-px">→</span></span>}
        </button>
      </form>

      <p className="mt-6 text-sm text-center text-c-muted">
        <Link to="/login" className="font-medium text-[#29B6F6] hover:underline">
          Back to login
        </Link>
      </p>
    </AuthLayout>
  )
}
