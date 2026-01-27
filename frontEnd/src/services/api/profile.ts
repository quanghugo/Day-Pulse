import api from './client';
import { User } from '@/types';

// Response shape from user-service /users/me endpoints
export interface UserProfileResponse {
  id: string;
  username: string;
  name: string;
  bio?: string;
  avatarUrl?: string;
  timezone?: string;
  language?: string;
  streak?: number;
  isOnline?: boolean;
  lastSeenAt?: string | null;
}

export interface ProfileUpdatePayload {
  username?: string;
  name?: string;
  bio?: string;
  avatarUrl?: string;
  timezone?: string;
  language?: string;
}

export const getMyProfile = async (): Promise<UserProfileResponse> => {
  const { data } = await api.get('/users/me');
  // API Gateway + user-service wrap result in { result: ... }
  return data.result as UserProfileResponse;
};

export const updateMyProfile = async (
  payload: ProfileUpdatePayload,
): Promise<UserProfileResponse> => {
  const { data } = await api.patch('/users/me', payload);
  return data.result as UserProfileResponse;
};

export const setupProfile = async (
  payload: ProfileUpdatePayload & { username: string },
): Promise<UserProfileResponse> => {
  const { data } = await api.post('/users/me/setup', payload);
  return data.result as UserProfileResponse;
};

// Helper to map backend profile to frontend User model
export const mapProfileToUser = (profile: UserProfileResponse): User => {
  return {
    id: profile.id,
    name: profile.name,
    username: profile.username,
    avatar: profile.avatarUrl,
    bio: profile.bio,
    timezone: profile.timezone ?? Intl.DateTimeFormat().resolvedOptions().timeZone,
    language: (profile.language as User['language']) ?? 'en',
    streak: profile.streak ?? 0,
    followersCount: 0,
    followingCount: 0,
    lastUpdated: new Date().toISOString(),
    isOnline: profile.isOnline ?? false,
    isSetupComplete: true,
  };
}

