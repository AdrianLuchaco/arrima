import '@fontsource-variable/atkinson-hyperlegible-next'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.tsx'
import './i18n'
import './index.css'
import { listenForInstallPrompt } from './pwa/install'
import { registerServiceWorker } from './pwa/registerServiceWorker'

listenForInstallPrompt()
registerServiceWorker()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
