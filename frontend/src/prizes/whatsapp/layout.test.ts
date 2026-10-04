import { describe, expect, it } from 'vitest'
import type { MeleeView, Prize } from '../../melee/types'
import { imageFileName, prizeCardText, sendingOrder, wrapText } from './layout'

/** Every character 10 px wide. */
const measure = (text: string) => text.length * 10

const melee = {
  playedOn: '2026-10-04',
  club: { name: 'Club Petanca Arrima', logoUrl: null },
  participants: [
    { id: 1, name: 'Manuel' }, { id: 2, name: 'Paqui' }, { id: 3, name: 'Pepe' },
  ],
  teams: [{ id: 10, number: 7, memberIds: [1, 2, 3] }],
} as unknown as MeleeView

describe('the prize images for the WhatsApp group', () => {
  it('carry the prize, the names (all three in a triplette), the club and the date', () => {
    const prize = { position: 1, teamId: 10 } as Prize

    expect(prizeCardText(melee, prize)).toEqual({
      heading: '1.er premio',
      names: 'Manuel, Paqui y Pepe',
      club: 'Club Petanca Arrima',
      date: 'Domingo, 4 de octubre de 2026',
    })
  })

  it('go from the first prize to the last, with names that keep that order', () => {
    const prizes = [3, 1, 2].map((position) => ({ position }) as Prize)

    expect(sendingOrder(prizes).map((prize) => prize.position)).toEqual([1, 2, 3])
    expect([1, 2, 10].map(imageFileName)).toEqual(['premio-01.jpg', 'premio-02.jpg', 'premio-10.jpg'])
  })

  it('wrap long names into lines that fit', () => {
    expect(wrapText('Manuel, Paqui y Pepe', 120, measure, 3)).toEqual(['Manuel,', 'Paqui y Pepe'])
    expect(wrapText('Ana', 120, measure, 3)).toEqual(['Ana'])
  })

  it('cut with an ellipsis what does not fit in the lines allowed', () => {
    const lines = wrapText('uno dos tres cuatro cinco seis', 90, measure, 2)

    expect(lines).toHaveLength(2)
    expect(lines[1].endsWith('…')).toBe(true)
    expect(measure(lines[1])).toBeLessThanOrEqual(90)
  })
})
