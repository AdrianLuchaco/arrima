import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '../ui/Button'
import { isInstalled, platformOf, promptInstall, useCanPromptInstall } from './install'

const DISMISSED_KEY = 'arrima.installCard.dismissed'

/**
 * "Instala Arrima en el móvil", on the admin's home screen until it is installed or dismissed.
 * Where the browser has an install dialog, a button opens it; otherwise, the steps to follow.
 */
export function InstallCard() {
  const { t } = useTranslation()
  const canPrompt = useCanPromptInstall()
  const [dismissed, setDismissed] = useState(readDismissed)
  const platform = platformOf(navigator.userAgent, navigator.maxTouchPoints)

  if (dismissed || isInstalled() || (!canPrompt && platform === 'other')) return null

  function dismiss() {
    try {
      localStorage.setItem(DISMISSED_KEY, '1')
    } catch {
      // Private mode: it will simply show up again next time.
    }
    setDismissed(true)
  }

  return (
    <section aria-labelledby="install-title" className="rounded-2xl border-2 border-gravel-300 bg-gravel-100 p-4">
      <div className="flex items-start gap-3">
        <img src="/icons/icon-192.png" alt="" className="size-14 shrink-0 rounded-xl" />
        <div>
          <h2 id="install-title" className="text-xl font-bold">
            {t('install.title')}
          </h2>
          <p className="text-lg">{t('install.why')}</p>
        </div>
      </div>

      {canPrompt ? (
        <Button className="mt-4 w-full" onClick={() => void promptInstall()}>
          {t('install.button')}
        </Button>
      ) : (
        <ol className="mt-3 list-decimal space-y-1 pl-7 text-lg">
          {platform === 'ios' ? (
            <>
              <li>
                {t('install.ios.share')} <ShareIcon />
              </li>
              <li>{t('install.ios.add')}</li>
              <li>{t('install.ios.confirm')}</li>
            </>
          ) : (
            <>
              <li>{t('install.android.menu')}</li>
              <li>{t('install.android.add')}</li>
            </>
          )}
        </ol>
      )}

      <Button variant="ghost" className="mt-2 w-full" onClick={dismiss}>
        {t('install.later')}
      </Button>
    </section>
  )
}

function readDismissed(): boolean {
  try {
    return localStorage.getItem(DISMISSED_KEY) === '1'
  } catch {
    return false
  }
}

/** Safari's share button (a box with an arrow), so it can be recognised on screen. */
function ShareIcon() {
  return (
    <svg viewBox="0 0 24 24" className="inline size-6 align-text-bottom text-blue-700" aria-hidden="true">
      <path d="M12 3v12M7.5 7.5 12 3l4.5 4.5M8 10H6a1 1 0 0 0-1 1v9a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-9a1 1 0 0 0-1-1h-2" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
