import { useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import {
  getOrgProfile,
  updateOrgProfile,
  deleteOrganization,
  getPaymentDetails,
  savePaymentDetails,
} from '@/api/organization.api'
import type { OrgProfileRequest, OrgPaymentDetailsRequest, OrgProfileResponse } from '@/api/organization.api'

const qk = (orgId: string) => ['org-profile', orgId]
const pdQk = (orgId: string) => ['org-payment-details', orgId]

/** Picks the fields that live in the auth store from a fresh OrgProfileResponse. */
function syncOrgToStore(
  data: OrgProfileResponse,
  org: ReturnType<typeof useAuthStore.getState>['org'],
  token: string | null,
  user: ReturnType<typeof useAuthStore.getState>['user'],
  setAuth: ReturnType<typeof useAuthStore.getState>['setAuth'],
) {
  if (!token || !user || !org) return
  setAuth(token, user, {
    ...org,
    name:                  data.name,
    currency:              data.currency,
    timezone:              data.timezone,
    paymentCollectionMode: data.paymentCollectionMode,
  })
}

export function useOrgProfile() {
  const orgId   = useAuthStore((s) => s.org?.id ?? '')
  const org     = useAuthStore((s) => s.org)
  const setAuth = useAuthStore((s) => s.setAuth)
  const token   = useAuthStore((s) => s.token)
  const user    = useAuthStore((s) => s.user)

  const query = useQuery({
    queryKey: qk(orgId),
    queryFn:  () => getOrgProfile(orgId),
    enabled:  !!orgId,
  })

  // Keep auth store in sync with the DB. The settings page always fetches fresh,
  // so any drift (stale localStorage, changes from another session) is corrected here.
  useEffect(() => {
    if (query.data) syncOrgToStore(query.data, org, token, user, setAuth)
  }, [query.data]) // eslint-disable-line react-hooks/exhaustive-deps

  return query
}

export function useUpdateOrgProfile() {
  const orgId   = useAuthStore((s) => s.org?.id ?? '')
  const org     = useAuthStore((s) => s.org)
  const setAuth = useAuthStore((s) => s.setAuth)
  const token   = useAuthStore((s) => s.token)
  const user    = useAuthStore((s) => s.user)
  const qc      = useQueryClient()
  return useMutation({
    mutationFn: (body: OrgProfileRequest) => updateOrgProfile(orgId, body),
    onSuccess:  (data, variables) => {
      // Sync from the server response (not variables) — server is authoritative
      syncOrgToStore(data, org, token, user, setAuth)
      qc.invalidateQueries({ queryKey: qk(orgId) })
      // If timezone changed, timeStatus on all invoices is now stale — refetch
      if (variables.timezone && variables.timezone !== org?.timezone) {
        qc.invalidateQueries({ queryKey: ['invoices', orgId] })
      }
    },
  })
}

export function useDeleteOrganization() {
  const orgId    = useAuthStore((s) => s.org?.id ?? '')
  const clearAuth = useAuthStore((s) => s.clearAuth)
  return useMutation({
    mutationFn: () => deleteOrganization(orgId),
    onSuccess:  () => clearAuth(),
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
