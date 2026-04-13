import { Send, Trash2, Download, Clock, Loader2, Ban } from 'lucide-react'
import type { LifeCycleStatus } from '@/types/invoice.types'

interface Props {
  lifeCycleStatus: LifeCycleStatus
  dueDate:         string | null
  onIssue:         () => void
  onDelete:        () => void
  onDownloadPdf:   () => void
  onFollowup:      () => void
  onCancel:        () => void
  isIssuing:       boolean
  isDownloading:   boolean
}

const btnBase = 'flex items-center gap-2 px-3.5 py-2 rounded-lg text-sm font-medium transition-all'

export default function InvoiceActionBar({
  lifeCycleStatus,
  dueDate,
  onIssue,
  onDelete,
  onDownloadPdf,
  onFollowup,
  onCancel,
  isIssuing,
  isDownloading,
}: Props) {
  const isDraft        = lifeCycleStatus === 'DRAFT'
  const isCancelled    = lifeCycleStatus === 'CANCELLED'
  const isPaid         = lifeCycleStatus === 'PAID'
  const isCancellable  = lifeCycleStatus === 'ISSUED' || lifeCycleStatus === 'PARTIALLY_PAID'

  return (
    <div className="flex flex-wrap items-center gap-2">
      {/* Issue invoice — draft only */}
      {isDraft && (
        <div title={!dueDate ? 'Set a due date before issuing' : undefined}>
          <button
            onClick={onIssue}
            disabled={isIssuing || !dueDate}
            className={`${btnBase} text-white hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed`}
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            {isIssuing
              ? <Loader2 size={15} className="animate-spin" />
              : <Send size={15} strokeWidth={2} />
            }
            Issue Invoice
          </button>
        </div>
      )}

      {/* Download PDF — any non-draft */}
      {!isDraft && (
        <button
          onClick={onDownloadPdf}
          disabled={isDownloading}
          className={`${btnBase} bg-white dark:bg-[#1B2838] border border-c-border text-[#0D1B2A] dark:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] shadow-sm disabled:opacity-50`}
        >
          {isDownloading
            ? <Loader2 size={15} className="animate-spin" />
            : <Download size={15} strokeWidth={2} />
          }
          Download PDF
        </button>
      )}

      {/* Follow-up — active invoices only */}
      {!isDraft && !isPaid && !isCancelled && (
        <button
          onClick={onFollowup}
          className={`${btnBase} bg-white dark:bg-[#1B2838] border border-c-border text-[#0D1B2A] dark:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] shadow-sm`}
        >
          <Clock size={15} strokeWidth={2} />
          Send Follow-up
        </button>
      )}

      {/* Cancel — issued or partially paid only */}
      {isCancellable && (
        <button
          onClick={onCancel}
          className={`${btnBase} border border-red-200 dark:border-red-500/20 text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10`}
        >
          <Ban size={15} strokeWidth={2} />
          Cancel Invoice
        </button>
      )}

      {/* Delete — draft only */}
      {isDraft && (
        <button
          onClick={onDelete}
          className={`${btnBase} border border-red-200 dark:border-red-500/20 text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10`}
        >
          <Trash2 size={15} strokeWidth={2} />
          Delete
        </button>
      )}
    </div>
  )
}
