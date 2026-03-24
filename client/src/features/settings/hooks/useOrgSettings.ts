import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { getOrgProfile, updateOrgProfile } from '@/api/organization.api'
import type { OrgProfileRequest } from '@/api/organization.api'

const qk = (orgId: string) => ['org-profile', orgId]

export function useOrgProfile() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  return useQuery({
    queryKey: qk(orgId),
    queryFn:  () => getOrgProfile(orgId),
    enabled:  !!orgId,
  })
}

export function useUpdateOrgProfile() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()
  return useMutation({
    mutationFn: (body: OrgProfileRequest) => updateOrgProfile(orgId, body),
    onSuccess:  () => qc.invalidateQueries({ queryKey: qk(orgId) }),
  })
}
