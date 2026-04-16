import { useState } from 'react'
import { X } from 'lucide-react'

interface Props {
  invoiceNumber: string
  onConfirm:     (note: string, newDueDate: string | undefined) => void
  onCancel:      () => void
  isLoading:     boolean
}

export default function RequestRemainingModal({ invoiceNumber, onConfirm, onCancel, isLoading }: Props) {
  const [note,       setNote]       = useState('')
  const [newDueDate, setNewDueDate] = useState('')

  // Min date for the date input — today
  const today = new Date().toISOString().split('T')[0]

  function handleConfirm() {
    onConfirm(note, newDueDate || undefined)
  }

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end sm:items-center sm:justify-center sm:p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onCancel} />
      <div className="relative w-full sm:max-w-sm bg-white dark:bg-[#1B2838] rounded-t-2xl sm:rounded-2xl shadow-xl flex flex-col max-h-[92dvh] sm:max-h-[90vh]">
        <div className="sm:hidden flex justify-center pt-3 pb-1 shrink-0">
          <div className="w-10 h-1 rounded-full bg-[#8A9BAE]/30" />
        </div>

        <div className="px-6 pt-4 pb-6 space-y-4 overflow-y-auto flex-1">
          {/* Header */}
          <div className="flex items-start justify-between">
            <div className="flex-1 min-w-0 pr-3">
              <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Approve & Request Remaining</h2>
              <p className="text-xs text-c-muted mt-0.5 leading-relaxed">
                Invoice {invoiceNumber} — partial payment will be recorded and the customer will receive a request for the balance.
              </p>
            </div>
            <button onClick={onCancel} className="text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors shrink-0">
              <X size={18} />
            </button>
          </div>

          {/* New due date */}
          <div>
            <label className="text-xs font-semibold uppercase tracking-wide text-c-muted">
              New due date <span className="normal-case font-normal text-c-muted">(optional)</span>
            </label>
            <p className="text-[11px] text-c-muted mt-0.5 mb-1.5">
              Set a new deadline for the remaining balance. Leave blank to keep the original due date.
            </p>
            <input
              type="date"
              value={newDueDate}
              min={today}
              onChange={(e) => setNewDueDate(e.target.value)}
              className="w-full text-sm rounded-lg border border-c-border bg-transparent text-[#0D1B2A] dark:text-white p-2.5 focus:outline-none focus:border-[#8A9BAE]/40 [color-scheme:light] dark:[color-scheme:dark]"
            />
          </div>

          {/* Note */}
          <div>
            <label className="text-xs font-semibold uppercase tracking-wide text-c-muted">
              Note <span className="normal-case font-normal text-c-muted">(optional)</span>
            </label>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={3}
              placeholder="Add a note for the customer..."
              className="mt-1.5 w-full text-sm rounded-lg border border-c-border bg-transparent text-[#0D1B2A] dark:text-white placeholder:text-c-muted p-2.5 focus:outline-none focus:border-[#8A9BAE]/40 resize-none"
            />
          </div>

          {/* Actions */}
          <div className="flex gap-3">
            <button
              onClick={onCancel}
              className="flex-1 py-2.5 rounded-xl text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleConfirm}
              disabled={isLoading}
              className="flex-1 py-2.5 rounded-xl text-sm font-semibold text-white disabled:opacity-50 transition-opacity hover:opacity-90"
              style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
            >
              {isLoading ? 'Processing…' : 'Approve & Request'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
