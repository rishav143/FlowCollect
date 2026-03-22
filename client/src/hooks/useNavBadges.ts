/**
 * useNavBadges
 *
 * Returns badge counts for sidebar nav items.
 *
 * Architecture note:
 *   This hook is the single seam between the Sidebar and the data layer.
 *   Currently returns mocked counts so the shell renders correctly during
 *   development. When feature API hooks are ready, replace the mock values
 *   with real useQuery calls — Sidebar.tsx never changes.
 *
 * TODO: Replace with real queries:
 *   const { data: followups } = useQuery({ queryKey: ['nav-followups'], queryFn: fetchFollowupBadgeCount })
 *   const { data: approvals } = useQuery({ queryKey: ['nav-approvals'], queryFn: fetchApprovalBadgeCount })
 */
export interface NavBadges {
  /** Count of invoices with status OVERDUE or DUE_TODAY */
  followups: number
  /** Count of payment claims with status PENDING_CONFIRMATION */
  approvals: number
}

export function useNavBadges(): NavBadges {
  return {
    followups: 5,
    approvals: 2,
  }
}
