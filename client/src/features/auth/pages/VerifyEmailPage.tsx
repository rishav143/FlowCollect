import { useEffect, useState } from 'react'
import { useSearchParams, useNavigate, Link } from 'react-router-dom'
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react'
import { verifyEmail, resendVerificationEmail } from '@/api/auth.api'
import { useAuthStore } from '@/store/auth.store'
import AuthLayout from '../components/AuthLayout'

type Status = 'verifying' | 'success' | 'error' | 'no-token'

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  const token = searchParams.get('token')

  const [status, setStatus]     = useState<Status>(token ? 'verifying' : 'no-token')
  const [errorMsg, setErrorMsg] = useState<string | null>(null)
  const [resendEmail, setResendEmail] = useState('')
  const [resendSent, setResendSent]   = useState(false)
  const [resendLoading, setResendLoading] = useState(false)

  useEffect(() => {
    if (!token) return

    verifyEmail(token)
      .then(({ token: jwt, user, org }) => {
        setAuth(jwt, user, org)
        setStatus('success')
        setTimeout(() => navigate('/dashboard', { replace: true }), 2000)
      })
      .catch((err) => {
        const msg = err?.response?.data?.message ?? 'This verification link is invalid or has expired.'
        setErrorMsg(msg)
        setStatus('error')
      })
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  async function handleResend(e: React.FormEvent) {
    e.preventDefault()
    if (!resendEmail.trim()) return
    setResendLoading(true)
    try {
      await resendVerificationEmail(resendEmail.trim().toLowerCase())
      setResendSent(true)
    } catch {
      // silently ignore — backend never reveals if email exists
      setResendSent(true)
    } finally {
      setResendLoading(false)
    }
  }

  return (
    <AuthLayout>
      {status === 'verifying' && (
        <div className="flex flex-col items-center gap-4 py-4">
          <Loader2 size={40} className="text-[#29B6F6] animate-spin" />
          <p className="text-sm text-c-muted text-center">Verifying your email…</p>
        </div>
      )}

      {status === 'success' && (
        <div className="flex flex-col items-center gap-4 py-4">
          <CheckCircle2 size={40} className="text-green-500" />
          <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Email verified!</h1>
          <p className="text-sm text-c-muted text-center">
            Your account is active. Redirecting to your dashboard…
          </p>
        </div>
      )}

      {(status === 'error' || status === 'no-token') && (
        <div className="flex flex-col items-center gap-4 py-4">
          <XCircle size={40} className="text-red-500" />
          <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Verification failed</h1>
          <p className="text-sm text-c-muted text-center">
            {errorMsg ?? 'No verification token found. Please use the link from your email.'}
          </p>

          {!resendSent ? (
            <form onSubmit={handleResend} className="w-full space-y-3 mt-2">
              <p className="text-xs text-c-muted text-center">Send a new verification email:</p>
              <input
                type="email"
                placeholder="your@email.com"
                required
                value={resendEmail}
                onChange={(e) => setResendEmail(e.target.value)}
                className={[
                  'w-full px-3 py-2.5 text-sm rounded-lg',
                  'bg-[#F4F7F9] dark:bg-[#243447]',
                  'border border-c-border',
                  'focus:outline-none focus:border-[#29B6F6]/60',
                  'text-[#0D1B2A] dark:text-white placeholder:text-c-muted',
                ].join(' ')}
              />
              <button
                type="submit"
                disabled={resendLoading}
                className="w-full py-2.5 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity disabled:opacity-50"
                style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
              >
                {resendLoading ? 'Sending…' : 'Resend verification email'}
              </button>
            </form>
          ) : (
            <p className="text-sm text-green-600 dark:text-green-400 text-center">
              If that email has an account, a new verification link is on its way.
            </p>
          )}

          <Link to="/login" className="text-xs text-[#29B6F6] hover:underline mt-1">
            Back to login
          </Link>
        </div>
      )}
    </AuthLayout>
  )
}
