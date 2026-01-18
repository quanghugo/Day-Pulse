import React, { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store';
import { Layout } from '@/components/layout';

// Lazy load all pages
const Feed = lazy(() => import('@/features/feed/pages/Feed'));
const Search = lazy(() => import('@/features/search/pages/Search'));
const ChatList = lazy(() => import('@/features/chat/pages/ChatList'));
const ChatRoom = lazy(() => import('@/features/chat/pages/ChatRoom'));
const Notifications = lazy(() => import('@/features/notifications/pages/Notifications'));
const Profile = lazy(() => import('@/features/profile/pages/Profile'));
const EditProfile = lazy(() => import('@/features/profile/pages/EditProfile'));
const Settings = lazy(() => import('@/features/settings/pages/Settings'));
const Login = lazy(() => import('@/features/auth/pages/Login'));
const Register = lazy(() => import('@/features/auth/pages/Register'));
const ForgotPassword = lazy(() => import('@/features/auth/pages/ForgotPassword'));
const VerifyOTP = lazy(() => import('@/features/auth/pages/VerifyOTP'));
const SetupProfile = lazy(() => import('@/features/auth/pages/SetupProfile'));

// Loading fallback component
const PageLoader = () => (
  <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-slate-950">
    <div className="flex flex-col items-center gap-4">
      <div className="w-12 h-12 border-4 border-brand-500 border-t-transparent rounded-full animate-spin"></div>
      <p className="text-slate-600 dark:text-slate-400 font-medium">Loading...</p>
    </div>
  </div>
);

// Protects main app routes: Requires Auth AND Completed Setup
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, user } = useAuthStore();
  
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (user && !user.isSetupComplete) return <Navigate to="/setup-profile" replace />;
  
  return <>{children}</>;
};

// Protects setup route: Requires Auth but Incomplete Setup
const SetupRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, user } = useAuthStore();
  
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (user && user.isSetupComplete) return <Navigate to="/feed" replace />;
  
  return <>{children}</>;
};

export const AppRoutes: React.FC = () => {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/verify-otp" element={<VerifyOTP />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        
        {/* Setup Profile Route - specific protection logic */}
        <Route path="/setup-profile" element={
          <SetupRoute>
            <SetupProfile />
          </SetupRoute>
        } />
        
        <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route index element={<Navigate to="/feed" replace />} />
          <Route path="feed" element={<Feed />} />
          <Route path="search" element={<Search />} />
          <Route path="notifications" element={<Notifications />} />
          <Route path="chat" element={<ChatList />} />
          <Route path="chat/:id" element={<ChatRoom />} />
          <Route path="profile/:id?" element={<Profile />} />
          <Route path="profile/edit" element={<EditProfile />} />
          <Route path="settings" element={<Settings />} />
        </Route>
      </Routes>
    </Suspense>
  );
};
