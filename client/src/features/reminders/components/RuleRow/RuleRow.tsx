import { memo } from 'react'
import { Mail, MessageSquare, MessageCircle, Pencil, Trash2, RotateCcw } from 'lucide-react'
import type { ReminderRuleResponse, ReminderChannel, ReminderTriggerType } from '@/types/reminder.types'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const CHANNEL_META: Record<ReminderChannel, { icon: React.ReactNode; color: string; bg: string; label: string }> = {
  EMAIL:    { icon: <Mail          size={15} strokeWidth={1.8} />, color: 'text-blue-500',  bg: 'bg-blue-500/10',  label: 'Email'    },
  SMS:      { icon: <MessageSquare size={15} strokeWidth={1.8} />, color: 'text-amber-500', bg: 'bg-amber-500/10', label: 'SMS'      },
  WHATSAPP: { icon: <MessageCircle size={15} strokeWidth={1.8} />, color: 'text-green-500', bg: 'bg-green-500/10', label: 'WhatsApp' },
}

export function timingLabel(daysOffset: number, triggerType: ReminderTriggerType): string {
  if (triggerType === 'ON_DUE_DATE') return 'On due date'
  const abs = Math.abs(daysOffset)
  const d   = `${abs} day${abs !== 1 ? 's' : ''}`
  return triggerType === 'BEFORE_DUE_DATE' ? `${d} before due` : `${d} after due`
}

// ---------------------------------------------------------------------------
// RuleRow
// ---------------------------------------------------------------------------

interface Props {
  rule:     ReminderRuleResponse
  isLast:   boolean
  onEdit:   (r: ReminderRuleResponse) => void
  onDelete: (r: ReminderRuleResponse) => void
  onToggle: (r: ReminderRuleResponse) => void
}

const RuleRow = memo(function RuleRow({ rule, isLast, onEdit, onDelete, onToggle }: Props) {
  const ch      = CHANNEL_META[rule.channel]
  const isCyclic = rule.maxOccurrences > 1

  return (
    <div className={[
      'flex items-center gap-4 px-5 py-4',
      !isLast ? 'border-b border-c-border' : '',
      !rule.active ? 'opacity-50' : '',
    ].join(' ')}>

      {/* Channel icon */}
      <div className={`w-9 h-9 rounded-full ${ch.bg} ${ch.color} flex items-center justify-center shrink-0`}>
        {ch.icon}
      </div>

      {/* Timing + detail */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white truncate">
          {timingLabel(rule.daysOffset, rule.triggerType)}
          {rule.name && (
            <span className="ml-1.5 text-xs font-normal text-c-muted">· {rule.name}</span>
          )}
        </p>
        <p className="text-xs text-c-muted mt-0.5 truncate">
          {ch.label}{rule.templateName ? ` · ${rule.templateName}` : ''}
        </p>
        {isCyclic && (
          <p className="text-xs text-c-muted flex items-center gap-1 mt-0.5">
            <RotateCcw size={10} strokeWidth={2} className="shrink-0" />
            <span>{rule.maxOccurrences}× every {rule.cycleIntervalDays}d</span>
          </p>
        )}
      </div>

      {/* Active toggle */}
      <button
        onClick={() => onToggle(rule)}
        aria-label={rule.active ? 'Deactivate' : 'Activate'}
        className={[
          'relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent',
          'transition-colors duration-200 focus:outline-none',
          rule.active ? 'bg-[#29B6F6]' : 'bg-[#8A9BAE]/30',
        ].join(' ')}
      >
        <span className={[
          'pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow transition duration-200',
          rule.active ? 'translate-x-4' : 'translate-x-0',
        ].join(' ')} />
      </button>

      {/* Edit */}
      <button
        onClick={() => onEdit(rule)}
        className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors"
        aria-label="Edit rule"
      >
        <Pencil size={14} strokeWidth={1.8} />
      </button>

      {/* Delete */}
      <button
        onClick={() => onDelete(rule)}
        className="p-1.5 text-c-muted hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 rounded-lg transition-colors"
        aria-label="Delete rule"
      >
        <Trash2 size={14} strokeWidth={1.8} />
      </button>
    </div>
  )
})

export default RuleRow
