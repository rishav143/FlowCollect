export type ReminderChannel     = 'EMAIL' | 'SMS' | 'WHATSAPP'
export type ReminderTriggerType = 'BEFORE_DUE_DATE' | 'ON_DUE_DATE' | 'AFTER_DUE_DATE'

export interface ReminderRuleResponse {
  id:               string
  name:             string | null
  daysOffset:       number          // negative=before, 0=on, positive=after due date
  triggerType:      ReminderTriggerType
  channel:          ReminderChannel
  templateId:       string | null
  templateName:     string | null
  attachPdf:        boolean
  active:           boolean
  maxOccurrences:   number          // >= 1; >1 means cyclic
  cycleIntervalDays: number         // 0 when maxOccurrences == 1
  startDate:        string | null   // YYYY-MM-DD; null = no restriction
  createdAt:        string
  updatedAt:        string
}

export interface ReminderRuleRequest {
  name?:             string
  daysOffset:        number
  triggerType:       ReminderTriggerType
  channel:           ReminderChannel
  templateId?:       string | null
  active?:           boolean
  maxOccurrences?:   number
  cycleIntervalDays?: number
  startDate?:        string | null  // YYYY-MM-DD
}
