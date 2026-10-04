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
 *
 * @param onSuccess runs even if the component that started the action has unmounted meanwhile
 *                  (a callback given to mutate() would not): the phase change often replaces it.
 */
export function useMeleeAction<TVariables>(
  meleeId: number,
  request: (variables: TVariables) => Promise<MeleeView>,
  onSuccess?: (view: MeleeView) => void,
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: request,
    onSuccess: (view) => {
      queryClient.setQueryData(meleeKey(meleeId), view)
      void queryClient.invalidateQueries({ queryKey: MELEES_KEY })
      onSuccess?.(view)
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
  drawTeams: (meleeId: number, acceptDifferentTeam: boolean, confirmLosses: boolean) =>
    api<MeleeView>(`${base(meleeId)}/teams/draw`, { method: 'POST', body: { acceptDifferentTeam, confirmLosses } }),
  resumeTeams: (meleeId: number) => api<MeleeView>(`${base(meleeId)}/teams/resume`, { method: 'POST' }),
  swapPlayers: (meleeId: number, firstPlayerId: number, secondPlayerId: number) =>
    api<MeleeView>(`${base(meleeId)}/teams/swap`, { method: 'POST', body: { firstPlayerId, secondPlayerId } }),
  substitute: (meleeId: number, leavingPlayerId: number, joiningPlayerId: number) =>
    api<MeleeView>(`${base(meleeId)}/teams/substitute`, { method: 'POST', body: { leavingPlayerId, joiningPlayerId } }),
  generateSchedule: (meleeId: number, confirmLosses: boolean) =>
    api<MeleeView>(`${base(meleeId)}/schedule/generate`, { method: 'POST', body: { confirmLosses } }),
  resumeSchedule: (meleeId: number) => api<MeleeView>(`${base(meleeId)}/schedule/resume`, { method: 'POST' }),
  setWinner: (meleeId: number, matchId: number, winnerTeamId: number | null) =>
    api<MeleeView>(`${base(meleeId)}/schedule/matchups/${matchId}/winner`, { method: 'PUT', body: { winnerTeamId } }),
  assignCourt: (meleeId: number, matchId: number, courtNumber: number) =>
    api<MeleeView>(`${base(meleeId)}/schedule/matchups/${matchId}/court`, { method: 'PUT', body: { courtNumber } }),
  goBack: (meleeId: number) => api<MeleeView>(`${base(meleeId)}/back`, { method: 'POST' }),
  close: (meleeId: number) => api<MeleeView>(`${base(meleeId)}/close`, { method: 'POST' }),
}
