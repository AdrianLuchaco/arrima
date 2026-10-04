import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { passwordReset } from '../auth/passwordReset'
import { Button } from '../ui/Button'
import { ErrorMessage } from '../ui/ErrorMessage'
import { TextField } from '../ui/TextField'
import { AuthPageFrame } from './AuthPageFrame'

/**
 * "¿Has olvidado la contraseña?". The answer is the same whether or not the e-mail has an account:
 * the page can't tell, so it says what to do in both cases.
 */
export function ForgotPasswordPage() {
  const { t } = useTranslation()
  const [email, setEmail] = useState('')
  const [busy, setBusy] = useState(false)
  const [sentTo, setSentTo] = useState<string | null>(null)
  const [error, setError] = useState<unknown>(null)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await passwordReset.request(email)
      setSentTo(email.trim())
    } catch (failure) {
      setError(failure)
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthPageFrame title={t('auth.forgot.title')}>
      {sentTo ? (
        <div role="status" className="flex flex-col gap-3 text-lg">
          <p>{t('auth.forgot.sent', { email: sentTo })}</p>
          <p className="text-steel-600">{t('auth.forgot.sentHelp')}</p>
        </div>
      ) : (
        <form onSubmit={submit} className="flex flex-col gap-4" noValidate>
          <p className="text-lg">{t('auth.forgot.explanation')}</p>
          <TextField
            label={t('auth.email')}
            type="email"
            autoComplete="email"
            inputMode="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <ErrorMessage error={error} />
          <Button type="submit" busy={busy} disabled={email.trim() === ''}>
            {t('auth.forgot.submit')}
          </Button>
        </form>
      )}
      <p className="mt-6 text-lg">
        <Link to="/entrar" className="font-semibold text-steel-800 underline underline-offset-4">
          {t('auth.forgot.back')}
        </Link>
      </p>
    </AuthPageFrame>
  )
}
