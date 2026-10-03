import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { meleeRequests, useMeleeAction } from '../meleeApi'
import type { MeleeView, Participant, ParticipantData } from '../types'
import { Button } from '../../ui/Button'
import { Dialog } from '../../ui/Dialog'
import { ErrorMessage } from '../../ui/ErrorMessage'
import { TextField } from '../../ui/TextField'
import { useErrorText } from '../../ui/useErrorText'

interface ParticipantDialogProps {
  melee: MeleeView
  /** null: adding someone new. */
  participant: Participant | null
  open: boolean
  onClose: () => void
}

export function ParticipantDialog({ melee, participant, open, onClose }: ParticipantDialogProps) {
  const { t } = useTranslation()
  return (
    <Dialog open={open} onClose={onClose} title={participant ? t('players.edit.title') : t('players.add.title')}>
      <ParticipantForm key={participant?.id ?? 'new'} melee={melee} participant={participant} onDone={onClose} />
    </Dialog>
  )
}

function ParticipantForm({ melee, participant, onDone }: { melee: MeleeView; participant: Participant | null; onDone: () => void }) {
  const { t } = useTranslation()
  const errors = useErrorText()
  const [name, setName] = useState(participant?.name ?? '')
  const [number, setNumber] = useState(participant?.listNumber?.toString() ?? '')

  const save = useMeleeAction(melee.id, (data: ParticipantData) =>
    participant ? meleeRequests.editParticipant(melee.id, participant.id, data) : meleeRequests.addParticipant(melee.id, data))
  const toggle = useMeleeAction(melee.id, () =>
    participant!.status === 'ACTIVE' ? meleeRequests.withdraw(melee.id, participant!.id) : meleeRequests.reinstate(melee.id, participant!.id))
  const remove = useMeleeAction(melee.id, () => meleeRequests.deleteParticipant(melee.id, participant!.id))

  const canChangeList = ['REGISTRATION', 'TEAMS', 'MATCHES'].includes(melee.status)

  function submit() {
    save.mutate({ name, listNumber: number.trim() === '' ? null : Number(number) }, { onSuccess: onDone })
  }

  return (
    <div className="flex flex-col gap-4">
      <TextField label={t('players.edit.name')} value={name} onChange={(event) => setName(event.target.value)} error={errors.field(save.error, 'name')} />
      <TextField
        label={t('players.edit.number')}
        help={t('players.edit.numberHelp')}
        inputMode="numeric"
        value={number}
        onChange={(event) => setNumber(event.target.value.replace(/\D/g, ''))}
        error={errors.field(save.error, 'listNumber')}
      />
      <ErrorMessage error={save.error ?? toggle.error ?? remove.error} />
      <Button busy={save.isPending} onClick={submit}>
        {t('common.save')}
      </Button>

      {participant && canChangeList && (
        <div className="mt-2 flex flex-col gap-3 border-t-2 border-gravel-300 pt-4">
          <Button
            variant={participant.status === 'ACTIVE' ? 'danger' : 'secondary'}
            busy={toggle.isPending}
            onClick={() => toggle.mutate(undefined, { onSuccess: onDone })}
          >
            {participant.status === 'ACTIVE' ? t('players.edit.withdraw') : t('players.edit.reinstate')}
          </Button>
          <p className="text-base text-steel-600">{t('players.edit.withdrawHelp')}</p>
          {melee.status === 'REGISTRATION' && (
            <Button variant="ghost" busy={remove.isPending} onClick={() => remove.mutate(undefined, { onSuccess: onDone })}>
              {t('players.edit.delete')}
            </Button>
          )}
        </div>
      )}
    </div>
  )
}
