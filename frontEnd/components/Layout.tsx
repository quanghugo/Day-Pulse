import React, { useState, useEffect, useRef } from 'react';
import { Outlet, NavLink, useNavigate, useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuthStore, useUIStore } from '../store';
import { useTranslation } from '../i18n';
import { mockService } from '../services/mock';
import Avatar from './Avatar';
import { 
  HomeIcon, 
  SearchIcon, 
  BellIcon, 
  ChatIcon, 
  LogoIcon, 
  SunIcon, 
  MoonIcon,
  SettingsIcon 
} from './Icons';

const Layout: React.FC = () => {
  const { user, logout } = useAuthStore();
  const { theme, setTheme, language } = useUIStore();
  const t = useTranslation(language);
  const navigate = useNavigate();
  const location = useLocation();

  const [showNav, setShowNav] = useState(true);
  const lastScrollY = useRef(0);

  // Profile Menu State
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const profileMenuRef = useRef<HTMLDivElement>(null);

  // Fetch unread counts
  const { data: chats } = useQuery({ 
    queryKey: ['chats'], 
    queryFn: mockService.getChats,
    staleTime: 60000 // Share cache with ChatList page
  });

  const { data: notifications } = useQuery({ 
    queryKey: ['notifications'], 
    queryFn: mockService.getNotifications,
    staleTime: 60000 // Share cache with Notifications page
  });

  const unreadChatCount = chats?.reduce((acc, chat) => acc + (chat.unreadCount || 0), 0) || 0;
  const unreadNotifCount = notifications?.filter(n => !n.isRead).length || 0;

  const getBadgeCount = (path: string) => {
    if (path === '/chat') return unreadChatCount;
    if (path === '/notifications') return unreadNotifCount;
    return 0;
  };

  const renderBadge = (count: number) => {
    if (count <= 0) return null;
    return (
      <span className="absolute -top-1 -right-1 bg-brand-500 text-white text-[10px] font-bold h-4 min-w-[16px] px-1 rounded-full flex items-center justify-center border-2 border-white dark:border-slate-900 animate-in zoom-in duration-300 z-10">
        {count > 99 ? '99+' : count}
      </span>
    );
  };

  // Detect if we are in a specific chat room (e.g., /chat/c1)
  const isChatRoom = /^\/chat\/.+/.test(location.pathname);

  useEffect(() => {
    const handleScroll = () => {
      const currentScrollY = window.scrollY;
      
      // Always show if near top or if bouncing at top (iOS)
      if (currentScrollY < 10) {
        setShowNav(true);
        lastScrollY.current = currentScrollY;
        return;
      }

      // Determine direction
      // Threshold of 5px to prevent jitter
      if (currentScrollY > lastScrollY.current + 5) {
        // Scrolling down -> hide
        setShowNav(false);
      } else if (currentScrollY < lastScrollY.current - 5) {
        // Scrolling up -> show
        setShowNav(true);
      }
      
      lastScrollY.current = currentScrollY;
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Click outside to close profile menu
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (profileMenuRef.current && !profileMenuRef.current.contains(event.target as Node)) {
        setIsProfileMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = [
    { to: '/feed', Icon: HomeIcon, label: t('feed') },
    { to: '/search', Icon: SearchIcon, label: t('search') },
    { to: '/notifications', Icon: BellIcon, label: t('notifications') },
    { to: '/chat', Icon: ChatIcon, label: t('chat') },
    { to: '/profile', Icon: ({ active }: { active: boolean }) => (
      <div className={`rounded-full border-2 ${active ? 'border-brand-500 dark:border-brand-400' : 'border-transparent'}`}>
        <Avatar src={user?.avatar} name={user?.name} size="sm" />
      </div>
    ), label: t('profile') },
  ];

  return (
    <div className="min-h-[100dvh] flex flex-col bg-slate-50 dark:bg-slate-950">
      {/* Top Navbar - Desktop Only */}
      <header className="hidden md:flex sticky top-0 z-50 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 h-16 items-center px-6 justify-between">
        <div className="flex items-center gap-3 cursor-pointer group" onClick={() => navigate('/feed')}>
          <LogoIcon className="w-8 h-8 text-brand-500 group-hover:scale-110 transition-transform duration-300" />
          <span className="text-2xl font-black text-slate-900 dark:text-white tracking-tighter">DayPulse</span>
        </div>

        <nav className="flex items-center gap-12">
          {navItems.filter(item => item.to !== '/profile').map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => 
                `relative flex items-center justify-center p-2 rounded-xl transition-colors ${
                  isActive 
                    ? 'text-brand-600 dark:text-brand-400 bg-brand-50 dark:bg-brand-500/10' 
                    : 'text-slate-400 hover:text-slate-600 dark:hover:text-slate-200'
                }`
              }
              title={item.label}
            >
              {({ isActive }) => (
                <>
                  <item.Icon active={isActive} className="w-7 h-7" />
                  {renderBadge(getBadgeCount(item.to))}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="flex items-center gap-4">
          <button 
            onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
            className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 dark:text-slate-400 transition-colors"
          >
            {theme === 'light' ? <MoonIcon className="w-6 h-6" /> : <SunIcon className="w-6 h-6" />}
          </button>
          
          {/* Profile Dropdown - Click Activated */}
          <div className="relative py-2" ref={profileMenuRef}>
            <button 
              onClick={() => setIsProfileMenuOpen(!isProfileMenuOpen)}
              className="rounded-full transition-transform active:scale-95 focus:outline-none"
            >
              <Avatar src={user?.avatar} name={user?.name} size="sm" />
            </button>
            
            {isProfileMenuOpen && (
              <div className="absolute right-0 top-full mt-2 w-48 bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800 rounded-2xl shadow-xl py-2 animate-in fade-in zoom-in-95 duration-200 z-50">
                <button 
                  onClick={() => { navigate('/profile'); setIsProfileMenuOpen(false); }} 
                  className="w-full text-left px-4 py-2 hover:bg-slate-50 dark:hover:bg-slate-800 font-medium text-sm text-slate-700 dark:text-slate-200"
                >
                  {t('profile')}
                </button>
                <button 
                  onClick={() => { navigate('/settings'); setIsProfileMenuOpen(false); }} 
                  className="w-full text-left px-4 py-2 hover:bg-slate-50 dark:hover:bg-slate-800 font-medium text-sm text-slate-700 dark:text-slate-200"
                >
                  {t('settings')}
                </button>
                <div className="h-px bg-slate-100 dark:bg-slate-800 my-1"></div>
                <button 
                  onClick={handleLogout} 
                  className="w-full text-left px-4 py-2 hover:bg-slate-50 dark:hover:bg-slate-800 text-red-500 font-medium text-sm"
                >
                  {t('logout')}
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Mobile Top Header - Hidden in ChatRoom - Fixed Position ALWAYS VISIBLE */}
      {!isChatRoom && (
        <header className="md:hidden flex fixed top-0 left-0 right-0 z-50 bg-white/95 dark:bg-slate-900/95 backdrop-blur-sm border-b border-slate-200 dark:border-slate-800 h-14 items-center px-4 justify-between transition-transform duration-300 translate-y-0">
          <div className="flex items-center gap-2" onClick={() => navigate('/feed')}>
            <LogoIcon className="w-8 h-8 text-brand-500" />
            <span className="text-xl font-black text-slate-900 dark:text-white tracking-tighter">DayPulse</span>
          </div>
          <div className="flex items-center gap-1">
            <button 
              onClick={() => navigate('/settings')}
              className="p-2 text-slate-500 dark:text-slate-400"
            >
              <SettingsIcon className="w-6 h-6" />
            </button>
            <button 
              onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
              className="p-2 text-slate-500 dark:text-slate-400"
            >
              {theme === 'light' ? <MoonIcon className="w-6 h-6" /> : <SunIcon className="w-6 h-6" />}
            </button>
          </div>
        </header>
      )}

      {/* Main Content Area */}
      {/* pt-14 added for mobile because header is fixed */}
      <main className={`flex-1 flex flex-col ${isChatRoom ? 'h-[100dvh]' : 'pt-14 pb-16 md:pb-0 md:pt-0'}`}>
        <div className={`
          flex-1 mx-auto w-full
          ${isChatRoom 
            ? 'max-w-none md:max-w-5xl md:px-6 md:py-4 px-0 py-0' 
            : 'max-w-7xl px-4 md:px-6 py-6'
          }
        `}>
          <Outlet />
        </div>
      </main>

      {/* Bottom Nav - Mobile Only - Hidden in ChatRoom - Scroll Hide */}
      {!isChatRoom && (
        <nav className={`md:hidden fixed bottom-0 left-0 right-0 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md border-t border-slate-200 dark:border-slate-800 h-16 flex items-center justify-around z-50 shadow-[0_-4px_20px_-5px_rgba(0,0,0,0.1)] transition-transform duration-300 ${showNav ? 'translate-y-0' : 'translate-y-full'}`}>
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => 
                `flex flex-col items-center justify-center w-full h-full transition-all ${
                  isActive 
                    ? 'text-brand-500 dark:text-brand-400' 
                    : 'text-slate-400 dark:text-slate-500'
                }`
              }
            >
              {({ isActive }) => (
                <span className={`relative ${item.to === '/profile' ? '' : 'p-1'}`}>
                  <item.Icon active={isActive} className="w-7 h-7" />
                  {item.to !== '/profile' && renderBadge(getBadgeCount(item.to))}
                </span>
              )}
            </NavLink>
          ))}
        </nav>
      )}
    </div>
  );
};

export default Layout;