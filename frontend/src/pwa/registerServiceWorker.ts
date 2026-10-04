/**
 * The service worker (pwa/sw.js) keeps the app on the phone so it opens without coverage.
 * Only in the production build: in development Vite serves fresh files on every change.
 */
export function registerServiceWorker() {
  if (!import.meta.env.PROD || !('serviceWorker' in navigator)) return
  window.addEventListener('load', () => {
    // A failure only means the app is not kept offline; everything else keeps working.
    navigator.serviceWorker.register('/sw.js').catch(() => undefined)
  })
}
