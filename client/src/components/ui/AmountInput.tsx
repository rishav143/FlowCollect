import { useState, useEffect } from 'react'
import { formatNumberInput, parseAmountInput } from '@/lib/format'

interface Props {
  value:       number
  currency:    string
  onChange:    (n: number) => void
  className?:  string
  placeholder?: string
  min?:        number
  max?:        number
}

/**
 * A text input that:
 * - Shows a locale-formatted number (e.g. 1,00,000 for INR) when blurred
 * - Strips formatting on focus so the user can type freely
 * - Calls onChange with the parsed integer on every change
 */
export default function AmountInput({
  value,
  currency,
  onChange,
  className = '',
  placeholder = '0',
  min,
  max,
}: Props) {
  const [display, setDisplay] = useState(() =>
    value ? formatNumberInput(value, currency) : '',
  )
  const [focused, setFocused] = useState(false)

  // Sync when the value changes externally (e.g. pre-fill on modal open)
  useEffect(() => {
    if (!focused) {
      setDisplay(value ? formatNumberInput(value, currency) : '')
    }
  }, [value, currency, focused])

  function handleFocus() {
    setFocused(true)
    setDisplay(value ? String(value) : '')
  }

  function handleBlur() {
    setFocused(false)
    const n = parseAmountInput(display)
    onChange(n)
    setDisplay(n ? formatNumberInput(n, currency) : '')
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const raw = e.target.value
    setDisplay(raw)
    const n = parseAmountInput(raw)
    if (max !== undefined && n > max) return
    onChange(n)
  }

  return (
    <input
      type="text"
      inputMode="numeric"
      value={display}
      placeholder={placeholder}
      onChange={handleChange}
      onFocus={handleFocus}
      onBlur={handleBlur}
      className={className}
      {...(min !== undefined ? { 'data-min': min } : {})}
    />
  )
}
