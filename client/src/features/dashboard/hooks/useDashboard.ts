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

  const invoices = allQuery.data?.content ?? []
  const now      = new Date()

  // ── KPI computations ───────────────────────────────────────────────────────

  const unpaidInvoices = invoices.filter((i) =>
    ['ISSUED', 'PARTIALLY_PAID'].includes(i.lifeCycleStatus),
  )

  const totalUnpaid      = unpaidInvoices.reduce((s, i) => s + i.remainingAmount, 0)
  const totalUnpaidCount = unpaidInvoices.length

  const overdueCount  = overdueQuery.data?.totalElements ?? 0
  const pastDueInvoices = invoices.filter(
    (i) => i.timeStatus === 'OVERDUE' && ['ISSUED', 'PARTIALLY_PAID'].includes(i.lifeCycleStatus),
  )
  const pastDueAmount = pastDueInvoices.reduce((s, i) => s + i.remainingAmount, 0)

  // Due in the next 30 days (not overdue, not paid)
  const today30 = new Date(now)
  today30.setDate(today30.getDate() + 30)
  const dueNext30Invoices = unpaidInvoices.filter((i) => {
    if (!i.dueDate || i.timeStatus === 'OVERDUE') return false
    const d = new Date(i.dueDate)
    return d >= now && d <= today30
  })
  const dueNext30Amount = dueNext30Invoices.reduce((s, i) => s + i.remainingAmount, 0)
  const dueNext30Count  = dueNext30Invoices.length

  // Average payment delay: days between issueDate and updatedAt for PAID invoices
  const paidWithDates = invoices.filter(
    (i) => i.lifeCycleStatus === 'PAID' && i.issueDate,
  )
  const avgPaymentDelayDays = paidWithDates.length > 0
    ? Math.round(
        paidWithDates.reduce((sum, i) => {
          const issued = new Date(i.issueDate!).getTime()
          const paid   = new Date(i.updatedAt).getTime()
          return sum + Math.max(0, (paid - issued) / 86_400_000)
        }, 0) / paidWithDates.length,
      )
    : null

  // Collected this month: invoices fully paid (updatedAt within current month)
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)
  const collectedThisMonth = invoices
    .filter((i) => i.lifeCycleStatus === 'PAID' && new Date(i.updatedAt) >= startOfMonth)
    .reduce((s, i) => s + i.totalAmount, 0)

  // Due soon: unpaid invoices with dueDate in next 14 days, sorted nearest-first
  const in14Days = new Date(now)
  in14Days.setDate(in14Days.getDate() + 14)
  const dueSoonInvoices = unpaidInvoices
    .filter((i) => {
      if (!i.dueDate || i.timeStatus === 'OVERDUE') return false
      const d = new Date(i.dueDate)
      return d >= now && d <= in14Days
    })
    .sort((a, b) => new Date(a.dueDate!).getTime() - new Date(b.dueDate!).getTime())
    .slice(0, 5)

  const allConfirmations    = confirmationsQuery.data?.content ?? []
  const pendingConfirmations = allConfirmations.filter((c) => c.status === 'PENDING')

  return {
    currency,
    isConfirmationFlow,
    isLoading: allQuery.isLoading,

    kpis: {
      totalUnpaid,
      totalUnpaidCount,
      overdueCount,
      pastDueAmount,
      collectedThisMonth,
      avgPaymentDelayDays,
      pendingApprovalsCount: pendingConfirmations.length,
    },

    overdueInvoices:     overdueQuery.data?.content ?? [],
    dueSoonInvoices,
    pendingConfirmations,
    recentInvoices:      invoices.slice(0, 5),
  }
}
