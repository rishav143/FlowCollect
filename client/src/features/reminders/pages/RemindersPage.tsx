import { useState } from 'react'
import { Bell, Plus } from 'lucide-react'
import {
  useReminderRules,
  useDeleteReminderRule,
  useToggleReminderRule,
} from '../hooks/useReminders'
import RuleRow from '../components/RuleRow/RuleRow'
import RuleModal from '../modals/RuleModal'
import type { ReminderRuleResponse } from '@/types/reminder.types'

// ---------------------------------------------------------------------------
// Skeleton
// ---------------------------------------------------------------------------

function RowSkeleton({ isLast }: { isLast: boolean }) {
  return (
    <div className={`flex items-center gap-4 px-5 py-4 animate-pulse ${!isLast ? 'border-b border-c-border' : ''}`}>
      <div className="w-9 h-9 rounded-full bg-[#F4F7F9] dark:bg-white/10 shrink-0" />
      <div className="flex-1 space-y-1.5">
        <div className="h-4 w-36 rounded bg-[#F4F7F9] dark:bg-white/10" />
        <div className="h-3 w-24 rounded bg-[#F4F7F9] dark:bg-white/10" />
      </div>
      <div className="w-9 h-5 rounded-full bg-[#F4F7F9] dark:bg-white/10" />
      <div className="w-7 h-7 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
      <div className="w-7 h-7 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
    </div>
  )
}

// ---------------------------------------------------------------------------
// Delete confirm
// ---------------------------------------------------------------------------

function DeleteConfirm({
  rule,
  onConfirm,
  onCancel,
  isDeleting,
}: {
  rule:       ReminderRuleResponse
  onConfirm:  () => void
  onCancel:   () => void
  isDeleting: boolean
}) {
  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onCancel} />
      <div className="relative w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6">
        <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white mb-2">Delete Rule</h2>
        <p className="text-sm text-c-muted mb-5">
          Remove this reminder rule? Automated follow-ups using it will stop.
        </p>
        <div className="flex gap-3 justify-end">
          <button
            onClick={onCancel}
            className="px-4 py-2 rounded-lg text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
          >
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
// Page
// ---------------------------------------------------------------------------

export default function RemindersPage() {
  const [showCreate, setShowCreate]   = useState(false)
  const [editing,    setEditing]      = useState<ReminderRuleResponse | null>(null)
  const [deleting,   setDeleting]     = useState<ReminderRuleResponse | null>(null)

  const { data, isLoading } = useReminderRules()
  const deleteMut  = useDeleteReminderRule()
  const toggleMut  = useToggleReminderRule()

  // Sort by triggerOffset ascending: before due → on due → after due
  const rules = [...(data?.content ?? [])].sort((a, b) => a.triggerOffset - b.triggerOffset)

  async function confirmDelete() {
    if (!deleting) return
    await deleteMut.mutateAsync(deleting.id)
    setDeleting(null)
  }

  function handleToggle(rule: ReminderRuleResponse) {
    toggleMut.mutate({ id: rule.id, active: !rule.active })
  }

  return (
    <>
      <div className="space-y-5">

        {/* Header */}
        <div className="flex items-start justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Reminders</h1>
            <p className="text-sm text-c-muted mt-0.5">
              Automate follow-ups at the right moment — before, on, or after the due date.
            </p>
          </div>
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-1.5 px-3 py-2 text-sm font-semibold text-white rounded-lg hover:opacity-90 transition-opacity shrink-0"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            <Plus size={15} strokeWidth={2.5} />
            <span className="hidden sm:inline">Add Rule</span>
          </button>
        </div>

        {/* Content */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
          {isLoading ? (
            <>
              {[...Array(3)].map((_, i) => (
                <RowSkeleton key={i} isLast={i === 2} />
              ))}
            </>
          ) : rules.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 gap-3 text-center">
              <div className="w-12 h-12 rounded-full bg-[#29B6F6]/10 flex items-center justify-center">
                <Bell size={20} className="text-[#29B6F6]" strokeWidth={1.5} />
              </div>
              <div>
                <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">No reminder rules yet</p>
                <p className="text-sm text-c-muted mt-1">
                  Add a rule to automatically follow up with clients at the right time.
                </p>
              </div>
              <button
                onClick={() => setShowCreate(true)}
                className="mt-1 px-4 py-2 text-sm font-semibold text-white rounded-lg hover:opacity-90 transition-opacity"
                style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
              >
                Add your first rule
              </button>
            </div>
          ) : (
            rules.map((rule, idx) => (
              <RuleRow
                key={rule.id}
                rule={rule}
                isLast={idx === rules.length - 1}
                onEdit={setEditing}
                onDelete={setDeleting}
                onToggle={handleToggle}
              />
            ))
          )}
        </div>

        {/* Info note */}
        {rules.length > 0 && (
          <p className="text-xs text-c-muted">
            Rules apply to all invoices where auto-reminders are enabled on the client.
          </p>
        )}
      </div>

      {showCreate && <RuleModal onClose={() => setShowCreate(false)} />}
      {editing    && <RuleModal rule={editing} onClose={() => setEditing(null)} />}

      {deleting && (
        <DeleteConfirm
          rule={deleting}
          onConfirm={confirmDelete}
          onCancel={() => setDeleting(null)}
          isDeleting={deleteMut.isPending}
        />
      )}
    </>
  )
}
