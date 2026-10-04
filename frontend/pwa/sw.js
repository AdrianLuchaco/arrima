/*
 * Arrima's service worker. Its only job: the app opens at once, even with no coverage at the
 * courts, because the page, code, styles and icons are kept on the phone.
 *
 * It never touches /api: results, live updates and sign-in always go to the network (the outbox in
 * the app keeps the taps made without coverage). Nothing private is ever stored here.
 *
 * This is a template: the build (pwa/serviceWorkerPlugin.ts) writes it to /sw.js with the real
 * VERSION and PRECACHE. A new deploy changes VERSION, phones install it in the background and it
 * applies the next time the app is opened.
 */
const VERSION = /* version */ 'dev'
const PRECACHE = /* precache */ []
const CACHE = `arrima-${VERSION}`
// Servers may answer "Vary: Origin", and the page's module scripts are requested with an Origin
// header that the precache requests did not have. Only our own static files are stored, so the
// stored copy is right whatever the request headers.
const MATCH = { ignoreVary: true }

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(CACHE)
      .then((cache) => cache.addAll(PRECACHE))
      .then(() => self.skipWaiting()),
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((key) => key.startsWith('arrima-') && key !== CACHE).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  const url = new URL(request.url)
  if (request.method !== 'GET' || url.origin !== self.location.origin || url.pathname.startsWith('/api/')) {
    return
  }
  event.respondWith(request.mode === 'navigate' ? appShell(request) : cacheFirst(request))
})

/** Every screen of the app is the same page ("/"); the router in the page picks what to show. */
async function appShell(request) {
  const cache = await caches.open(CACHE)
  return (await cache.match('/', MATCH)) ?? fetch(request)
}

/**
 * Built files carry a hash of their content in the name, so a stored copy is never out of date.
 * Fonts are stored the first time they are used (the browser only downloads the subsets it needs).
 */
async function cacheFirst(request) {
  const cache = await caches.open(CACHE)
  const cached = await cache.match(request, MATCH)
  if (cached) {
    return cached
  }
  const response = await fetch(request)
  if (response.ok && new URL(request.url).pathname.startsWith('/assets/')) {
    await cache.put(request, response.clone())
  }
  return response
}
