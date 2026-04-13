import { useState } from 'react'
import { Plus, Search } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { useClients } from '../hooks/useClients'
import { useDeleteClient, useArchiveClient } from '../hooks/useClients'
import { listInvoices } from '@/api/invoice.api'
import { useQuery } from '@tanstack/react-query'
import ClientMetricsStrip from '../components/ClientMetricsStrip/ClientMetricsStrip'
import ClientTable from '../components/ClientTable/ClientTable'
import AddClientModal from '../modals/AddClientModal'
import ViewToggle, { useViewPreference } from '@/ui/components/ViewToggle'
import type { CustomerResponse } from '@/types/customer.types'
import type { InvoiceResponse } from '@/types/invoice.types'

// ---------------------------------------------------------------------------
// Delete confirm dialog
// ---------------------------------------------------------------------------

function DeleteConfirm({
  client,
  invoiceCount,
  onConfirm,
  onArchive,
  onCancel,
  isDeleting,
  isArchiving,
}: {
  client:       CustomerResponse
  invoiceCount: number
  onConfirm:    () => void
  onArchive:    () => void
  onCancel:     () => void
  isDeleting:   boolean
  isArchiving:  boolean
}) {
  const hasInvoices = invoiceCount > 0

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onCancel} />
      <div className="relative w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6">
        <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white mb-2">
          {hasInvoices ? 'Cannot Delete Client' : 'Delete Client'}
        </h2>

        {hasInvoices ? (
          <>
            <p className="text-sm text-c-muted mb-1">
              <span className="font-semibold text-[#0D1B2A] dark:text-white">{client.name}</span> has {invoiceCount} invoice{invoiceCount !== 1 ? 's' : ''}.
            </p>
            <p className="text-sm text-c-muted mb-5">
              Deleting would destroy invoice history. Archive the client instead — they'll be hidden from active lists but all records are preserved.
            </p>
            <div className="flex gap-3 justify-end">
              <button onClick={onCancel} className="px-4 py-2 rounded-lg text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">
                Cancel
              </button>
              <button
                onClick={onArchive}
                disabled={isArchiving}
                className="px-4 py-2 rounded-lg text-sm font-semibold text-white hover:opacity-90 disabled:opacity-50 transition-opacity"
                style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
              >
                {isArchiving ? 'Archiving…' : 'Archive Client'}
              </button>
            </div>
          </>
        ) : (
          <>
            <p className="text-sm text-c-muted mb-5">
              Delete <span className="font-semibold text-[#0D1B2A] dark:text-white">{client.name}</span>? This cannot be undone.
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
          </>
        )}
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function ClientListPage() {
  const orgId    = useAuthStore((s) => s.org?.id ?? '')
  const currency = useAuthStore((s) => s.org?.currency ?? 'INR')

  const [search,       setSearch]       = useState('')
  const [showAdd,      setShowAdd]      = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<CustomerResponse | null>(null)
  const [view,         setView]         = useViewPreference('clients', 'list')

  const clientsQuery = useClients({ name: search.trim() || undefined, active: true, size: 200 })
  const deleteMut  = useDeleteClient()
  const archiveMut = useArchiveClient()

  // Fetch all invoices for cross-referencing client stats
  const invoicesQuery = useQuery({
    queryKey: ['invoices-all', orgId],
    queryFn:  () => listInvoices(orgId, { size: 500, sort: 'createdAt,desc' }),
    enabled:  !!orgId,
    staleTime: 60_000,
  })

  const customers = clientsQuery.data?.content ?? []

  // Build map: customerId → invoices[]
  const invoicesByCustomer: Record<string, InvoiceResponse[]> = {}
  for (const inv of invoicesQuery.data?.content ?? []) {
    if (!inv.customerId) continue
    if (!invoicesByCustomer[inv.customerId]) invoicesByCustomer[inv.customerId] = []
    invoicesByCustomer[inv.customerId].push(inv)
  }

  async function confirmDelete() {
    if (!deleteTarget) return
    await deleteMut.mutateAsync(deleteTarget.id)
    setDeleteTarget(null)
  }

  async function confirmArchive() {
    if (!deleteTarget) return
    await archiveMut.mutateAsync(deleteTarget.id)
    setDeleteTarget(null)
  }

  const isLoading = clientsQuery.isLoading || invoicesQuery.isLoading

  return (
    <>
      <div className="space-y-5">

        {/* Header */}
        <div className="flex items-center justify-between gap-3 flex-wrap">
          <div>
            <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Clients</h1>
            {!isLoading && (
              <p className="text-sm text-c-muted mt-0.5">
                {customers.length} client{customers.length !== 1 ? 's' : ''}
              </p>
            )}
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <ViewToggle value={view} onChange={setView} />
            <button
              onClick={() => setShowAdd(true)}
              className="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity"
              style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
            >
              <Plus size={15} strokeWidth={2.5} />
              Add Client
            </button>
          </div>
        </div>

        {/* Metrics */}
        <ClientMetricsStrip
          customers={customers}
          invoicesByCustomer={invoicesByCustomer}
          currency={currency}
          isLoading={isLoading}
        />

        {/* Search */}
        <div className="relative max-w-xs">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-c-muted pointer-events-none" />
          <input
            type="text"
            placeholder="Search by name…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-8 pr-4 py-2 text-sm bg-white dark:bg-[#1B2838] border border-c-border rounded-lg focus:outline-none focus:border-[#8A9BAE]/40 text-[#0D1B2A] dark:text-white placeholder:text-c-muted transition-colors"
          />
        </div>

        {/* Table / Grid */}
        <ClientTable
          customers={customers}
          invoicesByCustomer={invoicesByCustomer}
          currency={currency}
          isLoading={isLoading}
          view={view}
          onDelete={setDeleteTarget}
        />
      </div>

      {showAdd && <AddClientModal onClose={() => setShowAdd(false)} />}

      {deleteTarget && (
        <DeleteConfirm
          client={deleteTarget}
          invoiceCount={(invoicesByCustomer[deleteTarget.id] ?? []).length}
          onConfirm={confirmDelete}
          onArchive={confirmArchive}
          onCancel={() => setDeleteTarget(null)}
          isDeleting={deleteMut.isPending}
          isArchiving={archiveMut.isPending}
        />
      )}
    </>
  )
}
