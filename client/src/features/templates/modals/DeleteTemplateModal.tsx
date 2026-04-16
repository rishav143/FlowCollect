import { createPortal } from 'react-dom'
import { AlertTriangle, X } from 'lucide-react'
import { useDeleteTemplate } from '@/features/templates/hooks/useTemplateMutations'
import type { TemplateResponse } from '@/types/template.types'

interface Props {
  template: TemplateResponse
  onClose:  () => void
}

export default function DeleteTemplateModal({ template, onClose }: Props) {
  const { mutate, isPending, isError, error, reset } = useDeleteTemplate()

  const errorMessage =
    (error as { response?: { data?: { message?: string } } })?.response?.data?.message
    ?? 'Failed to delete template. It may be in use by a reminder rule.'

  function handleDelete() {
    reset()
    mutate(template.id, { onSuccess: onClose })
  }

  return createPortal(
    <>
      <div
        className="fixed inset-0 z-[200] bg-black/60"
        onClick={onClose}
        aria-hidden="true"
      />

      <div className="fixed inset-0 z-[200] flex flex-col justify-end sm:items-center sm:justify-center sm:p-4 pointer-events-none">
        <div
          className="pointer-events-auto w-full sm:max-w-sm bg-white dark:bg-[#1B2838] rounded-t-2xl sm:rounded-2xl shadow-2xl"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="sm:hidden flex justify-center pt-3 pb-1">
            <div className="w-10 h-1 rounded-full bg-[#8A9BAE]/30" />
          </div>
          <div className="flex items-center justify-between px-6 pt-4 sm:pt-5 pb-4 border-b border-c-border">
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Delete template?</h2>
            <button
              onClick={onClose}
              className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          <div className="px-6 py-5 space-y-4">
            <div className="flex gap-3">
              <AlertTriangle size={20} className="shrink-0 text-[#EF4444] mt-0.5" strokeWidth={1.8} />
              <p className="text-sm text-[#0D1B2A]/80 dark:text-white/70">
                <span className="font-semibold dark:text-white">{template.name}</span> will be permanently deleted.
                Any reminder rules using this template will need to be updated.
              </p>
            </div>

            {isError && (
              <div className="flex items-start gap-2 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700/40 px-3 py-2">
                <AlertTriangle size={14} className="text-red-500 mt-0.5 shrink-0" />
                <p className="text-xs text-red-600 dark:text-red-400">{errorMessage}</p>
              </div>
            )}

            <div className="flex gap-3">
              <button
                onClick={onClose}
                className="flex-1 py-2.5 text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleDelete}
                disabled={isPending}
                className="flex-1 py-2.5 text-sm font-semibold text-white bg-[#EF4444] hover:opacity-90 rounded-xl transition-opacity disabled:opacity-50"
              >
                {isPending ? 'Deleting…' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </>,
    document.body,
  )
}
