import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Plus, X } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { listCustomers } from '@/api/customer.api'
import LineItemsField, { type LineItem } from './LineItemsField'
import { formatCurrency } from '@/lib/format'

const inputCls = [
  'w-full px-3 py-2 text-sm bg-[#F4F7F9] dark:bg-[#243447] rounded-lg',
  'border border-transparent focus:border-[#8A9BAE]/40 focus:outline-none',
  'text-[#0D1B2A] dark:text-white placeholder:text-c-muted transition-colors',
].join(' ')

const labelCls = 'block text-xs font-semibold text-c-muted uppercase tracking-wide mb-1.5'

export interface TaxLineValue {
  label:  string
  amount: number
}

export interface InvoiceFormValues {
  invoiceNumber:  string
  customerId:     string
  dueDate:        string
  taxLines:       TaxLineValue[]
  discountAmount: number
  items:          LineItem[]
}

interface Props {
  initial?:  Partial<InvoiceFormValues>
  onChange:  (v: InvoiceFormValues) => void
  currency:  string
}

export default function InvoiceForm({ initial, onChange, currency }: Props) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  const [invoiceNumber,  setInvoiceNumber]  = useState(initial?.invoiceNumber  ?? '')
  const [customerId,     setCustomerId]     = useState(initial?.customerId     ?? '')
  const [dueDate,        setDueDate]        = useState(initial?.dueDate        ?? '')
  const [taxLines,       setTaxLines]       = useState<TaxLineValue[]>(initial?.taxLines ?? [])
  const [discountAmount, setDiscountAmount] = useState(initial?.discountAmount ?? 0)
  const [items,          setItems]          = useState<LineItem[]>(
    initial?.items ?? [{ description: '', quantity: 1, unitPrice: 0 }],
  )

  const { data: customersPage } = useQuery({
    queryKey: ['customers', orgId, 'all'],
    queryFn:  () => listCustomers(orgId, { size: 200, active: true }),
    enabled:  !!orgId,
  })
  const customers = customersPage?.content ?? []

  useEffect(() => {
    onChange({ invoiceNumber, customerId, dueDate, taxLines, discountAmount, items })
  }, [invoiceNumber, customerId, dueDate, taxLines, discountAmount, items]) // eslint-disable-line react-hooks/exhaustive-deps

  function addTaxLine() {
    setTaxLines((prev) => [...prev, { label: 'Tax', amount: 0 }])
  }

  function removeTaxLine(index: number) {
    setTaxLines((prev) => prev.filter((_, i) => i !== index))
  }

  function updateTaxLine(index: number, field: keyof TaxLineValue, value: string | number) {
    setTaxLines((prev) =>
      prev.map((tl, i) => (i === index ? { ...tl, [field]: value } : tl)),
    )
  }

  const subtotal    = Math.round(items.reduce((s, it) => s + it.quantity * it.unitPrice, 0))
  const taxTotal    = Math.round(taxLines.reduce((s, tl) => s + tl.amount, 0))
  const discount    = Math.round(discountAmount)
  const total       = Math.max(0, subtotal + taxTotal - discount)
  const showSummary = taxLines.length > 0 || discountAmount > 0

  return (
    <div className="space-y-5">
      {/* Row 1: Invoice number + Customer */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className={labelCls}>Invoice Number *</label>
          <input
            type="text"
            placeholder="INV-001"
            value={invoiceNumber}
            onChange={(e) => setInvoiceNumber(e.target.value)}
            className={inputCls}
          />
        </div>
        <div>
          <label className={labelCls}>Customer</label>
          <select
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
            className={inputCls}
          >
            <option value="">— No customer —</option>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}{c.companyName ? ` (${c.companyName})` : ''}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Due date */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className={labelCls}>Due Date</label>
          <input
            type="date"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            className={inputCls}
          />
        </div>
      </div>

      {/* Line items */}
      <div>
        <label className={labelCls}>Line Items *</label>
        <LineItemsField items={items} onChange={setItems} currency={currency} />
      </div>

      {/* Charges & Taxes */}
      <div>
        <div className="flex items-center justify-between mb-1.5">
          <label className={labelCls + ' mb-0'}>Charges & Taxes</label>
          <button
            type="button"
            onClick={addTaxLine}
            className="flex items-center gap-1 text-xs text-[#29B6F6] hover:text-[#4FC3F7] font-medium transition-colors"
          >
            <Plus size={13} />
            Add
          </button>
        </div>
        {taxLines.length === 0 && (
          <p className="text-xs text-c-muted">Click "Add" to include GST, VAT, shipping, service fees, etc.</p>
        )}
        <div className="space-y-2">
          {taxLines.map((tl, i) => (
            <div key={i} className="flex gap-2 items-center">
              <input
                type="text"
                placeholder="e.g. GST 9%, Shipping, Service Fee"
                value={tl.label}
                onChange={(e) => updateTaxLine(i, 'label', e.target.value)}
                className={inputCls + ' flex-1'}
              />
              <input
                type="number"
                min={0}
                placeholder="Amount"
                value={tl.amount || ''}
                onChange={(e) => updateTaxLine(i, 'amount', parseFloat(e.target.value) || 0)}
                className={inputCls + ' w-28'}
              />
              <button
                type="button"
                onClick={() => removeTaxLine(i)}
                className="p-1.5 rounded-lg text-c-muted hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors shrink-0"
              >
                <X size={14} />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Discount */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className={labelCls}>Discount (flat amount)</label>
          <input
            type="number"
            min={0}
            placeholder="0"
            value={discountAmount || ''}
            onChange={(e) => setDiscountAmount(parseFloat(e.target.value) || 0)}
            className={inputCls}
          />
        </div>
      </div>

      {/* Summary */}
      {showSummary && (
        <div className="bg-[#F4F7F9] dark:bg-[#243447] rounded-lg p-3 space-y-1 text-sm">
          <div className="flex justify-between text-c-muted">
            <span>Subtotal</span>
            <span>{formatCurrency(subtotal, currency, { decimals: false })}</span>
          </div>
          {taxLines.map((tl, i) => (
            <div key={i} className="flex justify-between text-c-muted">
              <span>{tl.label || `Tax line ${i + 1}`}</span>
              <span>{formatCurrency(tl.amount, currency, { decimals: false })}</span>
            </div>
          ))}
          {discount > 0 && (
            <div className="flex justify-between text-c-muted">
              <span>Discount</span>
              <span className="text-red-500">-{formatCurrency(discount, currency, { decimals: false })}</span>
            </div>
          )}
          <div className="flex justify-between font-semibold text-[#0D1B2A] dark:text-white border-t border-[#8A9BAE]/20 pt-1 mt-1">
            <span>Total</span>
            <span>{formatCurrency(total, currency, { decimals: false })}</span>
          </div>
        </div>
      )}
    </div>
  )
}
