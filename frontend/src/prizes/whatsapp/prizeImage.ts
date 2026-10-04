import type { MeleeView, Prize } from '../../melee/types'
import { imageFileName, prizeCardText, wrapText } from './layout'

/** WhatsApp shrinks images anyway: 1080 px wide is sharp on any phone and light to send. */
const WIDTH = 1080
/** Taller photos are cropped around the middle to 4:5, like WhatsApp shows them. */
const MAX_PHOTO_HEIGHT = 1350
const MARGIN = 60
const FONT = '"Atkinson Hyperlegible Next Variable", system-ui, sans-serif'
const STEEL = '#2c3439'
const JACK = '#e8590c'
const LIGHT = '#f7f3ec'
const GRAVEL = '#cbbda3'

/**
 * The photo of a prize with a strip underneath: the prize, the names, the club (with its logo) and
 * the date. The text goes inside the image, so it arrives whatever WhatsApp does with captions.
 * A prize without photo gets the strip alone, so the series is complete and in order.
 */
export async function prizeImage(melee: MeleeView, prize: Prize): Promise<File> {
  const text = prizeCardText(melee, prize)
  const photoUrl = (prize.photos.find((photo) => photo.main) ?? prize.photos[0])?.url
  const [photo, logo] = await Promise.all([
    photoUrl ? loadImage(photoUrl) : null,
    melee.club.logoUrl ? loadImage(melee.club.logoUrl).catch(() => null) : null,
    document.fonts.load(`800 64px ${FONT}`).catch(() => undefined),
  ])

  const measuring = document.createElement('canvas').getContext('2d')!
  measuring.font = `800 64px ${FONT}`
  const nameLines = wrapText(text.names, WIDTH - 2 * MARGIN, (line) => measuring.measureText(line).width, 3)

  const photoHeight = photo ? Math.min(MAX_PHOTO_HEIGHT, Math.round((WIDTH * photo.naturalHeight) / photo.naturalWidth)) : 0
  const stripHeight = 56 + 110 + nameLines.length * 78 + 28 + 96 + 52
  const canvas = document.createElement('canvas')
  canvas.width = WIDTH
  canvas.height = photoHeight + stripHeight
  const context = canvas.getContext('2d')!

  if (photo) drawCovering(context, photo, photoHeight)

  // The strip, with the jack's orange as a border on top.
  let y = photoHeight
  context.fillStyle = STEEL
  context.fillRect(0, y, WIDTH, stripHeight)
  context.fillStyle = JACK
  context.fillRect(0, y, WIDTH, 12)
  context.textBaseline = 'alphabetic'

  y += 56 + 88
  context.fillStyle = JACK
  context.font = `800 96px ${FONT}`
  context.fillText(text.heading, MARGIN, y)

  context.fillStyle = LIGHT
  context.font = `800 64px ${FONT}`
  for (const line of nameLines) {
    y += 78
    context.fillText(line, MARGIN, y)
  }

  y += 28
  const textX = logo ? MARGIN + 96 : MARGIN
  if (logo) drawRoundLogo(context, logo, MARGIN, y + 8, 80)
  context.fillStyle = LIGHT
  context.font = `700 40px ${FONT}`
  context.fillText(text.club, textX, y + 42, WIDTH - textX - MARGIN)
  context.fillStyle = GRAVEL
  context.font = `400 36px ${FONT}`
  context.fillText(text.date, textX, y + 88, WIDTH - textX - MARGIN)

  const blob = await new Promise<Blob>((resolve, reject) =>
    canvas.toBlob((result) => (result ? resolve(result) : reject(new Error('No image'))), 'image/jpeg', 0.9))
  return new File([blob], imageFileName(prize.position), { type: 'image/jpeg' })
}

/** Our own links (same address as the app), so the canvas can be exported. */
async function loadImage(url: string): Promise<HTMLImageElement> {
  const image = new Image()
  image.src = url
  await image.decode()
  return image
}

/** Fills the width; if the photo is taller than allowed, keeps its middle part. */
function drawCovering(context: CanvasRenderingContext2D, photo: HTMLImageElement, height: number) {
  const sourceHeight = Math.min(photo.naturalHeight, (photo.naturalWidth * height) / WIDTH)
  const sourceY = (photo.naturalHeight - sourceHeight) / 2
  context.drawImage(photo, 0, sourceY, photo.naturalWidth, sourceHeight, 0, 0, WIDTH, height)
}

function drawRoundLogo(context: CanvasRenderingContext2D, logo: HTMLImageElement, x: number, y: number, size: number) {
  context.save()
  context.beginPath()
  context.arc(x + size / 2, y + size / 2, size / 2, 0, Math.PI * 2)
  context.fillStyle = '#ffffff'
  context.fill()
  context.clip()
  const scale = Math.min(size / logo.naturalWidth, size / logo.naturalHeight)
  const width = logo.naturalWidth * scale
  const height = logo.naturalHeight * scale
  context.drawImage(logo, x + (size - width) / 2, y + (size - height) / 2, width, height)
  context.restore()
}
