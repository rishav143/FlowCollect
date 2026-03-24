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
      onRehydrateStorage: () => (state) => {
        // Re-apply seed after localStorage clear only in mock mode
        if (USE_MOCK && state && !state.token) {
          Object.assign(state, devSeed)
        }
      },
    },
  ),
)
