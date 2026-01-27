import React, { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useUIStore, useAuthStore } from '@/store';
import { Providers } from './providers';
import { AppRoutes } from './routes';
import { initKeycloak } from '@/services/keycloakService';

// Utility to reset scroll position on route change
const ScrollToTop = () => {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);
  return null;
};

const App: React.FC = () => {
  const { theme } = useUIStore();
  const { syncWithKeycloak } = useAuthStore();
  const [keycloakInitialized, setKeycloakInitialized] = useState(false);

  // Initialize Keycloak on app load
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const authenticated = await initKeycloak();
        setKeycloakInitialized(true);
        
        // Sync auth store with Keycloak state if authenticated
        if (authenticated) {
          await syncWithKeycloak();
        }
      } catch (error) {
        console.error('Failed to initialize Keycloak:', error);
        setKeycloakInitialized(true); // Still allow app to render
      }
    };
    
    initializeAuth();
  }, [syncWithKeycloak]);

  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  // Show loading state while Keycloak initializes
  if (!keycloakInitialized) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-950">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-brand-500 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-slate-600 dark:text-slate-400 font-medium">Initializing...</p>
        </div>
      </div>
    );
  }

  return (
    <Providers>
      <ScrollToTop />
      <AppRoutes />
    </Providers>
  );
};

export default App;
