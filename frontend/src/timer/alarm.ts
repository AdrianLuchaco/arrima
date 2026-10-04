/**
 * The alarm at the end of a round. Browsers only allow sound after the user has touched the page, so
 * the audio is unlocked on a touch: any touch, and explicitly «Empezar partida» and «Avísame».
 * The sound is generated (Web Audio): nothing to download, and it works without signal.
 */
let context: AudioContext | null = null
let stopCurrent: (() => void) | null = null

const BEEPS = 60
const HIGH_HZ = 880
const LOW_HZ = 660

export function unlockAudio() {
  try {
    context ??= new AudioContext()
    if (context.state === 'suspended') void context.resume()
    // A silent instant: iPhones only unlock the audio once something actually plays during a touch.
    const source = context.createBufferSource()
    source.buffer = context.createBuffer(1, 1, 22_050)
    source.connect(context.destination)
    source.start(0)
  } catch {
    // No audio on this browser: the notification and the screen still work.
  }
}

/** Every touch unlocks it, so anyone who touched the page once will hear the alarm. */
export function listenForAudioUnlock() {
  document.addEventListener('pointerdown', unlockAudio, { passive: true })
}

/** Two-tone beeps every second for a minute (or until stopped), and vibration where the phone allows it. */
export function startAlarm() {
  stopAlarm()
  navigator.vibrate?.([600, 200, 600, 200, 600, 200, 600, 200, 600])
  const audio = context
  if (!audio || audio.state !== 'running') return
  const volume = audio.createGain()
  volume.gain.value = 0.8
  volume.connect(audio.destination)
  // Everything scheduled at once on the audio clock: it keeps time even if the page is busy.
  const start = audio.currentTime + 0.05
  for (let second = 0; second < BEEPS; second++) {
    beep(audio, volume, start + second, HIGH_HZ)
    beep(audio, volume, start + second + 0.35, LOW_HZ)
  }
  stopCurrent = () => volume.disconnect()
}

export function stopAlarm() {
  stopCurrent?.()
  stopCurrent = null
  navigator.vibrate?.(0)
}

function beep(audio: AudioContext, output: AudioNode, at: number, frequency: number) {
  const oscillator = audio.createOscillator()
  oscillator.type = 'square'
  oscillator.frequency.value = frequency
  oscillator.connect(output)
  oscillator.start(at)
  oscillator.stop(at + 0.25)
}
