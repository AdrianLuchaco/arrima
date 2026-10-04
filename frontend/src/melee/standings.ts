import type { MeleeView, Team, WinTarget } from './types'

/**
 * Same rules as the backend's Standings (the reference): a bye counts as a win; for "all rounds
 * won" and "one less", how many teams have exactly that now and how many can still get there.
 * Used to update the screen at once while a result is still waiting to be sent.
 */
export function recomputeStandings(view: MeleeView): Pick<MeleeView, 'teams' | 'counter'> {
  const byes = new Set(view.rounds.map((round) => round.byeTeamId).filter((id): id is number => id !== null))
  const matches = view.rounds.flatMap((round) => round.matches)

  const teams: Team[] = view.teams.map((team) => {
    let wins = byes.has(team.id) ? 1 : 0
    let losses = 0
    let pending = 0
    for (const match of matches) {
      if (match.teamAId !== team.id && match.teamBId !== team.id) continue
      if (match.winnerTeamId === null) pending++
      else if (match.winnerTeamId === team.id) wins++
      else losses++
    }
    return { ...team, wins, losses, pending }
  })

  const rounds = view.settings.roundsCount
  const target = (wins: number): WinTarget => ({
    wins,
    reached: teams.filter((team) => team.wins === wins).length,
    canReach: teams.filter((team) => team.wins < wins && team.wins + team.pending >= wins).length,
  })
  const counter = view.rounds.length === 0 ? [] : [target(rounds), target(rounds - 1)].filter((item) => item.wins > 0)
  return { teams, counter }
}
