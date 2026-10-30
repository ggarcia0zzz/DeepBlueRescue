import { request } from './client'

// ADAPTADO: estos dos endpoints usan el id numérico del animal
export const getAnimal = (id) => request(`/api/animals/${id}`)

export const getAnimalTreatments = (id) =>
  request(`/api/animals/${id}/treatments`)

// Este sí usa el código (ej. "A-001")
export const getTreatmentEligibility = (animalCode) =>
  request(`/api/animals/${animalCode}/treatment-eligibility`)
