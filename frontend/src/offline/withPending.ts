import { recomputeStandings } from '../melee/standings'
import type { MeleeView } from '../melee/types'
import type { PendingAction } from './outbox'

/**
 * The melee as the admin expects to see it: the server's view plus the actions still in the queue.
 * Without this, a result tapped without signal would "disappear" every time the screen refreshes.
 */
export function withPending(view: MeleeView, pending: PendingAction[]): MeleeView {
  const mine = pending.filter((action) => action.meleeId === view.id && action.overlay.kind === 'winner')
  if (mine.length === 0) return view

  const winnerOf = new Map<number, number | null>()
  for (const action of mine) {
    if (action.overlay.kind === 'winner') winnerOf.set(action.overlay.matchId, action.overlay.winnerTeamId)
  }
  const rounds = view.rounds.map((round) => ({
    ...round,
    matches: round.matches.map((match) => (winnerOf.has(match.id) ? { ...match, winnerTeamId: winnerOf.get(match.id)! } : match)),
  }))
  const updated = { ...view, rounds }
  return { ...updated, ...recomputeStandings(updated) }
}
