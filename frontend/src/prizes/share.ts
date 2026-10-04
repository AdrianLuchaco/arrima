import { formatMeleeDate } from '../lib/dates'
import { teamNumber, teamPlayers } from '../melee/names'
import type { MeleeView, Prize } from '../melee/types'

/** "5.º premio — Manuel y Paqui": the text that goes with the photo in the club's group. */
export function prizeText(melee: MeleeView, prize: Prize): string {
  return `${prize.position}.º premio — ${teamPlayers(melee, prize.teamId)}`
}

/** The final classification, ready to paste in the WhatsApp group. */
export function classificationText(melee: MeleeView): string {
  const lines = [`🏆 ${melee.club.name} · melé del ${formatMeleeDate(melee.playedOn)}`, '']
  for (const prize of melee.prizes) {
    const points =
      prize.points === null ? 'sin jugar la Internacional' : `${prize.points} puntos`
    lines.push(`${prize.position}.º Equipo ${teamNumber(melee, prize.teamId)}: ${teamPlayers(melee, prize.teamId)} (${points})`)
  }
  return lines.join('\n')
}

/**
 * Sends the photo with its text to WhatsApp (or any app) through the phone's share sheet. Browsers
 * that cannot share files download the photo and open WhatsApp with the text instead; the photo
 * is then attached by hand. Only official ways: no unofficial WhatsApp libraries.
 */
export async function sharePhoto(photoUrl: string, text: string, fileName: string): Promise<'shared' | 'fallback' | 'cancelled'> {
  const blob = await (await fetch(photoUrl)).blob()
  const file = new File([blob], fileName, { type: blob.type || 'image/jpeg' })
  if (navigator.canShare?.({ files: [file] })) {
    try {
      await navigator.share({ files: [file], text })
      return 'shared'
    } catch {
      return 'cancelled'
    }
  }
  const link = document.createElement('a')
  link.href = URL.createObjectURL(file)
  link.download = fileName
  link.click()
  URL.revokeObjectURL(link.href)
  window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, '_blank', 'noopener')
  return 'fallback'
}
