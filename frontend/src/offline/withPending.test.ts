import { describe, expect, it } from 'vitest'
import type { MeleeView } from '../melee/types'
import type { PendingAction } from './outbox'
import { withPending } from './withPending'

const view = {
  id: 1,
  participants: [
    { id: 10, status: 'ACTIVE', paymentStatus: 'UNMARKED' },
    { id: 11, status: 'ACTIVE', paymentStatus: 'PAID' },
    { id: 12, status: 'WITHDRAWN', paymentStatus: 'UNMARKED' },
  ],
  payments: { expected: 2, paid: 1, unpaid: 0, unmarked: 1, entryFeeCents: 500, collectedCents: 500 },
  rounds: [],
  international: null,
} as unknown as MeleeView

const paid = (participantId: number): PendingAction => ({
  key: `payment:${participantId}`,
  meleeId: 1,
  path: '',
  body: {},
  overlay: { kind: 'payment', participantId, paymentStatus: 'PAID' },
  createdAt: 0,
})

describe('payments tapped without signal', () => {
  it('show at once, with the table figures updated', () => {
    const shown = withPending(view, [paid(10)])

    expect(shown.participants[0].paymentStatus).toBe('PAID')
    expect(shown.payments).toEqual({ expected: 2, paid: 2, unpaid: 0, unmarked: 0, entryFeeCents: 500, collectedCents: 1000 })
  })

  it('leave other melees alone', () => {
    expect(withPending(view, [{ ...paid(10), meleeId: 2 }])).toBe(view)
  })
})
