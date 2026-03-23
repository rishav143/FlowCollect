import { useState, useEffect } from 'react'
import type { ClientFormValues } from '../../schemas/client.schema'
import { validateClientForm } from '../../schemas/client.schema'
import type { CustomerResponse } from '@/types/customer.types'

interface Props {
  initial?: CustomerResponse
  onSubmit: (values: ClientFormValues) => void
  isLoading: boolean
}

const EMPTY: ClientFormValues = { name: '', email: '', phone: '', companyName: '', address: '' }

export default function ClientForm({ initial, onSubmit, isLoading }: Props) {
  const [values, setValues] = useState<ClientFormValues>(EMPTY)
  const [errors, setErrors] = useState<Partial<Record<keyof ClientFormValues, string>>>({})
  const [touched, setTouched] = useState<Partial<Record<keyof ClientFormValues, boolean>>>({})

  useEffect(() => {
    if (initial) {
      setValues({
        name:        initial.name,
        email:       initial.email        ?? '',
        phone:       initial.phone        ?? '',
        companyName: initial.companyName  ?? '',
        address:     initial.address      ?? '',
      })
    }
  }, [initial])

  function set(field: keyof ClientFormValues, value: string) {
    setValues((prev) => ({ ...prev, [field]: value }))
    setTouched((prev) => ({ ...prev, [field]: true }))
    setErrors((prev) => ({ ...prev, [field]: undefined }))
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const errs = validateClientForm(values)
    if (Object.keys(errs).length > 0) {
      setErrors(errs)
      setTouched({ name: true, email: true, phone: true, companyName: true, address: true })
      return
    }
    onSubmit(values)
  }

  function field(id: keyof ClientFormValues, label: string, placeholder: string, type = 'text') {
    const err = touched[id] ? errors[id] : undefined
    return (
      <div>
        <label className="block text-xs font-semibold uppercase tracking-wide text-[#8A9BAE] mb-1.5">
          {label}
        </label>
        <input
          type={type}
          value={values[id]}
          onChange={(e) => set(id, e.target.value)}
          placeholder={placeholder}
          className={[
            'w-full text-sm rounded-lg border bg-transparent text-[#0D1B2A] dark:text-white placeholder:text-[#8A9BAE] px-3 py-2.5 focus:outline-none transition-colors',
            err
              ? 'border-red-400 focus:border-red-500'
              : 'border-[#F4F7F9] dark:border-white/10 focus:border-[#8A9BAE]/40',
          ].join(' ')}
        />
        {err && <p className="text-xs text-red-500 mt-1">{err}</p>}
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {field('name',        'Name *',        'Full name or business name')}
      {field('companyName', 'Company',       'Company name (optional)')}
      {field('email',       'Email',         'billing@example.com', 'email')}
      {field('phone',       'Phone',         '+91 98765 43210', 'tel')}
      {field('address',     'Address',       'City, State (optional)')}
      <button
        type="submit"
        disabled={isLoading}
        className="w-full py-2.5 rounded-lg text-sm font-semibold text-white disabled:opacity-50 transition-opacity hover:opacity-90"
        style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
      >
        {isLoading ? 'Saving…' : (initial ? 'Save Changes' : 'Add Client')}
      </button>
    </form>
  )
}
