import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router'
import { ErrorMessage } from '../ui/ErrorMessage'
import { SwipeTabs } from '../ui/SwipeTabs'
import { useMeleeLive } from '../live/useMeleeLive'
import { useOutbox } from '../offline/useOutbox'
import { Button } from '../ui/Button'
import { withPending } from '../offline/withPending'
import { CounterFigures } from './Counter'
import { collectingPayments } from './payments/payments'
import { PaymentSummary } from './payments/PaymentSummary'
import { CourtsTab } from './courts/CourtsTab'
import { IntlBoard } from '../international/IntlBoard'
import { PrizesTab } from '../prizes/PrizesTab'
import { MeleeHeader, MeleeOptions } from './MeleeHeader'
import { meleeKey, meleeRequests, useMelee } from './meleeApi'
import { NextStepCard } from './NextStepCard'
import { PlayersTab } from './players/PlayersTab'
import { ShareDialog } from './ShareDialog'
import { TeamsTab } from './teams/TeamsTab'
import type { MeleeStatus, MeleeView } from './types'
import { BottomBar } from '../ui/BottomBar'
import { showsTimer } from '../timer/countdown'
import { TimerStrip } from '../timer/TimerStrip'
import { useMatchTimer } from '../timer/useMatchTimer'
import { useWakeLock } from '../timer/useWakeLock'

const TABS = ['jugadores', 'equipos', 'pistas', 'internacional', 'premios'] as const
type TabId = (typeof TABS)[number]

/** The most useful tab for each phase, shown when nothing else was chosen. */
function defaultTab(status: MeleeStatus): TabId {
  if (status === 'REGISTRATION') return 'jugadores'
  if (status === 'TEAMS') return 'equipos'
  if (status === 'MATCHES') return 'pistas'
  if (status === 'INTERNATIONAL') return 'internacional'
  return 'premios'
}

export function MeleePage() {
  const meleeId = Number(useParams().meleeId)
  const { data, error } = useMelee(meleeId)
  const { pending } = useOutbox()
  const queryClient = useQueryClient()
  // Another phone of the club may change the melee too: refresh when the server says so.
  useMeleeLive(data?.publicCode, () => void queryClient.invalidateQueries({ queryKey: meleeKey(meleeId) }))

  if (error) return <ErrorMessage error={error} />
  if (!data) return null
  // Results tapped without signal are shown at once, before the server confirms them.
  return <MeleeScreen melee={withPending(data, pending)} />
}

function MeleeScreen({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const [searchParams, setSearchParams] = useSearchParams()
  const [sharing, setSharing] = useState(false)
  const queryClient = useQueryClient()
  // When this phone's countdown reaches 0 it also tells the server: one more trigger for the
  // notifications (the server decides with its own clock).
  const { countdown, overlay } = useMatchTimer(melee, () => {
    meleeRequests.checkTimer(melee.id).then((view) => queryClient.setQueryData(meleeKey(melee.id), view)).catch(() => undefined)
  })
  // The admin's phone is the official alarm: its screen stays on while a countdown runs.
  useWakeLock(countdown?.timer.state === 'RUNNING')

  const requested = searchParams.get('tab') as TabId | null
  const hasInternational = melee.international !== null
  const hasPrizes = melee.prizes.length > 0
  const fallback = defaultTab(melee.status)
  const available = (tab: TabId) => (tab === 'internacional' ? hasInternational : tab === 'premios' ? hasPrizes : true)
  const active = requested && TABS.includes(requested) && available(requested) ? requested : available(fallback) ? fallback : 'pistas'

  // Only while the matches are being played: afterwards the figures no longer change.
  const showCounter = melee.counter.length > 0 && melee.status === 'MATCHES'
  const showCountdown = melee.status === 'MATCHES' && showsTimer(countdown)
  // While collecting at the table, the payment summary takes the place of the counter.
  const showPayments = collectingPayments(melee)
  const bottomSpace = showCounter && showCountdown ? 'pb-48' : showCounter || showCountdown || showPayments ? 'pb-28' : ''

  return (
    <div className={bottomSpace}>
      <MeleeHeader melee={melee} />
      <Button variant="secondary" className="mb-4 w-full" onClick={() => setSharing(true)}>
        {t('share.button')}
      </Button>
      {sharing && <ShareDialog melee={melee} onClose={() => setSharing(false)} />}
      <NextStepCard melee={melee} onShowTab={(tab) => setSearchParams({ tab }, { replace: true })} />
      <SwipeTabs
        label={t('melee.tabs.label')}
        active={active}
        onChange={(tab) => setSearchParams({ tab }, { replace: true })}
        tabs={[
          { id: 'jugadores', label: t('melee.tabs.players'), content: <PlayersTab melee={melee} /> },
          { id: 'equipos', label: t('melee.tabs.teams'), content: <TeamsTab melee={melee} /> },
          { id: 'pistas', label: t('melee.tabs.courts'), content: <CourtsTab melee={melee} /> },
          ...(hasInternational
            ? [{ id: 'internacional', label: t('melee.tabs.international'), content: <IntlBoard melee={melee} /> }]
            : []),
          ...(hasPrizes ? [{ id: 'premios', label: t('melee.tabs.prizes'), content: <PrizesTab melee={melee} /> }] : []),
        ]}
      />
      <MeleeOptions melee={melee} />
      {(showCounter || showCountdown) && (
        <BottomBar>
          {showCountdown && <TimerStrip countdown={countdown} />}
          {showCounter && <CounterFigures melee={melee} />}
        </BottomBar>
      )}
      {showPayments && melee.payments && <PaymentSummary payments={melee.payments} fixed />}
      {overlay}
    </div>
  )
}
