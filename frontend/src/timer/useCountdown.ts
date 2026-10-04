import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { MeleeView } from '../melee/types'
import { currentTimer, remainingMillis, type CountdownState } from './countdown'
import { serverNow } from './serverClock'

/** A countdown that ran out less than this long ago still sounds when the screen is opened. */
const RECENT_MILLIS = 2 * 60_000
const SEEN_KEY = 'arrima.timeUp.seen'

/**
 * The countdown to show, refreshed four times a second while it runs. {@code onTimeUp} is called once
 * per round when its time runs out with the screen open, or if it ran out just before opening it.
 */
export function useCountdown(melee: MeleeView, onTimeUp: (round: number) => void): CountdownState | null {
  const current = currentTimer(melee)
  const running = current?.timer.state === 'RUNNING'
  const [now, setNow] = useState(serverNow)
  // The latest callback, without restarting the effects below when the caller re-renders.
  const onTimeUpRef = useRef(onTimeUp)
  useLayoutEffect(() => {
    onTimeUpRef.current = onTimeUp
  })

  useEffect(() => {
    if (!running) return
    const tick = setInterval(() => setNow(serverNow()), 250)
    return () => clearInterval(tick)
  }, [running])

  const remaining = current ? remainingMillis(current.timer, now) : 0
  const ranOut = current !== null
    && ((running && remaining === 0)
      || (current.timer.endReason === 'TIME_UP' && current.timer.endedAt !== null
        && now - Date.parse(current.timer.endedAt) < RECENT_MILLIS))
  const key = current ? `${melee.id}:${current.round}:${current.timer.endsAt}` : null

  useEffect(() => {
    if (key && ranOut && !wasSeen(key)) {
      markSeen(key)
      onTimeUpRef.current(Number(key.split(':')[1]))
    }
  }, [key, ranOut])

  return current ? { ...current, remaining } : null
}

/** Remembered on the phone, so a reload does not sound the same alarm again. */
function wasSeen(key: string): boolean {
  return readSeen().includes(key)
}

function markSeen(key: string) {
  try {
    localStorage.setItem(SEEN_KEY, JSON.stringify([...readSeen(), key].slice(-30)))
  } catch {
    // Private mode: at worst the alarm sounds again after a reload.
  }
}

function readSeen(): string[] {
  try {
    return JSON.parse(localStorage.getItem(SEEN_KEY) ?? '[]') as string[]
  } catch {
    return []
  }
}
