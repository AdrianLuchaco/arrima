import { describe, expect, it } from 'vitest'
import { ordinalBeforeNoun } from './ordinal'

describe('ordinals before "premio"', () => {
  it('shortens primer and tercer', () => {
    expect([1, 2, 3, 4, 5, 10].map(ordinalBeforeNoun)).toEqual(['1.er', '2.º', '3.er', '4.º', '5.º', '10.º'])
  })

  it('knows undécimo, decimotercer and vigésimo primer', () => {
    expect([11, 13, 21, 23].map(ordinalBeforeNoun)).toEqual(['11.º', '13.er', '21.er', '23.er'])
  })
})
