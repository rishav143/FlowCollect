import React from 'react'
import { useQuery } from '@tanstack/react-query'
import { Sparkles, RefreshCw } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { getAiOverviewInsights } from '@/api/ai.api'
import { formatCurrency } from '@/lib/format'

// Parse "- bullet\n- bullet" into a string array
function parseBullets(raw: string): string[] {
  return raw
    .split('\n')
    .map((l) => l.replace(/^-\s*/, '').trim())
    .filter(Boolean)
}

// Reformat any raw currency amounts in AI text using proper locale formatting.
// Handles amounts like ₹826382.40 → ₹8,26,382.40 (INR) or $1234567 → $1,234,567
function reformatAmounts(text: string, currency: string): string {
  // Match currency symbol(s) followed by digits (with or without existing separators)
  return text.replace(
    /(₹|\$|€|£|¥|₩|A\$|S\$|AED\s?)([\d,]+(?:\.\d{1,2})?)/g,
    (_match, _symbol, numStr) => {
      const num = parseFloat(numStr.replace(/,/g, ''))
      if (isNaN(num)) return _match
      // Keep decimals only if original had them
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

export default function AiInsightBanner() {
  const orgId    = useAuthStore((s) => s.org?.id ?? '')
  const currency = useAuthStore((s) => s.org?.currency ?? 'USD')

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
              {renderBold(reformatAmounts(bullet, currency))}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
