import { useState } from 'react'
import { createPortal } from 'react-dom'
import { X, AlertCircle } from 'lucide-react'
import type { AxiosError } from 'axios'
import TemplateForm from '@/features/templates/components/TemplateForm/TemplateForm'
import { useCreateTemplate } from '@/features/templates/hooks/useTemplateMutations'
import type { TemplateFormValues } from '@/features/templates/schemas/template.schema'

interface Props {
  onClose: () => void
}

export default function CreateTemplateModal({ onClose }: Props) {
  const { mutate, isPending } = useCreateTemplate()
  const [error, setError] = useState<string | null>(null)

  function handleSubmit(values: TemplateFormValues) {
    setError(null)
    mutate(values, {
      onSuccess: onClose,
      onError: (err) => {
        const msg = (err as AxiosError<{ message?: string }>).response?.data?.message
        setError(msg ?? 'Failed to create template — please try again.')
      },
    })
  }

  return createPortal(
    <>
      {/* Backdrop — rendered into document.body, bypasses all stacking contexts */}
      <div
        className="fixed inset-0 z-[200] bg-black/60"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Dialog */}
      <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 pointer-events-none">
        <div
          className="pointer-events-auto w-full max-w-lg bg-white dark:bg-[#1B2838] rounded-2xl shadow-2xl max-h-[90vh] flex flex-col"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-c-border shrink-0">
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">New Template</h2>
            <button
              onClick={onClose}
              className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          <div className="px-6 py-5 overflow-y-auto flex-1">
            {error && (
              <div className="mb-4 flex items-start gap-2 px-3 py-2.5 rounded-lg bg-[#EF4444]/10 border border-[#EF4444]/30 text-sm text-[#EF4444]">
                <AlertCircle size={15} className="shrink-0 mt-px" />
                <span>{error}</span>
              </div>
            )}
            <TemplateForm
              onSubmit={handleSubmit}
              isPending={isPending}
              submitLabel="Create Template"
            />
          </div>
        </div>
      </div>
    </>,
    document.body,
  )
}
