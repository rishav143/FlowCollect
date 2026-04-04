export type FollowUpChannel     = 'EMAIL' | 'SMS' | 'WHATSAPP'
export type FollowUpStatus      = 'PENDING' | 'SENT' | 'FAILED' | 'CANCELLED'
export type FollowUpTriggerType = 'MANUAL' | 'AUTOMATED'
export type PaymentGateway      = 'STRIPE' | 'RAZORPAY'
export type ClickedLinkType     = 'PAYMENT_LINK' | 'CONFIRMATION_LINK'

export interface FollowUpResponse {
  id:                 string
  invoiceId:          string
  channel:            FollowUpChannel
  triggerType:        FollowUpTriggerType
  status:             FollowUpStatus
  templateId:         string | null
  reminderRuleId:     string | null
  scheduledForDate:   string | null
  attachPdf:          boolean
  sentAt:             string | null
  openedAt:           string | null
  clickedLinkType:    ClickedLinkType | null
  createdAt:          string
  updatedAt:          string
  paymentLinkId:      string | null
  paymentLinkUrl:     string | null
  paymentLinkGateway: PaymentGateway | null
  paymentLinkStatus:  string | null
}

export interface MultiChannelFollowUpRequest {
  channels:            FollowUpChannel[]
  templateId?:         string
  scheduledForDate?:   string
  attachPdf?:          boolean
  includePaymentLink?: boolean
  paymentGateway?:     PaymentGateway
}
