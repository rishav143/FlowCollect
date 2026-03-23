import { X } from 'lucide-react'
import ClientForm from '../components/ClientForm/ClientForm'
import { useCreateClient } from '../hooks/useClients'
import type { ClientFormValues } from '../schemas/client.schema'

interface Props {
  onClose: () => void
}

export default function AddClientModal({ onClose }: Props) {
  const create = useCreateClient()

  async function handleSubmit(values: ClientFormValues) {
    await create.mutateAsync({
      name:        values.name.trim(),
      email:       values.email.trim()       || undefined,
      phone:       values.phone.trim()       || undefined,
      companyName: values.companyName.trim() || undefined,
      address:     values.address.trim()     || undefined,
    })
    onClose()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-md bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Add Client</h2>
          <button onClick={onClose} className="text-[#8A9BAE] hover:text-[#0D1B2A] dark:hover:text-white transition-colors">
            <X size={18} />
          </button>
        </div>
        <ClientForm onSubmit={handleSubmit} isLoading={create.isPending} />
      </div>
    </div>
  )
}
