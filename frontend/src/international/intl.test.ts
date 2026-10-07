import { describe, expect, it } from 'vitest'
import type { MeleeView } from '../melee/types'
import type { PendingAction } from '../offline/outbox'
import { withPending } from '../offline/withPending'
import { localTurn, positionOf, tieBreakPoints } from './intl'

const scoring = {
  pointingOut: 0, pointingBigCircle: 1, pointingSmallCircle: 2, pointingNearJack: 3, pointingOnJack: 5,
  shootingMiss: 0, shootingHit: 1, shootingHitOut: 2, shootingCarreau: 5,
}

/** One group with teams 10 (doublette) and 20 (triplette) in round 100. */
function melee(status: 'PENDING' | 'IN_PROGRESS' | 'FINISHED' = 'IN_PROGRESS'): MeleeView {
  return {
    id: 1,
    scoring,
    teams: [{ id: 10, memberIds: [1, 2] }, { id: 20, memberIds: [3, 4, 5] }],
    international: {
      groups: [{
        id: 7, playOrder: 1, wins: 2, bestPosition: 1, worstPosition: 2, status, order: [10, 20], obsoletePlayed: false,
        rounds: [{ id: 100, number: 1, obsolete: false, complete: false, teams: [
          { teamId: 10, playOrder: 1, points: 0, balls: [] },
          { teamId: 20, playOrder: 2, points: 0, balls: [] },
        ] }],
      }],
      assured: [], turn: null, finalRanking: [], complete: false,
    },
  } as unknown as MeleeView
}

const throwAction = (teamId: number, ballKind: 'POINTING' | 'SHOOTING', ballNumber: number, outcome: string): PendingAction => ({
  key: `throw:100:${teamId}:${ballKind}:${ballNumber}`, meleeId: 1, path: '', body: {}, createdAt: 0,
  overlay: { kind: 'throw', roundId: 100, teamId, ballKind, ballNumber, outcome },
})

describe('positionOf', () => {
  it('follows the club rule for triplettes: punta, punta, medio / medio, tirador, tirador', () => {
    expect([1, 2, 3].map((ball) => positionOf(3, 'POINTING', ball))).toEqual(['POINTER', 'POINTER', 'MIDDLE'])
    expect([1, 2, 3].map((ball) => positionOf(3, 'SHOOTING', ball))).toEqual(['MIDDLE', 'SHOOTER', 'SHOOTER'])
  })
})

describe('localTurn with balls not yet sent', () => {
  it('moves to the next ball at once, pointing for every team before shooting', () => {
    const pending = [1, 2, 3].map((ball) => throwAction(10, 'POINTING', ball, 'ON_JACK'))

    expect(localTurn(withPending(melee(), pending))).toMatchObject({ teamId: 20, kind: 'POINTING', ballNumber: 1, position: 'POINTER' })
  })

  it('shows the points of the pending balls', () => {
    const view = withPending(melee(), [throwAction(10, 'POINTING', 1, 'ON_JACK'), throwAction(10, 'POINTING', 2, 'NEAR_JACK')])

    expect(view.international!.groups[0].rounds[0].teams[0].points).toBe(8)
  })

  it('waits for the server to know whether a finished round needs a tie-break', () => {
    const pending = [10, 20].flatMap((team) =>
      (['POINTING', 'SHOOTING'] as const).flatMap((kind) => [1, 2, 3].map((ball) => throwAction(team, kind, ball, kind === 'POINTING' ? 'OUT' : 'MISS'))))

    expect(localTurn(withPending(melee(), pending))).toEqual({ awaitingServer: true, groupId: 7 })
  })
})

describe('tieBreakPoints', () => {
  /** Teams 10, 20 and 30 tied on 12 in the regular round; 10 and 20 tied again in the first tie-break. */
  function tied(): MeleeView {
    const team = (teamId: number, points: number) => ({ teamId, playOrder: 1, points, balls: [] })
    return {
      international: {
        groups: [{
          id: 7, playOrder: 1, wins: 2, bestPosition: 2, worstPosition: 4, status: 'FINISHED', order: [20, 10, 30], obsoletePlayed: false,
          rounds: [
            { id: 100, number: 1, obsolete: false, complete: true, teams: [team(10, 12), team(20, 12), team(30, 12)] },
            { id: 102, number: 3, obsolete: false, complete: true, teams: [team(10, 4), team(20, 9)] },
            { id: 101, number: 2, obsolete: false, complete: true, teams: [team(10, 7), team(20, 7), team(30, 3)] },
            { id: 99, number: 2, obsolete: true, complete: true, teams: [team(10, 15), team(30, 1)] },
          ],
        }],
        assured: [], turn: null, finalRanking: [], complete: true,
      },
    } as unknown as MeleeView
  }

  it('gives each tie-break on its own, from zero, in order', () => {
    expect(tieBreakPoints(tied(), 20)).toEqual([7, 9])
    expect(tieBreakPoints(tied(), 10)).toEqual([7, 4])
    expect(tieBreakPoints(tied(), 30)).toEqual([3])
  })

  it('leaves out tie-breaks a correction made obsolete, and teams that did not tie', () => {
    expect(tieBreakPoints(tied(), 30)).not.toContain(1)
    expect(tieBreakPoints(tied(), 40)).toEqual([])
    expect(tieBreakPoints({ international: null } as unknown as MeleeView, 10)).toEqual([])
  })
})
