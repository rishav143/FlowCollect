import axios from 'axios'
import type {
  TemplateResponse,
  CreateTemplateRequest,
  UpdateTemplateRequest,
  ListTemplatesParams,
} from '@/types/template.types'

// Matches the Page<T> envelope returned by Spring Data
interface Page<T> {
  content:          T[]
  totalElements:    number
  totalPages:       number
  number:           number  // 0-based page index
  size:             number
}

// ---------------------------------------------------------------------------
// Base URL builder — keeps endpoints DRY
// ---------------------------------------------------------------------------

function base(orgId: string) {
  return `/api/v1/organizations/${orgId}/templates`
}

// ---------------------------------------------------------------------------
// CRUD
// ---------------------------------------------------------------------------

export async function createTemplate(
  orgId: string,
  body: CreateTemplateRequest,
): Promise<TemplateResponse> {
  const { data } = await axios.post<TemplateResponse>(base(orgId), body)
  return data
}

export async function listTemplates(
  orgId: string,
  params?: ListTemplatesParams,
): Promise<Page<TemplateResponse>> {
  const { data } = await axios.get<Page<TemplateResponse>>(base(orgId), { params })
  return data
}

export async function getTemplate(
  orgId: string,
  id: string,
): Promise<TemplateResponse> {
  const { data } = await axios.get<TemplateResponse>(`${base(orgId)}/${id}`)
  return data
}

export async function updateTemplate(
  orgId: string,
  id: string,
  body: UpdateTemplateRequest,
): Promise<TemplateResponse> {
  const { data } = await axios.patch<TemplateResponse>(`${base(orgId)}/${id}`, body)
  return data
}

// ---------------------------------------------------------------------------
// Activate / Deactivate
// ---------------------------------------------------------------------------

export async function activateTemplate(
  orgId: string,
  id: string,
): Promise<TemplateResponse> {
  const { data } = await axios.patch<TemplateResponse>(`${base(orgId)}/${id}/activate`)
  return data
}

export async function deactivateTemplate(
  orgId: string,
  id: string,
): Promise<TemplateResponse> {
  const { data } = await axios.patch<TemplateResponse>(`${base(orgId)}/${id}/deactivate`)
  return data
}

// ---------------------------------------------------------------------------
// Delete
// ---------------------------------------------------------------------------

export async function deleteTemplate(orgId: string, id: string): Promise<void> {
  await axios.delete(`${base(orgId)}/${id}`)
}
