import { z } from 'zod'

export const templateSchema = z.object({
  name:    z.string().min(1, 'Name is required').max(100),
  channel: z.enum(['EMAIL', 'SMS', 'WHATSAPP']),
  subject: z.string().max(200).optional(),
  body:    z.string().min(1, 'Message body is required'),
  tone:    z.enum(['POLITE', 'NEUTRAL', 'FIRM']),
})

export type TemplateFormValues = z.infer<typeof templateSchema>
