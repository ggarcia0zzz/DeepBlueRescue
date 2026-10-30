# DeepBlue Rescue — Frontend (React + Vite + TanStack Query v5)

## Ejecutar
1. Backend Spring Boot en http://localhost:8080 (con PostgreSQL y datos).
2. `npm install`
3. `npm run dev` → http://localhost:5173

## Adaptaciones respecto al enunciado del laboratorio
El backend real difiere del contrato del .md, por lo que se ajustó:

| Enunciado | Backend real | Dónde se ajustó |
|---|---|---|
| PATCH por `caseCode`, body `{status}` | PATCH por `id`, body `{newStatus}` | `api/rescueCases.js`, `ChangeStatus.jsx` |
| Animal y tratamientos por `animalCode` | Por `id` numérico | `api/animals.js`, `hooks/useAnimal.js` |
| `caseItem.animalCode` | `caseItem.animal.{id, animalCode}` (anidado) | `CaseDetail.jsx` |
| POST con `animalCode`/`specialistCode` | `animalId`/`specialistId` numéricos | `TreatmentForm.jsx` |
| Columna `specialistCode` | El DTO trae `specialistName` | `TreatmentList.jsx` |

Extra: al cambiar el estado de un caso también se invalida `['animal']`,
porque la elegibilidad para tratamiento depende del estado del caso.
