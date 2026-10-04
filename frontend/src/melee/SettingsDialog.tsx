import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '../ui/Button'
import { Dialog } from '../ui/Dialog'
import { ErrorMessage } from '../ui/ErrorMessage'
import { NumberStepper } from '../ui/NumberStepper'
import { meleeRequests, useMeleeAction } from './meleeApi'
import type { MeleeView } from './types'

/** Rounds, prizes and courts of this melee only; possible until the court schedule exists. */
export function SettingsDialog({ melee, onClose }: { melee: MeleeView; onClose: () => void }) {
  const { t } = useTranslation()
  const [settings, setSettings] = useState(melee.settings)
  const save = useMeleeAction(melee.id, () => meleeRequests.changeSettings(melee.id, settings))
  const set = (field: keyof typeof settings) => (value: number) => setSettings({ ...settings, [field]: value })

  return (
    <Dialog open onClose={onClose} title={t('settings.title')}>
      <div className="flex flex-col gap-5">
        <NumberStepper label={t('club.profile.rounds')} value={settings.roundsCount} min={1} max={20} onChange={set('roundsCount')} />
        {melee.teams.length > 1 && settings.roundsCount > melee.maxRounds && (
          <p className="text-lg font-semibold text-red-800">{t('settings.tooManyRounds', { teams: melee.teams.length, max: melee.maxRounds })}</p>
        )}
        <NumberStepper label={t('club.profile.prizes')} value={settings.prizeCount} min={1} max={100} onChange={set('prizeCount')} />
        <NumberStepper label={t('club.profile.courts')} value={settings.courtCount} min={1} max={200} onChange={set('courtCount')} />
        <ErrorMessage error={save.error} />
        <Button busy={save.isPending} onClick={() => save.mutate(undefined, { onSuccess: onClose })}>
          {t('common.save')}
        </Button>
      </div>
    </Dialog>
  )
}
