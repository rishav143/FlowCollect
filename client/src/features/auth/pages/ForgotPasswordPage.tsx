import { useState, type FormEvent } from 'react'
import type { AxiosError } from 'axios'
import { Link } from 'react-router-dom'
import { Mail } from 'lucide-react'
import { forgotPassword } from '@/api/auth.api'
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

export default function ForgotPasswordPage() {
  const [email,   setEmail]   = useState('')
  const [loading, setLoading] = useState(false)
  const [sent,    setSent]    = useState(false)
  const [error,   setError]   = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await forgotPassword(email.trim().toLowerCase())
      setSent(true)
    } catch (err) {
      const ax = err as AxiosError<{ message?: string }>
      setError(ax.response?.data?.message ?? 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  if (sent) {
    return (
      <AuthLayout>
        <div className="flex flex-col items-center gap-4 py-4 text-center">
          <div className="w-12 h-12 rounded-full bg-[#29B6F6]/10 flex items-center justify-center">
            <Mail size={22} className="text-[#29B6F6]" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Check your inbox</h1>
            <p className="text-sm text-c-muted mt-1.5">
              If <span className="font-medium text-[#0D1B2A] dark:text-white">{email}</span> is
              registered, a reset link is on its way. It expires in 1 hour.
            </p>
          </div>
          <p className="text-xs text-c-muted">
            Didn't get it?{' '}
            <button
              onClick={() => setSent(false)}
              className="text-[#29B6F6] hover:underline font-medium"
            >
              Send again
            </button>
          </p>
          <Link to="/login" className="text-xs text-[#29B6F6] hover:underline mt-1">
            Back to login
          </Link>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-[#0D1B2A] dark:text-white">Forgot password?</h1>
        <p className="text-sm text-c-muted mt-1">
          Enter your email and we'll send you a reset link.
        </p>
      </div>

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
          {loading ? 'Sending…' : 'Send reset link →'}
        </button>
      </form>

      <p className="mt-6 text-sm text-center text-c-muted">
        Remember it?{' '}
        <Link to="/login" className="font-medium text-[#29B6F6] hover:underline">
          Back to login
        </Link>
      </p>
    </AuthLayout>
  )
}
