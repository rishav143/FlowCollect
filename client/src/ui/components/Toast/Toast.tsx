import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import { CheckCircle, XCircle, Info, X } from 'lucide-react'
import { useToastStore } from '@/store/toast.store'
import type { Toast as ToastItem } from '@/store/toast.store'

// ---------------------------------------------------------------------------
// Single toast item with auto-dismiss
// ---------------------------------------------------------------------------

const STYLES = {
  success: {
    bar:  'bg-emerald-500',
    icon: <CheckCircle size={16} className="text-emerald-500 shrink-0 mt-0.5" />,
  },
  error: {
    bar:  'bg-red-500',
    icon: <XCircle size={16} className="text-red-500 shrink-0 mt-0.5" />,
  },
  info: {
    bar:  'bg-[#29B6F6]',
    icon: <Info size={16} className="text-[#29B6F6] shrink-0 mt-0.5" />,
  },
}

function ToastItem({ toast, onDismiss }: { toast: ToastItem; onDismiss: () => void }) {
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const s = STYLES[toast.type] ?? STYLES.info

  useEffect(() => {
    timerRef.current = setTimeout(onDismiss, toast.duration)
    return () => {
      if (timerRef.current !== null) clearTimeout(timerRef.current)
    }
  // onDismiss identity is stable (bound to a fixed id), safe to omit from deps
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [toast.duration])

  return (
    <div
      role="alert"
      aria-live="polite"
      className={[
        'relative flex items-start gap-3 w-full max-w-sm',
        'bg-white dark:bg-[#1B2838]',
        'border border-c-border rounded-xl shadow-lg overflow-hidden',
        'px-4 py-3',
        'animate-toast-in',
      ].join(' ')}
    >
      {/* Accent bar */}
      <span className={`absolute left-0 inset-y-0 w-1 rounded-l-xl ${s.bar}`} />

      {/* Icon */}
      {s.icon}

      {/* Message */}
      <p className="flex-1 text-sm text-[#0D1B2A] dark:text-white leading-snug">
        {toast.message}
      </p>

      {/* Dismiss */}
      <button
        onClick={onDismiss}
        aria-label="Dismiss notification"
        className="text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors"
      >
        <X size={14} />
      </button>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Container — renders all active toasts via a portal
// ---------------------------------------------------------------------------

export default function ToastContainer() {
  const toasts      = useToastStore((s) => s.toasts)
  const removeToast = useToastStore((s) => s.removeToast)

  if (toasts.length === 0) return null

  return createPortal(
    <div
      className="fixed top-4 right-4 z-[60] flex flex-col gap-2 pointer-events-none"
      aria-label="Notifications"
    >
      {toasts.map((toast) => (
        <div key={toast.id} className="pointer-events-auto">
          <ToastItem
            toast={toast}
            onDismiss={() => removeToast(toast.id)}
          />
        </div>
      ))}
    </div>,
    document.body,
  )
}
