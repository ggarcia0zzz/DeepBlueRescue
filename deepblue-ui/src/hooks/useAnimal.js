import { useQuery } from '@tanstack/react-query'
import {
  getAnimal,
  getAnimalTreatments,
  getTreatmentEligibility,
} from '../api/animals'

// ADAPTADO: recibe el objeto animal del caso ({ id, animalCode, ... })
// porque dos endpoints piden el id y uno pide el código.
export function useAnimal(animal) {
  const id = animal?.id
  const enabled = Boolean(id)

  const animalQuery = useQuery({
    queryKey: ['animal', id],
    queryFn: () => getAnimal(id),
    enabled,
  })

  const treatments = useQuery({
    queryKey: ['animal', id, 'treatments'],
    queryFn: () => getAnimalTreatments(id),
    enabled,
  })

  const eligibility = useQuery({
    queryKey: ['animal', id, 'eligibility'],
    queryFn: () => getTreatmentEligibility(animal.animalCode),
    enabled,
  })

  return { animal: animalQuery, treatments, eligibility }
}
