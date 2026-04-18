import { useEffect, useRef, useState, useCallback } from 'react'
import { createPortal } from 'react-dom'
import { Check, ChevronDown } from 'lucide-react'

export interface SelectOption {
  value: string
  label: string
}

interface Props {
  value:        string
  onChange:     (value: string) => void
  options:      SelectOption[]
  placeholder?: string
  disabled?:    boolean
  error?:       boolean
  className?:   string
}

interface DropPos {
  top:       number
  left:      number
  width:     number
  maxHeight: number
  openUp:    boolean
}

export default function Select({
  value,
  onChange,
  options,
  placeholder = 'Select…',
  disabled    = false,
  error       = false,
  className   = '',
}: Props) {
  const [open, setOpen]       = useState(false)
  const [dropPos, setDropPos] = useState<DropPos>({ top: 0, left: 0, width: 0, maxHeight: 240, openUp: false })

  const triggerRef = useRef<HTMLButtonElement>(null)
  const listRef    = useRef<HTMLUListElement>(null)

  const recalcPos = useCallback(() => {
    if (!triggerRef.current) return
    const r        = triggerRef.current.getBoundingClientRect()
    const GAP      = 6
    const MARGIN   = 8
    const MAX      = 240
    const MIN_FLIP = 120

    const spaceBelow = window.innerHeight - r.bottom - GAP - MARGIN
    const spaceAbove = r.top - GAP - MARGIN
    const openUp     = spaceBelow < MIN_FLIP && spaceAbove > spaceBelow

    setDropPos({
      top:       openUp ? r.top - GAP : r.bottom + GAP,
      left:      r.left,
      width:     r.width,
      maxHeight: Math.min(MAX, openUp ? spaceAbove : spaceBelow),
      openUp,
    })
  }, [])

  useEffect(() => {
    if (!open) return
    recalcPos()

    function onPointerDown(e: PointerEvent) {
      const t = e.target as Node
      if (
        !(triggerRef.current?.contains(t)) &&
        !(listRef.current?.contains(t))
      ) setOpen(false)
    }
    function onScroll(e: Event) {
      if (listRef.current && listRef.current.contains(e.target as Node)) return
      setOpen(false)
    }
    function onResize() { recalcPos() }

    document.addEventListener('pointerdown', onPointerDown, true)
    window.addEventListener('scroll', onScroll, true)
    window.addEventListener('resize', onResize)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown, true)
      window.removeEventListener('scroll', onScroll, true)
      window.removeEventListener('resize', onResize)
    }
  }, [open, recalcPos])

  const selected = options.find((o) => o.value === value)

  const triggerCls = [
    'w-full min-w-[5rem] flex items-center justify-between px-3 py-2 text-sm rounded-lg border transition-colors focus:outline-none',
    'bg-[#F4F7F9] dark:bg-[#243447]',
    selected ? 'border-[#8A9BAE]/50 dark:border-[#8A9BAE]/40' : 'border-transparent',
    error    ? '!border-red-400'               : '',
    disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer',
  ].join(' ')

  return (
    <div className={`relative ${className}`}>
      <button
        ref={triggerRef}
        type="button"
        onClick={() => !disabled && setOpen((o) => !o)}
        className={triggerCls}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <span className={selected ? 'text-[#0D1B2A] dark:text-white' : 'text-c-muted'}>
          {selected ? selected.label : placeholder}
        </span>
        <ChevronDown
          size={14}
          strokeWidth={2}
          className={`shrink-0 ml-2 text-c-muted transition-transform ${open ? 'rotate-180' : ''}`}
        />
      </button>

      {/* Native select — mobile only (< sm). Sits invisibly over the trigger so
          tapping opens the OS picker (wheel on iOS, sheet on Android).
          Eliminates all scroll-vs-tap bugs on touch devices. */}
      {!disabled && (
        <select
          className="absolute inset-0 w-full h-full opacity-0 sm:hidden cursor-pointer"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          aria-hidden="true"
          tabIndex={-1}
        >
          {!value && <option value="" disabled>{placeholder}</option>}
          {options.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
      )}

      {/* Custom dropdown — desktop only (sm+) */}
      {open && createPortal(
        <ul
          ref={listRef}
          role="listbox"
          style={{
            position:  'fixed',
            top:       dropPos.openUp ? undefined : dropPos.top,
            bottom:    dropPos.openUp ? window.innerHeight - dropPos.top : undefined,
            left:      dropPos.left,
            width:     dropPos.width,
            zIndex:    9999,
            maxHeight: dropPos.maxHeight,
            overflowY: 'auto',
          }}
          className="bg-white dark:bg-[#1B2838] border border-c-border rounded-lg shadow-xl"
        >
          {options.map((opt) => (
            <li
              key={opt.value}
              role="option"
              aria-selected={opt.value === value}
              onPointerDown={(e) => {
                e.preventDefault()
                onChange(opt.value)
                setOpen(false)
              }}
              className={[
                'flex items-center justify-between px-3 py-2.5 text-sm cursor-pointer transition-colors whitespace-nowrap',
                opt.value === value
                  ? 'bg-[#29B6F6]/10 text-[#0D1B2A] dark:text-white font-medium'
                  : 'text-[#0D1B2A] dark:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447]',
              ].join(' ')}
            >
              {opt.label}
              {opt.value === value && <Check size={13} strokeWidth={2.5} className="text-[#29B6F6] shrink-0 ml-2" />}
            </li>
          ))}
        </ul>,
        document.body,
      )}
    </div>
  )
}
