import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listInvoices } from '@/api/invoice.api'
import { listConfirmations } from '@/api/confirmation.api'
import { useNotificationStore } from '@/store/notification.store'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type NotificationType = 'overdue' | 'due-today' | 'approval'

export interface AppNotification {
  id:        string          // deterministic: 'overdue-{id}', 'due-today-{id}', 'approval-{id}'
  type:      NotificationType
  title:     string
  body:      string
  sortKey:   string          // ISO date string — used for sort order
  actionUrl: string
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function daysOverdue(dueDate: string): number {
  const due   = new Date(dueDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.max(0, Math.floor((today.getTime() - due.getTime()) / 86_400_000))
}

// ---------------------------------------------------------------------------
// Hook
// ---------------------------------------------------------------------------

export function useNotifications() {
  const orgId              = useAuthStore((s) => s.org?.id ?? '')
  const currency           = useAuthStore((s) => s.org?.currency ?? 'INR')
  const isConfirmationFlow = useAuthStore((s) => s.org?.paymentCollectionMode === 'CONFIRMATION_FLOW')
  const readIds            = useNotificationStore((s) => s.readIds)

  // Reuses dashboard-overdue cache when both are mounted simultaneously
  const overdueQuery = useQuery({
    queryKey:  ['invoices', orgId, 'dashboard-overdue'],
    queryFn:   () => listInvoices(orgId, { timeStatus: 'OVERDUE', size: 10, sort: 'dueDate,asc' }),
    enabled:   !!orgId,
    staleTime: 2 * 60 * 1000,
  })

  const dueTodayQuery = useQuery({
    queryKey:  ['invoices', orgId, 'notifications-due-today'],
    queryFn:   () => listInvoices(orgId, { timeStatus: 'DUE_TODAY', size: 10 }),
    enabled:   !!orgId,
    staleTime: 5 * 60 * 1000,
  })

  // Reuses dashboard confirmations cache
  const confirmationsQuery = useQuery({
    queryKey:  ['confirmations', orgId, 'dashboard'],
    queryFn:   () => listConfirmations(orgId, { status: 'PENDING_APPROVAL', size: 10, sort: 'amountClaimed,desc' }),
    enabled:   !!orgId && isConfirmationFlow,
    staleTime: 2 * 60 * 1000,
  })

  const fmt = new Intl.NumberFormat('en', {
    style:                 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  })

  const notifications: AppNotification[] = []

  // ── Overdue notifications ──────────────────────────────────────────────────
  for (const inv of overdueQuery.data?.content ?? []) {
    if (!['ISSUED', 'PARTIALLY_PAID'].includes(inv.lifeCycleStatus)) continue
    const days = inv.dueDate ? daysOverdue(inv.dueDate) : 0
    notifications.push({
      id:        `overdue-${inv.id}`,
      type:      'overdue',
      title:     `${inv.invoiceNumber} is ${days} day${days !== 1 ? 's' : ''} overdue`,
      body:      `${fmt.format(inv.remainingAmount)} remaining`,
      sortKey:   inv.dueDate ?? inv.createdAt,
      actionUrl: `/invoices/${inv.id}`,
    })
  }

  // ── Due today notifications ────────────────────────────────────────────────
  for (const inv of dueTodayQuery.data?.content ?? []) {
    if (!['ISSUED', 'PARTIALLY_PAID'].includes(inv.lifeCycleStatus)) continue
    // Skip if already in overdue list
    if (notifications.some((n) => n.id === `overdue-${inv.id}`)) continue
    notifications.push({
      id:        `due-today-${inv.id}`,
      type:      'due-today',
      title:     `${inv.invoiceNumber} is due today`,
      body:      `${fmt.format(inv.remainingAmount)} due`,
      sortKey:   inv.dueDate ?? inv.createdAt,
      actionUrl: `/invoices/${inv.id}`,
    })
  }

  // ── Approval notifications ────────────────────────────────────────────────
  for (const c of confirmationsQuery.data?.content ?? []) {
    notifications.push({
      id:        `approval-${c.id}`,
      type:      'approval',
      title:     `Payment claim for ${c.invoiceNumber}`,
      body:      `${fmt.format(c.amountClaimed)} claimed · awaiting your review`,
      sortKey:   c.createdAt,
      actionUrl: '/approvals',
    })
  }

  const unreadCount = notifications.filter((n) => !readIds.includes(n.id)).length

  return {
    notifications,
    unreadCount,
    isLoading: overdueQuery.isLoading || dueTodayQuery.isLoading,
  }
}
