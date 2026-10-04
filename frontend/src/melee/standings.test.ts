import { describe, expect, it } from 'vitest'
import { recomputeStandings } from './standings'
import type { MeleeView } from './types'

/** Four teams, three rounds; team 4 rests in round 1 (as if there were an odd team elsewhere). */
function melee(winners: (number | null)[]): MeleeView {
  const matches = [
    { id: 1, teamAId: 1, teamBId: 2, courtNumber: 1, winnerTeamId: winners[0] },
    { id: 2, teamAId: 1, teamBId: 3, courtNumber: 1, winnerTeamId: winners[1] },
    { id: 3, teamAId: 2, teamBId: 3, courtNumber: 2, winnerTeamId: winners[2] },
  ]
  return {
    settings: { roundsCount: 3, courtCount: 2, prizeCount: 2 },
    teams: [1, 2, 3, 4].map((id) => ({ id, number: id, memberIds: [], wins: 0, losses: 0, pending: 0 })),
    rounds: [
      { number: 1, matches: [matches[0]], byeTeamId: 4 },
      { number: 2, matches: [matches[1]], byeTeamId: null },
      { number: 3, matches: [matches[2]], byeTeamId: null },
    ],
  } as unknown as MeleeView
}

describe('recomputeStandings', () => {
  it('counts the bye as a win and undecided matches as pending', () => {
    const { teams } = recomputeStandings(melee([1, null, null]))

    expect(teams.map(({ id, wins, losses, pending }) => ({ id, wins, losses, pending }))).toEqual([
      { id: 1, wins: 1, losses: 0, pending: 1 },
      { id: 2, wins: 0, losses: 1, pending: 1 },
      { id: 3, wins: 0, losses: 0, pending: 2 },
      { id: 4, wins: 1, losses: 0, pending: 0 },
    ])
  })

  it('computes the counter like the backend', () => {
    const { counter } = recomputeStandings(melee([1, 1, 3]))

    // Team 1: 2 wins (done). Team 3: 1 win. Team 4: 1 (the bye). Team 2: 0.
    expect(counter).toEqual([
      { wins: 3, reached: 0, canReach: 0 },
      { wins: 2, reached: 1, canReach: 0 },
    ])
  })
})
