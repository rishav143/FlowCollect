import { useQuery } from '@tanstack/react-query'
import { Sparkles, RefreshCw } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { getAiOverviewInsights } from '@/api/ai.api'

// Parse "- bullet\n- bullet" into a string array
function parseBullets(raw: string): string[] {
  return raw
    .split('\n')
    .map((l) => l.replace(/^-\s*/, '').trim())
    .filter(Boolean)
}

export default function AiInsightBanner() {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  const { data, isLoading, isError, isFetching, refetch } = useQuery({
    queryKey:  ['ai-insights-overview', orgId],
    queryFn:   () => getAiOverviewInsights(orgId),
    enabled:   !!orgId,
    // Fetch only once per page load — never auto-refetch to save API cost.
    // The user can manually refresh via the button.
    staleTime: Infinity,
    retry:     false,
  })

  // Loading skeleton
  if (isLoading) {
    return (
      <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border px-5 py-4 animate-pulse">
        <div className="flex items-center gap-2 mb-3">
          <div className="h-4 w-4 rounded bg-[#F4F7F9] dark:bg-white/10" />
          <div className="h-3.5 w-28 rounded bg-[#F4F7F9] dark:bg-white/10" />
        </div>
        <div className="space-y-2">
          <div className="h-3 w-full rounded bg-[#F4F7F9] dark:bg-white/10" />
          <div className="h-3 w-5/6 rounded bg-[#F4F7F9] dark:bg-white/10" />
          <div className="h-3 w-4/6 rounded bg-[#F4F7F9] dark:bg-white/10" />
        </div>
      </div>
    )
  }

  // Silent failure — don't break the dashboard if AI is unavailable
  if (isError || !data) return null

  const bullets = parseBullets(data)
  if (bullets.length === 0) return null

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
      {/* Header bar */}
      <div className="flex items-center justify-between px-5 py-3 border-b border-c-border">
        <div className="flex items-center gap-2">
          <Sparkles size={14} className="text-[#29B6F6] shrink-0" />
          <span className="text-xs font-semibold text-[#0D1B2A] dark:text-white">
            AI Insights
          </span>
          <span className="text-xs text-c-muted">· based on your current data</span>
        </div>
        <button
          onClick={() => refetch()}
          disabled={isFetching}
          title="Refresh insights"
          className={[
            'text-c-muted hover:text-[#29B6F6] transition-colors p-1 rounded',
            isFetching ? 'opacity-40 cursor-not-allowed' : '',
          ].join(' ')}
        >
          <RefreshCw size={13} className={isFetching ? 'animate-spin' : ''} />
        </button>
      </div>

      {/* Bullets */}
      <ul className="px-5 py-3.5 space-y-2">
        {bullets.map((bullet, i) => (
          <li key={i} className="flex items-start gap-2.5">
            <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-[#29B6F6] shrink-0" />
            <span className="text-sm text-[#0D1B2A] dark:text-[#C9D9E8] leading-snug">
              {bullet}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
