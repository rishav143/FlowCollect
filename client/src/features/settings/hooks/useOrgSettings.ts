import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import {
  getOrgProfile,
  updateOrgProfile,
  getPaymentDetails,
  savePaymentDetails,
} from '@/api/organization.api'
import type { OrgProfileRequest, OrgPaymentDetailsRequest } from '@/api/organization.api'

const qk = (orgId: string) => ['org-profile', orgId]
const pdQk = (orgId: string) => ['org-payment-details', orgId]

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

export function usePaymentDetails() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  return useQuery({
    queryKey: pdQk(orgId),
    queryFn:  () => getPaymentDetails(orgId),
    enabled:  !!orgId,
  })
}

export function useSavePaymentDetails() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()
  return useMutation({
    mutationFn: (body: OrgPaymentDetailsRequest) => savePaymentDetails(orgId, body),
    onSuccess:  () => qc.invalidateQueries({ queryKey: pdQk(orgId) }),
  })
}
