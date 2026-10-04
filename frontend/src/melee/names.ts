import type { MeleeView } from './types'

/** "Manuel y Paqui", "Manuel, Paqui y Pepe": how teams are named aloud at the club. */
export function teamPlayers(view: MeleeView, teamId: number): string {
  const team = view.teams.find((candidate) => candidate.id === teamId)
  if (!team) return ''
  const names = team.memberIds.map((id) => view.participants.find((participant) => participant.id === id)?.name ?? '?')
  return new Intl.ListFormat('es', { style: 'long', type: 'conjunction' }).format(names)
}

export function teamNumber(view: MeleeView, teamId: number): number {
  return view.teams.find((team) => team.id === teamId)?.number ?? 0
}

export function participantName(view: MeleeView, participantId: number): string {
  return view.participants.find((participant) => participant.id === participantId)?.name ?? '?'
}
