import { NavLink, Outlet } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { useClubProfile } from '../club/clubApi'

/** Admin area frame: the club's logo and name always at the top, as on the club's paper sheet. */
export function AppShell() {
  const { t } = useTranslation()
  const { logout } = useAuth()
  const { data: club } = useClubProfile()

  const navClass = ({ isActive }: { isActive: boolean }) =>
    `rounded-xl px-3 py-2 text-lg font-semibold ${isActive ? 'bg-gravel-50 text-steel-900' : 'text-gravel-50 hover:bg-steel-900'}`

  return (
    <div className="min-h-dvh">
      <header className="bg-steel-800 text-gravel-50">
        <div className="mx-auto flex max-w-4xl flex-wrap items-center gap-3 px-4 py-3">
          {club?.logoUrl ? (
            <img src={club.logoUrl} alt="" className="size-12 rounded-full bg-white object-contain" />
          ) : (
            <span aria-hidden="true" className="size-12 rounded-full bg-[radial-gradient(circle_at_35%_30%,#f4f6f7,#9aa5ad_45%,#2c3439)]" />
          )}
          <p className="min-w-0 flex-1 truncate text-xl font-bold">{club?.name ?? t('app.name')}</p>
          <nav className="flex items-center gap-1" aria-label={t('nav.label')}>
            <NavLink to="/" end className={navClass}>
              {t('nav.melees')}
            </NavLink>
            <NavLink to="/club" className={navClass}>
              {t('nav.club')}
            </NavLink>
            <button type="button" onClick={() => void logout()} className="rounded-xl px-3 py-2 text-lg font-semibold underline underline-offset-4 hover:bg-steel-900">
              {t('auth.logout')}
            </button>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-4xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
