export type ConfirmationStatus =
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'REMAINING_REQUESTED'

export interface PaymentConfirmationResponse {
  id:                     string
  invoiceId:              string
  invoiceNumber:          string
  confirmationLinkId:     string
  amountClaimed:          number
  invoiceTotalAmount:     number
  invoiceRemainingAmount: number
  customerNote:           string | null
  status:                 ConfirmationStatus
  businessNote:           string | null
  reviewedAt:             string | null
  createdAt:              string
}
