import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ordinalBeforeNoun } from '../lib/ordinal'
import { Link, useNavigate, useParams } from 'react-router'
import { useMeleeLive } from '../live/useMeleeLive'
import { meleeKey, meleeRequests, useMelee, useMeleeAction } from '../melee/meleeApi'
import { teamNumber, teamPlayers } from '../melee/names'
import { Button } from '../ui/Button'
import { ConfirmDialog } from '../ui/ConfirmDialog'
import { ErrorMessage } from '../ui/ErrorMessage'
import { PrizePhotos } from './PrizePhotos'
import { SendPrizesPanel } from './whatsapp/SendPrizesPanel'
import { classificationText } from './share'

/**
 * "Entrega de premios": from the last prize to the first, each one in big letters with its team.
 * Showing a prize marks it as handed out, so spectators see it appear on their phones.
 */
export function PrizesPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const meleeId = Number(useParams().meleeId)
  const { data: melee, error } = useMelee(meleeId)
  const queryClient = useQueryClient()
  useMeleeLive(melee?.publicCode, () => void queryClient.invalidateQueries({ queryKey: meleeKey(meleeId) }))
  const [index, setIndex] = useState<number | null>(null)
  const [closing, setClosing] = useState(false)
  const [copied, setCopied] = useState(false)
  const award = useMeleeAction(meleeId, (prizeId: number) => meleeRequests.markAwarded(meleeId, prizeId))
  const close = useMeleeAction(meleeId, () => meleeRequests.close(meleeId), () => navigate(`/melees/${meleeId}`))
  const awarding = useRef(new Set<number>())

  // The ceremony starts at the last prize (the highest position).
  const ordered = melee ? [...melee.prizes].sort((a, b) => b.position - a.position) : []
  const current = index ?? 0
  const prize = ordered[current]
  const editable = melee?.status === 'PRIZES'

  useEffect(() => {
    if (prize && editable && !prize.awarded && !awarding.current.has(prize.id)) {
      awarding.current.add(prize.id)
      award.mutate(prize.id)
    }
  }, [prize, editable, award])

  if (error) return <ErrorMessage error={error} />
  if (!melee) return null
  if (!prize) return <p className="text-lg">{t('prizes.none')}</p>

  async function copyClassification() {
    await navigator.clipboard.writeText(classificationText(melee!))
    setCopied(true)
  }

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col items-start gap-1">
        <Link to={`/melees/${melee.id}`} className="text-base font-semibold underline underline-offset-4">
          ◀ {t('intl.backToMelee')}
        </Link>
        <h1 className="text-2xl font-extrabold">{t('prizes.title')}</h1>
      </div>

      <section key={prize.id} className="arrima-reveal rounded-3xl bg-steel-800 p-6 text-center text-gravel-50">
        <p className="font-extrabold text-jack-500">
          <span className="block text-7xl leading-none">{t('prizes.ordinal', { ordinal: ordinalBeforeNoun(prize.position) })}</span>
          <span className="block text-3xl">{t('prizes.prize')}</span>
        </p>
        <p className="mt-3 text-2xl font-bold">{t('teams.teamNumber', { number: teamNumber(melee, prize.teamId) })}</p>
        <p className="text-3xl font-extrabold">{teamPlayers(melee, prize.teamId)}</p>
        {prize.points !== null && <p className="mt-2 text-xl">{t('intl.points', { count: prize.points })}</p>}
      </section>

      <PrizePhotos melee={melee} prize={prize} editable={editable} />

      <div className="grid grid-cols-2 gap-3">
        <Button variant="secondary" disabled={current === 0} onClick={() => setIndex(current - 1)}>
          ◀ {t('prizes.previous')}
        </Button>
        <Button disabled={current === ordered.length - 1} onClick={() => setIndex(current + 1)}>
          {t('prizes.next')} ▶
        </Button>
      </div>

      <div className="flex flex-col gap-3 border-t-2 border-gravel-300 pt-4">
        <SendPrizesPanel melee={melee} />
        <Button variant="secondary" onClick={() => void copyClassification()}>
          {copied ? t('prizes.copied') : t('prizes.copyClassification')}
        </Button>
        {editable && (
          <Button variant="danger" onClick={() => setClosing(true)}>
            {t('prizes.close')}
          </Button>
        )}
        {editable && <p className="text-base text-steel-600">{t('prizes.autoCloseHelp')}</p>}
      </div>

      <ConfirmDialog
        open={closing}
        title={t('prizes.closeTitle')}
        confirmLabel={t('prizes.closeConfirm')}
        busy={close.isPending}
        error={close.error}
        onCancel={() => setClosing(false)}
        onConfirm={() => close.mutate(undefined)}
      >
        <p>{t('prizes.closeExplanation')}</p>
      </ConfirmDialog>
    </div>
  )
}
