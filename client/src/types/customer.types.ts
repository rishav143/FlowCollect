export interface CustomerResponse {
  id:                string
  organizationId:    string
  name:              string
  email:             string | null
  phone:             string | null
  companyName:       string | null
  address:           string | null
  active:            boolean
  automationEnabled: boolean
  createdAt:         string
  updatedAt:         string
}

export interface CustomerRequest {
  name:         string
  email?:       string
  phone?:       string
  companyName?: string
  address?:     string
}

export interface ListCustomersParams {
  name?:        string
  email?:       string
  phone?:       string
  companyName?: string
  active?:      boolean
  page?:        number
  size?:        number
}
