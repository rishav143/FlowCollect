import { create } from 'zustand'

export type ToastType = 'success' | 'error' | 'info'

export interface Toast {
  id:       string
  type:     ToastType
  message:  string
  duration: number  // ms
}

interface ToastState {
  toasts:      Toast[]
  addToast:    (toast: Omit<Toast, 'id'>) => string
  removeToast: (id: string) => void
}

export const useToastStore = create<ToastState>((set) => ({
  toasts: [],

  addToast: (toast) => {
    const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2)}`
    set((s) => ({ toasts: [...s.toasts, { ...toast, id }] }))
    return id
  },

  removeToast: (id) =>
    set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}))

// ---------------------------------------------------------------------------
// Convenience hook — use this in components instead of the raw store
// ---------------------------------------------------------------------------

const DEFAULT_DURATION = 4000

export function useToast() {
  const addToast    = useToastStore((s) => s.addToast)
  const removeToast = useToastStore((s) => s.removeToast)

  return {
    success: (message: string, duration = DEFAULT_DURATION) =>
      addToast({ type: 'success', message, duration }),

    error: (message: string, duration = DEFAULT_DURATION) =>
      addToast({ type: 'error', message, duration }),

    info: (message: string, duration = DEFAULT_DURATION) =>
      addToast({ type: 'info', message, duration }),

    dismiss: (id: string) => removeToast(id),
  }
}
