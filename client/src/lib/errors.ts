import { isAxiosError } from 'axios'
import type { ApiErrorResponse } from '@/types/api.types'

const FALLBACK = 'Something went wrong. Please try again.'

/**
 * Extracts a user-facing error message from any thrown value.
 *
 * Priority:
 *  1. Server's response.data.message (our backend always sets this)
 *  2. Server's response.data.error   (fallback field some endpoints use)
 *  3. "Network error" if the request never reached the server
 *  4. The provided fallback (or the default)
 *
 * Raw JS Error messages are intentionally suppressed — they are
 * technical and not suitable for end users.
 */
export function extractApiError(
  err:      unknown,
  fallback: string = FALLBACK,
): string {
  if (!err) return fallback

  if (isAxiosError<ApiErrorResponse>(err)) {
    const data = err.response?.data
    if (data?.message) return data.message
    if (data?.error)   return data.error
    if (!err.response) return 'Network error. Please check your connection.'
    return fallback
  }

  return fallback
}
