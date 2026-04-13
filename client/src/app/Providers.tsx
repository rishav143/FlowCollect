import { QueryClient, QueryClientProvider, MutationCache, keepPreviousData } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { useUIStore } from '@/store/ui.store'

const queryClient = new QueryClient({
  mutationCache: new MutationCache({
    onMutate:  () => useUIStore.getState().mutationStart(),
    onSettled: () => useUIStore.getState().mutationEnd(),
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
