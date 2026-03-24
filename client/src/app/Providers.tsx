import { QueryClient, QueryClientProvider, keepPreviousData } from '@tanstack/react-query'
import type { ReactNode } from 'react'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime:          1000 * 60 * 2,  // 2 min — change here to affect all queries
      retry:              1,
      refetchOnWindowFocus: false,
      placeholderData:    keepPreviousData, // keeps old data visible during filter/param changes
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
