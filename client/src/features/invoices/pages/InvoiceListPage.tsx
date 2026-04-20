import { useState, useEffect } from 'react'
import { Search, ChevronLeft, ChevronRight, FileText } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useSearchParams, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/auth.store'
import { useInvoices } from '../hooks/useInvoices'
import { useDeleteInvoice } from '../hooks/useInvoiceMutations'
import { listCustomers } from '@/api/customer.api'
import InvoiceStatusTabs, { type InvoiceFilter } from '../components/InvoiceStatusTabs/InvoiceStatusTabs'
import InvoiceTable from '../components/InvoiceTable/InvoiceTable'
import AddInvoiceModal from '../modals/AddInvoiceModal'
import ViewToggle, { useViewPreference } from '@/ui/components/ViewToggle'
import InvoiceCard from '../components/InvoiceCard/InvoiceCard'
import DateRangePicker, { type DateRangeValue } from '@/ui/components/DateRangePicker/DateRangePicker'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'

// ---------------------------------------------------------------------------
// Default date filters — created: last 30 days, due: open
// ---------------------------------------------------------------------------



// ---------------------------------------------------------------------------
// Pagination
// ---------------------------------------------------------------------------

function Pagination({
  page, totalPages, onChange,
}: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <div className="flex items-center justify-center gap-1 pt-2">
      <button
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
        className="p-1.5 rounded-lg text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] disabled:opacity-30 transition-colors"
      >
        <ChevronLeft size={16} />
      </button>
      {[...Array(totalPages)].map((_, i) => (
        <button
          key={i}
          onClick={() => onChange(i)}
          className={[
            'w-8 h-8 rounded-lg text-sm font-medium transition-colors',
            i === page
              ? 'bg-[#2E7A8E] text-white'
              : 'text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447]',
          ].join(' ')}
        >
          {i + 1}
        </button>
      ))}
      <button
        disabled={page === totalPages - 1}
        onClick={() => onChange(page + 1)}
        className="p-1.5 rounded-lg text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] disabled:opacity-30 transition-colors"
      >
        <ChevronRight size={16} />
      </button>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Delete confirm dialog
// ---------------------------------------------------------------------------

function DeleteConfirm({
  invoice,
  onConfirm,
  onCancel,
  isDeleting,
}: {
  invoice:    InvoiceResponse
  onConfirm:  () => void
  onCancel:   () => void
  isDeleting: boolean
}) {
  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end sm:items-center sm:justify-center sm:p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onCancel} />
      <div className="relative w-full sm:max-w-sm bg-white dark:bg-[#1B2838] rounded-t-2xl sm:rounded-2xl shadow-xl p-6">
        <div className="sm:hidden flex justify-center -mt-2 mb-4">
          <div className="w-10 h-1 rounded-full bg-[#8A9BAE]/30" />
        </div>
        <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white mb-2">Delete Invoice</h2>
        <p className="text-sm text-c-muted mb-5">
          Delete <span className="font-semibold text-[#0D1B2A] dark:text-white">{invoice.invoiceNumber}</span>? This cannot be undone.
        </p>
        <div className="flex gap-3 justify-end">
          <button onClick={onCancel} className="px-4 py-2 rounded-lg text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">
            Cancel
          </button>
          <button
            onClick={onConfirm}
            disabled={isDeleting}
            className="px-4 py-2 rounded-lg text-sm font-semibold text-white bg-red-500 hover:bg-red-600 disabled:opacity-50 transition-colors"
          >
            {isDeleting ? 'Deleting…' : 'Delete'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

const PAGE_SIZE = 15

export default function InvoiceListPage() {
  const orgId    = useAuthStore((s) => s.org?.id       ?? '')
  const currency = useAuthStore((s) => s.org?.currency ?? 'INR')
  const timezone = useAuthStore((s) => s.org?.timezone ?? 'UTC')

  const location                         = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()
  const [filter,       setFilter]       = useState<InvoiceFilter>(
    (location.state as InvoiceFilter | null) ?? {},
  )
  const [search,       setSearch]       = useState('')
  const [page,         setPage]         = useState(0)
  const [showCreate,   setShowCreate]   = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<InvoiceResponse | null>(null)
  const [view,         setView]         = useViewPreference('invoices', 'list')
  const [created,      setCreated]      = useState<DateRangeValue>({})
  const [due,          setDue]          = useState<DateRangeValue>({})

  const createParam = searchParams.get('create')
  useEffect(() => {
    if (createParam === 'true') {
      setShowCreate(true)
      setSearchParams({}, { replace: true })
    }
  }, [createParam])

  const deleteMutation = useDeleteInvoice()

  const invoicesQuery = useInvoices({
    ...filter,
    invoiceNumber: search.trim() || undefined,
    createdAtFrom: created.from,
    createdAtTo:   created.to,
    dueDateFrom:   due.from,
    dueDateTo:     due.to,
    page,
    size: PAGE_SIZE,
    sort: 'createdAt,desc',
  })

  const customersQuery = useQuery({
    queryKey: ['customers', orgId, 'all'],
    queryFn:  () => listCustomers(orgId, { size: 500 }),
    enabled:  !!orgId,
  })

  const customerMap: Record<string, CustomerResponse> = {}
  for (const c of customersQuery.data?.content ?? []) {
    customerMap[c.id] = c
  }

  const invoices   = invoicesQuery.data?.content      ?? []
  const totalPages = invoicesQuery.data?.totalPages    ?? 0
  const totalItems = invoicesQuery.data?.totalElements ?? 0

  function handleFilterChange(f: InvoiceFilter) {
    setFilter(f)
    setPage(0)
  }

  function handleSearch(v: string) {
    setSearch(v)
    setPage(0)
  }

  function handleCreatedChange(v: DateRangeValue) {
    setCreated(v)
    setPage(0)
  }

  function handleDueChange(v: DateRangeValue) {
    setDue(v)
    setPage(0)
  }

  async function confirmDelete() {
    if (!deleteTarget) return
    await deleteMutation.mutateAsync(deleteTarget.id)
    setDeleteTarget(null)
  }

  return (
    <>
      <div className="space-y-5">

        {/* ── Header ──────────────────────────────────────────────────── */}
        <div className="flex items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Invoices</h1>
            {!invoicesQuery.isLoading && (
              <p className="text-sm text-c-muted mt-0.5">{totalItems} invoice{totalItems !== 1 ? 's' : ''}</p>
            )}
          </div>
          {/* Hide view toggle on mobile — cards are always shown there */}
          <div className="hidden sm:block">
            <ViewToggle value={view} onChange={setView} />
          </div>
        </div>

        {/* ── Status tabs ─────────────────────────────────────────────── */}
        <InvoiceStatusTabs active={filter} onChange={handleFilterChange} />

        {/* ── Search + Date filters ────────────────────────────────────── */}
        <div className="flex flex-col sm:flex-row sm:items-center gap-2">
          {/* Search — full width on mobile, fixed on desktop */}
          <div className="relative w-full sm:w-52 shrink-0">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-c-muted pointer-events-none" />
            <input
              type="text"
              placeholder="Search invoice number…"
              value={search}
              onChange={(e) => handleSearch(e.target.value)}
              className="w-full pl-8 pr-4 py-2 text-sm bg-white dark:bg-[#1B2838] border border-c-border rounded-lg focus:outline-none focus:border-[#8A9BAE]/40 text-[#0D1B2A] dark:text-white placeholder:text-c-muted transition-colors"
            />
          </div>
          {/* Date pickers — side by side on mobile too */}
          <div className="flex items-center gap-2">
            <DateRangePicker label="Created"  value={created} onChange={handleCreatedChange} timezone={timezone} />
            <DateRangePicker label="Due date" value={due}     onChange={handleDueChange}     timezone={timezone} />
          </div>
        </div>

        {/* ── Content ──────────────────────────────────────────────────── */}

        {/* Table — desktop-only, list mode only */}
        {view === 'list' && (
          <div className="hidden sm:block">
            <InvoiceTable
              invoices={invoices}
              customerMap={customerMap}
              currency={currency}
              isLoading={invoicesQuery.isLoading}
              onDelete={setDeleteTarget}
            />
          </div>
        )}

        {/*
          Cards:
          - Mobile: always shown (single column), regardless of view preference
          - Desktop: shown only in grid mode (hidden when list mode shows the table)
        */}
        <div className={
          view === 'list'
            ? 'sm:hidden grid grid-cols-1 gap-3'
            : 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4'
        }>
          {invoicesQuery.isLoading ? (
            [...Array(4)].map((_, i) => (
              <div key={i} className="h-24 rounded-xl bg-[#F4F7F9] dark:bg-white/10 animate-pulse" />
            ))
          ) : invoices.length === 0 ? (
            <div className="col-span-full flex flex-col items-center justify-center py-16 gap-3 bg-white dark:bg-[#1B2838] rounded-xl border border-c-border">
              <div className="w-12 h-12 rounded-full bg-[#F4F7F9] dark:bg-white/10 flex items-center justify-center">
                <FileText size={20} className="text-c-muted" strokeWidth={1.5} />
              </div>
              <div className="text-center">
                <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">No invoices found</p>
                <p className="text-xs text-c-muted mt-0.5">Try adjusting your filters.</p>
              </div>
            </div>
          ) : (
            invoices.map((inv) => (
              <InvoiceCard
                key={inv.id}
                invoice={inv}
                customer={inv.customerId ? customerMap[inv.customerId] : undefined}
                currency={currency}
                onDelete={setDeleteTarget}
              />
            ))
          )}
        </div>

        {/* ── Pagination ───────────────────────────────────────────────── */}
        <Pagination page={page} totalPages={totalPages} onChange={setPage} />

      </div>

      {showCreate && <AddInvoiceModal onClose={() => setShowCreate(false)} />}

      {deleteTarget && (
        <DeleteConfirm
          invoice={deleteTarget}
          onConfirm={confirmDelete}
          onCancel={() => setDeleteTarget(null)}
          isDeleting={deleteMutation.isPending}
        />
      )}
    </>
  )
}
