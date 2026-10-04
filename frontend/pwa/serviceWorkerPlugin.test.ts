// @vitest-environment node
import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { precacheEntries, serviceWorkerSource, versionOf } from './serviceWorkerPlugin.ts'

const build = [
  'index.html',
  'assets/index-abc123.js',
  'assets/index-def456.css',
  'assets/atkinson-hyperlegible-next-latin-wght-normal-x1.woff2',
  'icons/icon-192.png',
  'favicon.svg',
  'manifest.webmanifest',
  'sw.js',
]

describe('the service worker build', () => {
  it('keeps the page, code, styles and icons, but not every font subset nor itself', () => {
    expect(precacheEntries(build)).toEqual([
      '/',
      '/assets/index-abc123.js',
      '/assets/index-def456.css',
      '/favicon.svg',
      '/icons/icon-192.png',
      '/manifest.webmanifest',
    ])
  })

  it('changes the version only when some content changes', () => {
    const a = new TextEncoder().encode('a')
    const b = new TextEncoder().encode('b')
    expect(versionOf([a, b])).toBe(versionOf([a, b]))
    expect(versionOf([a, b])).not.toBe(versionOf([a, a]))
  })

  it('fills in the template, which never handles /api', () => {
    const template = readFileSync(new URL('./sw.js', import.meta.url), 'utf8')
    const source = serviceWorkerSource(template, ['/', '/assets/index-abc123.js'], 'v1')

    expect(source).toContain('const VERSION = "v1"')
    expect(source).toContain('const PRECACHE = ["/","/assets/index-abc123.js"]')
    expect(source).toContain("url.pathname.startsWith('/api/')")
  })
})
