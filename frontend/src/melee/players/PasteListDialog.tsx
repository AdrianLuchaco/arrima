import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { meleeRequests, useMeleeAction } from '../meleeApi'
import type { ImportPreviewEntry, MeleeView, ParticipantData } from '../types'
import { Button } from '../../ui/Button'
import { Dialog } from '../../ui/Dialog'
import { ErrorMessage } from '../../ui/ErrorMessage'

interface Row extends ImportPreviewEntry {
  include: boolean
}

/**
 * "Pegar lista de WhatsApp": paste → editable preview → confirm. Flagged rows (struck through in
 * WhatsApp, probably part of the header, or already on the list) start unticked.
 * Every opening starts from scratch: the flow only exists while the dialog is open.
 */
export function PasteListDialog({ melee, open, onClose }: { melee: MeleeView; open: boolean; onClose: () => void }) {
  return open ? <PasteFlow melee={melee} onDone={onClose} /> : null
}

function PasteFlow({ melee, onDone }: { melee: MeleeView; onDone: () => void }) {
  const { t } = useTranslation()
  const [text, setText] = useState('')
  const [rows, setRows] = useState<Row[] | null>(null)

  const preview = useMutation({
    mutationFn: () => meleeRequests.previewImport(melee.id, text),
    onSuccess: (entries) =>
      setRows(entries.map((entry) => ({ ...entry, include: !entry.struckThrough && !entry.beforeListStart && !entry.alreadyRegistered }))),
  })
  const confirm = useMeleeAction(melee.id, (people: ParticipantData[]) => meleeRequests.importParticipants(melee.id, people))

  const included = rows?.filter((row) => row.include) ?? []
  const update = (index: number, change: Partial<Row>) => setRows(rows && rows.map((row, i) => (i === index ? { ...row, ...change } : row)))

  // The buttons live in the dialog's footer: always in view, however long the list is.
  const footer =
    rows === null ? (
      <div className="flex flex-col gap-3">
        <ErrorMessage error={preview.error} />
        <Button className="w-full" busy={preview.isPending} disabled={text.trim() === ''} onClick={() => preview.mutate()}>
          {t('players.paste.read')}
        </Button>
      </div>
    ) : (
      <div className="flex flex-col gap-3">
        <ErrorMessage error={confirm.error} />
        <div className="flex flex-col gap-3 sm:flex-row">
          <Button variant="secondary" onClick={() => setRows(null)}>
            {t('players.paste.again')}
          </Button>
          <Button
            variant="accent"
            className="flex-1"
            busy={confirm.isPending}
            disabled={included.length === 0}
            onClick={() =>
              confirm.mutate(
                included.map((row) => ({ listNumber: row.listNumber, name: row.name })),
                { onSuccess: onDone },
              )
            }
          >
            {t('players.paste.confirm', { count: included.length })}
          </Button>
        </div>
      </div>
    )

  return (
    <Dialog open onClose={onDone} title={t('players.paste.title')} wide footer={footer}>
      {rows === null ? (
        <div className="flex flex-col gap-4">
          <label htmlFor="whatsapp-text" className="text-lg font-semibold">
            {t('players.paste.instructions')}
          </label>
          <textarea
            id="whatsapp-text"
            value={text}
            onChange={(event) => setText(event.target.value)}
            rows={10}
            className="rounded-xl border-2 border-steel-400 bg-white p-3 text-lg"
            placeholder={t('players.paste.placeholder')}
          />
        </div>
      ) : rows.length === 0 ? (
        <p className="text-lg">{t('players.paste.nothingFound')}</p>
      ) : (
        <div className="flex flex-col gap-4">
          <p className="text-lg">{t('players.paste.reviewHelp')}</p>
          <ul className="flex flex-col gap-2">
            {rows.map((row, index) => (
              <li key={index} className={`rounded-2xl border-2 bg-white p-3 ${row.include ? 'border-steel-600' : 'border-gravel-300 opacity-70'}`}>
                <div className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    checked={row.include}
                    onChange={(event) => update(index, { include: event.target.checked })}
                    aria-label={t('players.paste.include', { name: row.name })}
                    className="size-8 shrink-0 accent-steel-800"
                  />
                  <input
                    inputMode="numeric"
                    value={row.listNumber ?? ''}
                    onChange={(event) => {
                      const digits = event.target.value.replace(/\D/g, '')
                      update(index, { listNumber: digits === '' ? null : Number(digits) })
                    }}
                    aria-label={t('players.edit.number')}
                    className="h-12 w-16 shrink-0 rounded-lg border-2 border-steel-400 text-center text-lg font-bold"
                  />
                  <input
                    value={row.name}
                    onChange={(event) => update(index, { name: event.target.value })}
                    aria-label={t('players.edit.name')}
                    className="h-12 min-w-0 flex-1 rounded-lg border-2 border-steel-400 px-3 text-lg"
                  />
                </div>
                {(row.struckThrough || row.beforeListStart || row.alreadyRegistered) && (
                  <p className="mt-2 text-base font-semibold text-amber-800">
                    {row.struckThrough && t('players.paste.flags.struck')}
                    {row.beforeListStart && t('players.paste.flags.header')}
                    {row.alreadyRegistered && t('players.paste.flags.duplicate')}
                  </p>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </Dialog>
  )
}
