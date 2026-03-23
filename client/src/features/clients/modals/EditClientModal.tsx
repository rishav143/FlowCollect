import { X } from 'lucide-react'
import ClientForm from '../components/ClientForm/ClientForm'
import { useUpdateClient } from '../hooks/useClients'
import type { ClientFormValues } from '../schemas/client.schema'
import type { CustomerResponse } from '@/types/customer.types'

interface Props {
  customer: CustomerResponse
  onClose:  () => void
}

export default function EditClientModal({ customer, onClose }: Props) {
  const update = useUpdateClient()

  async function handleSubmit(values: ClientFormValues) {
    await update.mutateAsync({
      id: customer.id,
      body: {
        name:        values.name.trim()        || undefined,
        email:       values.email.trim()       || undefined,
        phone:       values.phone.trim()       || undefined,
        companyName: values.companyName.trim() || undefined,
        address:     values.address.trim()     || undefined,
      },
    })
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-md bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Edit Client</h2>
          <button onClick={onClose} className="text-[#8A9BAE] hover:text-[#0D1B2A] dark:hover:text-white transition-colors">
            <X size={18} />
          </button>
        </div>
        <ClientForm initial={customer} onSubmit={handleSubmit} isLoading={update.isPending} />
      </div>
    </div>
  )
}
