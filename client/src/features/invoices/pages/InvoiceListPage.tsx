import { useState, useEffect } from 'react'
import { Plus, Search, ChevronLeft, ChevronRight, Archive } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { useAuthStore } from '@/store/auth.store'
import { useInvoices } from '../hooks/useInvoices'
import { useDeleteInvoice, useArchivePaidInvoices, useBulkArchiveInvoices, useUnarchiveInvoice } from '../hooks/useInvoiceMutations'
import { listCustomers } from '@/api/customer.api'
import InvoiceStatusTabs, { type InvoiceFilter } from '../components/InvoiceStatusTabs/InvoiceStatusTabs'
import InvoiceTable from '../components/InvoiceTable/InvoiceTable'
import AddInvoiceModal from '../modals/AddInvoiceModal'
import ViewToggle, { useViewPreference, gridClass } from '@/ui/components/ViewToggle'
import InvoiceCard from '../components/InvoiceCard/InvoiceCard'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { CustomerResponse } from '@/types/customer.types'

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
  invoice, onConfirm, onCancel, isDeleting,
}: {
  invoice:    InvoiceResponse
  onConfirm:  () => void
  onCancel:   () => void
  isDeleting: boolean
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onCancel} />
      <div className="relative w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6">
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
// Archive confirm dialog
// ---------------------------------------------------------------------------

function ArchiveConfirm({
  count, onConfirm, onCancel, isArchiving,
}: {
  count:       number
  onConfirm:   () => void
  onCancel:    () => void
  isArchiving: boolean
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onCancel} />
      <div className="relative w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6">
        <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white mb-2">Archive Invoices</h2>
        <p className="text-sm text-c-muted mb-5">
          Archive <span className="font-semibold text-[#0D1B2A] dark:text-white">{count} invoice{count !== 1 ? 's' : ''}</span>? They'll be hidden from your main list but can be restored from the Archived tab.
        </p>
        <div className="flex gap-3 justify-end">
          <button onClick={onCancel} className="px-4 py-2 rounded-lg text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">
            Cancel
          </button>
          <button
            onClick={onConfirm}
            disabled={isArchiving}
            className="px-4 py-2 rounded-lg text-sm font-semibold text-white bg-[#2E7A8E] hover:bg-[#245f70] disabled:opacity-50 transition-colors"
          >
            {isArchiving ? 'Archiving…' : 'Archive'}
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
  const orgId    = useAuthStore((s) => s.org?.id ?? '')
  const currency = useAuthStore((s) => s.org?.currency ?? 'INR')

  const [searchParams, setSearchParams] = useSearchParams()
  const [filter,        setFilter]       = useState<InvoiceFilter>({})
  const [search,        setSearch]       = useState('')
  const [page,          setPage]         = useState(0)
  const [showCreate,    setShowCreate]   = useState(false)
  const [deleteTarget,  setDeleteTarget] = useState<InvoiceResponse | null>(null)
  const [view,          setView]         = useViewPreference('invoices', 'list')
  const [selectedIds,   setSelectedIds]  = useState<Set<string>>(new Set())
  const [showArchiveConfirm, setShowArchiveConfirm] = useState(false)
  const [archivingAllPaid,   setArchivingAllPaid]   = useState(false)

  const showArchived = !!filter.archived

  useEffect(() => {
    if (searchParams.get('create') === 'true') {
      setShowCreate(true)
      setSearchParams({}, { replace: true })
    }
  }, [])

  // Clear selection when filter changes
  useEffect(() => { setSelectedIds(new Set()) }, [filter])

  const deleteMutation      = useDeleteInvoice()
  const archivePaidMutation = useArchivePaidInvoices()
  const bulkArchiveMutation = useBulkArchiveInvoices()
  const unarchiveMutation   = useUnarchiveInvoice()

  const invoicesQuery = useInvoices({
    ...filter,
    invoiceNumber: search.trim() || undefined,
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

  const invoices   = invoicesQuery.data?.content    ?? []
  const totalPages = invoicesQuery.data?.totalPages  ?? 0
  const totalItems = invoicesQuery.data?.totalElements ?? 0

  function handleFilterChange(f: InvoiceFilter) {
    setFilter(f)
    setPage(0)
  }

  function handleSearch(v: string) {
    setSearch(v)
    setPage(0)
  }

  async function confirmDelete() {
    if (!deleteTarget) return
    await deleteMutation.mutateAsync(deleteTarget.id)
    setDeleteTarget(null)
  }

  function handleSelect(id: string, checked: boolean) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      checked ? next.add(id) : next.delete(id)
      return next
    })
  }

  function handleSelectAll(checked: boolean) {
    setSelectedIds(checked ? new Set(invoices.map((i) => i.id)) : new Set())
  }

  async function confirmBulkArchive() {
    if (archivingAllPaid) {
      await archivePaidMutation.mutateAsync()
    } else {
      await bulkArchiveMutation.mutateAsync([...selectedIds])
      setSelectedIds(new Set())
    }
    setShowArchiveConfirm(false)
    setArchivingAllPaid(false)
  }

  function openClearPaid() {
    setArchivingAllPaid(true)
    setShowArchiveConfirm(true)
  }

  function openBulkArchive() {
    setArchivingAllPaid(false)
    setShowArchiveConfirm(true)
  }

  async function handleUnarchive(invoice: InvoiceResponse) {
    await unarchiveMutation.mutateAsync(invoice.id)
  }

  const isArchiving = archivePaidMutation.isPending || bulkArchiveMutation.isPending
  const archiveCount = archivingAllPaid ? 0 : selectedIds.size

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
          <div className="flex items-center gap-2 shrink-0">
            <ViewToggle value={view} onChange={setView} />
            {!showArchived && (
              <button
                onClick={openClearPaid}
                disabled={archivePaidMutation.isPending}
                className="flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-sm font-medium text-c-muted border border-c-border hover:border-[#8A9BAE]/50 hover:text-[#0D1B2A] dark:hover:text-white disabled:opacity-50 transition-colors"
              >
                <Archive size={14} strokeWidth={2} />
                Clear Paid
              </button>
            )}
            <button
              onClick={() => setShowCreate(true)}
              className="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity"
              style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
            >
              <Plus size={15} strokeWidth={2.5} />
              New Invoice
            </button>
          </div>
        </div>

        {/* ── Status tabs ──────────────────────────────────────────────── */}
        <InvoiceStatusTabs active={filter} onChange={handleFilterChange} />

        {/* ── Bulk action bar ───────────────────────────────────────────── */}
        {selectedIds.size > 0 && (
          <div className="flex items-center gap-3 px-4 py-2.5 bg-[#2E7A8E]/10 dark:bg-[#2E7A8E]/20 border border-[#2E7A8E]/30 rounded-xl">
            <span className="text-sm font-medium text-[#2E7A8E]">
              {selectedIds.size} selected
            </span>
            <div className="flex items-center gap-2 ml-auto">
              {showArchived ? (
                <button
                  onClick={async () => {
                    for (const id of selectedIds) await unarchiveMutation.mutateAsync(id)
                    setSelectedIds(new Set())
                  }}
                  disabled={unarchiveMutation.isPending}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-[#2E7A8E] border border-[#2E7A8E]/40 hover:bg-[#2E7A8E]/10 disabled:opacity-50 transition-colors"
                >
                  Restore selected
                </button>
              ) : (
                <button
                  onClick={openBulkArchive}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-[#2E7A8E] border border-[#2E7A8E]/40 hover:bg-[#2E7A8E]/10 transition-colors"
                >
                  <Archive size={13} strokeWidth={2} />
                  Archive selected
                </button>
              )}
              <button
                onClick={() => setSelectedIds(new Set())}
                className="px-3 py-1.5 rounded-lg text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
              >
                Clear
              </button>
            </div>
          </div>
        )}

        {/* ── Search ───────────────────────────────────────────────────── */}
        <div className="relative max-w-xs">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-c-muted pointer-events-none" />
          <input
            type="text"
            placeholder="Search invoice number…"
            value={search}
            onChange={(e) => handleSearch(e.target.value)}
            className="w-full pl-8 pr-4 py-2 text-sm bg-white dark:bg-[#1B2838] border border-c-border rounded-lg focus:outline-none focus:border-[#8A9BAE]/40 text-[#0D1B2A] dark:text-white placeholder:text-c-muted transition-colors"
          />
        </div>

        {/* ── Content ──────────────────────────────────────────────────── */}
        {view === 'list' ? (
          <InvoiceTable
            invoices={invoices}
            customerMap={customerMap}
            currency={currency}
            isLoading={invoicesQuery.isLoading}
            selectedIds={selectedIds}
            onSelect={handleSelect}
            onSelectAll={handleSelectAll}
            onDelete={setDeleteTarget}
            onUnarchive={handleUnarchive}
            showArchived={showArchived}
          />
        ) : (
          <div className={gridClass(view)}>
            {invoicesQuery.isLoading
              ? [...Array(6)].map((_, i) => (
                  <div key={i} className="h-32 rounded-xl bg-[#F4F7F9] dark:bg-white/10 animate-pulse" />
                ))
              : invoices.map((inv) => (
                  <InvoiceCard
                    key={inv.id}
                    invoice={inv}
                    customer={inv.customerId ? customerMap[inv.customerId] : undefined}
                    currency={currency}
                    selected={selectedIds.has(inv.id)}
                    onSelect={handleSelect}
                    onDelete={setDeleteTarget}
                    onUnarchive={handleUnarchive}
                    showArchived={showArchived}
                  />
                ))
            }
          </div>
        )}

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

      {showArchiveConfirm && (
        <ArchiveConfirm
          count={archivingAllPaid ? 0 : archiveCount}
          onConfirm={confirmBulkArchive}
          onCancel={() => { setShowArchiveConfirm(false); setArchivingAllPaid(false) }}
          isArchiving={isArchiving}
        />
      )}
    </>
  )
}
