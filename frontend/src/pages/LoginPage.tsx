import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../ui/Button'
import { ErrorMessage } from '../ui/ErrorMessage'
import { PasswordField } from '../ui/PasswordField'
import { TextField } from '../ui/TextField'
import { AuthPageFrame } from './AuthPageFrame'

export function LoginPage() {
  const { t } = useTranslation()
  const { login } = useAuth()
  const passwordChanged = (useLocation().state as { passwordChanged?: boolean } | null)?.passwordChanged === true
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await login(email, password)
    } catch (failure) {
      setError(failure)
      setBusy(false)
    }
  }

  return (
    <AuthPageFrame title={t('auth.login.title')}>
      <form onSubmit={submit} className="flex flex-col gap-4" noValidate>
        {passwordChanged && (
          <p role="status" className="rounded-xl bg-green-100 px-4 py-3 text-lg font-semibold text-green-900">
            {t('auth.login.passwordChanged')}
          </p>
        )}
        <TextField
          label={t('auth.email')}
          type="email"
          autoComplete="email"
          inputMode="email"
          required
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <PasswordField
          label={t('auth.password')}
          autoComplete="current-password"
          value={password}
          onChange={setPassword}
        />
        <ErrorMessage error={error} />
        <Button type="submit" busy={busy}>
          {t('auth.login.submit')}
        </Button>
      </form>
      <div className="mt-6 space-y-2 text-lg">
        <p>
          <Link to="/recuperar" className="font-semibold text-steel-800 underline underline-offset-4">
            {t('auth.login.forgot')}
          </Link>
        </p>
        <p>
          {t('auth.login.noAccount')}{' '}
          <Link to="/registro" className="font-semibold text-steel-800 underline underline-offset-4">
            {t('auth.login.registerLink')}
          </Link>
        </p>
      </div>
    </AuthPageFrame>
  )
}
