import { request } from './client'

export const getCasesByStatus = (status) =>
  request(`/api/rescue-cases?status=${status}`)

// ADAPTADO: el backend usa el id numérico y el body { newStatus }
export const changeCaseStatus = (id, newStatus) =>
  request(`/api/rescue-cases/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ newStatus }),
  })
