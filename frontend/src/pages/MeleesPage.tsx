import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import { useClubProfile } from '../club/clubApi'
import { settingsSummary } from '../melee/settingsSummary'
import { formatMeleeDate } from '../lib/dates'
import { useCreateMelee, useMelees } from '../melee/meleeApi'
import type { MeleeSettings, MeleeStatus, MeleeSummary } from '../melee/types'
import { Button } from '../ui/Button'
import { Dialog } from '../ui/Dialog'
import { ErrorMessage } from '../ui/ErrorMessage'
import { MoneyStepper } from '../ui/MoneyStepper'
import { NumberStepper } from '../ui/NumberStepper'
import { MAX_ENTRY_FEE_CENTS } from '../lib/money'
import { InstallCard } from '../pwa/InstallCard'

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
      <InstallCard />

      <CreateMeleeDialog open={creating} onClose={() => setCreating(false)} />
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
                <StatusPill status={melee.status} />
                <span className="w-full text-steel-600">
                  {t(`melee.teamSize.${melee.teamSize}`)} · {t('melees.players', { count: melee.activePlayers })}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

/** Setting up, being played (the ones to find quickly on the day) or closed. */
function StatusPill({ status }: { status: MeleeStatus }) {
  const { t } = useTranslation()
  const colours =
    status === 'CLOSED'
      ? 'bg-steel-200 text-steel-900'
      : status === 'REGISTRATION' || status === 'TEAMS'
        ? 'bg-gravel-100 text-steel-900'
        : 'bg-jack-700 text-white'
  return <span className={`rounded-full px-3 py-0.5 text-base font-bold ${colours}`}>{t(`melee.status.${status}`)}</span>
}

/**
 * "Crear melé clásica": what changes every time (doublettes or triplettes) at the top, the club's
 * usual settings as one line that can be changed for that day, and the button always in view.
 */
function CreateMeleeDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { data: club } = useClubProfile()
  const create = useCreateMelee()
  const [teamSize, setTeamSize] = useState<2 | 3>(2)
  const [settings, setSettings] = useState<MeleeSettings | null>(null)
  const [adjusting, setAdjusting] = useState(false)

  const values = settings ?? (club ? {
    courtCount: club.courtCount,
    roundsCount: club.roundsCount,
    prizeCount: club.prizeCount,
    entryFeeCents: club.entryFeeCents,
    matchMinutes: club.matchMinutes,
  } : null)
  const set = (field: keyof MeleeSettings) => (value: number) => values && setSettings({ ...values, [field]: value })

  function close() {
    setAdjusting(false)
    onClose()
  }

  function submit() {
    if (!values) return
    create.mutate({ teamSize, ...values }, { onSuccess: (view) => navigate(`/melees/${view.id}`) })
  }

  return (
    <Dialog
      open={open}
      onClose={close}
      title={t('melees.createClassic')}
      footer={
        <div className="flex flex-col gap-3">
          <ErrorMessage error={create.error} />
          <Button variant="accent" className="min-h-16 w-full text-xl" busy={create.isPending} disabled={!values} onClick={submit}>
            {t('melees.create')}
          </Button>
        </div>
      }
    >
      {values && (
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
          {adjusting ? (
            <>
              <p className="text-lg text-steel-600">{t('melees.settingsForToday')}</p>
              <NumberStepper label={t('club.profile.rounds')} value={values.roundsCount} min={1} max={20} onChange={set('roundsCount')} />
              <NumberStepper label={t('club.profile.prizes')} value={values.prizeCount} min={1} max={100} onChange={set('prizeCount')} />
              <NumberStepper label={t('club.profile.courts')} value={values.courtCount} min={1} max={200} onChange={set('courtCount')} />
              <NumberStepper label={t('club.profile.matchMinutes')} value={values.matchMinutes} min={5} max={180} onChange={set('matchMinutes')} />
              <MoneyStepper label={t('club.profile.fee')} value={values.entryFeeCents} max={MAX_ENTRY_FEE_CENTS} onChange={set('entryFeeCents')} help={t('club.profile.feeHelp')} />
            </>
          ) : (
            <div className="flex flex-col gap-3 rounded-2xl border-2 border-gravel-300 bg-white p-4">
              <p className="text-lg font-semibold">{t('melees.todaySettings')}</p>
              <p className="text-lg">{settingsSummary(t, values)}</p>
              <Button variant="secondary" onClick={() => setAdjusting(true)}>
                {t('melees.changeForToday')}
              </Button>
            </div>
          )}
        </div>
      )}
    </Dialog>
  )
}
