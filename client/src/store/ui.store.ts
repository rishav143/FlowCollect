import { create } from 'zustand'
import { persist } from 'zustand/middleware'

/**
 * UIStore — client-only UI state.
 *
 * sidebarOpen      → mobile/tablet drawer open state
 * sidebarCollapsed → desktop icon-only collapsed state (independent of drawer)
 * theme            → 'light' | 'dark'  (persisted across sessions)
 */
interface UIState {
  sidebarOpen:      boolean
  sidebarCollapsed: boolean
  theme:            'light' | 'dark'
  mutationDepth:    number          // # of in-flight mutations driving the top bar
  openSidebar:      () => void
  closeSidebar:     () => void
  toggleSidebar:    () => void
  toggleCollapsed:  () => void
  toggleTheme:      () => void
  mutationStart:    () => void
  mutationEnd:      () => void
}

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      sidebarOpen:      false,
      sidebarCollapsed: false,
      theme:            'light',
      mutationDepth:    0,
      openSidebar:      () => set({ sidebarOpen: true }),
      closeSidebar:     () => set({ sidebarOpen: false }),
      toggleSidebar:    () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
      toggleCollapsed:  () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
      toggleTheme:      () => set((s) => ({ theme: s.theme === 'light' ? 'dark' : 'light' })),
      mutationStart:    () => set((s) => ({ mutationDepth: s.mutationDepth + 1 })),
      mutationEnd:      () => set((s) => ({ mutationDepth: Math.max(0, s.mutationDepth - 1) })),
    }),
    {
      name: 'fc-ui',
      // Only persist theme and collapse state — not the transient drawer state
      partialize: (s) => ({ theme: s.theme, sidebarCollapsed: s.sidebarCollapsed }),
    },
  ),
)
