import { describe, expect, it } from 'vitest'
import type { MeleeView } from '../melee/types'
import { classificationText, prizeText } from './share'

const melee = {
  playedOn: '2026-10-03',
  club: { name: 'Club Petanca Arrima', logoUrl: null },
  participants: [
    { id: 1, name: 'Manuel' }, { id: 2, name: 'Paqui' }, { id: 3, name: 'Pepe' }, { id: 4, name: 'Lola' },
  ],
  teams: [{ id: 10, number: 7, memberIds: [1, 2] }, { id: 20, number: 2, memberIds: [3, 4] }],
  prizes: [
    { id: 1, position: 1, teamId: 10, points: 30, awarded: true, photos: [] },
    { id: 2, position: 2, teamId: 20, points: null, awarded: true, photos: [] },
  ],
} as unknown as MeleeView

describe('texts for the WhatsApp group', () => {
  it('names the prize and the players for the photo', () => {
    expect(prizeText(melee, melee.prizes[0])).toBe('1.º premio — Manuel y Paqui')
  })

  it('writes the classification ready to paste', () => {
    expect(classificationText(melee)).toBe(
      '🏆 Club Petanca Arrima · melé del sábado, 3 de octubre de 2026\n\n' +
        '1.º Equipo 7: Manuel y Paqui (30 puntos)\n' +
        '2.º Equipo 2: Pepe y Lola (sin jugar la Internacional)',
    )
  })
})
