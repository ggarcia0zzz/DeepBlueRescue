import { request } from './client'

// payload: { animalId, specialistId, performedAt, type, description }
export const registerTreatment = (payload) =>
  request('/api/treatments', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
