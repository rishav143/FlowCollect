import publicApi from '@/lib/publicAxios'

export interface PaymentDetails {
  bankName?:          string | null
  accountHolderName?: string | null
  accountNumber?:     string | null
  iban?:              string | null
  swiftCode?:         string | null
  routingNumber?:     string | null
  ifscCode?:          string | null
  upiId?:             string | null
  paypalEmail?:       string | null
  wiseEmail?:         string | null
  additionalNote?:    string | null
}

export interface CustomerConfirmationView {
  invoiceNumber:    string
  organizationName: string
  customerName:     string
  totalAmount:      number
  totalPaid:        number
  remainingAmount:  number
  dueDate:          string   // ISO local date
  currency:         string
  linkStatus:       'OPEN' | 'CLOSED'
  paymentDetails?:  PaymentDetails | null
}

export interface SubmitPaymentClaimRequest {
  amountClaimed: number
  customerNote?: string
}

export interface SubmitPaymentClaimResponse {
  confirmationId: string
  amountClaimed:  number
  status:         string
  createdAt:      string
}

const base = (token: string) => `/api/v1/public/confirmations/${token}`

export async function getConfirmationView(token: string): Promise<CustomerConfirmationView> {
  const { data } = await publicApi.get<CustomerConfirmationView>(base(token))
  return data
}

export async function submitPaymentClaim(
  token: string,
  body: SubmitPaymentClaimRequest,
): Promise<SubmitPaymentClaimResponse> {
  const { data } = await publicApi.post<SubmitPaymentClaimResponse>(base(token), body)
  return data
}
