import '@fontsource-variable/atkinson-hyperlegible-next'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.tsx'
import './i18n'
import './index.css'
import { listenForInstallPrompt } from './pwa/install'
import { registerServiceWorker } from './pwa/registerServiceWorker'
import { listenForAudioUnlock } from './timer/alarm'
import { syncServerClock } from './timer/serverClock'

listenForInstallPrompt()
registerServiceWorker()
listenForAudioUnlock()
// The countdowns use the server's clock: measured now and again whenever the app comes back.
void syncServerClock()
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') void syncServerClock()
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
