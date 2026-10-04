import { describe, expect, it } from 'vitest'
import { eurosForInput, formatEuros, parseEuros } from './money'

describe('money', () => {
  it('shows whole euros without cents and other amounts with them', () => {
    expect(formatEuros(17000)).toBe('170 €')
    expect(formatEuros(250)).toBe('2,50 €')
    expect(formatEuros(0)).toBe('0 €')
  })

  it('reads amounts typed in any usual way', () => {
    expect(parseEuros('5')).toBe(500)
    expect(parseEuros('2,5')).toBe(250)
    expect(parseEuros(' 2.50 € ')).toBe(250)
    expect(parseEuros('0,05')).toBe(5)
  })

  it('rejects what is not an amount', () => {
    expect(parseEuros('')).toBeNull()
    expect(parseEuros('cinco')).toBeNull()
    expect(parseEuros('-5')).toBeNull()
    expect(parseEuros('2,555')).toBeNull()
  })

  it('writes amounts for an input field', () => {
    expect(eurosForInput(500)).toBe('5,00')
    expect(eurosForInput(250)).toBe('2,50')
  })
})
