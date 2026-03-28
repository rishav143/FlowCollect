import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Trash2, Plus } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { listPayments, deletePayment } from '@/api/payment.api'
import RecordPaymentModal from '../../modals/RecordPaymentModal'
import type { PaymentMode } from '@/types/payment.types'
import { formatCurrency, formatDate } from '@/lib/format'

const MODE_LABEL: Record<PaymentMode, string> = {
  CASH:          'Cash',
  UPI:           'UPI',
  BANK_TRANSFER: 'Bank Transfer',
  CARD:          'Card',
  CHEQUE:        'Cheque',
}

interface Props {
  invoiceId:       string
  remainingAmount: number
  currency:        string
  lifeCycleStatus: string
}

export default function PaymentsTab({ invoiceId, remainingAmount, currency, lifeCycleStatus }: Props) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')
  const qc    = useQueryClient()
  const [showModal, setShowModal] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['payments', orgId, invoiceId],
    queryFn:  () => listPayments(orgId, invoiceId),
    enabled:  !!orgId && !!invoiceId,
  })

  const deleteMut = useMutation({
    mutationFn: (paymentId: string) => deletePayment(orgId, invoiceId, paymentId),
    onSuccess:  () => {
      qc.invalidateQueries({ queryKey: ['payments', orgId, invoiceId] })
      qc.invalidateQueries({ queryKey: ['invoices', orgId] })
    },
  })

  const payments     = data?.content ?? []
  const canAddPayment = lifeCycleStatus !== 'DRAFT' && lifeCycleStatus !== 'CANCELLED' && lifeCycleStatus !== 'PAID'

  if (isLoading) {
    return (
      <div className="space-y-2 animate-pulse">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="h-12 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
        ))}
      </div>
    )
  }

  return (
    <>
      <div className="space-y-4">
        {canAddPayment && (
          <div className="flex justify-end">
            <button
              onClick={() => setShowModal(true)}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity"
              style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
            >
              <Plus size={14} strokeWidth={2.5} />
              Record Payment
            </button>
          </div>
        )}

        {payments.length === 0 ? (
          <div className="py-10 text-center text-sm text-c-muted">No payments recorded</div>
        ) : (
          <div className="space-y-2">
            {payments.map((p) => (
              <div
                key={p.id}
                className="flex items-center gap-4 px-4 py-3 bg-[#F4F7F9] dark:bg-[#243447] rounded-lg"
              >
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">{formatCurrency(p.amount, currency, { decimals: false })}</p>
                  <p className="text-xs text-c-muted">
                    {p.mode ? MODE_LABEL[p.mode] : 'Unknown'} · {formatDate(p.paidAt ?? p.createdAt)}
                    {p.referenceId && ` · Ref: ${p.referenceId}`}
                  </p>
                </div>
                {p.notes && <p className="text-xs text-c-muted hidden sm:block">{p.notes}</p>}
                <button
                  onClick={() => deleteMut.mutate(p.id)}
                  disabled={deleteMut.isPending}
                  className="p-1.5 rounded text-c-muted hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors disabled:opacity-30"
                >
                  <Trash2 size={13} strokeWidth={2} />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {showModal && (
        <RecordPaymentModal
          invoiceId={invoiceId}
          remainingAmount={remainingAmount}
          currency={currency}
          onClose={() => setShowModal(false)}
        />
      )}
    </>
  )
}
