import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import TemplateForm from '@/features/templates/components/TemplateForm/TemplateForm'
import { useCreateTemplate } from '@/features/templates/hooks/useTemplateMutations'
import type { TemplateFormValues } from '@/features/templates/schemas/template.schema'

interface Props {
  onClose: () => void
}

export default function CreateTemplateModal({ onClose }: Props) {
  const { mutate, isPending } = useCreateTemplate()

  function handleSubmit(values: TemplateFormValues) {
    mutate(values, { onSuccess: onClose })
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
