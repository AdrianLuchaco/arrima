import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router'
import { ErrorMessage } from '../ui/ErrorMessage'
import { SwipeTabs } from '../ui/SwipeTabs'
import { MeleeHeader } from './MeleeHeader'
import { useMelee } from './meleeApi'
import { PlayersTab } from './players/PlayersTab'
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
  const { data: melee, error } = useMelee(meleeId)
  const [searchParams, setSearchParams] = useSearchParams()

  if (error) return <ErrorMessage error={error} />
  if (!melee) return null

  const requested = searchParams.get('tab') as TabId | null
  const active = requested && TABS.includes(requested) ? requested : defaultTab(melee.status)

  return (
    <div>
      <MeleeHeader melee={melee} />
      <SwipeTabs
        label={t('melee.tabs.label')}
        active={active}
        onChange={(tab) => setSearchParams({ tab }, { replace: true })}
        tabs={[
          { id: 'jugadores', label: t('melee.tabs.players'), content: <PlayersTab melee={melee} /> },
          { id: 'equipos', label: t('melee.tabs.teams'), content: <p className="text-lg text-steel-600">{t('teams.none')}</p> },
          { id: 'pistas', label: t('melee.tabs.courts'), content: <p className="text-lg text-steel-600">{t('courts.none')}</p> },
        ]}
      />
    </div>
  )
}
