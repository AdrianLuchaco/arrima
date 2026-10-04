/**
 * The server's clock as seen from this phone. A phone a minute out would show a countdown a minute
 * wrong, so we measure the difference once in a while, as NTP does: ask the time, assume the answer
 * took half the round trip to come back, and keep the measurement with the shortest round trip.
 */
let offsetMillis = 0

export function serverNow(): number {
  return Date.now() + offsetMillis
}

export function offsetFrom(samples: { sentAt: number; receivedAt: number; serverTime: number }[]): number {
  const best = samples.reduce((a, b) => (b.receivedAt - b.sentAt < a.receivedAt - a.sentAt ? b : a))
  return best.serverTime + (best.receivedAt - best.sentAt) / 2 - best.receivedAt
}

/** Three quick measurements; a failure (no signal) keeps the previous offset. */
export async function syncServerClock(fetchFn: typeof fetch = fetch): Promise<void> {
  const samples = []
  for (let i = 0; i < 3; i++) {
    try {
      const sentAt = Date.now()
      const response = await fetchFn('/api/time', { cache: 'no-store' })
      const receivedAt = Date.now()
      if (!response.ok) continue
      const { now } = (await response.json()) as { now: number }
      samples.push({ sentAt, receivedAt, serverTime: now })
    } catch {
      // No signal: try again later.
    }
  }
  if (samples.length > 0) offsetMillis = offsetFrom(samples)
}
