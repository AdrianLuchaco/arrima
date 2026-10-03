import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import { useClubProfile } from '../club/clubApi'
import { formatMeleeDate } from '../lib/dates'
import { useCreateMelee, useMelees } from '../melee/meleeApi'
import type { MeleeSummary } from '../melee/types'
import { Button } from '../ui/Button'
import { Dialog } from '../ui/Dialog'
import { ErrorMessage } from '../ui/ErrorMessage'
import { NumberStepper } from '../ui/NumberStepper'

export function MeleesPage() {
  const { t } = useTranslation()
  const { data: melees, error } = useMelees()
  const [creating, setCreating] = useState(false)

  const current = melees?.filter((melee) => melee.status !== 'CLOSED') ?? []
  const history = melees?.filter((melee) => melee.status === 'CLOSED') ?? []

  return (
    <div className="flex flex-col gap-8">
      <Button variant="accent" className="min-h-20 text-2xl" onClick={() => setCreating(true)}>
        {t('melees.createClassic')}
      </Button>
      <ErrorMessage error={error} />

      <MeleeList title={t('melees.current')} melees={current} empty={t('melees.noCurrent')} />
      <MeleeList title={t('melees.history')} melees={history} empty={t('melees.noHistory')} />

      <Dialog open={creating} onClose={() => setCreating(false)} title={t('melees.createClassic')}>
        <CreateMeleeForm />
      </Dialog>
    </div>
  )
}

function MeleeList({ title, melees, empty }: { title: string; melees: MeleeSummary[]; empty: string }) {
  const { t } = useTranslation()
  return (
    <section>
      <h2 className="mb-3 text-2xl font-bold">{title}</h2>
      {melees.length === 0 ? (
        <p className="text-lg text-steel-600">{empty}</p>
      ) : (
        <ul className="flex flex-col gap-3">
          {melees.map((melee) => (
            <li key={melee.id}>
              <Link
                to={`/melees/${melee.id}`}
                className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border-2 border-gravel-300 bg-white px-4 py-4 text-lg hover:border-steel-600"
              >
                <span className="font-bold first-letter:uppercase">{formatMeleeDate(melee.playedOn)}</span>
                <span className="text-steel-600">
                  {t(`melee.teamSize.${melee.teamSize}`)} · {t('melees.players', { count: melee.activePlayers })} ·{' '}
                  <strong className="text-steel-900">{t(`melee.status.${melee.status}`)}</strong>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function CreateMeleeForm() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { data: club } = useClubProfile()
  const create = useCreateMelee()
  const [teamSize, setTeamSize] = useState<2 | 3>(2)
  const [settings, setSettings] = useState<{ courtCount: number; roundsCount: number; prizeCount: number } | null>(null)

  if (!club) return null
  const values = settings ?? { courtCount: club.courtCount, roundsCount: club.roundsCount, prizeCount: club.prizeCount }
  const set = (field: keyof typeof values) => (value: number) => setSettings({ ...values, [field]: value })

  function submit() {
    create.mutate({ teamSize, ...values }, { onSuccess: (view) => navigate(`/melees/${view.id}`) })
  }

  return (
    <div className="flex flex-col gap-5">
      <fieldset>
        <legend className="mb-2 text-lg font-semibold">{t('melees.teamSizeQuestion')}</legend>
        <div className="grid grid-cols-2 gap-3">
          {([2, 3] as const).map((size) => (
            <button
              key={size}
              type="button"
              aria-pressed={teamSize === size}
              onClick={() => setTeamSize(size)}
              className={`min-h-16 rounded-2xl border-4 text-xl font-bold ${teamSize === size ? 'border-steel-800 bg-steel-800 text-white' : 'border-steel-400 bg-white'}`}
            >
              {t(`melee.teamSize.${size}`)}
            </button>
          ))}
        </div>
      </fieldset>
      <p className="text-lg text-steel-600">{t('melees.settingsForToday')}</p>
      <NumberStepper label={t('club.profile.rounds')} value={values.roundsCount} min={1} max={20} onChange={set('roundsCount')} />
      <NumberStepper label={t('club.profile.prizes')} value={values.prizeCount} min={1} max={100} onChange={set('prizeCount')} />
      <NumberStepper label={t('club.profile.courts')} value={values.courtCount} min={1} max={200} onChange={set('courtCount')} />
      <ErrorMessage error={create.error} />
      <Button variant="accent" busy={create.isPending} onClick={submit}>
        {t('melees.create')}
      </Button>
    </div>
  )
}
