import { describe, expect, it } from 'vitest'
import type { MeleeView, RoundTimer } from '../melee/types'
import { currentTimer, formatCountdown, nextRoundToStart, remainingMillis } from './countdown'
import { offsetFrom } from './serverClock'

const START = Date.parse('2026-10-04T10:00:00Z')
const MINUTE = 60_000

const running: RoundTimer = {
  state: 'RUNNING',
  startedAt: new Date(START).toISOString(),
  durationMillis: 45 * MINUTE,
  pausedAt: null,
  endsAt: new Date(START + 45 * MINUTE).toISOString(),
  endedAt: null,
  endReason: null,
}

describe('the countdown on the phone', () => {
  it('counts down from the server times and stops at zero', () => {
    expect(formatCountdown(remainingMillis(running, START))).toBe('45:00')
    expect(formatCountdown(remainingMillis(running, START + 1000))).toBe('44:59')
    expect(formatCountdown(remainingMillis(running, START + 45 * MINUTE - 1))).toBe('0:01')
    expect(remainingMillis(running, START + 50 * MINUTE)).toBe(0)
  })

  it('stands still while paused', () => {
    const paused: RoundTimer = { ...running, state: 'PAUSED', pausedAt: new Date(START + 10 * MINUTE).toISOString() }

    expect(formatCountdown(remainingMillis(paused, START + 30 * MINUTE))).toBe('35:00')
  })

  it('shows the countdown not ended yet, or else the last one whose time ran out', () => {
    const ended: RoundTimer = { ...running, state: 'ENDED', endedAt: running.endsAt, endReason: 'TIME_UP' }
    const melee = { rounds: [{ number: 1, timer: ended }, { number: 2, timer: running }, { number: 3, timer: null }] } as unknown as MeleeView

    expect(currentTimer(melee)?.round).toBe(2)
    expect(currentTimer({ rounds: [melee.rounds[0], { number: 2, timer: null }] } as unknown as MeleeView)?.round).toBe(1)
    expect(nextRoundToStart(melee)).toBe(3)
  })
})

describe("the phone's clock correction", () => {
  it('assumes the answer took half the round trip and keeps the quickest measurement', () => {
    // The phone is 60 s behind; the second measurement had the shortest round trip.
    const offset = offsetFrom([
      { sentAt: 1000, receivedAt: 1800, serverTime: 61_300 },
      { sentAt: 2000, receivedAt: 2100, serverTime: 62_050 },
    ])

    expect(offset).toBe(60_000)
  })
})
