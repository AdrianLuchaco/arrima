import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ServerGate } from './ServerGate'

describe('ServerGate', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('shows the loading screen and then the app once the server answers', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response('{"status":"UP"}', { status: 200 }))))

    render(<ServerGate><p>Contenido de la app</p></ServerGate>)

    expect(screen.getByRole('status')).toHaveTextContent('Cargando…')
    expect(await screen.findByText('Contenido de la app')).toBeInTheDocument()
  })
})
