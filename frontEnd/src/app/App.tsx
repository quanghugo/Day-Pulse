import React, { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useUIStore } from '@/store';
import { Providers } from './providers';
import { AppRoutes } from './routes';

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

  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  return (
    <Providers>
      <ScrollToTop />
      <AppRoutes />
    </Providers>
  );
};

export default App;
