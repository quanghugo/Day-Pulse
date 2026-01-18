export interface User {
  id: string;
  name: string;
  username: string;
  avatar?: string;
  bio?: string;
  timezone: string;
  language: 'en' | 'vi';
  streak: number;
  followersCount: number;
  followingCount: number;
  lastUpdated: string;
  isOnline: boolean;
  isSetupComplete?: boolean;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}
