import { formatMeleeDate } from '../../lib/dates'
import { ordinalBeforeNoun } from '../../lib/ordinal'
import { teamPlayers } from '../../melee/names'
import type { MeleeView, Prize } from '../../melee/types'

/** What goes in the strip of each prize image, so the text travels inside the picture. */
export interface PrizeCardText {
  heading: string
  names: string
  club: string
  date: string
}

export function prizeCardText(melee: MeleeView, prize: Prize): PrizeCardText {
  const date = formatMeleeDate(melee.playedOn)
  return {
    heading: `${ordinalBeforeNoun(prize.position)} premio`,
    names: teamPlayers(melee, prize.teamId),
    club: melee.club.name,
    date: date.charAt(0).toUpperCase() + date.slice(1),
  }
}

/** First prize first, as they must arrive in the group (the ceremony goes the other way). */
export function sendingOrder(prizes: Prize[]): Prize[] {
  return [...prizes].sort((a, b) => a.position - b.position)
}

/** "premio-01.jpg": numbered with zeros, so any app that sorts by name keeps the order. */
export function imageFileName(position: number): string {
  return `premio-${position.toString().padStart(2, '0')}.jpg`
}

/**
 * Splits a text into lines no wider than {@code maxWidth}, word by word, with at most
 * {@code maxLines}; if it does not fit, the last line ends in "…". {@code measure} gives the width
 * of a text (the canvas in the app, a fake in the tests).
 */
export function wrapText(text: string, maxWidth: number, measure: (text: string) => number, maxLines: number): string[] {
  const lines: string[] = []
  let current = ''
  for (const word of text.split(/\s+/).filter(Boolean)) {
    const candidate = current ? `${current} ${word}` : word
    if (measure(candidate) <= maxWidth || !current) {
      current = candidate
    } else {
      lines.push(current)
      current = word
    }
  }
  if (current) lines.push(current)
  if (lines.length <= maxLines) return lines
  const kept = lines.slice(0, maxLines)
  let last = `${kept[maxLines - 1]}…`
  while (measure(last) > maxWidth && last.length > 1) last = `${last.slice(0, -2)}…`
  kept[maxLines - 1] = last
  return kept
}
