import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { MeleeView } from '../melee/types'
import { isSubscribed, pushSupport, subscribe, type PushSupport } from '../push/push'
import { Button } from '../ui/Button'
import { unlockAudio } from '../timer/alarm'

type State = 'idle' | 'busy' | 'subscribed' | 'denied' | 'unavailable' | 'soundOnly'

/**
 * «Avísame cuando se acabe el tiempo»: a notification even with the phone locked or the app closed.
 * Where that is not possible (an iPhone without the app on the home screen, an old browser), the
 * alarm still sounds while the app is open, and the card says so.
 */
export function NotifyMeCard({ melee }: { melee: MeleeView }) {
  const { t } = useTranslation()
  const target = { kind: 'public', code: melee.publicCode } as const
  const [support, setSupport] = useState<PushSupport | null>(null)
  const [state, setState] = useState<State>(() => (isSubscribed(target) ? 'subscribed' : 'idle'))

  useEffect(() => {
    void pushSupport().then(setSupport)
  }, [])

  if (melee.status !== 'MATCHES' || support === null) return null

  async function notifyMe() {
    // This tap is also what lets the alarm sound with the app open.
    unlockAudio()
    setState('busy')
    try {
      setState(await subscribe(target))
    } catch {
      setState('unavailable')
    }
  }

  function soundOnly() {
    unlockAudio()
    setState('soundOnly')
  }

  return (
    <section className="mb-4 rounded-2xl border-2 border-steel-600 bg-white p-4 text-lg">
      {state === 'subscribed' ? (
        <p className="font-semibold text-green-900">✓ {t('timer.notify.subscribed')}</p>
      ) : state === 'soundOnly' ? (
        <p className="font-semibold">{t('timer.notify.soundOnly')}</p>
      ) : support === 'supported' ? (
        <div className="flex flex-col gap-3">
          <Button busy={state === 'busy'} onClick={() => void notifyMe()}>
            🔔 {t('timer.notify.button')}
          </Button>
          {state === 'denied' && <p>{t('timer.notify.denied')}</p>}
          {state === 'unavailable' && <p>{t('timer.notify.unavailable')}</p>}
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          <p className="font-bold">{t('timer.notify.title')}</p>
          {support === 'needsInstall' ? (
            <ol className="list-decimal space-y-1 pl-7">
              <li>{t('timer.notify.ios.share')}</li>
              <li>{t('timer.notify.ios.add')}</li>
              <li>{t('timer.notify.ios.open')}</li>
            </ol>
          ) : (
            <p>{t('timer.notify.unsupported')}</p>
          )}
          <Button variant="secondary" onClick={soundOnly}>
            🔊 {t('timer.notify.soundButton')}
          </Button>
        </div>
      )}
    </section>
  )
}
