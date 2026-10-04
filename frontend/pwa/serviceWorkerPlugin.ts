import { createHash } from 'node:crypto'
import { readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { join, relative, resolve, sep } from 'node:path'
import type { Plugin } from 'vite'

/**
 * Writes /sw.js after the build: the template in pwa/sw.js with the list of files to keep on the
 * phone and a version that changes whenever any of them changes.
 *
 * Done by hand instead of with vite-plugin-pwa/Workbox: the whole behaviour fits in pwa/sw.js,
 * there is nothing to configure and no extra dependency to keep up to date.
 */
export function serviceWorkerPlugin(): Plugin {
  let outDir = ''
  return {
    name: 'arrima-service-worker',
    apply: 'build',
    configResolved(config) {
      outDir = resolve(config.root, config.build.outDir)
    },
    closeBundle() {
      const entries = precacheEntries(listFiles(outDir))
      const version = versionOf(entries.map((entry) => readFileSync(join(outDir, entryFile(entry)))))
      const template = readFileSync(new URL('./sw.js', import.meta.url), 'utf8')
      writeFileSync(join(outDir, 'sw.js'), serviceWorkerSource(template, entries, version))
    },
  }
}

/**
 * What the app needs to open without coverage: the page (as "/", which is what every screen
 * loads), its code and styles, the manifest and the icons. Paths relative to the build folder.
 */
export function precacheEntries(files: string[]): string[] {
  if (!files.includes('index.html')) throw new Error('The build has no index.html')
  const entries = files
    .filter((file) => /^assets\/.+\.(js|css)$/.test(file) || file.startsWith('icons/') || ['favicon.svg', 'manifest.webmanifest'].includes(file))
    .map((file) => `/${file}`)
    .sort()
  return ['/', ...entries]
}

/** Short hash of the precached content: same files, same version, so phones download nothing. */
export function versionOf(contents: Uint8Array[]): string {
  const hash = createHash('sha256')
  contents.forEach((content) => hash.update(content))
  return hash.digest('hex').slice(0, 12)
}

export function serviceWorkerSource(template: string, entries: string[], version: string): string {
  const source = template
    .replace("/* version */ 'dev'", JSON.stringify(version))
    .replace('/* precache */ []', JSON.stringify(entries))
  if (source.includes('/* version */') || source.includes('/* precache */')) {
    throw new Error('pwa/sw.js no longer has the VERSION/PRECACHE markers')
  }
  return source
}

function entryFile(entry: string): string {
  return entry === '/' ? 'index.html' : entry.slice(1)
}

function listFiles(dir: string): string[] {
  return readdirSync(dir, { recursive: true, withFileTypes: true })
    .filter((entry) => entry.isFile())
    .map((entry) => relative(dir, join(entry.parentPath, entry.name)).split(sep).join('/'))
}
