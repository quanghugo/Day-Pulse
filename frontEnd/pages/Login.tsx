import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store';
import { mockService } from '../services/mock';
import { LogoIcon } from '../components/Icons';

const Login: React.FC = () => {
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const data = await mockService.login();
      setAuth(data.user, data.tokens);
      navigate('/feed');
    } catch (err) {
      alert('Login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    // Mock Google Login
    setLoading(true);
    setTimeout(() => {
      handleLogin({ preventDefault: () => {} } as any);
    }, 1000);
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex flex-col justify-center px-6 py-12 animate-in fade-in duration-500">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="flex justify-center mb-6">
          <LogoIcon className="w-16 h-16 text-brand-500" />
        </div>
        <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-8">Ready to sync your day?</h2>
      </div>

      <div className="mt-4 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white dark:bg-slate-900 py-8 px-6 shadow-xl rounded-[2.5rem] border border-slate-100 dark:border-slate-800">
          
          <button
            onClick={handleGoogleLogin}
            className="w-full flex items-center justify-center gap-3 bg-white dark:bg-slate-800 border-2 border-slate-100 dark:border-slate-700 rounded-2xl p-4 text-slate-700 dark:text-white font-bold transition-all hover:bg-slate-50 dark:hover:bg-slate-700 active:scale-95 mb-6"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
            </svg>
            Sign in with Google
          </button>

          <div className="relative mb-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-slate-200 dark:border-slate-700"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="bg-white dark:bg-slate-900 px-4 text-slate-400 font-bold uppercase tracking-wider text-[10px]">Or sign in with email</span>
            </div>
          </div>

          <form className="space-y-6" onSubmit={handleLogin}>
            <div>
              <label className="block text-xs font-black text-slate-400 uppercase tracking-widest ml-1 mb-2">Username or Email</label>
              <input
                type="text"
                defaultValue="arivera"
                required
                className="block w-full rounded-2xl border-none bg-slate-50 dark:bg-slate-800 px-4 py-3 focus:ring-2 focus:ring-brand-500 text-slate-900 dark:text-white text-base"
              />
            </div>

            <div>
              <div className="flex justify-between items-center mb-2">
                <label className="block text-xs font-black text-slate-400 uppercase tracking-widest ml-1">Password</label>
                <button type="button" onClick={() => navigate('/forgot-password')} className="text-xs font-bold text-brand-500 hover:text-brand-600">Forgot?</button>
              </div>
              <input
                type="password"
                defaultValue="password"
                required
                className="block w-full rounded-2xl border-none bg-slate-50 dark:bg-slate-800 px-4 py-3 focus:ring-2 focus:ring-brand-500 text-slate-900 dark:text-white text-base"
              />
            </div>

            <div>
              <button
                type="submit"
                disabled={loading}
                className="flex w-full justify-center rounded-2xl bg-brand-500 px-4 py-4 text-sm font-black text-white shadow-lg shadow-brand-500/30 hover:bg-brand-600 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 disabled:opacity-50 active:scale-95 transition-all"
              >
                {loading ? 'SYNCING...' : 'SIGN IN'}
              </button>
            </div>
          </form>

          <div className="mt-8 text-center">
             <p className="text-sm text-slate-500">
               New here? <button onClick={() => navigate('/register')} className="text-brand-500 font-bold hover:underline">Create an account</button>
             </p>
          </div>
        </div>
        
        <div className="mt-8 text-center text-xs text-slate-400 font-medium">
          <p>© 2024 DayPulse Network. Privacy & Terms.</p>
        </div>
      </div>
    </div>
  );
};

export default Login;