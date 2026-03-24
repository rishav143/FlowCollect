export type ReminderChannel = 'EMAIL' | 'SMS' | 'WHATSAPP'

export interface ReminderRuleResponse {
  id:           string
  name:         string | null
  triggerOffset: number        // days relative to due date: negative=before, 0=on, positive=after
  channel:      ReminderChannel
  templateId:   string | null
  templateName: string | null
  active:       boolean
  createdAt:    string
  updatedAt:    string
}

export interface ReminderRuleRequest {
  name?:          string
  triggerOffset:  number
  channel:        ReminderChannel
  templateId?:    string | null
}
