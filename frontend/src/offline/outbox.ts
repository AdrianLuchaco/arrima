import type { QueryClient } from '@tanstack/react-query'
import { ApiError } from '../api/ApiError'
import { api } from '../api/client'
import type { MeleeView, PaymentStatus } from '../melee/types'

/**
 * "Never lose a result already tapped": on the courts the signal comes and goes.
 *
 * Every result, Internacional ball and payment is first written to this queue in the phone's
 * storage, shown on screen at once, and sent in order when there is connection. It survives reloads
 * and closing the app. Each action is an idempotent PUT, so sending it twice is harmless; a newer
 * action for the same thing (correcting a result) replaces the older one.
 */
export interface PendingAction {
  /** Identifies what the action sets, e.g. "winner:42". Only the last action per key is kept. */
  key: string
  meleeId: number
  path: string
  body: unknown
  /** For the optimistic view while the action is pending. */
  overlay: Overlay
  createdAt: number
}

export type Overlay =
  | { kind: 'winner'; matchId: number; winnerTeamId: number | null }
  | { kind: 'throw'; roundId: number; teamId: number; ballKind: string; ballNumber: number; outcome: string }
  | { kind: 'payment'; participantId: number; paymentStatus: PaymentStatus }

export interface OutboxState {
  pending: PendingAction[]
  /** An action the server refused (e.g. the melee changed phase): it was dropped, the admin must know. */
  rejected: { action: PendingAction; error: ApiError } | null
  sending: boolean
}

const STORAGE_KEY = 'arrima.outbox.v1'
const RETRY_MS = 10_000

let state: OutboxState = { pending: read(), rejected: null, sending: false }
const listeners = new Set<() => void>()
let queryClient: QueryClient | null = null
let retryTimer: ReturnType<typeof setTimeout> | null = null

/** Called once at start-up: sends whatever was left from a previous session. */
export function startOutbox(client: QueryClient) {
  queryClient = client
  window.addEventListener('online', () => void flush())
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') void flush()
  })
  void flush()
}

export function enqueue(action: Omit<PendingAction, 'createdAt'>) {
  const others = state.pending.filter((pending) => pending.key !== action.key)
  update({ pending: [...others, { ...action, createdAt: Date.now() }] })
  void flush()
}

export function dismissRejected() {
  update({ rejected: null })
}

export function getOutboxState(): OutboxState {
  return state
}

export function subscribeOutbox(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

/** Sends the queue in order; stops at the first network failure and tries again later. */
export async function flush(): Promise<void> {
  if (state.sending || state.pending.length === 0) return
  update({ sending: true })
  try {
    while (state.pending.length > 0) {
      const action = state.pending[0]
      try {
        const view = await api<MeleeView>(action.path, { method: 'PUT', body: action.body })
        removeIfUnchanged(action)
        queryClient?.setQueryData(['melee', action.meleeId], view)
      } catch (error) {
        if (error instanceof ApiError && !error.isNetworkError && error.status !== 401 && error.status < 500) {
          removeIfUnchanged(action)
          update({ rejected: { action, error } })
          continue
        }
        scheduleRetry()
        return
      }
    }
  } finally {
    update({ sending: false })
  }
}

/** If the admin changed the same thing while it was being sent, keep the newer action. */
function removeIfUnchanged(sent: PendingAction) {
  update({ pending: state.pending.filter((pending) => pending !== sent) })
}

function scheduleRetry() {
  if (retryTimer) clearTimeout(retryTimer)
  retryTimer = setTimeout(() => {
    retryTimer = null
    void flush()
  }, RETRY_MS)
}

function update(change: Partial<OutboxState>) {
  state = { ...state, ...change }
  write(state.pending)
  listeners.forEach((listener) => listener())
}

function read(): PendingAction[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? (JSON.parse(stored) as PendingAction[]) : []
  } catch {
    return []
  }
}

function write(pending: PendingAction[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(pending))
  } catch {
    // Private mode or storage full: the queue still works while the app stays open.
  }
}
