import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import { ApiError } from '../api/ApiError'
import { passwordReset } from '../auth/passwordReset'
import { Button } from '../ui/Button'
import { ErrorMessage } from '../ui/ErrorMessage'
import { PasswordField } from '../ui/PasswordField'
import { useErrorText } from '../ui/useErrorText'
import { AuthPageFrame } from './AuthPageFrame'

/**
 * Opened from the link in the e-mail: /restablecer#<token>. The token goes after "#" so that it
 * never reaches any server's logs; it is read once and removed from the address bar.
 */
export function ResetPasswordPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const errors = useErrorText()
  const [token] = useState(() => window.location.hash.slice(1))
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const linkInvalid = token === '' || (error instanceof ApiError && error.code === 'RESET_LINK_INVALID')

  // Once read, the token leaves the address bar (and so the history and any screenshot).
  useEffect(() => {
    if (window.location.hash) history.replaceState(history.state, '', window.location.pathname)
  }, [])

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await passwordReset.confirm(token, password)
      navigate('/entrar', { replace: true, state: { passwordChanged: true } })
    } catch (failure) {
      setError(failure)
      setBusy(false)
    }
  }

  return (
    <AuthPageFrame title={t('auth.reset.title')}>
      {linkInvalid ? (
        <div className="flex flex-col gap-4 text-lg">
          <p role="alert">{t('errors.RESET_LINK_INVALID')}</p>
          <Link to="/recuperar" className="font-semibold text-steel-800 underline underline-offset-4">
            {t('auth.reset.askAgain')}
          </Link>
        </div>
      ) : (
        <form onSubmit={submit} className="flex flex-col gap-4" noValidate>
          <PasswordField
            label={t('auth.register.password')}
            autoComplete="new-password"
            value={password}
            onChange={setPassword}
            help={t('auth.register.passwordHelp')}
            error={errors.field(error, 'password')}
          />
          <ErrorMessage error={error} />
          <Button type="submit" busy={busy}>
            {t('auth.reset.submit')}
          </Button>
          <p className="text-base text-steel-600">{t('auth.reset.sessionsEnd')}</p>
        </form>
      )}
    </AuthPageFrame>
  )
}
