import { describe, expect, it, vi } from 'vitest'
import { waitForServer, type ServerStatus } from './waitForServer'

const ok = () => Promise.resolve(new Response('{"status":"UP"}', { status: 200 }))
const gatewayTimeout = () => Promise.resolve(new Response('', { status: 504 }))
const networkError = () => Promise.reject(new TypeError('Failed to fetch'))

function run(fetchFn: typeof fetch, options: { isOnline?: () => boolean; signal?: AbortSignal } = {}) {
  const statuses: ServerStatus[] = []
  const done = waitForServer({
    onStatus: (status) => statuses.push(status),
    signal: options.signal ?? new AbortController().signal,
    fetchFn,
    isOnline: options.isOnline ?? (() => true),
    wakingNoticeMs: 1000,
    retryDelayMs: 1,
  })
  return { statuses, done }
}

describe('waitForServer', () => {
  it('is ready straight away when the server answers', async () => {
    const { statuses, done } = run(vi.fn(ok))

    await done

    expect(statuses).toEqual(['checking', 'ready'])
  })

  it('keeps retrying while the server wakes up, without failing', async () => {
    const fetchFn = vi.fn<typeof fetch>()
      .mockImplementationOnce(gatewayTimeout)
      .mockImplementationOnce(networkError)
      .mockImplementation(ok)
    const { statuses, done } = run(fetchFn)

    await done

    expect(fetchFn).toHaveBeenCalledTimes(3)
    expect(statuses).toEqual(['checking', 'waking', 'waking', 'ready'])
  })

  it('says there is no connection when the phone is offline', async () => {
    const fetchFn = vi.fn<typeof fetch>().mockImplementationOnce(networkError).mockImplementation(ok)
    const online = vi.fn().mockReturnValueOnce(false).mockReturnValue(true)
    const { statuses, done } = run(fetchFn, { isOnline: online })

    await done

    expect(statuses).toEqual(['checking', 'offline', 'ready'])
  })

  it('stops retrying once aborted', async () => {
    const controller = new AbortController()
    const fetchFn = vi.fn<typeof fetch>(() => {
      controller.abort()
      return gatewayTimeout()
    })
    const { statuses, done } = run(fetchFn, { signal: controller.signal })

    await done

    expect(fetchFn).toHaveBeenCalledTimes(1)
    expect(statuses).toEqual(['checking'])
  })
})
