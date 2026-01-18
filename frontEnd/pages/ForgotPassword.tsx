import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { mockService } from '../services/mock';
import { useUIStore } from '../store';
import { LogoIcon } from '../components/Icons';

const ForgotPassword: React.FC = () => {
  const navigate = useNavigate();
  const { addToast } = useUIStore();
  const [loading, setLoading] = useState(false);
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await mockService.requestPasswordReset(email);
      setSent(true);
      addToast('Reset link sent to your email!', 'success');
    } catch (err) {
      addToast('Failed to send reset link', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex flex-col justify-center px-6 py-12 animate-in fade-in duration-500">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="flex justify-center mb-6" onClick={() => navigate('/login')}>
          <LogoIcon className="w-16 h-16 text-brand-500 cursor-pointer" />
        </div>
        <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-6">Reset your password</h2>
      </div>

      <div className="mt-2 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white dark:bg-slate-900 py-8 px-6 shadow-xl rounded-[2.5rem] border border-slate-100 dark:border-slate-800">
          
          {!sent ? (
            <form className="space-y-6" onSubmit={handleSubmit}>
              <div className="text-center mb-6">
                <div className="w-16 h-16 bg-brand-50 dark:bg-brand-500/10 rounded-full flex items-center justify-center text-3xl mx-auto mb-4">
                  🔑
                </div>
                <p className="text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
                  Enter your email address and we'll send you a link to reset your password.
                </p>
              </div>

              <div>
                <label className="block text-xs font-black text-slate-400 uppercase tracking-widest ml-1 mb-2">Email Address</label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="block w-full rounded-2xl border-none bg-slate-50 dark:bg-slate-800 px-4 py-3 focus:ring-2 focus:ring-brand-500 text-slate-900 dark:text-white text-base"
                  placeholder="name@example.com"
                />
              </div>

              <div>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex w-full justify-center rounded-2xl bg-brand-500 px-4 py-4 text-sm font-black text-white shadow-lg shadow-brand-500/30 hover:bg-brand-600 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 disabled:opacity-50 active:scale-95 transition-all"
                >
                  {loading ? 'SENDING LINK...' : 'SEND RESET LINK'}
                </button>
              </div>
            </form>
          ) : (
            <div className="text-center py-4">
               <div className="w-20 h-20 bg-green-50 dark:bg-green-500/10 text-green-500 rounded-full flex items-center justify-center text-4xl mx-auto mb-6 animate-in zoom-in duration-300">
                  ✨
               </div>
               <h3 className="text-xl font-black text-slate-900 dark:text-white mb-2">Check your inbox</h3>
               <p className="text-slate-500 dark:text-slate-400 mb-8">
                 We've sent a password reset link to <br/> <span className="font-bold text-slate-900 dark:text-white">{email}</span>
               </p>
               <button
                  onClick={() => navigate('/login')}
                  className="w-full rounded-2xl bg-slate-100 dark:bg-slate-800 px-4 py-4 text-sm font-black text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 transition-all"
                >
                  BACK TO SIGN IN
                </button>
            </div>
          )}

          {!sent && (
            <div className="mt-8 text-center">
               <button onClick={() => navigate('/login')} className="text-sm font-bold text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors">
                 ← Back to Sign In
               </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;