import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router'
import { ApiError } from '../api/ApiError'
import { api } from '../api/client'
import { formatMeleeDate } from '../lib/dates'
import { useMeleeLive, type LiveState } from '../live/useMeleeLive'
import { CounterBar } from '../melee/Counter'
import { CourtsTab } from '../melee/courts/CourtsTab'
import { PlayersTab } from '../melee/players/PlayersTab'
import { TeamsTab } from '../melee/teams/TeamsTab'
import type { MeleeView } from '../melee/types'
import { ErrorMessage } from '../ui/ErrorMessage'
import { SwipeTabs } from '../ui/SwipeTabs'
import { MyTeamCard } from './MyTeamCard'

/** What players see with the code or the QR: everything, live, read-only, no account. */
export function PublicMeleePage() {
  const { t } = useTranslation()
  const code = useParams().code ?? ''
  const queryClient = useQueryClient()
  const queryKey = ['public-melee', code]
  const { data: melee, error } = useQuery({
    queryKey,
    queryFn: () => api<MeleeView>(`/api/public/melees/${encodeURIComponent(code)}`, { authenticated: false }),
  })
  const live = useMeleeLive(melee?.publicCode, () => void queryClient.invalidateQueries({ queryKey }))
  const [tab, setTab] = useState<string | null>(null)

  if (error) {
    return (
      <main className="mx-auto max-w-md px-4 py-10 text-center">
        <p className="text-3xl font-extrabold">{t('app.name')}</p>
        <div className="mt-6">
          {error instanceof ApiError && error.status === 404 ? (
            <p className="text-xl">{t('public.notFound', { code })}</p>
          ) : (
            <ErrorMessage error={error} />
          )}
        </div>
      </main>
    )
  }
  if (!melee) return null

  const active = tab ?? (melee.status === 'REGISTRATION' ? 'jugadores' : melee.status === 'TEAMS' ? 'equipos' : 'pistas')
  const showCounter = melee.counter.length > 0 && melee.status !== 'CLOSED'

  return (
    <div className={`min-h-dvh ${showCounter ? 'pb-28' : ''}`}>
      <header className="bg-steel-800 text-gravel-50">
        <div className="mx-auto flex max-w-4xl items-center gap-3 px-4 py-3">
          {melee.club.logoUrl ? (
            <img src={melee.club.logoUrl} alt="" className="size-12 shrink-0 rounded-full bg-white object-contain" />
          ) : (
            <span aria-hidden="true" className="size-12 shrink-0 rounded-full bg-[radial-gradient(circle_at_35%_30%,#f4f6f7,#9aa5ad_45%,#2c3439)]" />
          )}
          <div className="min-w-0 flex-1">
            <p className="truncate text-xl font-bold">{melee.club.name}</p>
            <p className="text-base first-letter:uppercase">{formatMeleeDate(melee.playedOn)}</p>
          </div>
          <LiveIndicator state={live} />
        </div>
      </header>
      <main className="mx-auto max-w-4xl px-4 py-5">
        <p className="mb-4 text-xl font-bold">{t(`public.status.${melee.status}`)}</p>
        <MyTeamCard melee={melee} />
        <SwipeTabs
          label={t('melee.tabs.label')}
          active={active}
          onChange={setTab}
          tabs={[
            { id: 'jugadores', label: t('melee.tabs.players'), content: <PlayersTab melee={melee} readOnly /> },
            { id: 'equipos', label: t('melee.tabs.teams'), content: <TeamsTab melee={melee} readOnly /> },
            { id: 'pistas', label: t('melee.tabs.courts'), content: <CourtsTab melee={melee} readOnly /> },
          ]}
        />
      </main>
      {showCounter && <CounterBar melee={melee} />}
    </div>
  )
}

function LiveIndicator({ state }: { state: LiveState }) {
  const { t } = useTranslation()
  return (
    <span className="flex shrink-0 items-center gap-2 text-base font-semibold" role="status">
      <span aria-hidden="true" className={`size-3 rounded-full ${state === 'live' ? 'bg-green-400' : 'bg-amber-400'}`} />
      {t(`public.live.${state}`)}
    </span>
  )
}
