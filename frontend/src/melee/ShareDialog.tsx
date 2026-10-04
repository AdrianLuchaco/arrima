import qrcode from 'qrcode-generator'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '../ui/Button'
import { Dialog } from '../ui/Dialog'
import type { MeleeView } from './types'

/**
 * The public link of the melee, its QR to show from the phone and its code in big letters, so
 * players can follow everything from their own phones without an account.
 */
export function ShareDialog({ melee, onClose }: { melee: MeleeView; onClose: () => void }) {
  const { t } = useTranslation()
  const [copied, setCopied] = useState(false)
  const url = `${window.location.origin}/m/${melee.publicCode}`

  const qrImage = useMemo(() => {
    const qr = qrcode(0, 'M')
    qr.addData(url)
    qr.make()
    return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(qr.createSvgTag({ cellSize: 8, margin: 2, scalable: true }))}`
  }, [url])

  async function copy() {
    try {
      await navigator.clipboard.writeText(url)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  async function share() {
    try {
      await navigator.share({ title: t('share.title'), text: t('share.text', { club: melee.club.name }), url })
    } catch {
      // The person closed the share sheet: nothing to do.
    }
  }

  return (
    <Dialog open onClose={onClose} title={t('share.title')}>
      <div className="flex flex-col items-center gap-4 text-center">
        <img src={qrImage} alt={t('share.qrAlt')} className="w-full max-w-72 rounded-2xl bg-white p-2" />
        <p className="text-lg">{t('share.codeLabel')}</p>
        <p className="font-mono text-4xl font-extrabold tracking-[0.2em]">{melee.publicCode}</p>
        <p className="break-all text-lg text-steel-600">{url}</p>
        <div className="grid w-full gap-3 sm:grid-cols-2">
          {'share' in navigator && <Button onClick={() => void share()}>{t('share.share')}</Button>}
          <Button variant="secondary" onClick={() => void copy()}>
            {copied ? t('share.copied') : t('share.copy')}
          </Button>
        </div>
      </div>
    </Dialog>
  )
}
