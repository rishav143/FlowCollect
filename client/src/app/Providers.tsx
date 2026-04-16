import { QueryClient, QueryClientProvider, MutationCache, keepPreviousData } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useUIStore }    from '@/store/ui.store'
import { useToastStore } from '@/store/toast.store'
import { extractApiError } from '@/lib/errors'

const queryClient = new QueryClient({
  mutationCache: new MutationCache({
    onMutate:  () => useUIStore.getState().mutationStart(),
    onSettled: () => useUIStore.getState().mutationEnd(),
    onError: (error, _vars, _ctx, mutation) => {
      // Skip if the mutation defines its own onError — it shows inline feedback
      if (mutation.options.onError) return
      // Skip if explicitly silenced (e.g. mutations with call-site onError handlers)
      if (mutation.meta?.silent) return
      // 401/403 already handled by the axios interceptor (force logout + redirect)
      const status = (error as { response?: { status?: number } })?.response?.status
      if (status === 401 || status === 403) return

      useToastStore.getState().addToast({
        type:     'error',
        message:  extractApiError(error),
        duration: 5000,
      })
    },
  }),
  defaultOptions: {
    queries: {
      staleTime:          1000 * 60 * 2,
      retry:              1,
      refetchOnWindowFocus: false,
      placeholderData:    keepPreviousData,
    },
  },
})

export default function Providers({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  )
}
