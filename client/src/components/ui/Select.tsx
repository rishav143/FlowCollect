import { useEffect, useRef, useState } from 'react'
import { Check, ChevronDown } from 'lucide-react'

export interface SelectOption {
  value: string
  label: string
}

interface Props {
  value:       string
  onChange:    (value: string) => void
  options:     SelectOption[]
  placeholder?: string
  disabled?:   boolean
  error?:      boolean
  className?:  string
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
  const [open, setOpen] = useState(false)
  const ref             = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function onPointerDown(e: PointerEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    return () => document.removeEventListener('pointerdown', onPointerDown)
  }, [])

  const selected = options.find((o) => o.value === value)

  const triggerCls = [
    'w-full flex items-center justify-between px-3 py-2 text-sm rounded-lg',
    'bg-[#F4F7F9] dark:bg-[#243447] transition-colors',
    'border focus:outline-none',
    error    ? 'border-red-400'          : 'border-transparent focus:border-[#8A9BAE]/40',
    disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer',
  ].join(' ')

  return (
    <div ref={ref} className={`relative ${className}`}>
      <button
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

      {open && (
        <ul
          role="listbox"
          className="absolute z-20 mt-1 w-full bg-white dark:bg-[#1B2838] border border-c-border rounded-lg shadow-lg overflow-hidden max-h-60 overflow-y-auto"
        >
          {options.map((opt) => (
            <li
              key={opt.value}
              role="option"
              aria-selected={opt.value === value}
              onClick={() => { onChange(opt.value); setOpen(false) }}
              className="flex items-center justify-between px-3 py-2.5 text-sm text-[#0D1B2A] dark:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] cursor-pointer transition-colors"
            >
              {opt.label}
              {opt.value === value && <Check size={13} strokeWidth={2.5} className="text-[#29B6F6] shrink-0 ml-2" />}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
