/**
 * How the prize images reach the club's WhatsApp group. Today, through the phone's share sheet:
 * WhatsApp opens with the images and the admin chooses the group. There is no official free way to
 * post into an existing group automatically; if one comes, it would be another implementation of
 * these operations (e.g. a per-club channel in the backend) and the screens would not change.
 */
export interface PrizeImage {
  position: number
  file: File
  /** "1.er premio — Manuel y Paqui", for the one-by-one sending. */
  text: string
}

export type DeliveryOutcome = 'sent' | 'cancelled' | 'failed'

/** Whether this browser can hand images to another app (Chrome on Android, Safari on iPhone do). */
export function canShareImages(): boolean {
  try {
    return navigator.canShare?.({ files: [new File([new Uint8Array(1)], 'prueba.jpg', { type: 'image/jpeg' })] }) === true
  } catch {
    return false
  }
}

/** Every image at once, first prize first, with the classification as text. */
export function sendAll(images: PrizeImage[], text: string): Promise<DeliveryOutcome> {
  return share({ files: images.map((image) => image.file), text })
}

export function sendOne(image: PrizeImage): Promise<DeliveryOutcome> {
  return share({ files: [image.file], text: image.text })
}

/** Without a share sheet: the image is saved to the phone, to attach it in WhatsApp by hand. */
export function download(file: File) {
  const link = document.createElement('a')
  link.href = URL.createObjectURL(file)
  link.download = file.name
  link.click()
  setTimeout(() => URL.revokeObjectURL(link.href), 10_000)
}

/** WhatsApp sometimes drops the text that comes with files: it is also left in the clipboard. */
export async function copyText(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return false
  }
}

async function share(data: ShareData): Promise<DeliveryOutcome> {
  try {
    await navigator.share(data)
    return 'sent'
  } catch (error) {
    // Closing the share sheet is a normal "no"; anything else means it could not be done.
    return error instanceof DOMException && error.name === 'AbortError' ? 'cancelled' : 'failed'
  }
}
