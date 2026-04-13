import { memo } from 'react'
import { Mail, MessageSquare, MessageCircle, Pencil, Trash2, Sparkles } from 'lucide-react'
import type { TemplateResponse } from '@/types/template.types'

// ---------------------------------------------------------------------------
// Tone badge
// ---------------------------------------------------------------------------

const TONE_STYLES: Record<string, string> = {
  POLITE:  'bg-[#22C55E]/10 text-[#22C55E]',
  NEUTRAL: 'bg-[#8A9BAE]/15 text-[#0D1B2A] dark:text-c-muted',
  FIRM:    'bg-[#EF4444]/10 text-[#EF4444]',
}

function ToneBadge({ tone }: { tone: string }) {
  return (
    <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full uppercase tracking-wide ${TONE_STYLES[tone] ?? ''}`}>
      {tone.toLowerCase()}
    </span>
  )
}

// ---------------------------------------------------------------------------
// Channel icon map
// ---------------------------------------------------------------------------

const CHANNEL_ICON = {
  EMAIL:    <Mail          size={15} strokeWidth={1.8} />,
  SMS:      <MessageSquare size={15} strokeWidth={1.8} />,
  WHATSAPP: <MessageCircle size={15} strokeWidth={1.8} />,
}

// ---------------------------------------------------------------------------
// TemplateCard
// ---------------------------------------------------------------------------

interface Props {
  template:   TemplateResponse
  onEdit:     (t: TemplateResponse) => void
  onDelete:   (t: TemplateResponse) => void
  onToggle:   (t: TemplateResponse) => void
}

const TemplateCard = memo(function TemplateCard({ template, onEdit, onDelete, onToggle }: Props) {
  const isSystem = template.systemDefined

  return (
    <div className={[
      'rounded-xl border transition-colors',
      'bg-white dark:bg-[#1B2838]',
      template.active
        ? 'border-[#8A9BAE]/20 dark:border-white/10'
        : 'border-[#8A9BAE]/10 dark:border-white/5 opacity-60',
    ].join(' ')}>
      <div className="p-4">

        {/* Header row */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-2 min-w-0">
            <span className="text-c-muted shrink-0">
              {CHANNEL_ICON[template.channel] ?? <MessageSquare size={15} strokeWidth={1.8} />}
            </span>
            <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white truncate">
              {template.name}
            </p>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            {isSystem ? (
              <span className="flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-full uppercase tracking-wide bg-[#29B6F6]/10 text-[#29B6F6]">
                <Sparkles size={9} strokeWidth={2} />
                Default
              </span>
            ) : (
              <ToneBadge tone={template.tone} />
            )}

            {/* Active toggle — hidden for system/default templates */}
            {!isSystem && (
              <button
                onClick={() => onToggle(template)}
                aria-label={template.active ? 'Deactivate' : 'Activate'}
                className={[
                  'relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent',
                  'transition-colors duration-200 ease-in-out focus:outline-none',
                  template.active ? 'bg-[#29B6F6]' : 'bg-[#8A9BAE]/30',
                ].join(' ')}
              >
                <span
                  className={[
                    'pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow',
                    'transition duration-200 ease-in-out',
                    template.active ? 'translate-x-4' : 'translate-x-0',
                  ].join(' ')}
                />
              </button>
            )}
          </div>
        </div>

        {/* Subject — email only */}
        {template.channel === 'EMAIL' && template.subject && (
          <p className="mt-1.5 text-xs text-c-muted">
            Subject: <span className="text-[#0D1B2A] dark:text-white/80">{template.subject}</span>
          </p>
        )}

        {/* Body preview */}
        <p className="mt-2 text-sm text-[#0D1B2A]/70 dark:text-white/60 line-clamp-2 leading-relaxed">
          {template.body}
        </p>
      </div>

      {/* Footer actions */}
      <div className="flex items-center justify-end gap-1 px-4 py-2 border-t border-c-border">
        <button
          onClick={() => onEdit(template)}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-[#0D1B2A] dark:text-white hover:bg-[#F4F7F9] dark:hover:bg-white/10 rounded-lg transition-colors"
        >
          <Pencil size={13} strokeWidth={1.8} />
          Edit
        </button>
        {!isSystem && (
          <button
            onClick={() => onDelete(template)}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-[#EF4444] hover:bg-[#EF4444]/10 rounded-lg transition-colors"
          >
            <Trash2 size={13} strokeWidth={1.8} />
            Delete
          </button>
        )}
      </div>
    </div>
  )
})

export default TemplateCard
