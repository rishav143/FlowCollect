import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listInvoices } from '@/api/invoice.api'
import { listConfirmations } from '@/api/confirmation.api'
import { listCustomers } from '@/api/customer.api'
import { getDashboardStats } from '@/api/dashboard.api'

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

  // Overdue invoices for action table — includes ISSUED and PARTIALLY_PAID
  const overdueQuery = useQuery({
    queryKey: ['invoices', orgId, 'dashboard-overdue'],
    queryFn:  () => listInvoices(orgId, { timeStatus: 'OVERDUE', size: 10, sort: 'dueDate,asc' }),
    enabled:  !!orgId,
  })

  // Customers for name lookup
  const customersQuery = useQuery({
    queryKey: ['customers', orgId, 'dashboard'],
    queryFn:  () => listCustomers(orgId, { size: 200 }),
    enabled:  !!orgId,
  })

  // Dashboard stats — needsAttention list from backend
  const dashboardStatsQuery = useQuery({
    queryKey: ['dashboard-stats', orgId],
    queryFn:  () => getDashboardStats(orgId),
    enabled:  !!orgId,
    staleTime: 0,
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

  // Collected this month: sum of totalPaid for PAID and PARTIALLY_PAID invoices
  // whose latest payment activity (updatedAt) falls within the current month.
  // Using totalPaid (not totalAmount) correctly counts only the amount actually
  // received — for PAID invoices totalPaid === totalAmount; for PARTIALLY_PAID
  // it is the partial amount collected so far.
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)
  const collectedThisMonth = invoices
    .filter(
      (i) =>
        ['PAID', 'PARTIALLY_PAID'].includes(i.lifeCycleStatus) &&
        new Date(i.updatedAt) >= startOfMonth,
    )
    .reduce((s, i) => s + i.totalPaid, 0)

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

  // ── Collections trend (last 6 months + 1 forecast month) ──────────────────
  // Build monthly buckets for the past 6 months using PAID/PARTIALLY_PAID
  // invoices. The forecast is the weighted average of the last 3 months,
  // giving more weight to recent months (3x, 2x, 1x) so the prediction
  // adapts quickly to changing collection velocity.

  const MONTHS_BACK = 6
  const SHORT_MONTH = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

  type TrendPoint = { month: string; collected: number; forecast?: number }

  const trendData: TrendPoint[] = []

  for (let i = MONTHS_BACK - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const y = d.getFullYear()
    const m = d.getMonth()
    const collected = invoices
      .filter((inv) => {
        if (!['PAID', 'PARTIALLY_PAID'].includes(inv.lifeCycleStatus)) return false
        const upd = new Date(inv.updatedAt)
        return upd.getFullYear() === y && upd.getMonth() === m
      })
      .reduce((s, inv) => s + inv.totalPaid, 0)
    trendData.push({ month: SHORT_MONTH[m], collected })
  }

  // Weighted forecast: w3 * last + w2 * prev + w1 * two-ago
  const last3 = trendData.slice(-3).map((p) => p.collected)
  const weights = [1, 2, 3]
  const totalWeight = weights.reduce((a, b) => a + b, 0)
  const forecast = last3.length > 0
    ? Math.round(
        last3.reduce((sum, val, idx) => sum + val * weights[idx], 0) / totalWeight,
      )
    : 0

  const nextMonthIdx = (now.getMonth() + 1) % 12
  trendData.push({ month: SHORT_MONTH[nextMonthIdx], collected: 0, forecast })

  return {
    currency,
    isConfirmationFlow,
    isLoading: allQuery.isLoading,

    kpis: {
      totalUnpaid,
      totalUnpaidCount,
      overdueCount: pastDueInvoices.length,
      pastDueAmount,
      collectedThisMonth,
      avgPaymentDelayDays,
      pendingApprovalsCount: confirmationsQuery.data?.totalElements ?? 0,
    },

    agingBuckets,
    customerMap,
    collectionsTrend: trendData,
    needsAttention: dashboardStatsQuery.data?.needsAttention ?? [],
    overdueInvoices:     (overdueQuery.data?.content ?? [])
      .filter(i => i.remainingAmount > 0 && ['ISSUED', 'PARTIALLY_PAID'].includes(i.lifeCycleStatus))
      .slice(0, 3),
    dueSoonInvoices,
    pendingConfirmations,
    recentInvoices:      invoices.slice(0, 5),
  }
}
