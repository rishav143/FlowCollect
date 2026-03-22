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
// Dev seed — pre-populates store in development so the shell renders with
// real-looking data without requiring a login flow.
// Cleared automatically in production (token: null).
// ---------------------------------------------------------------------------

const devSeed: Pick<AuthState, 'token' | 'user' | 'org'> = import.meta.env.DEV
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
        // CONFIRMATION_FLOW → Approvals nav item is visible
        paymentCollectionMode: 'CONFIRMATION_FLOW',
        currency: 'INR',
        timezone: 'Asia/Kolkata',
      },
    }
  : { token: null, user: null, org: null }

// ---------------------------------------------------------------------------
// Store
// ---------------------------------------------------------------------------

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
      // In dev: if localStorage was cleared by logout, re-apply the seed on next load
      // so the shell always renders with data without requiring a real login flow.
      onRehydrateStorage: () => (state) => {
        if (import.meta.env.DEV && state && !state.token) {
          Object.assign(state, devSeed)
        }
      },
    },
  ),
)
