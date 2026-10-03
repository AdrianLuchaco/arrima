import { useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import {
  useClubProfile,
  useRemoveLogo,
  useUpdateClubProfile,
  useUploadLogo,
  type ClubProfileUpdate,
  type ScoringTable,
} from '../club/clubApi'
import { compressImage } from '../lib/images'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { ErrorMessage } from '../ui/ErrorMessage'
import { useErrorText } from '../ui/useErrorText'
import { NumberStepper } from '../ui/NumberStepper'
import { TextField } from '../ui/TextField'

const POINTING_ROWS: (keyof ScoringTable)[] = ['pointingOut', 'pointingBigCircle', 'pointingSmallCircle', 'pointingNearJack', 'pointingOnJack']
const SHOOTING_ROWS: (keyof ScoringTable)[] = ['shootingMiss', 'shootingHit', 'shootingHitOut', 'shootingCarreau']

export function ClubProfilePage() {
  const { t } = useTranslation()
  const { data: profile, error: loadError } = useClubProfile()

  if (loadError) return <ErrorMessage error={loadError} />
  if (!profile) return null

  const { logoUrl, ...editable } = profile
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-3xl font-extrabold">{t('club.profile.title')}</h1>
      <LogoCard logoUrl={logoUrl} />
      <ProfileForm initial={editable} />
    </div>
  )
}

/** Edits a copy of the profile; it is saved only with the button. */
function ProfileForm({ initial }: { initial: ClubProfileUpdate }) {
  const { t } = useTranslation()
  const errors = useErrorText()
  const update = useUpdateClubProfile()
  const [form, setForm] = useState(initial)

  const set = <K extends keyof ClubProfileUpdate>(field: K, value: ClubProfileUpdate[K]) => setForm({ ...form, [field]: value })
  const setPoints = (field: keyof ScoringTable, value: number) => set('scoring', { ...form.scoring, [field]: value })

  function submit(event: FormEvent) {
    event.preventDefault()
    update.mutate(form)
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-6" noValidate>
      <Card>
        <TextField
          label={t('club.profile.name')}
          value={form.name}
          onChange={(event) => set('name', event.target.value)}
          error={errors.field(update.error, 'name')}
        />
      </Card>

      <Card title={t('club.profile.meleeDefaults')}>
        <p className="mb-4 text-lg text-steel-600">{t('club.profile.meleeDefaultsHelp')}</p>
        <div className="grid gap-4 sm:grid-cols-3">
          <NumberStepper label={t('club.profile.courts')} value={form.courtCount} min={1} max={200} onChange={(value) => set('courtCount', value)} />
          <NumberStepper label={t('club.profile.rounds')} value={form.roundsCount} min={1} max={20} onChange={(value) => set('roundsCount', value)} />
          <NumberStepper label={t('club.profile.prizes')} value={form.prizeCount} min={1} max={100} onChange={(value) => set('prizeCount', value)} />
        </div>
      </Card>

      <Card title={t('club.profile.scoring')}>
        <p className="mb-4 text-lg text-steel-600">{t('club.profile.scoringHelp')}</p>
        <PointsTable title={t('club.profile.pointing')} rows={POINTING_ROWS} scoring={form.scoring} onChange={setPoints} />
        <PointsTable title={t('club.profile.shooting')} rows={SHOOTING_ROWS} scoring={form.scoring} onChange={setPoints} />
      </Card>

      <ErrorMessage error={update.error} />
      {update.isSuccess && !update.isPending && (
        <p role="status" className="text-lg font-semibold text-green-800">
          {t('club.profile.saved')}
        </p>
      )}
      <Button type="submit" busy={update.isPending} className="self-start">
        {t('club.profile.save')}
      </Button>
    </form>
  )
}

function PointsTable({ title, rows, scoring, onChange }: {
  title: string
  rows: (keyof ScoringTable)[]
  scoring: ScoringTable
  onChange: (field: keyof ScoringTable, value: number) => void
}) {
  const { t } = useTranslation()
  return (
    <fieldset className="mb-6 last:mb-0">
      <legend className="mb-3 text-xl font-bold">{title}</legend>
      <div className="grid gap-5 sm:grid-cols-2">
        {rows.map((field) => (
          <NumberStepper
            key={field}
            label={t(`club.profile.points.${field}`)}
            value={scoring[field]}
            min={0}
            max={99}
            onChange={(value) => onChange(field, value)}
          />
        ))}
      </div>
    </fieldset>
  )
}

function LogoCard({ logoUrl }: { logoUrl: string | null }) {
  const { t } = useTranslation()
  const upload = useUploadLogo()
  const remove = useRemoveLogo()
  const input = useRef<HTMLInputElement>(null)
  const [preparing, setPreparing] = useState(false)

  async function choose(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setPreparing(true)
    try {
      // Logos keep transparency as PNG; a 512 px side is plenty for the header.
      upload.mutate(await compressImage(file, { maxSide: 512, type: 'image/png' }))
    } catch {
      upload.reset()
    } finally {
      setPreparing(false)
    }
  }

  return (
    <Card title={t('club.profile.logo')}>
      <div className="flex flex-wrap items-center gap-4">
        {logoUrl ? (
          <img src={logoUrl} alt={t('club.profile.logo')} className="size-24 rounded-2xl border-2 border-gravel-300 bg-white object-contain" />
        ) : (
          <div className="flex size-24 items-center justify-center rounded-2xl border-2 border-dashed border-steel-400 text-steel-600">
            {t('club.profile.noLogo')}
          </div>
        )}
        <div className="flex flex-wrap gap-3">
          <input ref={input} type="file" accept="image/*" className="sr-only" onChange={choose} />
          <Button variant="secondary" busy={preparing || upload.isPending} onClick={() => input.current?.click()}>
            {logoUrl ? t('club.profile.changeLogo') : t('club.profile.addLogo')}
          </Button>
          {logoUrl && (
            <Button variant="danger" busy={remove.isPending} onClick={() => remove.mutate()}>
              {t('club.profile.removeLogo')}
            </Button>
          )}
        </div>
      </div>
      <div className="mt-3">
        <ErrorMessage error={upload.error ?? remove.error} />
      </div>
    </Card>
  )
}
