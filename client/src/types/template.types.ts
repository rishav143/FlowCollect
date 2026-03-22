export type TemplateChannel = 'EMAIL' | 'SMS' | 'WHATSAPP'
export type TemplateTone    = 'POLITE' | 'NEUTRAL' | 'FIRM'

export interface TemplateResponse {
  id:        string
  name:      string
  channel:   TemplateChannel
  subject:   string | null   // email only
  body:      string
  tone:      TemplateTone
  active:    boolean
  createdAt: string          // ISO-8601
  updatedAt: string
}

export interface CreateTemplateRequest {
  name:    string
  channel: TemplateChannel
  subject?: string
  body:    string
  tone:    TemplateTone
}

// All fields optional for PATCH
export type UpdateTemplateRequest = Partial<CreateTemplateRequest>

export interface ListTemplatesParams {
  name?:    string
  channel?: TemplateChannel
  tone?:    TemplateTone
  page?:    number
  size?:    number
  sort?:    string
}
