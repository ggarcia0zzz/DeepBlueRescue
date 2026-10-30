import { useAnimal } from '../hooks/useAnimal'
import ChangeStatus from './ChangeStatus'
import ErrorMessage from './ErrorMessage'
import TreatmentForm from './TreatmentForm'
import TreatmentList from './TreatmentList'

export default function CaseDetail({ caseItem, onChanged }) {
  // ADAPTADO: el caso trae el animal anidado (caseItem.animal)
  const caseAnimal = caseItem.animal
  const { animal, treatments, eligibility } = useAnimal(caseAnimal)
  const a = animal.data

  return (
    <div>
      <section>
        <h2>Caso {caseItem.caseCode}</h2>
        <p>
          <strong>Estado:</strong> {caseItem.status} <br />
          <strong>Centro:</strong> {caseItem.centerCode} — {caseItem.centerName} <br />
          <strong>Lugar:</strong> {caseItem.rescueLocation} <br />
          <strong>Fecha:</strong> {caseItem.rescueDate}
        </p>
      </section>

      <ChangeStatus
        key={caseItem.caseCode}
        caseItem={caseItem}
        onChanged={onChanged}
      />

      <section>
        <h3>Animal {caseAnimal?.animalCode ?? '(sin animal)'}</h3>
        {animal.isPending && caseAnimal && <p>Cargando...</p>}
        {animal.isError && <ErrorMessage error={animal.error} />}

        {a && (
          <ul>
            <li><strong>Nombre común:</strong> {a.commonName}</li>
            <li><strong>Nombre científico:</strong> {a.scientificName}</li>
            <li><strong>Sexo:</strong> {a.sex}</li>
            <li><strong>Dispositivo:</strong> {a.trackingDeviceCode ?? '—'}</li>
            {a.medicalRecord && (
              <>
                <li><strong>Peso inicial:</strong> {String(a.medicalRecord.initialWeight ?? '—')}</li>
                <li><strong>Condición inicial:</strong> {a.medicalRecord.initialCondition ?? '—'}</li>
                <li><strong>Lesiones:</strong> {a.medicalRecord.injuries ?? '—'}</li>
                <li><strong>Observaciones:</strong> {a.medicalRecord.observations ?? '—'}</li>
              </>
            )}
          </ul>
        )}

        {eligibility.isError && <ErrorMessage error={eligibility.error} />}
        {eligibility.data && (
          <p>
            Puede recibir tratamiento:{' '}
            <span className={eligibility.data.eligible ? 'ok' : 'bad'}>
              {eligibility.data.eligible ? 'Sí' : 'No'}
            </span>
          </p>
        )}
      </section>

      <TreatmentList query={treatments} />

      {caseAnimal && (
        <TreatmentForm key={caseAnimal.id} animalId={caseAnimal.id} />
      )}
    </div>
  )
}
