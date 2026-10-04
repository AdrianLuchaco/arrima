import { useState } from 'react'
import type { MeleeView } from '../melee/types'
import { startAlarm, stopAlarm } from './alarm'
import { TimeUpOverlay } from './TimeUpOverlay'
import { useCountdown } from './useCountdown'

/** The countdown plus what happens when it runs out: alarm, vibration and the full-screen notice. */
export function useMatchTimer(melee: MeleeView, onTimeUp?: () => void) {
  const [ringingRound, setRingingRound] = useState<number | null>(null)
  const countdown = useCountdown(melee, (round) => {
    setRingingRound(round)
    startAlarm()
    onTimeUp?.()
  })
  const overlay =
    ringingRound !== null ? (
      <TimeUpOverlay
        round={ringingRound}
        onClose={() => {
          stopAlarm()
          setRingingRound(null)
        }}
      />
    ) : null
  return { countdown, overlay }
}
