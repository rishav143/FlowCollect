import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listInvoices } from '@/api/invoice.api'
import { listConfirmations } from '@/api/confirmation.api'

export function useDashboard() {
  const orgId             = useAuthStore((s) => s.org?.id ?? '')
  const currency          = useAuthStore((s) => s.org?.currency ?? 'INR')
  const isConfirmationFlow = useAuthStore((s) => s.org?.paymentCollectionMode === 'CONFIRMATION_FLOW')

  // Broad fetch for KPI computation (latest 200 invoices)
  const allQuery = useQuery({
    queryKey: ['invoices', orgId, 'dashboard-all'],
    queryFn:  () => listInvoices(orgId, { size: 200, sort: 'createdAt,desc' }),
    enabled:  !!orgId,
  })

  // Overdue invoices for action table — sorted by most overdue first
  const overdueQuery = useQuery({
    queryKey: ['invoices', orgId, 'dashboard-overdue'],
    queryFn:  () => listInvoices(orgId, { timeStatus: 'OVERDUE', size: 5, sort: 'dueDate,asc' }),
    enabled:  !!orgId,
  })

  // Pending confirmations — CONFIRMATION_FLOW orgs only
  const confirmationsQuery = useQuery({
    queryKey: ['confirmations', orgId, 'dashboard'],
    queryFn:  () => listConfirmations(orgId, { size: 10 }),
    enabled:  !!orgId && isConfirmationFlow,
  })

  const invoices  = allQuery.data?.content ?? []
  const now       = new Date()
  const thisMonth = now.getMonth()
  const thisYear  = now.getFullYear()

  // ── KPI computations ───────────────────────────────────────────────────────

  const outstanding = invoices
    .filter((i) => ['ISSUED', 'PARTIALLY_PAID'].includes(i.lifeCycleStatus))
    .reduce((sum, i) => sum + i.remainingAmount, 0)

  const overdueCount = overdueQuery.data?.totalElements ?? 0

  const collectedThisMonth = invoices
    .filter((i) => {
      if (i.lifeCycleStatus !== 'PAID') return false
      const d = new Date(i.updatedAt)
      return d.getMonth() === thisMonth && d.getFullYear() === thisYear
    })
    .reduce((sum, i) => sum + i.totalPaid, 0)

  const draftCount = invoices.filter((i) => i.lifeCycleStatus === 'DRAFT').length

  const allConfirmations    = confirmationsQuery.data?.content ?? []
  const pendingConfirmations = allConfirmations.filter((c) => c.status === 'PENDING')

  return {
    currency,
    isConfirmationFlow,
    isLoading: allQuery.isLoading,

    kpis: {
      outstanding,
      overdueCount,
      collectedThisMonth,
      draftCount,
      pendingApprovalsCount: pendingConfirmations.length,
    },

    overdueInvoices:     overdueQuery.data?.content ?? [],
    pendingConfirmations,
    recentInvoices:      invoices.slice(0, 8),
  }
}
