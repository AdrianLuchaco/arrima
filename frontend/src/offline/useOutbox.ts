import { useSyncExternalStore } from 'react'
import { getOutboxState, subscribeOutbox } from './outbox'

export function useOutbox() {
  return useSyncExternalStore(subscribeOutbox, getOutboxState)
}

function subscribeOnline(listener: () => void) {
  window.addEventListener('online', listener)
  window.addEventListener('offline', listener)
  return () => {
    window.removeEventListener('online', listener)
    window.removeEventListener('offline', listener)
  }
}

export function useOnline() {
  return useSyncExternalStore(subscribeOnline, () => navigator.onLine)
}
