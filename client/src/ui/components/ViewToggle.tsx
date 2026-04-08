/**
 * ViewToggle
 *
 * A compact icon-button group that lets users pick how they want items displayed.
 * Preference is persisted to localStorage so it survives page navigation.
 *
 * Usage:
 *   const [view, setView] = useViewPreference('invoices', 'list')
 *   <ViewToggle value={view} onChange={setView} />
 */

import { useState, useCallback } from 'react'
import { List, Grip } from 'lucide-react'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type ViewMode = 'list' | 'grid'

const ICONS: Record<ViewMode, { icon: React.ReactNode; title: string }> = {
  list: { icon: <List size={15} strokeWidth={1.8} />, title: 'List view' },
  grid: { icon: <Grip size={15} strokeWidth={1.8} />, title: 'Grid view' },
}

// ---------------------------------------------------------------------------
// Hook — persists preference to localStorage
// ---------------------------------------------------------------------------

export function useViewPreference(
  storageKey: string,
  defaultView: ViewMode,
): [ViewMode, (v: ViewMode) => void] {
  const key = `fc-view-${storageKey}`

  const [view, setViewState] = useState<ViewMode>(() => {
    try {
      const saved = localStorage.getItem(key)
      // Migrate old 3-option values to the new 2-option set
      if (saved === 'grid2' || saved === 'grid3') {
        localStorage.setItem(key, 'grid')
        return 'grid'
      }
      if (saved && saved in ICONS) return saved as ViewMode
    } catch { /* ignore */ }
    return defaultView
  })

  const setView = useCallback((v: ViewMode) => {
    setViewState(v)
    try { localStorage.setItem(key, v) } catch { /* ignore */ }
  }, [key])

  return [view, setView]
}

// ---------------------------------------------------------------------------
// Grid class helper
// ---------------------------------------------------------------------------

export function gridClass(view: ViewMode): string {
  if (view === 'grid') return 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4'
  return 'flex flex-col gap-3'
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

interface Props {
  value:    ViewMode
  onChange: (v: ViewMode) => void
}

export default function ViewToggle({ value, onChange }: Props) {
  return (
    <div className="flex items-center gap-0.5 p-1 rounded-lg bg-[#F4F7F9] dark:bg-[#243447]">
      {(Object.keys(ICONS) as ViewMode[]).map((opt) => {
        const { icon, title } = ICONS[opt]
        const active = value === opt
        return (
          <button
            key={opt}
            title={title}
            onClick={() => onChange(opt)}
            className={[
              'w-7 h-7 flex items-center justify-center rounded-md transition-colors',
              active
                ? 'bg-white dark:bg-[#1B2838] text-[#0D1B2A] dark:text-white shadow-sm'
                : 'text-c-muted hover:text-[#0D1B2A] dark:hover:text-white',
            ].join(' ')}
          >
            {icon}
          </button>
        )
      })}
    </div>
  )
}
