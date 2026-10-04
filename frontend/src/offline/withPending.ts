import { pointsFor, positionOf } from '../international/intl'
import { recomputeStandings } from '../melee/standings'
import type { International, MeleeView, Payments, PaymentStatus, ThrowKind, ThrowOutcome } from '../melee/types'
import type { PendingAction } from './outbox'

/**
 * The melee as the admin expects to see it: the server's view plus the actions still in the queue.
 * Without this, a result or a ball tapped without signal would "disappear" every time the screen
 * refreshes.
 */
export function withPending(view: MeleeView, pending: PendingAction[]): MeleeView {
  const mine = pending.filter((action) => action.meleeId === view.id)
  if (mine.length === 0) return view
  return applyPayments(applyThrows(applyWinners(view, mine), mine), mine)
}

function applyPayments(view: MeleeView, actions: PendingAction[]): MeleeView {
  const statusOf = new Map<number, PaymentStatus>()
  for (const { overlay } of actions) {
    if (overlay.kind === 'payment') statusOf.set(overlay.participantId, overlay.paymentStatus)
  }
  if (statusOf.size === 0 || !view.payments) return view
  const participants = view.participants.map((participant) =>
    statusOf.has(participant.id) ? { ...participant, paymentStatus: statusOf.get(participant.id)! } : participant)
  return { ...view, participants, payments: paymentsOf(participants, view.payments.entryFeeCents) }
}

/** Same counts as the backend's PaymentSummary: people who did not come are not expected to pay. */
export function paymentsOf(participants: MeleeView['participants'], entryFeeCents: number): Payments {
  const active = participants.filter((participant) => participant.status === 'ACTIVE')
  const count = (status: PaymentStatus) => active.filter((participant) => participant.paymentStatus === status).length
  const paid = count('PAID')
  return {
    expected: active.length,
    paid,
    unpaid: count('UNPAID'),
    unmarked: count('UNMARKED'),
    entryFeeCents,
    collectedCents: paid * entryFeeCents,
  }
}

function applyWinners(view: MeleeView, actions: PendingAction[]): MeleeView {
  const winnerOf = new Map<number, number | null>()
  for (const { overlay } of actions) {
    if (overlay.kind === 'winner') winnerOf.set(overlay.matchId, overlay.winnerTeamId)
  }
  if (winnerOf.size === 0) return view
  const rounds = view.rounds.map((round) => ({
    ...round,
    matches: round.matches.map((match) => (winnerOf.has(match.id) ? { ...match, winnerTeamId: winnerOf.get(match.id)! } : match)),
  }))
  const updated = { ...view, rounds }
  return { ...updated, ...recomputeStandings(updated) }
}

function applyThrows(view: MeleeView, actions: PendingAction[]): MeleeView {
  if (!view.international) return view
  let international: International = view.international
  for (const { overlay } of actions) {
    if (overlay.kind !== 'throw') continue
    const outcome = overlay.outcome as ThrowOutcome
    const kind = overlay.ballKind as ThrowKind
    const teamSize = view.teams.find((team) => team.id === overlay.teamId)?.memberIds.length ?? 2
    const ball = {
      kind,
      ballNumber: overlay.ballNumber,
      position: positionOf(teamSize, kind, overlay.ballNumber),
      outcome,
      points: pointsFor(view.scoring, outcome),
      corrected: false,
    }
    international = {
      ...international,
      groups: international.groups.map((group) => ({
        ...group,
        rounds: group.rounds.map((round) => {
          if (round.id !== overlay.roundId) return round
          const teams = round.teams.map((team) => {
            if (team.teamId !== overlay.teamId) return team
            const balls = [...team.balls.filter((other) => other.kind !== kind || other.ballNumber !== overlay.ballNumber), ball]
            return { ...team, balls, points: balls.reduce((sum, each) => sum + each.points, 0) }
          })
          return { ...round, teams, complete: teams.every((team) => team.balls.length === 6) }
        }),
      })),
    }
  }
  return { ...view, international }
}
