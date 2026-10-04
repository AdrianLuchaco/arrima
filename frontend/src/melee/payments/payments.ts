import type { MeleeStatus, MeleeView, Participant } from '../types'

/** Payments are recorded at the sign-up table and can be corrected until the court schedule exists. */
export function canRecordPayments(melee: MeleeView): boolean {
  return melee.payments !== null && (['REGISTRATION', 'TEAMS'] as MeleeStatus[]).includes(melee.status)
}

/** Present but still not asked whether they paid. */
export function unmarkedPeople(melee: MeleeView): Participant[] {
  if (!melee.payments) return []
  return melee.participants.filter((participant) => participant.status === 'ACTIVE' && participant.paymentStatus === 'UNMARKED')
}
