import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router'
import { ErrorMessage } from '../ui/ErrorMessage'
import { SwipeTabs } from '../ui/SwipeTabs'
import { useOutbox } from '../offline/useOutbox'
import { withPending } from '../offline/withPending'
import { CounterBar } from './Counter'
import { CourtsTab } from './courts/CourtsTab'
import { MeleeHeader, MeleeOptions } from './MeleeHeader'
import { useMelee } from './meleeApi'
import { NextStepCard } from './NextStepCard'
import { PlayersTab } from './players/PlayersTab'
import { TeamsTab } from './teams/TeamsTab'
import type { MeleeStatus } from './types'

const TABS = ['jugadores', 'equipos', 'pistas'] as const
type TabId = (typeof TABS)[number]

/** The most useful tab for each phase, shown when nothing else was chosen. */
function defaultTab(status: MeleeStatus): TabId {
  if (status === 'REGISTRATION') return 'jugadores'
  if (status === 'TEAMS') return 'equipos'
  return 'pistas'
}

export function MeleePage() {
  const { t } = useTranslation()
  const meleeId = Number(useParams().meleeId)
  const { data, error } = useMelee(meleeId)
  const { pending } = useOutbox()
  const [searchParams, setSearchParams] = useSearchParams()

  if (error) return <ErrorMessage error={error} />
  if (!data) return null

  // Results tapped without signal are shown at once, before the server confirms them.
  const melee = withPending(data, pending)

  const requested = searchParams.get('tab') as TabId | null
  const active = requested && TABS.includes(requested) ? requested : defaultTab(melee.status)

  const showCounter = melee.counter.length > 0 && melee.status !== 'CLOSED'

  return (
    <div className={showCounter ? 'pb-28' : ''}>
      <MeleeHeader melee={melee} />
      <NextStepCard melee={melee} onShowTab={(tab) => setSearchParams({ tab }, { replace: true })} />
      <SwipeTabs
        label={t('melee.tabs.label')}
        active={active}
        onChange={(tab) => setSearchParams({ tab }, { replace: true })}
        tabs={[
          { id: 'jugadores', label: t('melee.tabs.players'), content: <PlayersTab melee={melee} /> },
          { id: 'equipos', label: t('melee.tabs.teams'), content: <TeamsTab melee={melee} /> },
          { id: 'pistas', label: t('melee.tabs.courts'), content: <CourtsTab melee={melee} /> },
        ]}
      />
      <MeleeOptions melee={melee} />
      {showCounter && <CounterBar melee={melee} />}
    </div>
  )
}
