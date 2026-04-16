import { useState, useRef, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, FileText, Users, Loader2 } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { useDebounce } from '@/hooks/useDebounce'
import { listInvoices } from '@/api/invoice.api'
import { listCustomers } from '@/api/customer.api'
import { formatCurrency } from '@/lib/format'
import type { InvoiceResponse, LifeCycleStatus } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'

// ---------------------------------------------------------------------------
// Status badge config
// ---------------------------------------------------------------------------

const STATUS_STYLES: Record<LifeCycleStatus, string> = {
  DRAFT:          'bg-[#F4F7F9] dark:bg-white/10 text-c-muted',
  ISSUED:         'bg-blue-100 dark:bg-blue-500/15 text-blue-700 dark:text-blue-400',
  PARTIALLY_PAID: 'bg-amber-100 dark:bg-amber-500/15 text-amber-700 dark:text-amber-400',
  PAID:           'bg-green-100 dark:bg-green-500/15 text-green-700 dark:text-green-400',
  CANCELLED:      'bg-[#F4F7F9] dark:bg-white/10 text-c-muted line-through',
}

const STATUS_LABEL: Record<LifeCycleStatus, string> = {
  DRAFT:          'Draft',
  ISSUED:         'Issued',
  PARTIALLY_PAID: 'Partial',
  PAID:           'Paid',
  CANCELLED:      'Cancelled',
}

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type ResultItem =
  | { type: 'invoice'; data: InvoiceResponse }
  | { type: 'client';  data: CustomerResponse }

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const MIN_CHARS   = 2
const MAX_RESULTS = 5

// ---------------------------------------------------------------------------
// GlobalSearch
// ---------------------------------------------------------------------------

export default function GlobalSearch() {
  const navigate = useNavigate()
  const orgId    = useAuthStore((s) => s.org?.id ?? '')
  const currency = useAuthStore((s) => s.org?.currency ?? 'USD')

  const [query,        setQuery]        = useState('')
  const [open,         setOpen]         = useState(false)
  const [focusedIndex, setFocusedIndex] = useState(-1)

  const debouncedQuery = useDebounce(query, 300)
  const containerRef   = useRef<HTMLDivElement>(null)
  const inputRef       = useRef<HTMLInputElement>(null)
  const itemRefs       = useRef<(HTMLButtonElement | null)[]>([])

  const enabled = debouncedQuery.trim().length >= MIN_CHARS

  const invoicesQuery = useQuery({
    queryKey:  ['global-search', 'invoices', orgId, debouncedQuery],
    queryFn:   () => listInvoices(orgId, { invoiceNumber: debouncedQuery.trim(), size: MAX_RESULTS }),
    enabled:   !!orgId && enabled,
    staleTime: 30_000,
  })

  const clientsQuery = useQuery({
    queryKey:  ['global-search', 'clients', orgId, debouncedQuery],
    queryFn:   () => listCustomers(orgId, { name: debouncedQuery.trim(), size: MAX_RESULTS }),
    enabled:   !!orgId && enabled,
    staleTime: 30_000,
  })

  const invoices: InvoiceResponse[]  = invoicesQuery.data?.content ?? []
  const clients:  CustomerResponse[] = clientsQuery.data?.content  ?? []
  const isLoading  = enabled && (invoicesQuery.isFetching || clientsQuery.isFetching)
  const hasResults = invoices.length > 0 || clients.length > 0
  const showEmpty  = enabled && !isLoading && !hasResults

  // Flat list used for index-based keyboard navigation
  const allItems: ResultItem[] = [
    ...invoices.map((data): ResultItem => ({ type: 'invoice', data })),
    ...clients.map((data):  ResultItem => ({ type: 'client',  data })),
  ]

  // Open/reset when debounced query changes
  useEffect(() => {
    setOpen(enabled)
    setFocusedIndex(-1)
    itemRefs.current = []
  }, [enabled, debouncedQuery])

  // Close on click outside
  const handlePointerDown = useCallback((e: PointerEvent) => {
    if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
      setOpen(false)
      setFocusedIndex(-1)
    }
  }, [])

  useEffect(() => {
    document.addEventListener('pointerdown', handlePointerDown)
    return () => document.removeEventListener('pointerdown', handlePointerDown)
  }, [handlePointerDown])

  // Scroll focused item into view
  useEffect(() => {
    if (focusedIndex >= 0) {
      itemRefs.current[focusedIndex]?.scrollIntoView({ block: 'nearest' })
    }
  }, [focusedIndex])

  function handleKeyDown(e: React.KeyboardEvent) {
    if (!open) return

    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setFocusedIndex((prev) => Math.min(prev + 1, allItems.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setFocusedIndex((prev) => Math.max(prev - 1, -1))
    } else if (e.key === 'Enter' && focusedIndex >= 0) {
      e.preventDefault()
      select(allItems[focusedIndex])
    } else if (e.key === 'Escape') {
      setOpen(false)
      setFocusedIndex(-1)
      inputRef.current?.blur()
    }
  }

  function select(item: ResultItem) {
    setQuery('')
    setOpen(false)
    setFocusedIndex(-1)
    if (item.type === 'invoice') {
      navigate(`/invoices/${item.data.id}`)
    } else {
      navigate(`/clients/${item.data.id}`)
    }
  }

  function itemClass(index: number) {
    return [
      'w-full flex items-center gap-3 px-4 py-2.5 transition-colors text-left',
      focusedIndex === index
        ? 'bg-[#F4F7F9] dark:bg-[#243447]'
        : 'hover:bg-[#F4F7F9] dark:hover:bg-[#243447]',
    ].join(' ')
  }

  return (
    <div ref={containerRef} className="relative w-full max-w-md">

      {/* Input */}
      <div className="relative">
        <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-c-muted pointer-events-none" />
        {isLoading && (
          <Loader2 size={13} className="absolute right-3 top-1/2 -translate-y-1/2 text-c-muted animate-spin pointer-events-none" />
        )}
        <input
          ref={inputRef}
          type="text"
          placeholder="Search invoices / clients..."
          value={query}
          onChange={(e) => { setQuery(e.target.value); setFocusedIndex(-1) }}
          onFocus={() => { if (enabled) setOpen(true) }}
          onKeyDown={handleKeyDown}
          className="w-full pl-8 pr-8 py-1.5 text-sm bg-[#F4F7F9] dark:bg-[#243447] rounded-lg border border-transparent focus:border-[#8A9BAE]/40 focus:outline-none text-[#0D1B2A] dark:text-white placeholder:text-c-muted transition-colors"
        />
      </div>

      {/* Dropdown */}
      {open && (
        <div className="absolute top-full left-0 right-0 mt-1.5 bg-white dark:bg-[#1B2838] border border-c-border rounded-xl shadow-xl z-50 overflow-hidden max-h-[420px] overflow-y-auto">

          {/* Loading skeleton */}
          {isLoading && !hasResults && (
            <div className="py-3 px-4 space-y-2.5">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="flex items-center gap-3 animate-pulse">
                  <div className="w-6 h-6 rounded-lg bg-[#F4F7F9] dark:bg-white/10 shrink-0" />
                  <div className="flex-1 space-y-1.5">
                    <div className="h-3 w-32 rounded bg-[#F4F7F9] dark:bg-white/10" />
                    <div className="h-2.5 w-20 rounded bg-[#F4F7F9] dark:bg-white/10" />
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* No results */}
          {showEmpty && (
            <div className="py-8 text-center">
              <p className="text-sm text-c-muted">
                No results for{' '}
                <span className="font-medium text-[#0D1B2A] dark:text-white">"{debouncedQuery}"</span>
              </p>
            </div>
          )}

          {/* Invoices section */}
          {invoices.length > 0 && (
            <div>
              <p className="px-4 pt-3 pb-1.5 text-[10px] font-bold uppercase tracking-widest text-c-muted flex items-center gap-1.5">
                <FileText size={10} strokeWidth={2.5} /> Invoices
              </p>
              {invoices.map((inv, i) => (
                <button
                  key={inv.id}
                  ref={(el) => { itemRefs.current[i] = el }}
                  onPointerDown={(e) => { e.preventDefault(); select({ type: 'invoice', data: inv }) }}
                  onMouseEnter={() => setFocusedIndex(i)}
                  className={itemClass(i)}
                >
                  <div className="w-7 h-7 rounded-lg bg-blue-500/10 flex items-center justify-center shrink-0">
                    <FileText size={13} className="text-blue-500" strokeWidth={1.8} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-[#0D1B2A] dark:text-white truncate">{inv.invoiceNumber}</p>
                    <p className="text-xs text-c-muted">
                      {formatCurrency(inv.totalAmount, currency)}
                      {inv.dueDate ? ` · Due ${inv.dueDate}` : ''}
                    </p>
                  </div>
                  <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded-full shrink-0 ${STATUS_STYLES[inv.lifeCycleStatus]}`}>
                    {STATUS_LABEL[inv.lifeCycleStatus]}
                  </span>
                </button>
              ))}
            </div>
          )}

          {/* Divider */}
          {invoices.length > 0 && clients.length > 0 && (
            <div className="border-t border-c-border mx-4 my-1" />
          )}

          {/* Clients section */}
          {clients.length > 0 && (
            <div>
              <p className="px-4 pt-3 pb-1.5 text-[10px] font-bold uppercase tracking-widest text-c-muted flex items-center gap-1.5">
                <Users size={10} strokeWidth={2.5} /> Clients
              </p>
              {clients.map((c, i) => {
                const idx = invoices.length + i
                return (
                  <button
                    key={c.id}
                    ref={(el) => { itemRefs.current[idx] = el }}
                    onPointerDown={(e) => { e.preventDefault(); select({ type: 'client', data: c }) }}
                    onMouseEnter={() => setFocusedIndex(idx)}
                    className={itemClass(idx)}
                  >
                    <div className="w-7 h-7 rounded-lg bg-[#29B6F6]/10 flex items-center justify-center shrink-0 text-[11px] font-bold text-[#29B6F6]">
                      {c.name.charAt(0).toUpperCase()}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-[#0D1B2A] dark:text-white truncate">{c.name}</p>
                      {c.email && <p className="text-xs text-c-muted truncate">{c.email}</p>}
                    </div>
                  </button>
                )
              })}
            </div>
          )}

          {hasResults && <div className="h-2" />}

        </div>
      )}
    </div>
  )
}
