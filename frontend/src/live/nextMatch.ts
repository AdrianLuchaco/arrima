import type { Match, MeleeView } from '../melee/types'

export type NextMatch =
  | { kind: 'match'; round: number; match: Match; rivalId: number }
  | { kind: 'bye'; round: number }
  | { kind: 'finished'; wins: number }
  | { kind: 'notScheduled' }

/**
 * "Tu próxima partida" for a chosen team: the first round in which its match is still undecided.
 * A bye counts as "now" while its round is still being played; once every match of that round has
 * a result, the team's next round is shown instead.
 */
export function nextMatchOf(view: MeleeView, teamId: number): NextMatch {
  if (view.rounds.length === 0) return { kind: 'notScheduled' }
  for (const round of view.rounds) {
    if (round.byeTeamId === teamId) {
      const roundStillPlaying = round.matches.some((match) => match.winnerTeamId === null)
      if (roundStillPlaying) return { kind: 'bye', round: round.number }
      continue
    }
    const match = round.matches.find((candidate) => candidate.teamAId === teamId || candidate.teamBId === teamId)
    if (match && match.winnerTeamId === null) {
      return { kind: 'match', round: round.number, match, rivalId: match.teamAId === teamId ? match.teamBId : match.teamAId }
    }
  }
  return { kind: 'finished', wins: view.teams.find((team) => team.id === teamId)?.wins ?? 0 }
}
