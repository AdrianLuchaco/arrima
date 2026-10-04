import { useRef, useState, type ChangeEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { compressImage } from '../lib/images'
import { meleeRequests, useMeleeAction } from '../melee/meleeApi'
import type { MeleeView, Prize } from '../melee/types'
import { Button } from '../ui/Button'
import { ErrorMessage } from '../ui/ErrorMessage'
import { prizeText, sharePhoto } from './share'

/**
 * The photos of a prize: take one with the camera (several are fine), share each one with its text,
 * delete a bad one. During the ceremony only; afterwards they are read-only.
 */
export function PrizePhotos({ melee, prize, editable }: { melee: MeleeView; prize: Prize; editable: boolean }) {
  const { t } = useTranslation()
  const input = useRef<HTMLInputElement>(null)
  const [preparing, setPreparing] = useState(false)
  const [shareNote, setShareNote] = useState<string | null>(null)
  const upload = useMeleeAction(melee.id, (photo: Blob) => meleeRequests.uploadPhoto(melee.id, prize.id, photo))
  const remove = useMeleeAction(melee.id, (photoId: number) => meleeRequests.deletePhoto(melee.id, prize.id, photoId))

  async function take(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setPreparing(true)
    try {
      // About 300 KB instead of several MB: the courts' signal and the free 1 GB of storage.
      upload.mutate(await compressImage(file, { maxSide: 1600, type: 'image/jpeg', quality: 0.8 }))
    } finally {
      setPreparing(false)
    }
  }

  async function share(url: string, index: number) {
    const result = await sharePhoto(url, prizeText(melee, prize), `premio-${prize.position}-${index + 1}.jpg`)
    setShareNote(result === 'fallback' ? t('prizes.shareFallback') : null)
  }

  return (
    <div className="flex flex-col gap-3">
      {editable && (
        <>
          {/* capture: opens the camera directly on phones (the gallery is still available). */}
          <input ref={input} type="file" accept="image/*" capture="environment" className="sr-only" onChange={take} />
          <Button variant="accent" className="min-h-16 text-xl" busy={preparing || upload.isPending} onClick={() => input.current?.click()}>
            📷 {prize.photos.length === 0 ? t('prizes.takePhoto') : t('prizes.takeAnother')}
          </Button>
        </>
      )}
      <ErrorMessage error={upload.error ?? remove.error} />
      {shareNote && <p role="status" className="text-lg text-steel-600">{shareNote}</p>}
      <ul className="flex flex-col gap-4">
        {prize.photos.map((photo, index) => (
          <li key={photo.id} className="flex flex-col gap-2">
            <img src={photo.url} alt={t('prizes.photoAlt', { position: prize.position })} className="aspect-[4/3] w-full rounded-xl object-cover" />
            <div className="flex gap-2">
              <Button variant="secondary" className="flex-1" onClick={() => void share(photo.url, index)}>
                {t('prizes.share')}
              </Button>
              {editable && (
                <Button variant="ghost" busy={remove.isPending} onClick={() => remove.mutate(photo.id)}>
                  {t('prizes.deletePhoto')}
                </Button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
