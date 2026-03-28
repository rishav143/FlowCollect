import { Plus, Trash2 } from 'lucide-react'
import { formatCurrency } from '@/lib/format'

export interface LineItem {
  description: string
  quantity:    number
  unitPrice:   number
}

interface Props {
  items:    LineItem[]
  onChange: (items: LineItem[]) => void
  currency: string
}

const inputCls = [
  'w-full px-3 py-2 text-sm bg-[#F4F7F9] dark:bg-[#243447] rounded-lg',
  'border border-transparent focus:border-[#8A9BAE]/40 focus:outline-none',
  'text-[#0D1B2A] dark:text-white placeholder:text-c-muted transition-colors',
].join(' ')

export default function LineItemsField({ items, onChange, currency }: Props) {
  function update(idx: number, field: keyof LineItem, value: string | number) {
    const next = items.map((item, i) =>
      i === idx ? { ...item, [field]: value } : item,
    )
    onChange(next)
  }

  function add() {
    onChange([...items, { description: '', quantity: 1, unitPrice: 0 }])
  }

  function remove(idx: number) {
    onChange(items.filter((_, i) => i !== idx))
  }

  const subtotal = items.reduce((s, it) => s + it.quantity * it.unitPrice, 0)

  return (
    <div className="space-y-2">
      {/* Header */}
      <div className="grid grid-cols-[1fr_60px_100px_80px_28px] gap-2 px-1">
        {['Description', 'Qty', 'Unit Price', 'Amount', ''].map((h) => (
          <span key={h} className="text-[11px] font-semibold uppercase tracking-wide text-c-muted">
            {h}
          </span>
        ))}
      </div>

      {/* Rows */}
      {items.map((item, idx) => (
        <div key={idx} className="grid grid-cols-[1fr_60px_100px_80px_28px] gap-2 items-center">
          <input
            type="text"
            placeholder="Description"
            value={item.description}
            onChange={(e) => update(idx, 'description', e.target.value)}
            className={inputCls}
          />
          <input
            type="number"
            min={1}
            step={1}
            value={item.quantity}
            onChange={(e) => update(idx, 'quantity', Math.max(1, parseInt(e.target.value) || 1))}
            className={inputCls + ' text-center'}
          />
          <input
            type="number"
            min={0}
            step={0.01}
            value={item.unitPrice}
            onChange={(e) => update(idx, 'unitPrice', parseFloat(e.target.value) || 0)}
            className={inputCls}
          />
          <span className="text-sm text-right font-medium text-[#0D1B2A] dark:text-white tabular-nums">
            {formatCurrency(item.quantity * item.unitPrice, currency, { decimals: false })}
          </span>
          <button
            type="button"
            onClick={() => remove(idx)}
            disabled={items.length === 1}
            className="p-1 rounded text-c-muted hover:text-red-500 disabled:opacity-30 transition-colors"
          >
            <Trash2 size={14} strokeWidth={2} />
          </button>
        </div>
      ))}

      {/* Add row */}
      <button
        type="button"
        onClick={add}
        className="flex items-center gap-1.5 text-sm text-[#29B6F6] hover:opacity-80 transition-opacity mt-1"
      >
        <Plus size={14} strokeWidth={2.5} />
        Add line item
      </button>

      {/* Subtotal */}
      <div className="flex justify-end pt-2 border-t border-c-border">
        <span className="text-sm text-c-muted">
          Subtotal: <span className="font-semibold text-[#0D1B2A] dark:text-white">{formatCurrency(subtotal, currency, { decimals: false })}</span>
        </span>
      </div>
    </div>
  )
}
