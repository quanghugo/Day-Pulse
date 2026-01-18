
import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore, useUIStore } from '../store';
import { mockService } from '../services/mock';
import { LogoIcon } from '../components/Icons';

const VerifyOTP: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { setAuth } = useAuthStore();
  const { addToast } = useUIStore();
  
  // Get email from previous navigation state, or fallback
  const email = location.state?.email || 'your email';
  
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      // Mock OTP is '123456'
      const data = await mockService.verifyOtp(email, otp);
      setAuth(data.user, data.tokens);
      addToast('Email verified successfully! ✅', 'success');
      // Navigate to Setup Profile
      navigate('/setup-profile');
    } catch (err) {
      addToast('Invalid verification code. Try 123456.', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex flex-col justify-center px-6 py-12 animate-in fade-in duration-500">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="flex justify-center mb-6">
          <LogoIcon className="w-16 h-16 text-brand-500" />
        </div>
        <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-6">Check your inbox</h2>
      </div>

      <div className="mt-2 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white dark:bg-slate-900 py-8 px-6 shadow-xl rounded-[2.5rem] border border-slate-100 dark:border-slate-800 text-center">
          
          <div className="w-16 h-16 bg-brand-50 dark:bg-brand-500/10 rounded-full flex items-center justify-center text-3xl mx-auto mb-4">
            ✉️
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mb-8">
            We've sent a 6-digit code to <br/> <span className="font-bold text-slate-900 dark:text-white">{email}</span>
          </p>

          <form className="space-y-6" onSubmit={handleVerify}>
            <div>
              <input
                type="text"
                required
                maxLength={6}
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/[^0-9]/g, ''))}
                className="block w-full text-center tracking-[1em] font-black text-2xl rounded-2xl border-none bg-slate-50 dark:bg-slate-800 px-4 py-4 focus:ring-2 focus:ring-brand-500 text-slate-900 dark:text-white placeholder:tracking-normal"
                placeholder="000000"
              />
            </div>

            <div>
              <button
                type="submit"
                disabled={loading || otp.length < 6}
                className="flex w-full justify-center rounded-2xl bg-brand-500 px-4 py-4 text-sm font-black text-white shadow-lg shadow-brand-500/30 hover:bg-brand-600 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 disabled:opacity-50 active:scale-95 transition-all"
              >
                {loading ? 'VERIFYING...' : 'VERIFY CODE'}
              </button>
            </div>
          </form>

          <div className="mt-8">
             <button className="text-sm text-brand-500 font-bold hover:underline">Resend Code</button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VerifyOTP;
