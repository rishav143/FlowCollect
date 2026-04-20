import { useState, useRef, useEffect, useCallback } from 'react'
import { createPortal } from 'react-dom'
import { DayPicker } from 'react-day-picker'
import { CalendarDays, ChevronDown, X } from 'lucide-react'
import 'react-day-picker/style.css'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface Props {
  label:      string
  value:      string        // YYYY-MM-DD or ''
  onChange:   (v: string) => void
  timezone?:  string        // org timezone, e.g. 'Asia/Kolkata' — defaults to 'UTC'
}

// ---------------------------------------------------------------------------
// Helpers — same conventions as DateRangePicker
// ---------------------------------------------------------------------------

function toStr(d: Date) {
  const y  = d.getFullYear()
  const m  = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

function parseDate(s: string): Date | undefined {
  return s ? new Date(s + 'T12:00:00') : undefined
}

function fmtShort(s: string, tz: string) {
  return new Date(s + 'T12:00:00').toLocaleDateString('en-GB', {
    timeZone: tz,
    day: 'numeric', month: 'short', year: 'numeric',
  })
}

/** Returns today's date (at noon) in the given org timezone. */
function todayInTz(tz: string): Date {
  const ymd = new Intl.DateTimeFormat('en-CA', {
    timeZone: tz,
    year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(new Date())       // → "YYYY-MM-DD"
  return new Date(ymd + 'T12:00:00')
}

const PRESETS = [
  { label: '7d',  offset: 7  },
  { label: '15d', offset: 15 },
  { label: '30d', offset: 30 },
  { label: '60d', offset: 60 },
]

// Compact DayPicker CSS — smaller than standalone DateRangePicker since this
// lives inside dense forms/modals
const DAY_PICKER_STYLE = {
  '--rdp-accent-color':             '#29B6F6',
  '--rdp-accent-background-color':  'rgba(41,182,246,0.12)',
  '--rdp-day_button-border-radius': '999px',
  '--rdp-day_button-width':         '26px',
  '--rdp-day_button-height':        '26px',
  '--rdp-day-width':                '26px',
  '--rdp-day-height':               '26px',
  '--rdp-weekday-padding':          '0 0 2px 0',
  '--rdp-month-caption-font':       '500 12px/1.2 inherit',
  '--rdp-weekday-font':             '500 10px/1 inherit',
  '--rdp-day-font':                 '11px/1 inherit',
} as React.CSSProperties

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function DatePicker({ label, value, onChange, timezone = 'UTC' }: Props) {
  const [open, setOpen]         = useState(false)
  const [dropPos, setDropPos]   = useState<{ top: number; left: number; width: number } | null>(null)
  const triggerRef              = useRef<HTMLButtonElement>(null)
  const panelRef                = useRef<HTMLDivElement>(null)

  const isMobile = () => window.innerWidth < 640   // matches Tailwind sm:

  // Calculate fixed position for the portal dropdown relative to the trigger
  const recalcPos = useCallback(() => {
    if (!triggerRef.current) return
    const r = triggerRef.current.getBoundingClientRect()
    setDropPos({ top: r.bottom + 6, left: r.left, width: r.width })
  }, [])

  function handleOpen() {
    if (!open) recalcPos()
    setOpen((v) => !v)
  }

  // Close on outside click / scroll / resize
  useEffect(() => {
    if (!open) return

    function close(e: MouseEvent) {
      if (
        panelRef.current && !panelRef.current.contains(e.target as Node) &&
        triggerRef.current && !triggerRef.current.contains(e.target as Node)
      ) setOpen(false)
    }

    function onScroll() { recalcPos() }
    function onResize() { recalcPos() }

    document.addEventListener('mousedown', close)
    window.addEventListener('scroll', onScroll, true)
    window.addEventListener('resize', onResize)
    return () => {
      document.removeEventListener('mousedown', close)
      window.removeEventListener('scroll', onScroll, true)
      window.removeEventListener('resize', onResize)
    }
  }, [open, recalcPos])

  // Lock body scroll on mobile sheet
  useEffect(() => {
    if (open && isMobile()) document.body.style.overflow = 'hidden'
    else document.body.style.overflow = ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  const selected  = parseDate(value)
  const hasValue  = !!value
  const orgToday  = todayInTz(timezone)

  function handleSelect(day: Date | undefined) {
    onChange(day ? toStr(day) : '')
    if (day) setOpen(false)
  }

  function applyPreset(offset: number) {
    const d = todayInTz(timezone)
    d.setDate(d.getDate() + offset)
    onChange(toStr(d))
    setOpen(false)
  }

  function clear(e: React.MouseEvent) {
    e.stopPropagation()
    onChange('')
    setOpen(false)
  }

  // Shared panel content
  const panelContent = (
    <>
      {/* Quick picks */}
      <div className="flex gap-1 p-2 border-b border-c-border">
        {PRESETS.map((p) => (
          <button
            key={p.label}
            type="button"
            onClick={() => applyPreset(p.offset)}
            className="flex-1 px-1.5 py-1 text-xs font-medium rounded-lg text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors whitespace-nowrap"
          >
            +{p.label}
          </button>
        ))}
      </div>

      {/* Calendar */}
      <div className="p-1.5 flex justify-center">
        <DayPicker
          mode="single"
          selected={selected}
          onSelect={handleSelect}
          showOutsideDays
          today={orgToday}
          style={DAY_PICKER_STYLE}
        />
      </div>

      {/* Clear */}
      {hasValue && (
        <div className="px-3 pb-2.5 pt-0 flex justify-end">
          <button
            type="button"
            onClick={clear}
            className="text-xs text-c-muted hover:text-red-500 transition-colors"
          >
            Clear
          </button>
        </div>
      )}
    </>
  )

  return (
    <div className="relative w-full">
      {/* ── Trigger ─────────────────────────────────────────────────── */}
      <button
        ref={triggerRef}
        type="button"
        onClick={handleOpen}
        className={[
          'w-full flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm border transition-colors select-none',
          open || hasValue
            ? 'border-[#2E7A8E] bg-[#2E7A8E]/5 text-[#2E7A8E] dark:border-[#29B6F6] dark:bg-[#29B6F6]/5 dark:text-[#29B6F6]'
            : 'border-c-border bg-[#F4F7F9] dark:bg-[#243447] text-c-muted hover:border-[#8A9BAE]/50 hover:text-[#0D1B2A] dark:hover:text-white',
        ].join(' ')}
      >
        <CalendarDays size={13} strokeWidth={1.8} className="shrink-0" />
        {hasValue ? (
          <>
            <span className="flex-1 font-semibold whitespace-nowrap text-[#0D1B2A] dark:text-white text-left">
              {fmtShort(value, timezone)}
            </span>
            <X size={12} onClick={clear} className="shrink-0 hover:text-red-500 transition-colors" />
          </>
        ) : (
          <>
            <span className="flex-1 font-medium whitespace-nowrap text-left">{label}</span>
            <ChevronDown
              size={12}
              className={`shrink-0 transition-transform duration-150 ${open ? 'rotate-180' : ''}`}
            />
          </>
        )}
      </button>

      {open && createPortal(
        // Single wrapper ref — covers both mobile sheet and desktop dropdown
        // so the outside-click handler correctly ignores taps inside either panel.
        <div ref={panelRef}>
          {/* ── Mobile: bottom sheet — z-[200] clears any modal z-50 ── */}
          <div className="sm:hidden fixed inset-0 z-[200] flex flex-col justify-end">
            <div className="absolute inset-0 bg-black/40" onClick={() => setOpen(false)} />
            <div className="relative bg-white dark:bg-[#1B2838] rounded-t-2xl shadow-2xl overflow-y-auto max-h-[90dvh]">
              <div className="flex justify-center pt-3 pb-1">
                <div className="w-10 h-1 rounded-full bg-[#8A9BAE]/30" />
              </div>
              <div className="flex items-center justify-between px-4 pb-2">
                <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">{label}</p>
                <button
                  type="button"
                  onClick={() => setOpen(false)}
                  className="p-1.5 rounded-lg text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
                >
                  <X size={16} strokeWidth={2} />
                </button>
              </div>
              {panelContent}
            </div>
          </div>

          {/* ── Desktop/tablet: fixed portal dropdown ──────────────── */}
          <div
            className="hidden sm:block fixed z-[200] bg-white dark:bg-[#1B2838] border border-c-border rounded-xl shadow-2xl overflow-hidden"
            style={{
              top:      dropPos?.top  ?? 0,
              left:     dropPos?.left ?? 0,
              minWidth: '240px',
            }}
          >
            {panelContent}
          </div>
        </div>,
        document.body,
      )}
    </div>
  )
}
