import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listMembers, inviteMember, revokeInvite, removeMember } from '@/api/organization.api'

const qk = (orgId: string) => ['team', orgId]

export function useTeam() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  return useQuery({
    queryKey: qk(orgId),
    queryFn:  () => listMembers(orgId),
    enabled:  !!orgId,
  })
}

function useInvalidate() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()
  return () => qc.invalidateQueries({ queryKey: qk(orgId) })
}

export function useInviteMember() {
  const orgId      = useAuthStore((s) => s.org?.id ?? '')
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: (body: { email: string; role: 'ADMIN' | 'STAFF' }) => inviteMember(orgId, body),
    onSuccess:  invalidate,
  })
}

export function useRevokeInvite() {
  const orgId      = useAuthStore((s) => s.org?.id ?? '')
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: (inviteId: string) => revokeInvite(orgId, inviteId),
    onSuccess:  invalidate,
  })
}

export function useRemoveMember() {
  const orgId      = useAuthStore((s) => s.org?.id ?? '')
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: (userId: string) => removeMember(orgId, userId),
    onSuccess:  invalidate,
  })
}
