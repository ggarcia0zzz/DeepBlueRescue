# Respuestas — Laboratorio DeepBlue Rescue

Nombre(s): Gabriela García y Juan David Pardo

## Para pensar

**Checkpoint 1 — ¿Por qué `/api/...` y no `http://localhost:8080/api/...`?**
Porque la URL relativa se resuelve contra el origen de Vite (5173) y el proxy la reenvía a Spring Boot (8080). Así el navegador cree que habla con un solo origen y no aplica CORS; además la URL no queda atada a un servidor concreto.

**Checkpoint 2 — ¿Por qué `request` lanza el error en lugar de devolverlo?**
TanStack Query decide `isError` según si la promesa de `queryFn`/`mutationFn` se rechaza. Si se devolviera el error, la promesa se resolvería y se trataría como éxito (`data` sería el error).

**Checkpoint 3 — ¿Qué pasaría si `queryKey` fuera solo `['rescue-cases']`?**
Todos los estados compartirían la misma entrada de caché. Al cambiar el filtro no se dispararía una consulta nueva y se verían los datos del estado anterior.

**Checkpoint 4 — ¿Para qué sirve `enabled`?**
Para no ejecutar la consulta hasta que existan sus parámetros. Sin él, con el id `undefined`, se pediría `/api/animals/undefined` y el backend respondería con un error.

**Checkpoint 5 — ¿Qué hace `invalidateQueries({ queryKey: ['rescue-cases'] })`?**
Marca como obsoletas todas las consultas cuya clave empiece por `['rescue-cases']` y refetchea las que están en uso. Afecta a todas las listas por estado porque la coincidencia es por prefijo: `['rescue-cases','ADMITTED']`, `['rescue-cases','RELEASED']`, etc. Un caso que cambia de estado debe salir de una lista y entrar en otra.

**Checkpoint 6 — ¿Por qué `useReducer` y no un `useState` por campo? ¿Por qué `invalidateQueries` usa `['animal', id]` sin sufijos?**
`useReducer` concentra el estado del formulario en un objeto, con un solo `handleChange` para todos los campos y un `reset` simple. La clave `['animal', id]` es un prefijo común de animal, tratamientos y elegibilidad, así que una sola invalidación refresca los tres.

## Pruebas del Paso 5 (cambio de estado)

| Prueba | Resultado observado |
|---|---|
| ADMITTED → UNDER_EVALUATION | `200`. El estado del caso cambió y el caso pasó de la lista ADMITTED a UNDER_EVALUATION sin recargar la página. |
| Repetir UNDER_EVALUATION → UNDER_EVALUATION | `409`: "Invalid rescue status transition: UNDER_EVALUATION -> UNDER_EVALUATION". El caso ya estaba en ese estado y el backend no permite repetirlo. |
| Repetir IN_REHABILITATION → IN_REHABILITATION | `409`: "Invalid rescue status transition: IN_REHABILITATION -> IN_REHABILITATION". Mismo motivo: el segundo clic sobre el mismo estado se rechaza. |
| ADMITTED → RELEASED (salto inválido) | `409`: "Invalid rescue status transition: ADMITTED -> RELEASED". Solo se permite avanzar un estado a la vez. |
| IN_REHABILITATION → READY_FOR_RELEASE sin tratamientos | `409`: "Case RC-2026-001 cannot be READY_FOR_RELEASE without at least one treatment" (regla R-C4). |
| IN_REHABILITATION → READY_FOR_RELEASE con un tratamiento registrado | `200`. El caso pasó a READY_FOR_RELEASE. |
| READY_FOR_RELEASE → RELEASED | `200`. "Puede recibir tratamiento" cambió de Sí a No sin recargar la página. |

El `409` se muestra en rojo y la aplicación sigue funcionando.

## Pruebas del Paso 6 (registrar tratamiento)

| Prueba | Resultado observado |
|---|---|
| Descripción corta | `201`. Nuestro backend no valida el tamaño de la descripción (el enunciado esperaba `400`). |
| specialistId vacío | `400` "Request validation failed", con `specialistId` en el detalle por campo. |
| Fecha futura | `409`: "Treatment date cannot be in the future: ...". Es `409` y no `400` porque es una regla de negocio del servicio. |
| Fecha anterior al rescate | `409`: "Treatment date ... is before the rescue date 2026-10-20". |
| Fecha vacía | `400` "Request validation failed", con `performedAt` en el detalle por campo. |
| specialistId inexistente | `404`: "Specialist not found: 999". También lo vimos con el id `1` antes de crear el especialista. |
| Caso RELEASED o CLOSED | `409`: "Case RC-2026-001 is RELEASED and no longer accepts treatments" (regla R-T2). |
| Datos válidos | `201`. El tratamiento apareció en la tabla sin recargar y el formulario se limpió. |

## Observaciones de la pestaña Red (Checkpoints 3, 4 y 6)

- Al registrar un tratamiento válido aparecieron: `POST /api/treatments` con `201`, una nueva petición `GET` de los tratamientos del animal y una nueva consulta a `treatment-eligibility`, todo sin recargar la página. Esto ocurre porque `invalidateQueries(['animal', id])` refresca todas las consultas que empiezan con ese prefijo.
- Al cambiar de filtro y volver al anterior, o al cambiar entre casos y volver al primero, no apareció "Cargando...": los datos de la caché se muestran al instante. Aun así puede verse una petición en segundo plano, porque con la configuración por defecto (`staleTime: 0`) TanStack Query considera los datos viejos y los revalida al volver a montar el componente.
- Con el backend apagado, la lista muestra un mensaje de error en rojo ("No se pudo contactar el servidor") en lugar de una pantalla en blanco, porque `request` convierte el fallo en un `ApiError`.

## Adaptaciones al backend real

El backend que usamos difiere del contrato del enunciado, así que ajustamos el frontend:

- El cambio de estado usa el `id` numérico del caso (`PATCH /api/rescue-cases/{id}/status`) y el body es `{ "newStatus": ... }`, no `{ "status": ... }`.
- Animal y tratamientos se piden por `id` numérico (`/api/animals/{id}`); solo la elegibilidad usa `animalCode`.
- El caso trae el animal anidado (`caseItem.animal.id` y `caseItem.animal.animalCode`), no un campo `animalCode` suelto.
- El POST de tratamientos usa `animalId` y `specialistId` numéricos, no códigos.
- La tabla de tratamientos muestra `specialistName`, porque el DTO no trae `specialistCode`.
- Al cambiar el estado de un caso también se invalida `['animal']`, porque la elegibilidad depende del estado del caso.

## Diferencias respecto a lo que esperaba el enunciado

- La descripción corta no produce `400`, porque el backend no valida su tamaño.
- La fecha futura produce `409` y no `400`, porque es una regla de negocio (`BusinessException`).