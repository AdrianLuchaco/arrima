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
