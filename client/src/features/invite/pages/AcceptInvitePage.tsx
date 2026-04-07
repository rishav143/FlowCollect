import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { AlertCircle, CheckCircle2, Eye, EyeOff } from 'lucide-react'
import { getInviteView, acceptInvite } from '@/api/invite.api'
import { useAuthStore } from '@/store/auth.store'

// ---------------------------------------------------------------------------
// Loading skeleton
// ---------------------------------------------------------------------------

function LoadingSkeleton() {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-sm border border-gray-100 p-8 animate-pulse space-y-4">
        <div className="h-4 w-32 rounded bg-gray-200" />
        <div className="h-6 w-48 rounded bg-gray-200" />
        <div className="h-4 w-full rounded bg-gray-200" />
        <div className="h-10 w-full rounded bg-gray-200" />
        <div className="h-10 w-full rounded bg-gray-200" />
        <div className="h-11 w-full rounded bg-gray-200" />
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Error / expired states
// ---------------------------------------------------------------------------

function ErrorPage({ message }: { message: string }) {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <div className="text-center space-y-3 max-w-sm">
        <AlertCircle size={40} className="text-red-400 mx-auto" />
        <p className="text-base font-semibold text-gray-800">Invitation unavailable</p>
        <p className="text-sm text-gray-500">{message}</p>
      </div>
    </div>
  )
}

function ExpiredPage({ orgName }: { orgName: string }) {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <div className="text-center space-y-3 max-w-sm">
        <AlertCircle size={40} className="text-amber-400 mx-auto" />
        <p className="text-base font-semibold text-gray-800">Invitation expired</p>
        <p className="text-sm text-gray-500">
          Your invitation to join <strong>{orgName}</strong> has expired.
          Please ask an administrator to send a new invite.
        </p>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Success state
// ---------------------------------------------------------------------------

function SuccessPage() {
  const navigate = useNavigate()
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <div className="text-center space-y-3 max-w-sm">
        <CheckCircle2 size={44} className="text-[#2a9d8f] mx-auto" />
        <p className="text-base font-semibold text-gray-800">You're all set!</p>
        <p className="text-sm text-gray-500">Your account has been created. Taking you to the dashboard…</p>
        <button
          onClick={() => navigate('/dashboard', { replace: true })}
          className="mt-2 px-6 py-2.5 rounded-xl text-sm font-semibold text-white"
          style={{ background: '#29B6F6' }}
        >
          Go to dashboard
        </button>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Accept form
// ---------------------------------------------------------------------------

function AcceptForm({
  token,
  orgName,
  inviterName,
  email,
  role,
  onSuccess,
}: {
  token:       string
  orgName:     string
  inviterName: string
  email:       string
  role:        string
  onSuccess:   () => void
}) {
  const setAuth    = useAuthStore((s) => s.setAuth)
  const navigate   = useNavigate()

  const [name,        setName]        = useState('')
  const [password,    setPassword]    = useState('')
  const [showPass,    setShowPass]    = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  const { mutate, isPending } = useMutation({
    mutationFn: () => acceptInvite(token, { name: name.trim(), password }),
    onSuccess: (authResponse) => {
      setAuth(authResponse.token, authResponse.user, authResponse.org)
      onSuccess()
      // Small delay so the success state is visible before redirect
      setTimeout(() => navigate('/dashboard', { replace: true }), 1500)
    },
    onError: (err: any) => {
      setSubmitError(err?.response?.data?.message ?? 'Something went wrong. Please try again.')
    },
  })

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim() || password.length < 8) return
    setSubmitError(null)
    mutate()
  }

  const roleLabel = role === 'ADMIN' ? 'Admin' : 'Staff'

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <div className="w-full max-w-sm bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {/* Header */}
        <div style={{ background: '#29B6F6' }} className="px-6 py-5 text-white">
          <p className="text-lg font-bold tracking-tight">
            <span className="font-black">Paid</span>Peace
          </p>
          <p className="text-sm opacity-90 mt-0.5">You've been invited to join a team</p>
        </div>

        {/* Invite context */}
        <div className="px-6 pt-5 pb-4 border-b border-gray-100">
          <p className="text-sm text-gray-600">
            <strong>{inviterName}</strong> invited you to join{' '}
            <strong>{orgName}</strong> as <strong>{roleLabel}</strong>.
          </p>
          <p className="text-xs text-gray-400 mt-1">{email}</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1.5">
              Your name
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Jane Smith"
              required
              minLength={2}
              maxLength={100}
              className="w-full text-sm rounded-xl border border-gray-200 bg-white text-gray-900 placeholder:text-gray-400 px-3.5 py-2.5 focus:outline-none focus:border-[#29B6F6] transition-colors"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wide text-gray-500 mb-1.5">
              Password
            </label>
            <div className="relative">
              <input
                type={showPass ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="At least 8 characters"
                required
                minLength={8}
                className="w-full text-sm rounded-xl border border-gray-200 bg-white text-gray-900 placeholder:text-gray-400 px-3.5 py-2.5 pr-10 focus:outline-none focus:border-[#29B6F6] transition-colors"
              />
              <button
                type="button"
                onClick={() => setShowPass((v) => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                tabIndex={-1}
              >
                {showPass ? <EyeOff size={15} /> : <Eye size={15} />}
              </button>
            </div>
          </div>

          {submitError && (
            <div className="flex items-start gap-2 px-3 py-2.5 rounded-xl bg-red-50 border border-red-200 text-sm text-red-600">
              <AlertCircle size={15} className="shrink-0 mt-px" />
              <span>{submitError}</span>
            </div>
          )}

          <button
            type="submit"
            disabled={isPending || !name.trim() || password.length < 8}
            className="w-full py-3 rounded-2xl text-white text-sm font-semibold transition-opacity disabled:opacity-50"
            style={{ background: '#29B6F6' }}
          >
            {isPending ? 'Creating account…' : 'Accept invitation'}
          </button>

          <p className="text-center text-xs text-gray-400">
            By accepting, you agree to our terms of service.
          </p>
        </form>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page entry point
// ---------------------------------------------------------------------------

export default function AcceptInvitePage() {
  const { token = '' } = useParams<{ token: string }>()
  const [accepted, setAccepted] = useState(false)

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['invite-view', token],
    queryFn:  () => getInviteView(token),
    enabled:  !!token,
    retry:    false,
    staleTime: Infinity,
  })

  if (isLoading) return <LoadingSkeleton />

  if (isError) {
    const status = (error as any)?.response?.status
    const message = status === 404
      ? 'This invitation link is invalid or has already been used.'
      : 'Unable to load the invitation. Please try again later.'
    return <ErrorPage message={message} />
  }

  if (!data) return <ErrorPage message="Invitation not found." />

  if (data.expired) return <ExpiredPage orgName={data.organizationName} />

  if (accepted) return <SuccessPage />

  return (
    <AcceptForm
      token={token}
      orgName={data.organizationName}
      inviterName={data.inviterName}
      email={data.email}
      role={data.role}
      onSuccess={() => setAccepted(true)}
    />
  )
}
