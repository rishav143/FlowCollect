import { z } from 'zod'

export const lineItemSchema = z.object({
  description: z.string().min(1, 'Description is required').max(255),
  quantity:    z.number().int().positive('Must be at least 1'),
  unitPrice:   z.number().min(0, 'Price must be 0 or more'),
})

export const taxLineSchema = z.object({
  label:  z.string().min(1, 'Label is required').max(50),
  amount: z.number().min(0, 'Amount must be 0 or more'),
})

export const createInvoiceSchema = z.object({
  invoiceNumber:  z.string().min(1, 'Invoice number is required').max(100),
  customerId:     z.string().optional(),
  dueDate:        z.string().optional(),
  taxLines:       z.array(taxLineSchema).optional(),
  discountAmount: z.number().min(0).optional(),
  items:          z.array(lineItemSchema).min(1, 'Add at least one line item'),
})

export type CreateInvoiceValues = z.infer<typeof createInvoiceSchema>
