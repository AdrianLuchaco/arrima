import type { MeleeView, RoundTimer } from '../melee/types'

export interface CountdownState {
  round: number
  timer: RoundTimer
  remaining: number
}

/** Shown during the matches while there is a countdown to see (one stopped by the next round is not). */
export function showsTimer(countdown: CountdownState | null): countdown is CountdownState {
  return countdown !== null && (countdown.timer.state !== 'ENDED' || countdown.timer.endReason === 'TIME_UP')
}

/** What is left of a countdown at the server's moment {@code now}, never below zero. */
export function remainingMillis(timer: RoundTimer, now: number): number {
  if (timer.state === 'ENDED') return 0
  const reference = timer.state === 'PAUSED' && timer.pausedAt ? Date.parse(timer.pausedAt) : now
  return Math.max(0, Date.parse(timer.endsAt) - reference)
}

/** "44:59"; rounded up, so it reads 0:00 only when the time is really up. */
export function formatCountdown(millis: number): string {
  const totalSeconds = Math.ceil(millis / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}

/**
 * The countdown to show: the one not ended yet (there is at most one), otherwise the last one that
 * ran out, so "Tiempo cumplido" stays visible until the next round starts.
 */
export function currentTimer(melee: MeleeView): { round: number; timer: RoundTimer } | null {
  const started = melee.rounds.filter((round) => round.timer !== null)
  const live = started.find((round) => round.timer!.state !== 'ENDED')
  const latest = live ?? started.filter((round) => round.timer!.endReason === 'TIME_UP').at(-1)
  return latest ? { round: latest.number, timer: latest.timer! } : null
}

/** The next round whose countdown can be started: the first one never started. */
export function nextRoundToStart(melee: MeleeView): number | null {
  return melee.rounds.find((round) => round.timer === null)?.number ?? null
}
