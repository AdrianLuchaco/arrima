import { useErrorText } from './useErrorText'

export function ErrorMessage({ error }: { error: unknown }) {
  const { message } = useErrorText()
  if (!error) return null
  return (
    <p role="alert" className="rounded-xl border-2 border-red-700 bg-red-50 px-4 py-3 text-lg font-semibold text-red-900">
      {message(error)}
    </p>
  )
}
