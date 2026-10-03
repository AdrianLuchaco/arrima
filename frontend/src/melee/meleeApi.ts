import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { ImportPreviewEntry, MeleeSummary, MeleeView, ParticipantData } from './types'

export const meleeKey = (id: number) => ['melee', id] as const
const MELEES_KEY = ['melees'] as const

export function useMelees() {
  return useQuery({ queryKey: MELEES_KEY, queryFn: () => api<MeleeSummary[]>('/api/melees') })
}

export function useMelee(id: number) {
  return useQuery({ queryKey: meleeKey(id), queryFn: () => api<MeleeView>(`/api/melees/${id}`) })
}

export function useCreateMelee() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: { teamSize: 2 | 3; courtCount: number; roundsCount: number; prizeCount: number }) =>
      api<MeleeView>('/api/melees', { method: 'POST', body }),
    onSuccess: (view) => {
      queryClient.setQueryData(meleeKey(view.id), view)
      void queryClient.invalidateQueries({ queryKey: MELEES_KEY })
    },
  })
}

export function useDeleteMelee() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/melees/${id}`, { method: 'DELETE' }),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: meleeKey(id) })
      void queryClient.invalidateQueries({ queryKey: MELEES_KEY })
    },
  })
}

/**
 * Every melee change answers with the whole updated view: we store it as the new truth.
 * The list of melees is refreshed too (status and player counts may have changed).
 */
export function useMeleeAction<TVariables>(meleeId: number, request: (variables: TVariables) => Promise<MeleeView>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: request,
    onSuccess: (view) => {
      queryClient.setQueryData(meleeKey(meleeId), view)
      void queryClient.invalidateQueries({ queryKey: MELEES_KEY })
    },
  })
}

const base = (meleeId: number) => `/api/melees/${meleeId}`

export const meleeRequests = {
  previewImport: (meleeId: number, text: string) =>
    api<ImportPreviewEntry[]>(`${base(meleeId)}/participants/preview`, { method: 'POST', body: { text } }),
  importParticipants: (meleeId: number, participants: ParticipantData[]) =>
    api<MeleeView>(`${base(meleeId)}/participants/import`, { method: 'POST', body: { participants } }),
  addParticipant: (meleeId: number, data: ParticipantData) =>
    api<MeleeView>(`${base(meleeId)}/participants`, { method: 'POST', body: data }),
  editParticipant: (meleeId: number, id: number, data: ParticipantData) =>
    api<MeleeView>(`${base(meleeId)}/participants/${id}`, { method: 'PUT', body: data }),
  withdraw: (meleeId: number, id: number) =>
    api<MeleeView>(`${base(meleeId)}/participants/${id}/withdraw`, { method: 'POST' }),
  reinstate: (meleeId: number, id: number) =>
    api<MeleeView>(`${base(meleeId)}/participants/${id}/reinstate`, { method: 'POST' }),
  deleteParticipant: (meleeId: number, id: number) =>
    api<MeleeView>(`${base(meleeId)}/participants/${id}`, { method: 'DELETE' }),
  changeSettings: (meleeId: number, settings: MeleeView['settings']) =>
    api<MeleeView>(`${base(meleeId)}/settings`, { method: 'PUT', body: settings }),
  goBack: (meleeId: number) => api<MeleeView>(`${base(meleeId)}/back`, { method: 'POST' }),
  close: (meleeId: number) => api<MeleeView>(`${base(meleeId)}/close`, { method: 'POST' }),
}
