import type { MeleeView, Participant } from '../types'

/** Collecting at the table: until the court schedule exists (the summary is then fixed at the bottom). */
export function collectingPayments(melee: MeleeView): boolean {
  return melee.payments !== null && (melee.status === 'REGISTRATION' || melee.status === 'TEAMS')
}

/**
 * Whether this person's payment can be recorded now: until the court schedule exists, and during
 * the matches only for someone in no team (a late arrival who will replace a player).
 */
export function canRecordPaymentOf(melee: MeleeView, participant: Participant): boolean {
  if (!melee.payments || participant.status !== 'ACTIVE') return false
  if (collectingPayments(melee)) return true
  return melee.status === 'MATCHES' && !melee.teams.some((team) => team.memberIds.includes(participant.id))
}

/** Present but still not asked whether they paid. */
export function unmarkedPeople(melee: MeleeView): Participant[] {
  if (!melee.payments) return []
  return melee.participants.filter((participant) => participant.status === 'ACTIVE' && participant.paymentStatus === 'UNMARKED')
}
