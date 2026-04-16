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
    <div className="fixed inset-0 z-50 flex flex-col justify-end sm:items-center sm:justify-center sm:p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full sm:max-w-md bg-white dark:bg-[#1B2838] rounded-t-2xl sm:rounded-2xl shadow-xl flex flex-col max-h-[95dvh] sm:max-h-[90vh]">
        <div className="sm:hidden flex justify-center pt-3 pb-1 shrink-0">
          <div className="w-10 h-1 rounded-full bg-[#8A9BAE]/30" />
        </div>
        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-c-border shrink-0">
          <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Edit Client</h2>
          <button onClick={onClose} className="text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors">
            <X size={18} />
          </button>
        </div>
        <div className="px-6 py-5 overflow-y-auto flex-1">
          <ClientForm initial={customer} onSubmit={handleSubmit} isLoading={update.isPending} />
        </div>
      </div>
    </div>
  )
}
