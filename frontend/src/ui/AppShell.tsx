import { useState } from 'react'
import { NavLink, Outlet } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { useClubProfile } from '../club/clubApi'
import { ConfirmDialog } from './ConfirmDialog'
import { ConnectionBadge } from './ConnectionBadge'

/**
 * Admin area frame: the club's logo and name always at the top, as on the club's paper sheet.
 * The menu entries look like buttons (outlined, the current one filled), not like loose text, and
 * leaving the account asks first: one stray tap must not log the club out at the courts.
 */
export function AppShell() {
  const { t } = useTranslation()
  const { logout } = useAuth()
  const { data: club } = useClubProfile()
  const [confirmingLogout, setConfirmingLogout] = useState(false)

  const navClass = ({ isActive }: { isActive: boolean }) =>
    `inline-flex min-h-12 items-center rounded-xl border-2 px-4 text-lg font-bold ${isActive ? 'border-gravel-50 bg-gravel-50 text-steel-900' : 'border-steel-400 text-gravel-50 hover:bg-steel-900'}`

  return (
    <div className="min-h-dvh">
      <header className="bg-steel-800 text-gravel-50">
        <div className="mx-auto flex max-w-4xl flex-wrap items-center gap-x-3 gap-y-3 px-4 py-3">
          {/* On phones the club name gets its own line and the menu goes below it. */}
          <div className="flex min-w-0 flex-1 basis-full items-center gap-3 sm:basis-0">
            {club?.logoUrl ? (
              <img src={club.logoUrl} alt="" className="size-10 shrink-0 rounded-full bg-white object-contain" />
            ) : (
              <span aria-hidden="true" className="size-10 shrink-0 rounded-full bg-[radial-gradient(circle_at_35%_30%,#f4f6f7,#9aa5ad_45%,#2c3439)]" />
            )}
            <p className="min-w-0 truncate text-xl font-bold">{club?.name ?? t('app.name')}</p>
          </div>
          <nav className="flex w-full items-center gap-2 sm:w-auto" aria-label={t('nav.label')}>
            <NavLink to="/" end className={navClass}>
              {t('nav.melees')}
            </NavLink>
            <NavLink to="/club" className={navClass}>
              {t('nav.club')}
            </NavLink>
            <button
              type="button"
              onClick={() => setConfirmingLogout(true)}
              className="ml-auto min-h-12 rounded-xl px-3 text-lg font-semibold underline underline-offset-4 hover:bg-steel-900 sm:ml-4"
            >
              {t('auth.logout')}
            </button>
          </nav>
        </div>
      </header>
      <ConnectionBadge />
      <main className="mx-auto max-w-4xl px-4 py-4">
        <Outlet />
      </main>
      <ConfirmDialog
        open={confirmingLogout}
        title={t('auth.logoutConfirm.title')}
        confirmLabel={t('auth.logoutConfirm.confirm')}
        cancelLabel={t('common.no')}
        onCancel={() => setConfirmingLogout(false)}
        onConfirm={() => {
          setConfirmingLogout(false)
          void logout()
        }}
      >
        <p>{t('auth.logoutConfirm.text')}</p>
      </ConfirmDialog>
    </div>
  )
}
