export type LifeCycleStatus = 'DRAFT' | 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED'
export type TimeStatus     = 'NOT_DUE' | 'DUE_TODAY' | 'OVERDUE'

export interface InvoiceItem {
  id:          string
  description: string
  quantity:    number
  unitPrice:   number
  amount:      number
}

export interface TaxLine {
  label:  string
  amount: number
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
  discountAmount:   number
  taxLines:         TaxLine[]
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
  createdAtFrom?:   string   // YYYY-MM-DD
  createdAtTo?:     string   // YYYY-MM-DD
  dueDateFrom?:     string   // YYYY-MM-DD
  dueDateTo?:       string   // YYYY-MM-DD
  customerActive?:  boolean
  page?:            number
  size?:            number
  sort?:            string
}
