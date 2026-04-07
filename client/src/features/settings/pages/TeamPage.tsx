import { useState } from 'react'
import { createPortal } from 'react-dom'
import { UserPlus, Trash2, X, Clock } from 'lucide-react'
import { useTeam, useInviteMember, useRevokeInvite, useRemoveMember } from '../hooks/useTeam'
import { useAuthStore } from '@/store/auth.store'
import type { MemberResponse } from '@/api/organization.api'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function getInitials(name: string) {
  if (!name || !name.trim()) return '?'
  return name.split(' ').slice(0, 2).map((n) => n[0]).join('').toUpperCase()
}

const ROLE_META = {
  ADMIN: { text: 'text-[#29B6F6]',                    label: 'Admin' },
  STAFF: { text: 'text-[#0D1B2A] dark:text-white/70', label: 'Staff' },
}

// ---------------------------------------------------------------------------
// Invite modal
// ---------------------------------------------------------------------------

function InviteModal({ onClose }: { onClose: () => void }) {
  const [email, setEmail] = useState('')
  const [role,  setRole]  = useState<'ADMIN' | 'STAFF'>('STAFF')
  const [error, setError] = useState<string | null>(null)
  const invite = useInviteMember()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!email.trim()) return
    setError(null)
    try {
      await invite.mutateAsync({ email: email.trim().toLowerCase(), role })
      onClose()
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Failed to send invite. Please try again.')
    }
  }

  return createPortal(
    <>
      <div className="fixed inset-0 z-[200] bg-black/60" onClick={onClose} aria-hidden="true" />
      <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 pointer-events-none">
        <div
          className="pointer-events-auto w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-2xl"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-c-border">
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Invite Team Member</h2>
            <button
              onClick={onClose}
              className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-1.5">
                Email
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="colleague@company.com"
                required
                className="w-full text-sm rounded-lg border border-c-border bg-transparent text-[#0D1B2A] dark:text-white placeholder:text-c-muted px-3 py-2.5 focus:outline-none focus:border-[#8A9BAE]/40 transition-colors"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-1.5">
                Role
              </label>
              <div className="flex gap-2">
                {(['STAFF', 'ADMIN'] as const).map((r) => (
                  <button
                    key={r}
                    type="button"
                    onClick={() => setRole(r)}
                    className={[
                      'flex-1 py-2 text-sm font-medium rounded-lg border transition-colors',
                      role === r
                        ? 'border-[#29B6F6] text-[#29B6F6] bg-[#29B6F6]/5'
                        : 'border-c-border text-c-muted hover:text-[#0D1B2A] dark:hover:text-white',
                    ].join(' ')}
                  >
                    {r === 'STAFF' ? 'Staff' : 'Admin'}
                  </button>
                ))}
              </div>
              <p className="text-xs text-c-muted mt-1.5">
                {role === 'ADMIN'
                  ? 'Full access including settings and billing.'
                  : 'Can manage invoices and clients.'}
              </p>
            </div>

            {error && (
              <p className="text-xs text-red-500 bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20 px-3 py-2 rounded-lg">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={invite.isPending}
              className="w-full py-2.5 text-sm font-semibold text-white rounded-lg disabled:opacity-50 hover:opacity-90 transition-opacity"
              style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
            >
              {invite.isPending ? 'Sending…' : 'Send Invite'}
            </button>
          </form>
        </div>
      </div>
    </>,
    document.body,
  )
}

// ---------------------------------------------------------------------------
// Member row
// ---------------------------------------------------------------------------

function MemberRow({
  member, isLast, isSelf, onRemove, onRevoke,
}: {
  member:   MemberResponse
  isLast:   boolean
  isSelf:   boolean
  onRemove: (m: MemberResponse) => void
  onRevoke: (m: MemberResponse) => void
}) {
  const isPending = member.memberType === 'INVITE'
  const meta      = ROLE_META[member.role]

  return (
    <div className={`flex items-center gap-3 px-5 py-3.5 ${!isLast ? 'border-b border-c-border' : ''}`}>
      {/* Avatar */}
      <div className={[
        'w-9 h-9 rounded-full text-white text-xs font-bold flex items-center justify-center shrink-0 select-none',
        isPending ? 'bg-[#8A9BAE]' : 'bg-[#2E7A8E]',
      ].join(' ')}>
        {isPending ? <Clock size={14} /> : getInitials(member.name)}
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-[#0D1B2A] dark:text-white truncate">
          {isPending ? (
            <span className="text-c-muted italic">Pending invite</span>
          ) : (
            <>
              {member.name}
              {isSelf && <span className="ml-1.5 text-xs text-c-muted">(you)</span>}
            </>
          )}
        </p>
        <p className="text-xs text-c-muted truncate">{member.email}</p>
      </div>

      {/* Role */}
      <span className={`text-xs font-medium shrink-0 ${meta.text}`}>{meta.label}</span>

      {/* Status badge for pending */}
      {isPending && (
        <span className="text-xs px-2 py-0.5 rounded-full bg-amber-50 dark:bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-200 dark:border-amber-500/20 shrink-0">
          Invited
        </span>
      )}

      {/* Action button */}
      {isPending ? (
        <button
          onClick={() => onRevoke(member)}
          className="p-1.5 text-c-muted hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 rounded-lg transition-colors shrink-0"
          aria-label="Revoke invite"
          title="Revoke invite"
        >
          <X size={14} strokeWidth={2} />
        </button>
      ) : !isSelf ? (
        <button
          onClick={() => onRemove(member)}
          className="p-1.5 text-c-muted hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 rounded-lg transition-colors shrink-0"
          aria-label="Remove member"
          title="Remove member"
        >
          <Trash2 size={14} strokeWidth={1.8} />
        </button>
      ) : null}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function TeamPage() {
  const [showInvite, setShowInvite] = useState(false)
  const currentUserId = useAuthStore((s) => s.user?.id)

  const { data: members = [], isLoading, isError } = useTeam()
  const removeMut = useRemoveMember()
  const revokeMut = useRevokeInvite()

  const [removeError, setRemoveError] = useState<string | null>(null)

  async function handleRemove(member: MemberResponse) {
    setRemoveError(null)
    try {
      await removeMut.mutateAsync(member.id)
    } catch (err: any) {
      setRemoveError(err?.response?.data?.message ?? 'Failed to remove member.')
    }
  }

  async function handleRevoke(member: MemberResponse) {
    try {
      await revokeMut.mutateAsync(member.id)
    } catch {
      // silent — list will not change on failure
    }
  }

  return (
    <>
      <div className="space-y-4">
        {/* Header */}
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Team Members</h2>
            {!isLoading && (
              <p className="text-xs text-c-muted mt-0.5">
                {members.filter((m) => m.memberType === 'USER').length} member
                {members.filter((m) => m.memberType === 'USER').length !== 1 ? 's' : ''}
                {members.some((m) => m.memberType === 'INVITE') &&
                  ` · ${members.filter((m) => m.memberType === 'INVITE').length} pending`}
              </p>
            )}
          </div>
          <button
            onClick={() => setShowInvite(true)}
            className="flex items-center gap-1.5 px-3 py-2 text-sm font-semibold text-white rounded-lg hover:opacity-90 transition-opacity shrink-0"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            <UserPlus size={14} strokeWidth={2.5} />
            Invite
          </button>
        </div>

        {/* Error banner */}
        {removeError && (
          <div className="flex items-center justify-between gap-3 px-4 py-3 rounded-xl bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20 text-sm text-red-600 dark:text-red-400">
            <span>{removeError}</span>
            <button onClick={() => setRemoveError(null)} className="shrink-0"><X size={14} /></button>
          </div>
        )}

        {/* List */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
          {isLoading ? (
            <div className="space-y-px animate-pulse p-5">
              {[...Array(2)].map((_, i) => (
                <div key={i} className="h-14 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
              ))}
            </div>
          ) : isError ? (
            <p className="px-5 py-6 text-sm text-center text-c-muted">
              Failed to load team members.
            </p>
          ) : members.length === 0 ? (
            <p className="px-5 py-6 text-sm text-center text-c-muted">
              No team members yet. Invite someone to get started.
            </p>
          ) : (
            members.map((m, idx) => (
              <MemberRow
                key={m.id}
                member={m}
                isLast={idx === members.length - 1}
                isSelf={m.id === currentUserId}
                onRemove={handleRemove}
                onRevoke={handleRevoke}
              />
            ))
          )}
        </div>

        <p className="text-xs text-c-muted">
          Invited members will receive an email to join your organisation.
        </p>
      </div>

      {showInvite && <InviteModal onClose={() => setShowInvite(false)} />}
    </>
  )
}
