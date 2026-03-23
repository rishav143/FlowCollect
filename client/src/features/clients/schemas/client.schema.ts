export interface ClientFormValues {
  name:         string
  email:        string
  phone:        string
  companyName:  string
  address:      string
}

export function validateClientForm(v: ClientFormValues): Partial<Record<keyof ClientFormValues, string>> {
  const errors: Partial<Record<keyof ClientFormValues, string>> = {}
  if (!v.name.trim())                                                      errors.name  = 'Name is required'
  if (v.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.email))            errors.email = 'Enter a valid email'
  return errors
}
