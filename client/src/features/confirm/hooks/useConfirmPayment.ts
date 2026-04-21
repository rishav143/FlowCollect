import { useQuery, useMutation } from '@tanstack/react-query'
import {
  getConfirmationView,
  submitPaymentClaim,
  type SubmitPaymentClaimRequest,
} from '@/api/publicConfirmation.api'

export function useConfirmationView(token: string) {
  return useQuery({
    queryKey: ['public', 'confirmation', token],
    queryFn:  () => getConfirmationView(token),
    retry:    2,
    staleTime: 0,
  })
}

export function useSubmitPaymentClaim(token: string) {
  return useMutation({
    mutationFn: (body: SubmitPaymentClaimRequest) => submitPaymentClaim(token, body),
  })
}
