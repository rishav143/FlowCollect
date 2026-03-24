import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import TemplateForm from '@/features/templates/components/TemplateForm/TemplateForm'
import { useUpdateTemplate } from '@/features/templates/hooks/useTemplateMutations'
import type { TemplateResponse } from '@/types/template.types'
import type { TemplateFormValues } from '@/features/templates/schemas/template.schema'

interface Props {
  template: TemplateResponse
  onClose:  () => void
}

export default function EditTemplateModal({ template, onClose }: Props) {
  const { mutate, isPending } = useUpdateTemplate(template.id)

  function handleSubmit(values: TemplateFormValues) {
    mutate(values, { onSuccess: onClose })
  }

  return createPortal(
    <>
      <div
        className="fixed inset-0 z-[200] bg-black/60"
        onClick={onClose}
        aria-hidden="true"
      />

      <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 pointer-events-none">
        <div
          className="pointer-events-auto w-full max-w-lg bg-white dark:bg-[#1B2838] rounded-2xl shadow-2xl"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-c-border">
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Edit Template</h2>
            <button
              onClick={onClose}
              className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          <div className="px-6 py-5">
            <TemplateForm
              defaultValues={{
                name:    template.name,
                channel: template.channel,
                subject: template.subject ?? undefined,
                body:    template.body,
                tone:    template.tone,
              }}
              onSubmit={handleSubmit}
              isPending={isPending}
              submitLabel="Save Changes"
            />
          </div>
        </div>
      </div>
    </>,
    document.body,
  )
}
