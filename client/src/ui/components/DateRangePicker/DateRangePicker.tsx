import { useState, useRef, useEffect } from 'react'
import { DayPicker } from 'react-day-picker'
import type { DateRange } from 'react-day-picker'
import { CalendarDays, ChevronDown, X } from 'lucide-react'
import 'react-day-picker/style.css'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface DateRangeValue {
  from?: string  // YYYY-MM-DD
  to?:   string  // YYYY-MM-DD
}

interface Props {
  label:    string
  value:    DateRangeValue
  onChange: (v: DateRangeValue) => void
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function toStr(d: Date) {
  const y  = d.getFullYear()
  const m  = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

function parseDate(s?: string): Date | undefined {
  return s ? new Date(s + 'T12:00:00') : undefined
}

function fmtShort(s?: string) {
  if (!s) return '…'
  return new Date(s + 'T12:00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })
}

function rangeLabel(v: DateRangeValue) {
  if (!v.from && !v.to) return null
  if (v.from && v.to && v.from === v.to) return fmtShort(v.from)
  return `${fmtShort(v.from)} – ${fmtShort(v.to)}`
}

const PRESETS = [
  { label: 'Today',       offset: 0  },
  { label: 'Last 7d',     offset: 7  },
  { label: 'Last 30d',    offset: 30 },
  { label: 'Last 3 mo',   offset: 90 },
]

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function DateRangePicker({ label, value, onChange }: Props) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  // Close on outside click (desktop only — mobile uses backdrop)
  useEffect(() => {
    function onMouseDown(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onMouseDown)
    return () => document.removeEventListener('mousedown', onMouseDown)
  }, [])

  // Prevent body scroll when mobile sheet is open
  useEffect(() => {
    if (open) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => { document.body.style.overflow = '' }
  }, [open])

  const selected: DateRange = {
    from: parseDate(value.from),
    to:   parseDate(value.to),
  }

  const hasValue     = !!(value.from || value.to)
  const displayRange = rangeLabel(value)

  function handleSelect(range: DateRange | undefined) {
    if (!range) { onChange({}); return }
    const next: DateRangeValue = {
      from: range.from ? toStr(range.from) : undefined,
      to:   range.to   ? toStr(range.to)   : undefined,
    }
    onChange(next)
    if (range.from && range.to) setOpen(false)
  }

  function applyPreset(offset: number) {
    const to   = new Date()
    const from = new Date()
    from.setDate(from.getDate() - offset)
    onChange({ from: toStr(from), to: toStr(to) })
    setOpen(false)
  }

  function clear(e: React.MouseEvent) {
    e.stopPropagation()
    onChange({})
    setOpen(false)
  }

  // Shared panel content rendered inside both mobile sheet and desktop dropdown
  const panelContent = (
    <>
      {/* Presets */}
      <div className="flex gap-1 p-2 border-b border-c-border">
        {PRESETS.map((p) => (
          <button
            key={p.label}
            onClick={() => applyPreset(p.offset)}
            className="flex-1 px-2 py-1.5 text-xs font-medium rounded-lg text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors whitespace-nowrap"
          >
            {p.label}
          </button>
        ))}
      </div>

      {/* Calendar */}
      <div className="p-3 flex justify-center">
        <DayPicker
          mode="range"
          selected={selected}
          onSelect={handleSelect}
          showOutsideDays
          style={{
            '--rdp-accent-color':            '#29B6F6',
            '--rdp-accent-background-color': 'rgba(41,182,246,0.12)',
            '--rdp-day_button-border-radius': '999px',
          } as React.CSSProperties}
        />
      </div>

      {/* Footer — clear */}
      {hasValue && (
        <div className="px-3 pb-3 pt-0 flex justify-end">
          <button
            onClick={(e) => clear(e)}
            className="text-xs text-c-muted hover:text-red-500 transition-colors"
          >
            Clear
          </button>
        </div>
      )}
    </>
  )

  return (
    <div ref={ref} className="relative">
      {/* ── Trigger button ──────────────────────────────────────── */}
      <button
        onClick={() => setOpen((v) => !v)}
        className={[
          'flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm border transition-colors select-none',
          open || hasValue
            ? 'border-[#2E7A8E] bg-[#2E7A8E]/5 text-[#2E7A8E] dark:border-[#29B6F6] dark:bg-[#29B6F6]/5 dark:text-[#29B6F6]'
            : 'border-c-border bg-white dark:bg-[#1B2838] text-c-muted hover:border-[#8A9BAE]/50 hover:text-[#0D1B2A] dark:hover:text-white',
        ].join(' ')}
      >
        <CalendarDays size={13} strokeWidth={1.8} className="shrink-0" />
        <span className="font-medium whitespace-nowrap">{label}</span>
        {displayRange ? (
          <>
            <span className="font-semibold whitespace-nowrap">{displayRange}</span>
            <X
              size={12}
              onClick={clear}
              className="shrink-0 ml-0.5 hover:text-red-500 transition-colors"
            />
          </>
        ) : (
          <>
            <span className="opacity-60 whitespace-nowrap">Any</span>
            <ChevronDown
              size={12}
              className={`shrink-0 transition-transform duration-150 ${open ? 'rotate-180' : ''}`}
            />
          </>
        )}
      </button>

      {open && (
        <>
          {/* ── Mobile: fixed bottom sheet ──────────────────────── */}
          <div className="sm:hidden fixed inset-0 z-50 flex flex-col justify-end">
            {/* Backdrop */}
            <div
              className="absolute inset-0 bg-black/40"
              onClick={() => setOpen(false)}
            />
            {/* Sheet */}
            <div className="relative bg-white dark:bg-[#1B2838] rounded-t-2xl shadow-2xl overflow-hidden max-h-[90dvh] overflow-y-auto">
              {/* Handle */}
              <div className="flex justify-center pt-3 pb-1">
                <div className="w-10 h-1 rounded-full bg-[#8A9BAE]/30" />
              </div>
              {/* Close row */}
              <div className="flex items-center justify-between px-4 pb-2">
                <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">{label}</p>
                <button
                  onClick={() => setOpen(false)}
                  className="p-1.5 rounded-lg text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
                >
                  <X size={16} strokeWidth={2} />
                </button>
              </div>
              {panelContent}
            </div>
          </div>

          {/* ── Desktop: absolute dropdown ────────────────────── */}
          <div className="hidden sm:block absolute top-full mt-2 right-0 z-50 bg-white dark:bg-[#1B2838] border border-c-border rounded-2xl shadow-2xl overflow-hidden min-w-[300px]">
            {panelContent}
          </div>
        </>
      )}
    </div>
  )
}
