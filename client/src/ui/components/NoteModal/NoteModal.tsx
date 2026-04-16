import { useState } from 'react'
import { X } from 'lucide-react'

interface Props {
  title:        string
  subtitle:     string
  confirmLabel: string
  confirmStyle: 'danger' | 'primary'
  onConfirm:    (note: string) => void
  onCancel:     () => void
  isLoading:    boolean
}

export default function NoteModal({
  title,
  subtitle,
  confirmLabel,
  confirmStyle,
  onConfirm,
  onCancel,
  isLoading,
}: Props) {
  const [note, setNote] = useState('')

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end sm:items-center sm:justify-center sm:p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onCancel} />
      <div className="relative w-full sm:max-w-sm bg-white dark:bg-[#1B2838] rounded-t-2xl sm:rounded-2xl shadow-xl">
        <div className="sm:hidden flex justify-center pt-3 pb-1">
          <div className="w-10 h-1 rounded-full bg-[#8A9BAE]/30" />
        </div>
        <div className="px-6 pt-4 pb-6 space-y-4">
          <div className="flex items-start justify-between">
            <div className="flex-1 min-w-0 pr-3">
              <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">{title}</h2>
              <p className="text-xs text-c-muted mt-0.5 leading-relaxed">{subtitle}</p>
            </div>
            <button onClick={onCancel} className="text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors shrink-0">
              <X size={18} />
            </button>
          </div>

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

          <div className="flex gap-3">
            <button
              onClick={onCancel}
              className="flex-1 py-2.5 rounded-xl text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={() => onConfirm(note)}
              disabled={isLoading}
              className={[
                'flex-1 py-2.5 rounded-xl text-sm font-semibold text-white disabled:opacity-50 transition-opacity hover:opacity-90',
                confirmStyle === 'danger' ? 'bg-red-500' : '',
              ].join(' ')}
              style={confirmStyle === 'primary' ? { background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' } : undefined}
            >
              {isLoading ? 'Processing…' : confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
