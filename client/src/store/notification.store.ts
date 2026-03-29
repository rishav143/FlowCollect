import { create } from 'zustand'
import { persist } from 'zustand/middleware'

// ---------------------------------------------------------------------------
// State shape
// ---------------------------------------------------------------------------

interface NotificationState {
  readIds:     string[]
  markRead:    (id: string) => void
  markUnread:  (id: string) => void
  markAllRead: (ids: string[]) => void
}

// ---------------------------------------------------------------------------
// Store — persisted to localStorage
// ---------------------------------------------------------------------------

export const useNotificationStore = create<NotificationState>()(
  persist(
    (set) => ({
      readIds: [],

      markRead: (id) =>
        set((s) => ({
          readIds: s.readIds.includes(id) ? s.readIds : [...s.readIds, id],
        })),

      markUnread: (id) =>
        set((s) => ({ readIds: s.readIds.filter((r) => r !== id) })),

      markAllRead: (ids) =>
        set((s) => ({ readIds: [...new Set([...s.readIds, ...ids])] })),
    }),
    {
      name:       'notification-read-state',
      partialize: (s) => ({ readIds: s.readIds }),
    },
  ),
)
