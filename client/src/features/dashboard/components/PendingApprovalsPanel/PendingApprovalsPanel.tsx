import { ApprovalsSection } from '../ActionTable/ActionTable'
import type { PaymentConfirmationResponse } from '@/types/confirmation.types'

interface Props {
  confirmations:      PaymentConfirmationResponse[]
  currency:           string
  isConfirmationFlow: boolean
  isLoading:          boolean
}

export default function PendingApprovalsPanel({
  confirmations,
  currency,
  isConfirmationFlow,
  isLoading,
}: Props) {
  if (!isConfirmationFlow) return null

  return (
    <ApprovalsSection
      confirmations={confirmations}
      currency={currency}
      isLoading={isLoading}
    />
  )
}
