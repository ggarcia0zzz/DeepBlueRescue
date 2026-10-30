import { useMutation, useQueryClient } from '@tanstack/react-query'
import { registerTreatment } from '../api/treatments'

export function useRegisterTreatment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: registerTreatment,
    onSuccess: (created) => {
      // Prefijo ['animal', id]: refresca animal, tratamientos y elegibilidad
      queryClient.invalidateQueries({
        queryKey: ['animal', created.animalId],
      })
    },
  })
}
