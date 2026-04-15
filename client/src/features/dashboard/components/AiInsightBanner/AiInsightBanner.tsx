import React, { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Sparkles, RefreshCw } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { getAiOverviewInsights } from '@/api/ai.api'
import { formatCurrency } from '@/lib/format'
import { usePlan } from '@/hooks/usePlan'

// Parse "- bullet\n- bullet" into a string array
function parseBullets(raw: string): string[] {
  return raw
    .split('\n')
    .map((l) => l.replace(/^-\s*/, '').trim())
    .filter(Boolean)
}

// Reformat any raw currency amounts in AI text using proper locale formatting.
function reformatAmounts(text: string, currency: string): string {
  return text.replace(
    /(₹|\$|€|£|¥|₩|A\$|S\$|AED\s?)([\d,]+(?:\.\d{1,2})?)/g,
    (_match, _symbol, numStr) => {
      const num = parseFloat(numStr.replace(/,/g, ''))
      if (isNaN(num)) return _match
      const hasDecimals = numStr.includes('.')
      return formatCurrency(num, currency, { decimals: hasDecimals })
    },
  )
}

// Render a string that may contain **bold** segments as React nodes
function renderBold(text: string): React.ReactNode {
  const parts = text.split(/\*\*(.+?)\*\*/g)
  return parts.map((part, i) =>
    i % 2 === 1 ? <strong key={i}>{part}</strong> : part
  )
}

// "2h ago", "just now", "3d ago"
function timeAgo(isoString: string): string {
  const diffMs = Date.now() - new Date(isoString).getTime()
  const mins   = Math.floor(diffMs / 60_000)
  const hours  = Math.floor(diffMs / 3_600_000)
  const days   = Math.floor(diffMs / 86_400_000)
  if (mins < 1)   return 'just now'
  if (mins < 60)  return `${mins}m ago`
  if (hours < 24) return `${hours}h ago`
  return `${days}d ago`
}

export default function AiInsightBanner() {
  const { isPro } = usePlan()
  const orgId    = useAuthStore((s) => s.org?.id ?? '')
  const currency = useAuthStore((s) => s.org?.currency ?? 'USD')
  const qc       = useQueryClient()
  const queryKey = ['ai-insights-overview', orgId]

  const [isRefreshing, setIsRefreshing] = useState(false)

  const { data, isLoading, isError } = useQuery({
    queryKey,
    queryFn:   () => getAiOverviewInsights(orgId),
    enabled:   !!orgId && isPro,
    // Never auto-refetch — backend cache handles staleness (48h TTL).
    staleTime: Infinity,
    retry:     false,
  })

  async function handleRefresh() {
    setIsRefreshing(true)
    try {
      const fresh = await getAiOverviewInsights(orgId, true)
      qc.setQueryData(queryKey, fresh)
    } finally {
      setIsRefreshing(false)
    }
  }

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

  // Not on PRO — don't render the banner at all
  if (!isPro) return null

  // Silent failure — don't break the dashboard if AI is unavailable
  if (isError || !data) return null

  const bullets = parseBullets(data.insights)
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
          <span className="text-xs text-c-muted">
            · {data.cached ? `from ${timeAgo(data.generatedAt)}` : 'just generated'}
          </span>
        </div>
        <button
          onClick={handleRefresh}
          disabled={isRefreshing}
          title="Refresh insights"
          className={[
            'text-c-muted hover:text-[#29B6F6] transition-colors p-1 rounded',
            isRefreshing ? 'opacity-40 cursor-not-allowed' : '',
          ].join(' ')}
        >
          <RefreshCw size={13} className={isRefreshing ? 'animate-spin' : ''} />
        </button>
      </div>

      {/* Bullets */}
      <ul className="px-5 py-3.5 space-y-2">
        {bullets.map((bullet, i) => (
          <li key={i} className="flex items-start gap-2.5">
            <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-[#29B6F6] shrink-0" />
            <span className="text-sm text-[#0D1B2A] dark:text-[#C9D9E8] leading-snug">
              {renderBold(reformatAmounts(bullet, currency))}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
