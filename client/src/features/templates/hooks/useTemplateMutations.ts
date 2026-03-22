import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import {
  createTemplate,
  updateTemplate,
  activateTemplate,
  deactivateTemplate,
  deleteTemplate,
} from '@/api/template.api'
import type { CreateTemplateRequest, UpdateTemplateRequest } from '@/types/template.types'

function useOrgId() {
  return useAuthStore((s) => s.org?.id ?? '')
}

// ---------------------------------------------------------------------------

export function useCreateTemplate() {
  const orgId = useOrgId()
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (body: CreateTemplateRequest) => createTemplate(orgId, body),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['templates', orgId] }),
  })
}

export function useUpdateTemplate(id: string) {
  const orgId = useOrgId()
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (body: UpdateTemplateRequest) => updateTemplate(orgId, id, body),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['templates', orgId] }),
  })
}

export function useToggleTemplate(id: string, active: boolean) {
  const orgId = useOrgId()
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: () =>
      active ? deactivateTemplate(orgId, id) : activateTemplate(orgId, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['templates', orgId] }),
  })
}

export function useDeleteTemplate() {
  const orgId = useOrgId()
  const qc    = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => deleteTemplate(orgId, id),
    onSuccess:  () => qc.invalidateQueries({ queryKey: ['templates', orgId] }),
  })
}
