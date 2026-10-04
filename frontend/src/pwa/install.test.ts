import { describe, expect, it } from 'vitest'
import { platformOf } from './install'

describe('where the install steps come from', () => {
  it('recognises iPhones, and iPads that ask for the desktop site', () => {
    expect(platformOf('Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 Version/18.5 Mobile/15E148 Safari/604.1', 5)).toBe('ios')
    expect(platformOf('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/18.5 Safari/605.1.15', 5)).toBe('ios')
  })

  it('tells a Mac apart from an iPad', () => {
    expect(platformOf('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/18.5 Safari/605.1.15', 0)).toBe('other')
  })

  it('recognises Android', () => {
    expect(platformOf('Mozilla/5.0 (Linux; Android 15; Pixel 8) AppleWebKit/537.36 Chrome/140.0 Mobile Safari/537.36', 5)).toBe('android')
  })
})
