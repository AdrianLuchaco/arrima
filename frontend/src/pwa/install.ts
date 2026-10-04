import { useSyncExternalStore } from 'react'

/**
 * Installing Arrima on the home screen. Chrome (Android and computers) offers its own install
 * dialog, which we keep and open from our card at a calm moment. Safari on iPhone has no such
 * dialog: the card explains where "Añadir a pantalla de inicio" is.
 */

/** Chrome's event; not in TypeScript's DOM types because it is not a web standard. */
interface BeforeInstallPromptEvent extends Event {
  prompt(): Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

let deferredPrompt: BeforeInstallPromptEvent | null = null
const listeners = new Set<() => void>()

/** Called once at start-up: Chrome may fire the event before React has rendered anything. */
export function listenForInstallPrompt() {
  window.addEventListener('beforeinstallprompt', (event) => {
    // Instead of the browser's own banner, which would cover the screen at any moment.
    event.preventDefault()
    deferredPrompt = event as BeforeInstallPromptEvent
    notify()
  })
  window.addEventListener('appinstalled', () => {
    deferredPrompt = null
    notify()
  })
}

export function useCanPromptInstall(): boolean {
  return useSyncExternalStore(subscribe, () => deferredPrompt !== null)
}

/** Opens Chrome's install dialog. True if the admin accepted. */
export async function promptInstall(): Promise<boolean> {
  const event = deferredPrompt
  if (!event) return false
  // The event can only be used once.
  deferredPrompt = null
  notify()
  await event.prompt()
  return (await event.userChoice).outcome === 'accepted'
}

/** Already opened from the home screen icon. */
export function isInstalled(): boolean {
  return window.matchMedia('(display-mode: standalone)').matches || (navigator as { standalone?: boolean }).standalone === true
}

export type Platform = 'ios' | 'android' | 'other'

export function platformOf(userAgent: string, maxTouchPoints: number): Platform {
  // iPads ask for the desktop site and say "Macintosh"; touch gives them away.
  if (/iPhone|iPad|iPod/.test(userAgent) || (/Macintosh/.test(userAgent) && maxTouchPoints > 1)) return 'ios'
  if (/Android/.test(userAgent)) return 'android'
  return 'other'
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

function notify() {
  listeners.forEach((listener) => listener())
}
