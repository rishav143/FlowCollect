import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
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

export interface InvoiceFormValues {
  invoiceNumber:     string
  customerId:        string
  dueDate:           string
  taxLabel:          string   // e.g. "GST 9%", "VAT 20%"
  taxPercentage:     number
  discountPercentage: number
  items:             LineItem[]
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
  const [taxLabel,       setTaxLabel]       = useState(initial?.taxLabel       ?? '')
  const [taxPercentage,  setTaxPercentage]  = useState(initial?.taxPercentage  ?? 0)
  const [discountPercentage, setDiscountPercentage] = useState(initial?.discountPercentage ?? 0)
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
    onChange({ invoiceNumber, customerId, dueDate, taxLabel, taxPercentage, discountPercentage, items })
  }, [invoiceNumber, customerId, dueDate, taxLabel, taxPercentage, discountPercentage, items]) // eslint-disable-line react-hooks/exhaustive-deps

  const subtotal         = Math.round(items.reduce((s, it) => s + it.quantity * it.unitPrice, 0))
  const taxAmount        = taxPercentage > 0 ? Math.round(subtotal * taxPercentage / 100) : 0
  const discountAmount   = discountPercentage > 0 ? Math.round(subtotal * discountPercentage / 100) : 0
  const total            = Math.max(0, subtotal + taxAmount - discountAmount)
  const showSummary      = taxPercentage > 0 || discountPercentage > 0

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

      {/* Row 2: Due date + Discount */}
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
        <div>
          <label className={labelCls}>Discount (%)</label>
          <input
            type="number"
            min={0}
            placeholder="0"
            value={discountPercentage || ''}
            onChange={(e) => setDiscountPercentage(parseFloat(e.target.value) || 0)}
            className={inputCls}
          />
        </div>
      </div>

      {/* Row 3: Tax label + Tax % */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className={labelCls}>Tax Label</label>
          <input
            type="text"
            placeholder="e.g. GST, VAT, Tax"
            value={taxLabel}
            onChange={(e) => setTaxLabel(e.target.value)}
            className={inputCls}
          />
        </div>
        <div>
          <label className={labelCls}>Tax (%)</label>
          <input
            type="number"
            min={0}
            max={100}
            step={0.01}
            placeholder="0"
            value={taxPercentage || ''}
            onChange={(e) => setTaxPercentage(parseFloat(e.target.value) || 0)}
            className={inputCls}
          />
        </div>
      </div>

      {/* Line items */}
      <div>
        <label className={labelCls}>Line Items *</label>
        <LineItemsField items={items} onChange={setItems} currency={currency} />
      </div>

      {/* Summary — only shown when tax or discount is set */}
      {showSummary && (
        <div className="bg-[#F4F7F9] dark:bg-[#243447] rounded-lg p-3 space-y-1 text-sm">
          <div className="flex justify-between text-c-muted">
            <span>Subtotal</span>
            <span>{formatCurrency(subtotal, currency, { decimals: false })}</span>
          </div>
          {taxAmount > 0 && (
            <div className="flex justify-between text-c-muted">
              <span>{taxLabel || 'Tax'} ({taxPercentage}%)</span>
              <span>{formatCurrency(taxAmount, currency, { decimals: false })}</span>
            </div>
          )}
          {discountAmount > 0 && (
            <div className="flex justify-between text-c-muted">
              <span>Discount ({discountPercentage}%)</span>
              <span className="text-red-500">-{formatCurrency(discountAmount, currency, { decimals: false })}</span>
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
