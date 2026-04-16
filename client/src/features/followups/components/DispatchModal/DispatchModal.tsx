import { useState, useEffect } from 'react'
import { Send, X, AlertCircle, Mail } from 'lucide-react'
import { useDispatchFollowup } from '../../hooks/useFollowups'
import { useToast } from '@/store/toast.store'
import { useTemplates } from '@/features/templates/hooks/useTemplates'
import Select from '@/components/ui/Select'
import type { InvoiceResponse } from '@/types/invoice.types'

interface Props {
  invoice: InvoiceResponse
  onClose: () => void
}

export default function DispatchModal({ invoice, onClose }: Props) {
  const [templateId, setTemplateId] = useState('')
  const [attachPdf,  setAttachPdf]  = useState(true)
  const [sendError,  setSendError]  = useState<string | null>(null)

  const hasNoClient = !invoice.customerId
  const toast       = useToast()
  const dispatch    = useDispatchFollowup(invoice.id)

  const { data: templatesData, isLoading, isFetching } = useTemplates({ size: 100, mode: 'MANUAL' })
  const templatesLoading = isLoading || isFetching
  const activeTemplates  = (templatesData?.content ?? []).filter((t) => t.channel === 'EMAIL' && t.active)
  const hasTemplates     = activeTemplates.length > 0

  // Auto-select default template once templates load
  useEffect(() => {
    if (templatesLoading || templateId || activeTemplates.length === 0) return
    const defaultTpl = activeTemplates.find((t) => t.name === 'Default Email Reminder') ?? activeTemplates[0]
    setTemplateId(defaultTpl.id)
  }, [templatesLoading, activeTemplates.length]) // eslint-disable-line react-hooks/exhaustive-deps

  const canSend = !dispatch.isPending && !hasNoClient && hasTemplates && !!templateId

  async function handleSend() {
    if (!canSend) {
      if (!templateId) { setSendError('Please select a template.'); return }
      return
    }
    setSendError(null)
    try {
      const result = await dispatch.mutateAsync({ channels: ['EMAIL'], templateId, attachPdf })
      if (result.some((f) => f.status === 'FAILED')) {
        setSendError('Follow-up could not be delivered. Please check the client email address.')
        return
      }
      toast.success('Follow-up sent via Email')
      onClose()
    } catch (err: unknown) {
      setSendError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Something went wrong. Please try again.',
      )
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end sm:items-center sm:justify-center sm:p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full sm:max-w-sm bg-white dark:bg-[#1B2838] rounded-t-2xl sm:rounded-2xl shadow-xl overflow-hidden">
        <div className="sm:hidden flex justify-center pt-3 pb-1">
          <div className="w-10 h-1 rounded-full bg-[#8A9BAE]/30" />
        </div>

        {/* Header */}
        <div className="flex items-start justify-between px-6 pt-5 pb-4 border-b border-c-border">
          <div>
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Send Follow-up</h2>
            <p className="text-xs text-c-muted mt-0.5">{invoice.invoiceNumber}</p>
          </div>
          <button onClick={onClose} className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors">
            <X size={16} />
          </button>
        </div>

        {/* Body */}
        <div className="px-6 py-5 space-y-4">

          {/* No-client warning */}
          {hasNoClient && (
            <div className="flex items-start gap-2 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/40 px-3 py-2.5">
              <AlertCircle size={14} className="text-amber-500 mt-0.5 shrink-0" />
              <p className="text-xs text-amber-700 dark:text-amber-400">
                No client assigned. Please assign a client before sending.
              </p>
            </div>
          )}

          {/* Channel badge */}
          <div className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-[#29B6F6]/40 bg-[#29B6F6]/8 text-[#29B6F6] text-xs font-semibold">
            <Mail size={12} strokeWidth={2} />
            Email
          </div>

          {/* Template selector */}
          <div className="space-y-1.5">
            <p className="text-xs font-semibold uppercase tracking-wide text-c-muted">Template</p>
            {templatesLoading ? (
              <div className="h-9 rounded-lg bg-[#F4F7F9] dark:bg-white/10 animate-pulse" />
            ) : !hasTemplates ? (
              <div className="flex items-start gap-2 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/40 px-3 py-2.5">
                <AlertCircle size={14} className="text-amber-500 mt-0.5 shrink-0" />
                <p className="text-xs text-amber-700 dark:text-amber-400">
                  No email template found.{' '}
                  <a href="/templates" className="underline font-medium">Create one in Templates.</a>
                </p>
              </div>
            ) : (
              <Select
                value={templateId}
                onChange={setTemplateId}
                placeholder="Select template…"
                options={activeTemplates.map((t) => ({ value: t.id, label: t.name }))}
              />
            )}
          </div>

          {/* Attach PDF toggle */}
          <label className="flex items-center justify-between gap-3 cursor-pointer">
            <span className="text-sm text-[#0D1B2A] dark:text-white">Attach invoice PDF</span>
            <button
              type="button"
              role="switch"
              aria-checked={attachPdf}
              onClick={() => setAttachPdf(!attachPdf)}
              className={[
                'relative w-9 h-5 rounded-full transition-colors shrink-0',
                attachPdf ? 'bg-[#29B6F6]' : 'bg-[#E2E8F0] dark:bg-white/20',
              ].join(' ')}
            >
              <span className={[
                'absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform',
                attachPdf ? 'translate-x-4' : 'translate-x-0',
              ].join(' ')} />
            </button>
          </label>

          {/* Error */}
          {sendError && (
            <div className="flex items-start gap-2 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700/40 px-3 py-2.5">
              <AlertCircle size={14} className="text-red-500 mt-0.5 shrink-0" />
              <p className="text-xs text-red-600 dark:text-red-400">{sendError}</p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 pb-5 pt-2 flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 py-2.5 rounded-lg text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSend}
            disabled={!canSend}
            className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg text-sm font-semibold text-white disabled:opacity-50 hover:opacity-90 transition-opacity"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            <Send size={13} strokeWidth={2.5} />
            {dispatch.isPending ? 'Sending…' : 'Send'}
          </button>
        </div>
      </div>
    </div>
  )
}
