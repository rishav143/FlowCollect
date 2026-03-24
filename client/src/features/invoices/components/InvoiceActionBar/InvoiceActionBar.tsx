import { Send, Trash2, Download, Clock, Loader2 } from 'lucide-react'
import type { LifeCycleStatus } from '@/types/invoice.types'

interface Props {
  lifeCycleStatus: LifeCycleStatus
  onIssue:         () => void
  onDelete:        () => void
  onDownloadPdf:   () => void
  onFollowup:      () => void
  isIssuing:       boolean
  isDownloading:   boolean
}

const btnBase = 'flex items-center gap-2 px-3.5 py-2 rounded-lg text-sm font-medium transition-all'

export default function InvoiceActionBar({
  lifeCycleStatus,
  onIssue,
  onDelete,
  onDownloadPdf,
  onFollowup,
  isIssuing,
  isDownloading,
}: Props) {
  const isDraft     = lifeCycleStatus === 'DRAFT'
  const isCancelled = lifeCycleStatus === 'CANCELLED'
  const isPaid      = lifeCycleStatus === 'PAID'

  return (
    <div className="flex flex-wrap items-center gap-2">
      {/* Issue invoice — draft only */}
      {isDraft && (
        <button
          onClick={onIssue}
          disabled={isIssuing}
          className={`${btnBase} text-white hover:opacity-90 disabled:opacity-50`}
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          {isIssuing
            ? <Loader2 size={15} className="animate-spin" />
            : <Send size={15} strokeWidth={2} />
          }
          Issue Invoice
        </button>
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
