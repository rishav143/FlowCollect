import { X, Clock } from 'lucide-react'

interface Props {
  invoiceNumber: string
  onClose:       () => void
}

export default function FollowupModal({ invoiceNumber, onClose }: Props) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6 text-center">
        <button type="button" onClick={onClose} className="absolute top-4 right-4 p-1.5 rounded-lg text-[#8A9BAE] hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">
          <X size={18} />
        </button>
        <div className="flex items-center justify-center w-12 h-12 rounded-full bg-[#29B6F6]/10 mx-auto mb-4">
          <Clock size={22} className="text-[#29B6F6]" />
        </div>
        <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white mb-1">Send Follow-up</h2>
        <p className="text-sm text-[#8A9BAE] mb-4">
          Follow-up for <span className="font-semibold text-[#0D1B2A] dark:text-white">{invoiceNumber}</span> — go to the Follow-ups page to manage reminders.
        </p>
        <button
          onClick={onClose}
          className="w-full py-2 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          Got it
        </button>
      </div>
    </div>
  )
}
