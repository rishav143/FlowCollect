/** Shape of every error response from the Spring backend. */
export interface ApiErrorResponse {
  message?: string
  error?:   string
  status?:  number
  path?:    string
}
