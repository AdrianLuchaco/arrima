import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../ui/Button'
import { ErrorMessage } from '../ui/ErrorMessage'
import { useErrorText } from '../ui/useErrorText'
import { PasswordField } from '../ui/PasswordField'
import { TextField } from '../ui/TextField'
import { AuthPageFrame } from './AuthPageFrame'

export function RegisterPage() {
  const { t } = useTranslation()
  const { register } = useAuth()
  const errors = useErrorText()
  const [form, setForm] = useState({ invitationCode: '', clubName: '', email: '', password: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)

  const update = (field: keyof typeof form) => (value: string) => setForm((current) => ({ ...current, [field]: value }))

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await register(form)
    } catch (failure) {
      setError(failure)
      setBusy(false)
    }
  }

  return (
    <AuthPageFrame title={t('auth.register.title')}>
      <form onSubmit={submit} className="flex flex-col gap-4" noValidate>
        <TextField
          label={t('auth.register.invitationCode')}
          help={t('auth.register.invitationHelp')}
          autoCapitalize="characters"
          autoComplete="off"
          value={form.invitationCode}
          onChange={(event) => update('invitationCode')(event.target.value)}
          error={errors.field(error, 'invitationCode')}
        />
        <TextField
          label={t('auth.register.clubName')}
          autoComplete="organization"
          value={form.clubName}
          onChange={(event) => update('clubName')(event.target.value)}
          error={errors.field(error, 'clubName')}
        />
        <TextField
          label={t('auth.email')}
          type="email"
          inputMode="email"
          autoComplete="email"
          value={form.email}
          onChange={(event) => update('email')(event.target.value)}
          error={errors.field(error, 'email')}
        />
        <PasswordField
          label={t('auth.register.password')}
          help={t('auth.register.passwordHelp')}
          autoComplete="new-password"
          value={form.password}
          onChange={update('password')}
          error={errors.field(error, 'password')}
        />
        <ErrorMessage error={error} />
        <Button type="submit" busy={busy}>
          {t('auth.register.submit')}
        </Button>
      </form>
      <p className="mt-6 text-lg">
        {t('auth.register.haveAccount')}{' '}
        <Link to="/entrar" className="font-semibold text-steel-800 underline underline-offset-4">
          {t('auth.register.loginLink')}
        </Link>
      </p>
    </AuthPageFrame>
  )
}
