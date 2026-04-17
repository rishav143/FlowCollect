import { useState, useEffect } from 'react'
import { useOrgProfile, useUpdateOrgProfile } from '../hooks/useOrgSettings'
import Select from '@/components/ui/Select'

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const CURRENCIES = ['INR', 'USD', 'EUR', 'GBP', 'AED', 'SGD', 'AUD']

const TIMEZONES = [
  { value: 'Asia/Kolkata',        label: 'India (IST, UTC+5:30)'         },
  { value: 'UTC',                 label: 'UTC'                            },
  { value: 'Asia/Dubai',          label: 'Dubai (GST, UTC+4)'            },
  { value: 'Asia/Singapore',      label: 'Singapore (SGT, UTC+8)'        },
  { value: 'Europe/London',       label: 'London (GMT/BST)'              },
  { value: 'America/New_York',    label: 'New York (EST/EDT)'            },
  { value: 'Australia/Sydney',    label: 'Sydney / Melbourne (AEST, UTC+10/+11)' },
  { value: 'Australia/Brisbane',  label: 'Brisbane (AEST, UTC+10)'               },
  { value: 'Australia/Perth',     label: 'Perth (AWST, UTC+8)'           },
]

// ---------------------------------------------------------------------------
// Shared field wrapper
// ---------------------------------------------------------------------------

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-1.5">
        {label}
      </label>
      {children}
    </div>
  )
}

const inputCls = 'w-full text-sm rounded-lg border border-c-border bg-transparent text-[#0D1B2A] dark:text-white placeholder:text-c-muted px-3 py-2.5 focus:outline-none focus:border-[#8A9BAE]/40 transition-colors'

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function OrgSettingsPage() {
  const { data, isLoading } = useOrgProfile()
  const update = useUpdateOrgProfile()

  const [name,     setName]     = useState('')
  const [email,    setEmail]    = useState('')
  const [phone,    setPhone]    = useState('')
  const [address,  setAddress]  = useState('')
  const [currency, setCurrency] = useState('INR')
  const [timezone, setTimezone] = useState('Asia/Kolkata')
  const [mode,     setMode]     = useState<'PAYMENT_LINK' | 'CONFIRMATION_FLOW'>('CONFIRMATION_FLOW')
  const [saved,    setSaved]    = useState(false)

  useEffect(() => {
    if (data) {
      setName(data.name)
      setEmail(data.email)
      setPhone(data.phone ?? '')
      setAddress(data.address ?? '')
      setCurrency(data.currency)
      setTimezone(data.timezone)
      setMode(data.paymentCollectionMode)
    }
  }, [data])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    await update.mutateAsync({ name, email, phone: phone || undefined, address: address || undefined, currency, timezone, paymentCollectionMode: mode })
    setSaved(true)
    setTimeout(() => setSaved(false), 2500)
  }

  if (isLoading) {
    return (
      <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-5 space-y-4 animate-pulse">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-10 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
        ))}
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
        <div className="px-5 py-4 border-b border-c-border">
          <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Organisation Profile</h2>
        </div>
        <div className="p-5 space-y-4">

          <Field label="Organisation Name">
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Your business name"
              className={inputCls}
              required
            />
          </Field>

          <Field label="Billing Email">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="billing@yourcompany.com"
              className={inputCls}
            />
          </Field>

          <Field label="Phone (optional)">
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="+1 555 000 0000"
              className={inputCls}
            />
          </Field>

          <Field label="Business Address (optional)">
            <textarea
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder={"123 Main St\nCity, State / Province\nCountry"}
              rows={3}
              className={inputCls + ' resize-none'}
            />
            <p className="text-xs text-c-muted mt-1">Appears on invoices sent to your clients.</p>
          </Field>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Field label="Currency">
              <Select
                value={currency}
                onChange={setCurrency}
                options={CURRENCIES.map((c) => ({ value: c, label: c }))}
              />
            </Field>

            <Field label="Timezone">
              <Select
                value={timezone}
                onChange={setTimezone}
                options={TIMEZONES}
              />
            </Field>
          </div>

          <Field label="Payment Collection Mode">
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setMode('CONFIRMATION_FLOW')}
                className={[
                  'flex-1 py-2.5 text-sm font-medium rounded-lg border transition-colors',
                  mode === 'CONFIRMATION_FLOW'
                    ? 'border-[#29B6F6] text-[#29B6F6] bg-[#29B6F6]/5'
                    : 'border-c-border text-c-muted hover:text-[#0D1B2A] dark:hover:text-white',
                ].join(' ')}
              >
                Confirmation Flow
              </button>
              <button
                type="button"
                disabled
                className="flex-1 py-2.5 text-sm font-medium rounded-lg border border-c-border text-c-muted opacity-50 cursor-not-allowed relative"
              >
                Payment Link
                <span className="ml-1.5 text-[10px] font-semibold uppercase tracking-wide text-[#29B6F6]">
                  Coming Soon
                </span>
              </button>
            </div>
            <p className="text-xs text-c-muted mt-1.5">
              {mode === 'CONFIRMATION_FLOW'
                ? 'Clients submit payment proof and you approve it manually.'
                : 'Clients pay directly via a payment link you send.'}
            </p>
          </Field>

        </div>
        <div className="flex items-center justify-end gap-3 px-5 py-4 border-t border-c-border">
          {saved && <span className="text-sm text-green-600 dark:text-green-400">Saved</span>}
          <button
            type="submit"
            disabled={update.isPending}
            className="px-5 py-2 text-sm font-semibold text-white rounded-lg disabled:opacity-50 hover:opacity-90 transition-opacity"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            {update.isPending ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </div>
    </form>
  )
}
