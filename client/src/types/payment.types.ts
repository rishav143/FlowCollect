export type PaymentMode = 'CASH' | 'UPI' | 'BANK_TRANSFER' | 'CARD' | 'CHEQUE'

export interface PaymentResponse {
  id:          string
  invoiceId:   string
  amount:      number
  mode:        PaymentMode | null
  referenceId: string | null
  notes:       string | null
  paidAt:      string | null
  createdAt:   string
}

export interface PaymentRequest {
  amount:       number
  mode?:        PaymentMode
  referenceId?: string
  notes?:       string
}
