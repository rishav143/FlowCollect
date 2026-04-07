import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { User, Org } from '@/types/auth.types'

// ---------------------------------------------------------------------------
// State shape
// ---------------------------------------------------------------------------

interface AuthState {
  token: string | null
  user: User | null
  org: Org | null
  setAuth: (token: string, user: User, org: Org) => void
  clearAuth: () => void
}

// ---------------------------------------------------------------------------
// Dev seed — only active when VITE_USE_MOCK=true (local UI development).
// Set VITE_USE_MOCK=false (or leave unset) to use a real backend.
// ---------------------------------------------------------------------------

const USE_MOCK = import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true'

const devSeed: Pick<AuthState, 'token' | 'user' | 'org'> = USE_MOCK
  ? {
      token: 'dev-token',
      user: {
        id: 'u1',
        name: 'Rishav Choudhary',
        email: 'rishav@example.com',
        role: 'ADMIN',
      },
      org: {
        id: 'o1',
        name: "Rishav's Agency",
        paymentCollectionMode: 'CONFIRMATION_FLOW',
        currency: 'INR',
        timezone: 'Asia/Kolkata',
        autoRecoveryEnabled: true,
      },
    }
  : { token: null, user: null, org: null }

// ---------------------------------------------------------------------------
// Store
// ---------------------------------------------------------------------------

/** Returns true if a JWT token string is expired (checks the `exp` claim). */
function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    // exp is in seconds; Date.now() is in milliseconds
    return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now()
  } catch {
    // Malformed token — treat as expired
    return true
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      ...devSeed,
      setAuth: (token, user, org) => set({ token, user, org }),
      clearAuth: () => set({ token: null, user: null, org: null }),
    }),
    {
      name: 'fc-auth',
      partialize: (state) => ({ token: state.token, user: state.user, org: state.org }),
      onRehydrateStorage: () => (state) => {
        if (!state) return

        // Re-apply seed after localStorage clear only in mock mode
        if (USE_MOCK && !state.token) {
          Object.assign(state, devSeed)
          return
        }

        // Clear stale session on page load if the JWT is already expired.
        // Without this the user sees empty UI (all API calls return 401) instead
        // of being redirected to /login.
        if (state.token && !USE_MOCK && isTokenExpired(state.token)) {
          state.token = null
          state.user  = null
          state.org   = null
        }
      },
    },
  ),
)
