import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ordinalBeforeNoun } from '../../lib/ordinal'
import { meleeRequests, useMeleeAction } from '../../melee/meleeApi'
import type { MeleeView } from '../../melee/types'
import { Button } from '../../ui/Button'
import { ConfirmDialog } from '../../ui/ConfirmDialog'
import { ErrorMessage } from '../../ui/ErrorMessage'
import { classificationText, prizeText } from '../share'
import { canShareImages, copyText, download, sendAll, sendOne, type PrizeImage } from './delivery'
import { sendingOrder } from './layout'
import { prizeImage } from './prizeImage'

type Step = 'idle' | 'confirm' | 'oneByOne' | 'manual' | 'askSent'

/**
 * «Enviar premios al grupo de WhatsApp», at the end of the ceremony. The images are prepared as
 * soon as the admin taps, so that the "Sí" that shares them is still a fresh tap (browsers only
 * share right after one). Every image at once, or one by one if a phone mixes up the order.
 */
export function SendPrizesPanel({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const [step, setStep] = useState<Step>('idle')
  const [images, setImages] = useState<PrizeImage[] | null>(null)
  const [preparingError, setPreparingError] = useState<unknown>(null)
  const [sentOnes, setSentOnes] = useState<number[]>([])
  const [notice, setNotice] = useState<string | null>(null)
  const preparing = useRef<{ photos: string; images: Promise<PrizeImage[]> } | null>(null)
  const markShared = useMeleeAction(melee.id, () => meleeRequests.markPrizesShared(melee.id))
  const canShare = canShareImages()

  if (melee.status !== 'PRIZES' || melee.prizes.length === 0) return null

  /** Builds the images once, and again only if the photos changed since. */
  function prepare() {
    setPreparingError(null)
    const photos = melee.prizes.map((prize) => `${prize.id}:${prize.photos.find((photo) => photo.main)?.id ?? 0}`).join(',')
    if (preparing.current?.photos !== photos) {
      setImages(null)
      const images = Promise.all(
        sendingOrder(melee.prizes).map(async (prize) => ({
          position: prize.position,
          file: await prizeImage(melee, prize),
          text: prizeText(melee, prize),
        })),
      )
      preparing.current = { photos, images }
      images.then(setImages, (error: unknown) => {
        preparing.current = null
        setPreparingError(error)
      })
    }
  }

  function start(next: 'confirm' | 'oneByOne') {
    setNotice(null)
    // This tap may write to the clipboard: the classification is copied now, just in case.
    void copyText(classificationText(melee))
    prepare()
    setStep(canShare ? next : 'manual')
  }

  async function sendEverything() {
    if (!images) return
    const outcome = await sendAll(images, classificationText(melee))
    if (outcome === 'sent') setStep('askSent')
    else if (outcome === 'cancelled') setStep('idle')
    else {
      // Some phones refuse several images at once: one by one always works.
      setNotice(t('sendPrizes.allAtOnceFailed'))
      setStep('oneByOne')
    }
  }

  async function sendSingle(image: PrizeImage) {
    if ((await sendOne(image)) === 'sent') setSentOnes((done) => [...new Set([...done, image.position])])
  }

  const count = melee.prizes.length

  return (
    <section className="flex flex-col gap-3 rounded-2xl border-2 border-steel-600 bg-white p-4">
      {melee.prizesSharedAt && (
        <p className="text-lg font-semibold text-green-900">
          ✓ {t('sendPrizes.alreadySent', { time: new Date(melee.prizesSharedAt).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' }) })}
        </p>
      )}
      {notice && <p className="text-lg">{notice}</p>}

      {step !== 'oneByOne' && step !== 'manual' && (
        <>
          <Button variant="accent" className="min-h-16 text-xl" onClick={() => start('confirm')}>
            {t('sendPrizes.button')}
          </Button>
          {canShare && (
            <Button variant="ghost" onClick={() => start('oneByOne')}>
              {t('sendPrizes.oneByOneButton')}
            </Button>
          )}
        </>
      )}

      {step === 'oneByOne' && (
        <div className="flex flex-col gap-3">
          <p className="text-lg">{t('sendPrizes.oneByOneHelp')}</p>
          {(images ?? []).map((image) => (
            <Button key={image.position} variant={sentOnes.includes(image.position) ? 'secondary' : 'primary'} onClick={() => void sendSingle(image)}>
              {sentOnes.includes(image.position) ? '✓ ' : ''}
              {t('sendPrizes.sendOne', { ordinal: ordinalBeforeNoun(image.position) })}
            </Button>
          ))}
          {!images && !preparingError && <p className="text-lg">{t('sendPrizes.preparing')}</p>}
          <Button variant="secondary" onClick={() => setStep('askSent')}>
            {t('sendPrizes.done')}
          </Button>
        </div>
      )}

      {step === 'manual' && (
        <div className="flex flex-col gap-3">
          <p className="text-lg">{t('sendPrizes.manualHelp')}</p>
          {(images ?? []).map((image) => (
            <Button key={image.position} variant="secondary" onClick={() => download(image.file)}>
              {t('sendPrizes.download', { ordinal: ordinalBeforeNoun(image.position) })}
            </Button>
          ))}
          {!images && !preparingError && <p className="text-lg">{t('sendPrizes.preparing')}</p>}
          <a
            href={`https://wa.me/?text=${encodeURIComponent(classificationText(melee))}`}
            target="_blank"
            rel="noopener noreferrer"
            className="text-center text-lg font-semibold underline underline-offset-4"
          >
            {t('sendPrizes.openWhatsApp')}
          </a>
          <Button variant="secondary" onClick={() => setStep('askSent')}>
            {t('sendPrizes.done')}
          </Button>
        </div>
      )}

      <ErrorMessage error={preparingError ?? markShared.error} />

      <ConfirmDialog
        open={step === 'confirm'}
        title={t('sendPrizes.confirmTitle')}
        confirmLabel={images ? t('sendPrizes.confirm') : t('sendPrizes.preparing')}
        cancelLabel={t('common.no')}
        busy={!images && !preparingError}
        onCancel={() => setStep('idle')}
        onConfirm={() => void sendEverything()}
      >
        <p>{t('sendPrizes.confirmText', { count })}</p>
        <p className="text-steel-600">{t('sendPrizes.copied')}</p>
      </ConfirmDialog>

      <ConfirmDialog
        open={step === 'askSent'}
        title={t('sendPrizes.askSentTitle')}
        confirmLabel={t('sendPrizes.askSentYes')}
        cancelLabel={t('common.no')}
        busy={markShared.isPending}
        error={markShared.error}
        onCancel={() => setStep('idle')}
        onConfirm={() => markShared.mutate(undefined, { onSuccess: () => setStep('idle') })}
      >
        <p>{t('sendPrizes.askSentText')}</p>
      </ConfirmDialog>
    </section>
  )
}
