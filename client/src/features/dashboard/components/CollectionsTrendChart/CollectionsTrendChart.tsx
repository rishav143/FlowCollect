import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LabelList,
  Cell,
} from 'recharts'
import { useUIStore } from '@/store/ui.store'

type TrendPoint = {
  month: string
  collected: number
  forecast?: number
}

function compactAmount(value: number, currency: string): string {
  const symbol =
    new Intl.NumberFormat('en', {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    })
      .formatToParts(0)
      .find((p) => p.type === 'currency')?.value ?? ''

  if (value === 0) return ''
  if (value >= 1_000_000) return `${symbol}${(value / 1_000_000).toFixed(1)}M`
  if (value >= 1_000)     return `${symbol}${(value / 1_000).toFixed(1)}k`
  return `${symbol}${value}`
}

// Custom top-of-bar label
function TopLabel(props: {
  x?: number; y?: number; width?: number; value?: number; currency: string
}) {
  const { x = 0, y = 0, width = 0, value = 0, currency } = props
  if (!value) return null
  return (
    <text
      x={x + width / 2}
      y={y - 5}
      textAnchor="middle"
      fontSize={10}
      fontWeight={600}
      fill="currentColor"
      className="text-[#0D1B2A] dark:text-white"
    >
      {compactAmount(value, currency)}
    </text>
  )
}

// Dashed forecast bar via custom shape
function DashedBar(props: {
  x?: number; y?: number; width?: number; height?: number
  fill?: string; isDark?: boolean
}) {
  const { x = 0, y = 0, width = 0, height = 0, isDark } = props
  if (height <= 0) return null
  const stroke = isDark ? '#60a5fa' : '#29B6F6'
  return (
    <g>
      <rect
        x={x} y={y} width={width} height={height}
        fill={isDark ? 'rgba(96,165,250,0.12)' : 'rgba(41,182,246,0.10)'}
        stroke={stroke}
        strokeWidth={1.5}
        strokeDasharray="4 3"
        rx={4}
      />
    </g>
  )
}

interface Props {
  data: TrendPoint[]
  currency: string
  isLoading?: boolean
}

export default function CollectionsTrendChart({ data, currency, isLoading }: Props) {
  const isDark = useUIStore((s) => s.theme === 'dark')

  const barColor  = isDark ? '#29B6F6' : '#29B6F6'
  const gridColor = isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'
  const axisColor = isDark ? 'rgba(255,255,255,0.35)' : 'rgba(13,27,42,0.4)'

  // Split actual vs forecast points
  const actualData   = data.map((d) => ({ ...d, bar: d.forecast == null ? d.collected : 0 }))
  const forecastData = data.map((d) => ({ ...d, bar: d.forecast ?? 0 }))

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border px-5 pt-4 pb-3">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Collections Trend</h2>
          <p className="text-xs text-c-muted mt-0.5">Monthly collected · last 6 months + forecast</p>
        </div>
        {/* Legend */}
        <div className="flex items-center gap-3 text-xs text-c-muted">
          <span className="flex items-center gap-1">
            <span className="inline-block w-3 h-2.5 rounded-sm bg-[#29B6F6]" />
            Collected
          </span>
          <span className="flex items-center gap-1">
            <svg width="14" height="10" className="shrink-0">
              <rect x="0" y="1" width="14" height="8" rx="2"
                fill="none" stroke={isDark ? '#60a5fa' : '#29B6F6'}
                strokeWidth="1.5" strokeDasharray="3 2" />
            </svg>
            Forecast
          </span>
        </div>
      </div>

      {/* Chart */}
      {isLoading ? (
        <div className="h-48 flex items-end gap-2 px-2 animate-pulse">
          {[60, 80, 50, 90, 70, 85, 65].map((h, i) => (
            <div
              key={i}
              className="flex-1 rounded-t bg-[#F4F7F9] dark:bg-white/10"
              style={{ height: `${h}%` }}
            />
          ))}
        </div>
      ) : (
        <ResponsiveContainer width="100%" height={200}>
          <BarChart
            data={data}
            margin={{ top: 22, right: 4, left: 0, bottom: 0 }}
            barCategoryGap="30%"
          >
            <CartesianGrid vertical={false} stroke={gridColor} />
            <XAxis
              dataKey="month"
              tick={{ fontSize: 11, fill: axisColor }}
              axisLine={false}
              tickLine={false}
            />
            <YAxis hide />
            <Tooltip
              cursor={{ fill: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.04)' }}
              content={({ active, payload, label }) => {
                if (!active || !payload?.length) return null
                const item = payload[0]?.payload as TrendPoint
                const isForecast = item.forecast != null && item.collected === 0
                return (
                  <div className="bg-white dark:bg-[#243447] border border-c-border rounded-lg px-3 py-2 shadow-sm text-xs">
                    <p className="font-semibold text-[#0D1B2A] dark:text-white mb-1">{label}</p>
                    {isForecast ? (
                      <p className="text-[#29B6F6]">Forecast: {compactAmount(item.forecast!, currency)}</p>
                    ) : (
                      <p className="text-[#0D1B2A] dark:text-white">
                        Collected: {compactAmount(item.collected, currency)}
                      </p>
                    )}
                  </div>
                )
              }}
            />

            {/* Actual collected bars */}
            <Bar dataKey="collected" radius={[4, 4, 0, 0]} maxBarSize={48}>
              {data.map((entry, index) => (
                <Cell
                  key={index}
                  fill={entry.forecast != null ? 'transparent' : barColor}
                />
              ))}
              <LabelList
                content={(props) => (
                  <TopLabel {...(props as any)} currency={currency} />
                )}
              />
            </Bar>

            {/* Forecast bar (dashed outline, last month only) */}
            <Bar
              dataKey="forecast"
              maxBarSize={48}
              shape={(props: any) => <DashedBar {...props} isDark={isDark} />}
            >
              <LabelList
                content={(props) => (
                  <TopLabel {...(props as any)} currency={currency} />
                )}
              />
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}
