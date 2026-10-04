import { describe, expect, it } from 'vitest'
import type { MeleeView } from '../melee/types'
import { nextMatchOf } from './nextMatch'

const match = (id: number, a: number, b: number, winner: number | null, court: number | null = 1) => ({
  id, teamAId: a, teamBId: b, courtNumber: court, winnerTeamId: winner,
})

function view(rounds: MeleeView['rounds']): MeleeView {
  return { rounds, teams: [{ id: 1, wins: 2 }] } as unknown as MeleeView
}

describe('nextMatchOf', () => {
  it('is the first undecided match of the team', () => {
    const next = nextMatchOf(view([
      { number: 1, matches: [match(1, 1, 2, 1)], byeTeamId: null },
      { number: 2, matches: [match(2, 1, 3, null, 4)], byeTeamId: null },
    ]), 1)

    expect(next).toEqual({ kind: 'match', round: 2, match: match(2, 1, 3, null, 4), rivalId: 3 })
  })

  it('says the team rests while its bye round is being played', () => {
    const next = nextMatchOf(view([
      { number: 1, matches: [match(1, 2, 3, null)], byeTeamId: 1 },
      { number: 2, matches: [match(2, 1, 3, null)], byeTeamId: 2 },
    ]), 1)

    expect(next).toEqual({ kind: 'bye', round: 1 })
  })

  it('moves on once the bye round is over', () => {
    const next = nextMatchOf(view([
      { number: 1, matches: [match(1, 2, 3, 2)], byeTeamId: 1 },
      { number: 2, matches: [match(2, 1, 3, null)], byeTeamId: 2 },
    ]), 1)

    expect(next).toMatchObject({ kind: 'match', round: 2, rivalId: 3 })
  })

  it('knows when the team has finished', () => {
    expect(nextMatchOf(view([{ number: 1, matches: [match(1, 1, 2, 1)], byeTeamId: null }]), 1))
      .toEqual({ kind: 'finished', wins: 2 })
  })

  it('waits for the schedule', () => {
    expect(nextMatchOf(view([]), 1)).toEqual({ kind: 'notScheduled' })
  })
})
