import type { ScoringTable } from '../club/clubApi'
import { enqueue } from '../offline/outbox'
import type { MeleeView, PlayerPosition, ThrowKind, ThrowOutcome, Turn } from '../melee/types'

export const POINTING_OUTCOMES: ThrowOutcome[] = ['ON_JACK', 'NEAR_JACK', 'SMALL_CIRCLE', 'BIG_CIRCLE', 'OUT']
export const SHOOTING_OUTCOMES: ThrowOutcome[] = ['CARREAU', 'HIT_OUT', 'HIT', 'MISS']

const POINTS_FIELD: Record<ThrowOutcome, keyof ScoringTable> = {
  OUT: 'pointingOut',
  BIG_CIRCLE: 'pointingBigCircle',
  SMALL_CIRCLE: 'pointingSmallCircle',
  NEAR_JACK: 'pointingNearJack',
  ON_JACK: 'pointingOnJack',
  MISS: 'shootingMiss',
  HIT: 'shootingHit',
  HIT_OUT: 'shootingHitOut',
  CARREAU: 'shootingCarreau',
}

export function pointsFor(scoring: ScoringTable, outcome: ThrowOutcome): number {
  return scoring[POINTS_FIELD[outcome]]
}

/** Same rule as the backend's ThrowSequence: who throws each ball. */
export function positionOf(teamSize: number, kind: ThrowKind, ballNumber: number): PlayerPosition {
  if (teamSize === 3) {
    if (kind === 'POINTING') return ballNumber === 3 ? 'MIDDLE' : 'POINTER'
    return ballNumber === 1 ? 'MIDDLE' : 'SHOOTER'
  }
  return kind === 'POINTING' ? 'POINTER' : 'SHOOTER'
}

export type LocalTurn = Turn | { awaitingServer: true; groupId: number } | null

/**
 * The next ball, worked out on the phone while some balls are still waiting to be sent (same order
 * as the backend's TurnOrder). Whether a finished round needs a tie-break is only decided by the
 * server: until it answers, we say so instead of guessing.
 */
export function localTurn(view: MeleeView): LocalTurn {
  const intl = view.international
  if (!intl) return null
  const sizeOf = (teamId: number) => view.teams.find((team) => team.id === teamId)?.memberIds.length ?? 2
  for (const group of [...intl.groups].sort((a, b) => a.playOrder - b.playOrder)) {
    for (const round of group.rounds) {
      if (round.obsolete) continue
      for (const kind of ['POINTING', 'SHOOTING'] as const) {
        for (const team of [...round.teams].sort((a, b) => a.playOrder - b.playOrder)) {
          for (let ballNumber = 1; ballNumber <= 3; ballNumber++) {
            if (!team.balls.some((ball) => ball.kind === kind && ball.ballNumber === ballNumber)) {
              return { groupId: group.id, roundId: round.id, teamId: team.teamId, kind, ballNumber, position: positionOf(sizeOf(team.teamId), kind, ballNumber) }
            }
          }
        }
      }
    }
    if (group.status !== 'FINISHED') return { awaitingServer: true, groupId: group.id }
  }
  return null
}

/** Recording and correcting a ball are the same idempotent PUT, sent through the offline queue. */
export function recordThrow(melee: MeleeView, roundId: number, teamId: number, kind: ThrowKind, ballNumber: number, outcome: ThrowOutcome) {
  enqueue({
    key: `throw:${roundId}:${teamId}:${kind}:${ballNumber}`,
    meleeId: melee.id,
    path: `/api/melees/${melee.id}/international/rounds/${roundId}/teams/${teamId}/throws/${kind}/${ballNumber}`,
    body: { outcome },
    overlay: { kind: 'throw', roundId, teamId, ballKind: kind, ballNumber, outcome },
  })
}
