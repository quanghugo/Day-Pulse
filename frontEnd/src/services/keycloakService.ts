import Keycloak from 'keycloak-js';

/**
 * Keycloak instance configured for DayPulse frontend
 */
const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'daypulse',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'daypulse-frontend',
});

/**
 * Initialize Keycloak
 * @returns Promise that resolves to authentication status
 */
export const initKeycloak = async (): Promise<boolean> => {
  try {
    const authenticated = await keycloak.init({
      onLoad: 'check-sso', // Check if user is already logged in
      checkLoginIframe: false, // Disable iframe check for simpler setup
      pkceMethod: 'S256', // Use PKCE for enhanced security
    });
    
    console.log('Keycloak initialized. Authenticated:', authenticated);
    
    // Set up token refresh
    if (authenticated) {
      // Refresh token every 30 seconds before expiration
      setInterval(() => {
        keycloak.updateToken(30).catch(() => {
          console.error('Failed to refresh token');
        });
      }, 10000); // Check every 10 seconds
    }
    
    return authenticated;
  } catch (error) {
    console.error('Failed to initialize Keycloak:', error);
    return false;
  }
};

/**
 * Login with Keycloak
 * Redirects to Keycloak login page
 */
export const keycloakLogin = () => {
  keycloak.login({
    redirectUri: window.location.origin + '/feed',
  });
};

/**
 * Login with Google OAuth via Keycloak
 * Uses idpHint to redirect directly to Google
 */
export const keycloakLoginGoogle = () => {
  keycloak.login({
    idpHint: 'google', // Direct to Google OAuth
    redirectUri: window.location.origin + '/feed',
  });
};

/**
 * Register new user with Keycloak
 * Redirects to Keycloak registration page
 */
export const keycloakRegister = () => {
  keycloak.register({
    redirectUri: window.location.origin + '/feed',
  });
};

/**
 * Logout from Keycloak
 * Clears session and redirects to login page
 */
export const keycloakLogout = () => {
  keycloak.logout({
    redirectUri: window.location.origin + '/login',
  });
};

/**
 * Get current access token
 */
export const getKeycloakToken = (): string | undefined => {
  return keycloak.token;
};

/**
 * Get current refresh token
 */
export const getKeycloakRefreshToken = (): string | undefined => {
  return keycloak.refreshToken;
};

/**
 * Check if user is authenticated
 */
export const isKeycloakAuthenticated = (): boolean => {
  return keycloak.authenticated || false;
};

/**
 * Get user profile from Keycloak
 */
export const getKeycloakUserProfile = async () => {
  if (keycloak.authenticated) {
    try {
      const profile = await keycloak.loadUserProfile();
      return profile;
    } catch (error) {
      console.error('Failed to load user profile:', error);
      return null;
    }
  }
  return null;
};

/**
 * Update token if it's about to expire
 * @param minValidity - Minimum validity in seconds (default: 30)
 */
export const updateKeycloakToken = async (minValidity: number = 30): Promise<boolean> => {
  try {
    const refreshed = await keycloak.updateToken(minValidity);
    return refreshed;
  } catch (error) {
    console.error('Failed to update token:', error);
    return false;
  }
};

/**
 * Get user roles from token
 */
export const getKeycloakRoles = (): string[] => {
  if (keycloak.tokenParsed && keycloak.tokenParsed.realm_access) {
    return keycloak.tokenParsed.realm_access.roles || [];
  }
  return [];
};

/**
 * Check if user has specific role
 */
export const hasKeycloakRole = (role: string): boolean => {
  return keycloak.hasRealmRole(role);
};

export default keycloak;
