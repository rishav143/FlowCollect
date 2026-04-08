import { useState, memo, useCallback } from 'react'
import { Plus, FileText } from 'lucide-react'
import { useTemplates } from '@/features/templates/hooks/useTemplates'
import { useToggleTemplate } from '@/features/templates/hooks/useTemplateMutations'
import TemplateCard from '@/features/templates/components/TemplateCard/TemplateCard'
import CreateTemplateModal from '@/features/templates/modals/CreateTemplateModal'
import EditTemplateModal   from '@/features/templates/modals/EditTemplateModal'
import DeleteTemplateModal from '@/features/templates/modals/DeleteTemplateModal'
import ViewToggle, { useViewPreference, gridClass } from '@/ui/components/ViewToggle'
import type { TemplateResponse, TemplateChannel } from '@/types/template.types'

const CHANNEL_TABS: { label: string; value: TemplateChannel | 'ALL' }[] = [
  { label: 'All',      value: 'ALL'      },
  { label: 'Email',    value: 'EMAIL'    },
  { label: 'SMS',      value: 'SMS'      },
  { label: 'WhatsApp', value: 'WHATSAPP' },
]

function CardSkeleton() {
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-[#8A9BAE]/20 dark:border-white/10 p-4 space-y-3 animate-pulse">
      <div className="flex items-center gap-2">
        <div className="w-4 h-4 rounded bg-[#F4F7F9] dark:bg-white/10" />
        <div className="h-4 w-40 rounded bg-[#F4F7F9] dark:bg-white/10" />
      </div>
      <div className="h-3 w-full rounded bg-[#F4F7F9] dark:bg-white/10" />
      <div className="h-3 w-4/5 rounded bg-[#F4F7F9] dark:bg-white/10" />
    </div>
  )
}

export default function TemplatesPage() {
  const [channelFilter, setChannelFilter] = useState<TemplateChannel | 'ALL'>('ALL')
  const [showCreate, setShowCreate] = useState(false)
  const [editing,    setEditing]    = useState<TemplateResponse | null>(null)
  const [deleting,   setDeleting]   = useState<TemplateResponse | null>(null)
  const [view, setView] = useViewPreference('templates', 'grid')

  const { data, isLoading, isError } = useTemplates(
    channelFilter !== 'ALL' ? { channel: channelFilter, mode: 'MANUAL' } : { mode: 'MANUAL' },
  )

  const allTemplates  = (data?.content ?? []).filter((t) => t.mode === 'MANUAL')
  const defaults      = allTemplates.filter((t) => t.systemDefined)
  const userTemplates = allTemplates.filter((t) => !t.systemDefined)

  return (
    <div className="space-y-5">

      {/* Page header */}
      <div className="flex items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Templates</h1>
          <p className="text-sm text-c-muted mt-0.5">Reusable messages for your follow-ups and reminders</p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <ViewToggle value={view} onChange={setView} />
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-1.5 px-3 py-2 text-sm font-semibold text-white rounded-lg hover:opacity-90 transition-opacity"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            <Plus size={15} strokeWidth={2.5} />
            <span className="hidden sm:inline">New Template</span>
          </button>
        </div>
      </div>

      {/* Channel filter tabs */}
      <div className="flex gap-1 p-1 bg-[#F4F7F9] dark:bg-[#1B2838] rounded-lg w-fit">
        {CHANNEL_TABS.map((tab) => (
          <button
            key={tab.value}
            onClick={() => setChannelFilter(tab.value)}
            className={[
              'px-4 py-1.5 text-sm font-medium rounded-md transition-colors',
              channelFilter === tab.value
                ? 'bg-white dark:bg-[#243447] text-[#0D1B2A] dark:text-white shadow-sm'
                : 'text-c-muted hover:text-[#0D1B2A] dark:hover:text-white',
            ].join(' ')}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      {isLoading && (
        <div className={gridClass(view)}>
          {[1, 2, 3].map((i) => <CardSkeleton key={i} />)}
        </div>
      )}

      {isError && (
        <div className="py-12 text-center text-sm text-[#EF4444]">
          Failed to load templates. Please refresh and try again.
        </div>
      )}

      {!isLoading && !isError && allTemplates.length === 0 && (
        <div className="py-16 flex flex-col items-center gap-3 text-center">
          <div className="w-12 h-12 rounded-full bg-[#F4F7F9] dark:bg-white/10 flex items-center justify-center">
            <FileText size={22} className="text-c-muted" strokeWidth={1.5} />
          </div>
          <div>
            <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">No templates yet</p>
            <p className="text-xs text-c-muted mt-1">Create a template and reuse it across reminder rules and follow-ups.</p>
          </div>
          <button
            onClick={() => setShowCreate(true)}
            className="mt-1 px-4 py-2 text-sm font-semibold text-white rounded-lg hover:opacity-90 transition-opacity"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            Create your first template
          </button>
        </div>
      )}

      {!isLoading && !isError && defaults.length > 0 && (
        <div className="space-y-3">
          <p className="text-xs font-semibold uppercase tracking-wide text-c-muted">Default Templates</p>
          <div className={gridClass(view)}>
            {defaults.map((t) => (
              <TemplateCardWithToggle
                key={t.id}
                template={t}
                onEdit={setEditing}
                onDelete={setDeleting}
              />
            ))}
          </div>
        </div>
      )}

      {!isLoading && !isError && userTemplates.length > 0 && (
        <div className="space-y-3">
          {defaults.length > 0 && (
            <p className="text-xs font-semibold uppercase tracking-wide text-c-muted">Your Templates</p>
          )}
          <div className={gridClass(view)}>
            {userTemplates.map((t) => (
              <TemplateCardWithToggle
                key={t.id}
                template={t}
                onEdit={setEditing}
                onDelete={setDeleting}
              />
            ))}
          </div>
        </div>
      )}

      {showCreate && <CreateTemplateModal onClose={() => setShowCreate(false)} />}
      {editing    && <EditTemplateModal   template={editing}  onClose={() => setEditing(null)} />}
      {deleting   && <DeleteTemplateModal template={deleting} onClose={() => setDeleting(null)} />}
    </div>
  )
}

const TemplateCardWithToggle = memo(function TemplateCardWithToggle({
  template,
  onEdit,
  onDelete,
}: {
  template: TemplateResponse
  onEdit:   (t: TemplateResponse) => void
  onDelete: (t: TemplateResponse) => void
}) {
  const { mutate } = useToggleTemplate(template.id, template.active)
  const onToggle = useCallback(() => mutate(), [mutate])
  return (
    <TemplateCard
      template={template}
      onEdit={onEdit}
      onDelete={onDelete}
      onToggle={onToggle}
    />
  )
})
