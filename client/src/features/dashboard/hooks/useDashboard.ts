import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listInvoices } from '@/api/invoice.api'
import { listConfirmations } from '@/api/confirmation.api'
import { listCustomers } from '@/api/customer.api'

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

  // Overdue invoices for action table — fetch extra to cover zero-remaining edge cases
  const overdueQuery = useQuery({
    queryKey: ['invoices', orgId, 'dashboard-overdue'],
    queryFn:  () => listInvoices(orgId, { lifeCycleStatus: 'ISSUED', timeStatus: 'OVERDUE', size: 10, sort: 'dueDate,asc' }),
    enabled:  !!orgId,
  })

  // Customers for name lookup
  const customersQuery = useQuery({
    queryKey: ['customers', orgId, 'dashboard'],
    queryFn:  () => listCustomers(orgId, { size: 200 }),
    enabled:  !!orgId,
  })

  // Pending confirmations — top 3 by largest amount, CONFIRMATION_FLOW orgs only
  const confirmationsQuery = useQuery({
    queryKey: ['confirmations', orgId, 'dashboard'],
    queryFn:  () => listConfirmations(orgId, { status: 'PENDING_APPROVAL', size: 3, sort: 'amountClaimed,desc' }),
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

  const pastDueInvoices = invoices.filter(
    (i) => i.timeStatus === 'OVERDUE' && ['ISSUED', 'PARTIALLY_PAID'].includes(i.lifeCycleStatus),
  )
  const pastDueAmount = pastDueInvoices.reduce((s, i) => s + i.remainingAmount, 0)

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
    .slice(0, 6)

  // AR Aging buckets — unpaid invoices grouped by days past due date
  const todayMidnight = new Date(now)
  todayMidnight.setHours(0, 0, 0, 0)

  type AgingBucket = { label: string; amount: number; count: number }
  const agingBuckets: { current: AgingBucket; d1_30: AgingBucket; d31_60: AgingBucket; d60plus: AgingBucket } = {
    current:  { label: 'Current',  amount: 0, count: 0 },
    d1_30:    { label: '1–30 d',   amount: 0, count: 0 },
    d31_60:   { label: '31–60 d',  amount: 0, count: 0 },
    d60plus:  { label: '60+ d',    amount: 0, count: 0 },
  }
  for (const inv of unpaidInvoices) {
    const daysPast = inv.dueDate
      ? Math.floor((todayMidnight.getTime() - new Date(inv.dueDate).getTime()) / 86_400_000)
      : -1
    if (daysPast <= 0)       { agingBuckets.current.amount += inv.remainingAmount; agingBuckets.current.count++ }
    else if (daysPast <= 30) { agingBuckets.d1_30.amount   += inv.remainingAmount; agingBuckets.d1_30.count++   }
    else if (daysPast <= 60) { agingBuckets.d31_60.amount  += inv.remainingAmount; agingBuckets.d31_60.count++  }
    else                     { agingBuckets.d60plus.amount  += inv.remainingAmount; agingBuckets.d60plus.count++ }
  }

  const pendingConfirmations = confirmationsQuery.data?.content ?? []

  const customerMap: Record<string, string> = {}
  for (const c of customersQuery.data?.content ?? []) {
    customerMap[c.id] = c.name
  }

  return {
    currency,
    isConfirmationFlow,
    isLoading: allQuery.isLoading,

    kpis: {
      totalUnpaid,
      totalUnpaidCount,
      overdueCount: overdueQuery.data?.totalElements ?? 0,
      pastDueAmount,
      collectedThisMonth,
      avgPaymentDelayDays,
      pendingApprovalsCount: pendingConfirmations.length,
    },

    agingBuckets,
    customerMap,
    overdueInvoices:     (overdueQuery.data?.content ?? [])
      .filter(i => i.remainingAmount > 0 && i.lifeCycleStatus === 'ISSUED')
      .slice(0, 3),
    dueSoonInvoices,
    pendingConfirmations,
    recentInvoices:      invoices.slice(0, 5),
  }
}
