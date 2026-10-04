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
  /** null until la Internacional starts. */
  international: International | null
  prizes: Prize[]
}

export interface Prize {
  id: number
  position: number
  teamId: number
  /** La Internacional's points; null if the team did not need to play. */
  points: number | null
  awarded: boolean
  photos: { id: number; url: string }[]
}

export type ThrowKind = 'POINTING' | 'SHOOTING'
export type PlayerPosition = 'POINTER' | 'MIDDLE' | 'SHOOTER'
export type PointingOutcome = 'OUT' | 'BIG_CIRCLE' | 'SMALL_CIRCLE' | 'NEAR_JACK' | 'ON_JACK'
export type ShootingOutcome = 'MISS' | 'HIT' | 'HIT_OUT' | 'CARREAU'
export type ThrowOutcome = PointingOutcome | ShootingOutcome

export interface Ball {
  kind: ThrowKind
  ballNumber: number
  position: PlayerPosition
  outcome: ThrowOutcome
  points: number
  corrected: boolean
}

export interface IntlTeam {
  teamId: number
  playOrder: number
  points: number
  balls: Ball[]
}

export interface IntlRound {
  id: number
  /** 1 is the regular round; 2+ are tie-breaks among tied teams. */
  number: number
  obsolete: boolean
  complete: boolean
  teams: IntlTeam[]
}

export interface IntlGroup {
  id: number
  playOrder: number
  wins: number
  bestPosition: number
  worstPosition: number
  status: 'PENDING' | 'IN_PROGRESS' | 'FINISHED'
  order: number[]
  obsoletePlayed: boolean
  rounds: IntlRound[]
}

export interface Turn {
  groupId: number
  roundId: number
  teamId: number
  kind: ThrowKind
  ballNumber: number
  position: PlayerPosition
}

export interface Ranked {
  position: number
  teamId: number
  points: number | null
  tieBreak: boolean
}

export interface International {
  groups: IntlGroup[]
  assured: Ranked[]
  turn: Turn | null
  finalRanking: Ranked[]
  complete: boolean
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
