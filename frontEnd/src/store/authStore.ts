import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { User, AuthTokens } from '@/types';
import { getKeycloakToken, getKeycloakRefreshToken, isKeycloakAuthenticated, getKeycloakUserProfile, getKeycloakRoles, keycloakLogout } from '@/services/keycloakService';

interface AuthState {
  user: User | null;
  tokens: AuthTokens | null;
  isAuthenticated: boolean;
  setAuth: (user: User, tokens: AuthTokens) => void;
  logout: () => void;
  updateUser: (user: Partial<User>) => void;
  syncWithKeycloak: () => Promise<void>;
  setKeycloakAuth: (user: User) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      tokens: null,
      isAuthenticated: false,
      setAuth: (user, tokens) => set({ user, tokens, isAuthenticated: true }),
      logout: () => {
        keycloakLogout();
        set({ user: null, tokens: null, isAuthenticated: false });
      },
      updateUser: (updatedFields) => 
        set((state) => ({ 
          user: state.user ? { ...state.user, ...updatedFields } : null 
        })),
      /**
       * Sync auth store with Keycloak authentication state
       * Called after Keycloak initialization to restore session
       */
      syncWithKeycloak: async () => {
        if (isKeycloakAuthenticated()) {
          const accessToken = getKeycloakToken();
          const refreshToken = getKeycloakRefreshToken();
          
          if (accessToken && refreshToken) {
            try {
              // Get user profile from Keycloak
              const keycloakProfile = await getKeycloakUserProfile();
              const roles = getKeycloakRoles();
              
              if (keycloakProfile) {
                // Map Keycloak profile to our User type
                const user: User = {
                  id: keycloakProfile.id || '',
                  name: `${keycloakProfile.firstName || ''} ${keycloakProfile.lastName || ''}`.trim() || keycloakProfile.username || '',
                  username: keycloakProfile.username || keycloakProfile.email || '',
                  avatar: undefined,
                  bio: undefined,
                  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                  language: 'en',
                  streak: 0,
                  followersCount: 0,
                  followingCount: 0,
                  lastUpdated: new Date().toISOString(),
                  isOnline: true,
                  isSetupComplete: false,
                };
                
                const tokens: AuthTokens = {
                  accessToken,
                  refreshToken,
                };
                
                set({ user, tokens, isAuthenticated: true });
              }
            } catch (error) {
              console.error('Failed to sync with Keycloak:', error);
            }
          }
        }
      },
      /**
       * Set authentication from Keycloak after login/registration
       */
      setKeycloakAuth: async (user: User) => {
        const accessToken = getKeycloakToken();
        const refreshToken = getKeycloakRefreshToken();
        
        if (accessToken && refreshToken) {
          const tokens: AuthTokens = {
            accessToken,
            refreshToken,
          };
          set({ user, tokens, isAuthenticated: true });
        }
      },
    }),
    { name: 'daypulse-auth' }
  )
);
