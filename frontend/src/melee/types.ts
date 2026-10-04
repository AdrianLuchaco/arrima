import type { ScoringTable } from '../club/clubApi'

export type MeleeStatus = 'REGISTRATION' | 'TEAMS' | 'MATCHES' | 'INTERNATIONAL' | 'PRIZES' | 'CLOSED'
export type ParticipantStatus = 'ACTIVE' | 'WITHDRAWN'

export const STATUS_ORDER: MeleeStatus[] = ['REGISTRATION', 'TEAMS', 'MATCHES', 'INTERNATIONAL', 'PRIZES', 'CLOSED']

export interface Participant {
  id: number
  listNumber: number | null
  name: string
  status: ParticipantStatus
}

export interface TeamPlan {
  activePlayers: number
  fits: boolean
  playable: boolean
  teamCount: number
  teamsBySize: Record<string, number>
}

export interface MeleeView {
  id: number
  publicCode: string
  playedOn: string
  format: 'CLASSIC'
  teamSize: 2 | 3
  status: MeleeStatus
  revision: number
  settings: { courtCount: number; roundsCount: number; prizeCount: number }
  scoring: ScoringTable
  club: { name: string; logoUrl: string | null }
  participants: Participant[]
  teamPlan: TeamPlan
  teams: Team[]
  teamIssues: TeamIssues
  maxRounds: number
  rounds: Round[]
  counter: WinTarget[]
}

export interface Team {
  id: number
  number: number
  memberIds: number[]
  /** Includes the bye, which counts as a win. */
  wins: number
  losses: number
  pending: number
}

export interface TeamIssues {
  unassignedPlayers: number[]
  withdrawnMembers: number[]
  teamsWithoutActivePlayers: number[]
}

export interface Match {
  id: number
  teamAId: number
  teamBId: number
  /** null while waiting for a free court. */
  courtNumber: number | null
  winnerTeamId: number | null
}

export interface Round {
  number: number
  matches: Match[]
  byeTeamId: number | null
}

export interface WinTarget {
  wins: number
  reached: number
  canReach: number
}

export interface MeleeSummary {
  id: number
  playedOn: string
  status: MeleeStatus
  teamSize: 2 | 3
  publicCode: string
  activePlayers: number
}

export interface ImportPreviewEntry {
  listNumber: number | null
  name: string
  struckThrough: boolean
  beforeListStart: boolean
  alreadyRegistered: boolean
}

export interface ParticipantData {
  listNumber: number | null
  name: string
}
