export interface User {
  id: string
  name: string
  email: string
  role: 'ADMIN' | 'STAFF'
}

export interface Org {
  id: string
  name: string
  email?: string
  phone?: string
  address?: string
  /** Drives Approvals nav visibility and page access */
  paymentCollectionMode: 'PAYMENT_LINK' | 'CONFIRMATION_FLOW'
  currency: string
  timezone: string
  /** Whether the Recover engine (AUTO rules) is active for this org */
  autoRecoveryEnabled: boolean
}
