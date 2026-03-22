import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listTemplates } from '@/api/template.api'
import type { ListTemplatesParams } from '@/types/template.types'

export function useTemplates(params?: ListTemplatesParams) {
  const orgId = useAuthStore((s) => s.org?.id ?? '')

  return useQuery({
    queryKey: ['templates', orgId, params],
    queryFn:  () => listTemplates(orgId, params),
    enabled:  !!orgId,
  })
}
