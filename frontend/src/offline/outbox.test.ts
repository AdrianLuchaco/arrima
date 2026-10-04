import { QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const view = (id: number) => ({ id, rounds: [], teams: [] })

async function freshOutbox() {
  vi.resetModules()
  return import('./outbox')
}

function winner(matchId: number, winnerTeamId: number | null) {
  return {
    key: `winner:${matchId}`,
    meleeId: 7,
    path: `/api/melees/7/schedule/matchups/${matchId}/winner`,
    body: { winnerTeamId },
    overlay: { kind: 'winner' as const, matchId, winnerTeamId },
  }
}

const settle = () => new Promise((resolve) => setTimeout(resolve, 0))

describe('outbox', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => vi.unstubAllGlobals())

  it('keeps a tapped result while there is no connection and sends it later', async () => {
    const fetchMock = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'))
    vi.stubGlobal('fetch', fetchMock)
    const outbox = await freshOutbox()

    outbox.enqueue(winner(1, 10))
    await settle()

    expect(outbox.getOutboxState().pending).toHaveLength(1)
    expect(JSON.parse(localStorage.getItem('arrima.outbox.v1')!)).toHaveLength(1)

    fetchMock.mockResolvedValue(new Response(JSON.stringify(view(7)), { status: 200 }))
    await outbox.flush()

    expect(outbox.getOutboxState().pending).toHaveLength(0)
  })

  it('a correction of the same result replaces the pending one', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    const outbox = await freshOutbox()

    outbox.enqueue(winner(1, 10))
    outbox.enqueue(winner(1, 11))
    await settle()

    expect(outbox.getOutboxState().pending.map((action) => action.body)).toEqual([{ winnerTeamId: 11 }])
  })

  it('survives closing the app', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    const first = await freshOutbox()
    first.enqueue(winner(1, 10))
    await settle()

    const reopened = await freshOutbox()

    expect(reopened.getOutboxState().pending).toHaveLength(1)
  })

  it('drops an action the server refuses and tells the admin', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(JSON.stringify({ code: 'INVALID_STATE' }), { status: 409 }))))
    const outbox = await freshOutbox()

    outbox.enqueue(winner(1, 10))
    await settle()
    await settle()

    expect(outbox.getOutboxState().pending).toHaveLength(0)
    expect(outbox.getOutboxState().rejected?.error.code).toBe('INVALID_STATE')
  })

  it('stores the server view once sent', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(JSON.stringify(view(7)), { status: 200 }))))
    const outbox = await freshOutbox()
    const client = new QueryClient()
    outbox.startOutbox(client)

    outbox.enqueue(winner(1, 10))
    await settle()
    await settle()

    expect(client.getQueryData(['melee', 7])).toEqual(view(7))
  })
})
