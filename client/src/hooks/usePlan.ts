import { useBilling } from '@/features/settings/hooks/useBilling'

const STARTER_INVOICE_LIMIT = 3

export function usePlan() {
  const { data: billing, isLoading } = useBilling()

  const plan               = billing?.plan ?? 'STARTER'
  const isPro              = plan === 'PRO' || plan === 'BUSINESS'
  const activeInvoiceCount = billing?.activeInvoiceCount ?? 0
  const atInvoiceLimit     = !isPro && activeInvoiceCount >= STARTER_INVOICE_LIMIT

  return {
    plan,
    isPro,
    isLoading,
    activeInvoiceCount,
    invoiceLimit:   isPro ? null : STARTER_INVOICE_LIMIT,
    atInvoiceLimit,
  }
}
