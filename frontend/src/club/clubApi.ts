import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'

export interface ScoringTable {
  pointingOut: number
  pointingBigCircle: number
  pointingSmallCircle: number
  pointingNearJack: number
  pointingOnJack: number
  shootingMiss: number
  shootingHit: number
  shootingHitOut: number
  shootingCarreau: number
}

export interface ClubProfile {
  name: string
  logoUrl: string | null
  courtCount: number
  roundsCount: number
  prizeCount: number
  scoring: ScoringTable
}

export type ClubProfileUpdate = Omit<ClubProfile, 'logoUrl'>

const CLUB_KEY = ['club'] as const

export function useClubProfile() {
  return useQuery({ queryKey: CLUB_KEY, queryFn: () => api<ClubProfile>('/api/club') })
}

export function useUpdateClubProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (update: ClubProfileUpdate) => api<ClubProfile>('/api/club', { method: 'PUT', body: update }),
    onSuccess: (profile) => queryClient.setQueryData(CLUB_KEY, profile),
  })
}

export function useUploadLogo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (image: Blob) => {
      const form = new FormData()
      form.append('file', image, image.type === 'image/png' ? 'logo.png' : 'logo.jpg')
      return api<ClubProfile>('/api/club/logo', { method: 'POST', body: form })
    },
    onSuccess: (profile) => queryClient.setQueryData(CLUB_KEY, profile),
  })
}

export function useRemoveLogo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api<ClubProfile>('/api/club/logo', { method: 'DELETE' }),
    onSuccess: (profile) => queryClient.setQueryData(CLUB_KEY, profile),
  })
}
