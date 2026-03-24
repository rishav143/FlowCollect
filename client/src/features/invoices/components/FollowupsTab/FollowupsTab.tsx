import { useParams } from 'react-router-dom'
import { Bell, Mail, MessageSquare, Phone } from 'lucide-react'
import { useInvoiceFollowups } from '@/features/followups/hooks/useFollowups'
import type { FollowUpChannel, FollowUpStatus } from '@/types/followup.types'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const CHANNEL_ICON: Record<FollowUpChannel, React.ReactNode> = {
  EMAIL:    <Mail size={14} />,
  SMS:      <Phone size={14} />,
  WHATSAPP: <MessageSquare size={14} />,
}

const STATUS_DOT: Record<FollowUpStatus, { dot: string; text: string; label: string }> = {
  PENDING:   { dot: 'bg-amber-400', text: 'text-amber-600 dark:text-amber-400', label: 'Pending'   },
  SENT:      { dot: 'bg-green-500', text: 'text-green-600 dark:text-green-400', label: 'Sent'      },
  FAILED:    { dot: 'bg-red-500',   text: 'text-red-600 dark:text-red-400',     label: 'Failed'    },
  CANCELLED: { dot: 'bg-[#8A9BAE]', text: 'text-c-muted',                      label: 'Cancelled' },
}

function fmtDate(d: string | null) {
  if (!d) return '—'
  return new Date(d).toLocaleString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export default function FollowupsTab() {
  const { id } = useParams<{ id: string }>()
  const { data: followups = [], isLoading } = useInvoiceFollowups(id ?? '')

  if (isLoading) {
    return (
      <div className="space-y-3 animate-pulse">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-14 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
        ))}
      </div>
    )
  }

  if (followups.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 gap-3 text-center">
        <div className="w-12 h-12 rounded-full bg-[#29B6F6]/10 flex items-center justify-center">
          <Bell size={20} className="text-[#29B6F6]" />
        </div>
        <div>
          <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">No follow-ups sent yet</p>
          <p className="text-sm text-c-muted mt-0.5">
            Use the "Send Follow-up" button above to notify your customer.
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      {followups.map((fu) => (
        <div
          key={fu.id}
          className="flex items-center gap-3 py-3 border-b border-c-border last:border-0"
        >
          {/* Channel icon */}
          <div className="w-8 h-8 rounded-full bg-[#29B6F6]/10 flex items-center justify-center text-[#29B6F6] shrink-0">
            {CHANNEL_ICON[fu.channel]}
          </div>

          {/* Info */}
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-[#0D1B2A] dark:text-white capitalize">
              {fu.channel.charAt(0) + fu.channel.slice(1).toLowerCase()}
              {fu.triggerType === 'AUTOMATED' && (
                <span className="ml-1.5 text-[10px] font-semibold text-c-muted uppercase tracking-wide">
                  Auto
                </span>
              )}
            </p>
            <p className="text-xs text-c-muted mt-0.5">
              {fu.sentAt ? `Sent ${fmtDate(fu.sentAt)}` : fu.scheduledForDate ? `Scheduled ${fmtDate(fu.scheduledForDate)}` : `Created ${fmtDate(fu.createdAt)}`}
            </p>
          </div>

          {/* Status */}
          {(() => { const { dot, text, label } = STATUS_DOT[fu.status]; return (
            <span className="inline-flex items-center gap-1.5 shrink-0">
              <span className={`w-1.5 h-1.5 rounded-full ${dot}`} />
              <span className={`text-xs font-medium ${text}`}>{label}</span>
            </span>
          )})()}
        </div>
      ))}
    </div>
  )
}
