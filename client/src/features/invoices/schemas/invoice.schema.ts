import { z } from 'zod'

export const lineItemSchema = z.object({
  description: z.string().min(1, 'Description is required').max(255),
  quantity:    z.number().int().positive('Must be at least 1'),
  unitPrice:   z.number().min(0, 'Price must be 0 or more'),
})

export const createInvoiceSchema = z.object({
  invoiceNumber:  z.string().min(1, 'Invoice number is required').max(100),
  customerId:     z.string().optional(),
  dueDate:        z.string().optional(),
  taxPercentage:  z.number().min(0).max(100).optional(),
  items:          z.array(lineItemSchema).min(1, 'Add at least one line item'),
})

export type CreateInvoiceValues = z.infer<typeof createInvoiceSchema>
