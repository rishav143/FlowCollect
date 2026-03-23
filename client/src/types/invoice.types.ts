export type LifeCycleStatus = 'DRAFT' | 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED'
export type TimeStatus     = 'NOT_DUE' | 'DUE_TODAY' | 'OVERDUE'

export interface InvoiceItem {
  id:          string
  description: string
  quantity:    number
  unitPrice:   number
  amount:      number
}

export interface InvoiceResponse {
  id:               string
  organizationId:   string
  customerId:       string | null
  createdByUserId:  string | null
  invoiceNumber:    string
  timeStatus:       TimeStatus
  lifeCycleStatus:  LifeCycleStatus
  issueDate:        string | null  // "YYYY-MM-DD"
  dueDate:          string | null  // "YYYY-MM-DD"
  subtotal:         number
  taxPercentage:    number
  totalAmount:      number
  totalPaid:        number
  remainingAmount:  number
  items:            InvoiceItem[]
  createdAt:        string         // ISO instant
  updatedAt:        string
}

export interface ListInvoicesParams {
  timeStatus?:      TimeStatus
  lifeCycleStatus?: LifeCycleStatus
  invoiceNumber?:   string
  page?:            number
  size?:            number
  sort?:            string
}
