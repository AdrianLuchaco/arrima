/**
 * The last melee a player looked at. An app installed from the public view opens at "/", which is the
 * admin's sign-in: for a player it should open their melee instead (see SignedInOnly).
 */
const KEY = 'arrima.lastPublicMelee'

export function rememberPublicMelee(code: string) {
  try {
    localStorage.setItem(KEY, code)
  } catch {
    // Private mode: the installed app opens the sign-in, from where the code still works.
  }
}

export function lastPublicMelee(): string | null {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}
