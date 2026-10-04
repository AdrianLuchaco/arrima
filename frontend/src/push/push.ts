import { api } from '../api/client'
import { isInstalled, platformOf } from '../pwa/install'

/**
 * "Avísame cuando se acabe el tiempo" with the browser's Web Push. The server sends one notification
 * per round, when the time is up; it arrives even with the app closed.
 *
 * Where it works: Chrome on Android (and computers); Safari on iPhone only when the app is on the
 * home screen (iOS 16.4+); nowhere in development (`npm run dev` has no service worker).
 */
export type PushSupport = 'supported' | 'needsInstall' | 'unsupported'

export type PushTarget = { kind: 'public'; code: string } | { kind: 'admin'; meleeId: number }

export type SubscribeResult = 'subscribed' | 'denied' | 'unavailable'

const STORAGE_KEY = 'arrima.push.subscribed'

export async function pushSupport(): Promise<PushSupport> {
  const ios = platformOf(navigator.userAgent, navigator.maxTouchPoints) === 'ios'
  if (!('serviceWorker' in navigator) || !('PushManager' in window) || !('Notification' in window)) {
    return ios && !isInstalled() ? 'needsInstall' : 'unsupported'
  }
  return (await navigator.serviceWorker.getRegistration()) ? 'supported' : 'unsupported'
}

/** Must start from a tap: browsers only ask for permission after one. */
export async function subscribe(target: PushTarget): Promise<SubscribeResult> {
  // First, before any other wait: the permission prompt needs the tap to be recent.
  const permission = await Notification.requestPermission()
  if (permission !== 'granted') return 'denied'
  const key = await serverKey()
  if (!key) return 'unavailable'
  const registration = await navigator.serviceWorker.ready
  const subscription = await browserSubscription(registration, key)
  await api(path(target), { method: 'POST', body: subscription.toJSON(), authenticated: target.kind === 'admin' })
  remember(target)
  return 'subscribed'
}

export function isSubscribed(target: PushTarget): boolean {
  return readSubscribed().includes(keyOf(target))
}

async function browserSubscription(registration: ServiceWorkerRegistration, key: Uint8Array): Promise<PushSubscription> {
  const options = { userVisibleOnly: true, applicationServerKey: key as BufferSource }
  try {
    return (await registration.pushManager.getSubscription()) ?? (await registration.pushManager.subscribe(options))
  } catch {
    // Subscribed before with other server keys: start again.
    await (await registration.pushManager.getSubscription())?.unsubscribe()
    return registration.pushManager.subscribe(options)
  }
}

let cachedKey: Promise<Uint8Array | null> | null = null

/** Fetched once: null when the server has notifications off (no VAPID keys). */
function serverKey(): Promise<Uint8Array | null> {
  cachedKey ??= api<{ publicKey: string }>('/api/public/push/key', { authenticated: false })
    .then(({ publicKey }) => base64UrlToBytes(publicKey))
    .catch(() => null)
  return cachedKey
}

function path(target: PushTarget): string {
  return target.kind === 'public'
    ? `/api/public/melees/${encodeURIComponent(target.code)}/push-subscriptions`
    : `/api/melees/${target.meleeId}/push-subscriptions`
}

function keyOf(target: PushTarget): string {
  return target.kind === 'public' ? `public:${target.code}` : `admin:${target.meleeId}`
}

function remember(target: PushTarget) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify([...readSubscribed(), keyOf(target)].slice(-50)))
  } catch {
    // Private mode: it will offer to subscribe again next time, which is harmless.
  }
}

function readSubscribed(): string[] {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]') as string[]
  } catch {
    return []
  }
}

export function base64UrlToBytes(value: string): Uint8Array {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(value.length / 4) * 4, '=')
  return Uint8Array.from(atob(base64), (char) => char.charCodeAt(0))
}
