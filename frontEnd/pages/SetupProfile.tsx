
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore, useUIStore } from '../store';
import { mockService } from '../services/mock';
import { LogoIcon } from '../components/Icons';

const SetupProfile: React.FC = () => {
  const navigate = useNavigate();
  const { user, updateUser } = useAuthStore();
  const { addToast } = useUIStore();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    username: '',
    bio: ''
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    
    // Basic validation
    if (formData.username.length < 3) {
      addToast('Username must be at least 3 characters.', 'error');
      return;
    }

    setLoading(true);
    try {
      const updatedUser = await mockService.completeSetup(user.id, formData);
      updateUser(updatedUser);
      addToast('Profile setup complete! Welcome! 🎉', 'success');
      navigate('/feed');
    } catch (err) {
      addToast('Failed to save profile.', 'error');
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
        <h2 className="text-xl font-bold text-slate-900 dark:text-white mb-6">Tell us about yourself</h2>
      </div>

      <div className="mt-2 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white dark:bg-slate-900 py-8 px-6 shadow-xl rounded-[2.5rem] border border-slate-100 dark:border-slate-800">
          
          <div className="text-center mb-8">
            <div className="w-24 h-24 bg-brand-100 dark:bg-slate-800 rounded-full mx-auto flex items-center justify-center text-4xl mb-3 border-4 border-white dark:border-slate-700 shadow-sm">
              😎
            </div>
            <p className="text-xs font-bold text-brand-500 uppercase tracking-widest">Setup your profile</p>
          </div>

          <form className="space-y-6" onSubmit={handleSubmit}>
            <div>
              <label className="block text-xs font-black text-slate-400 uppercase tracking-widest ml-1 mb-2">Display Name</label>
              <input
                type="text"
                required
                value={formData.name}
                onChange={(e) => setFormData({...formData, name: e.target.value})}
                className="block w-full rounded-2xl border-none bg-slate-50 dark:bg-slate-800 px-4 py-3 focus:ring-2 focus:ring-brand-500 text-slate-900 dark:text-white text-base"
                placeholder="e.g. Alex Rivera"
              />
            </div>

            <div>
              <label className="block text-xs font-black text-slate-400 uppercase tracking-widest ml-1 mb-2">Username</label>
              <div className="relative group">
                <span className="absolute inset-y-0 left-0 flex items-center pl-4 text-slate-400 font-bold group-focus-within:text-brand-500 transition-colors">@</span>
                <input
                  type="text"
                  required
                  value={formData.username}
                  onChange={(e) => setFormData({...formData, username: e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '')})}
                  className="block w-full rounded-2xl border-none bg-slate-50 dark:bg-slate-800 pl-8 pr-4 py-3 focus:ring-2 focus:ring-brand-500 text-slate-900 dark:text-white text-base"
                  placeholder="unique_handle"
                />
              </div>
              <p className="mt-1 text-[10px] text-slate-400 font-medium ml-1">Use letters, numbers, and underscores.</p>
            </div>

            <div>
              <label className="block text-xs font-black text-slate-400 uppercase tracking-widest ml-1 mb-2">Short Bio</label>
              <textarea
                value={formData.bio}
                onChange={(e) => setFormData({...formData, bio: e.target.value})}
                className="block w-full rounded-2xl border-none bg-slate-50 dark:bg-slate-800 px-4 py-3 focus:ring-2 focus:ring-brand-500 text-slate-900 dark:text-white text-base resize-none"
                placeholder="What's your vibe?"
                rows={3}
              />
            </div>

            <div>
              <button
                type="submit"
                disabled={loading}
                className="flex w-full justify-center rounded-2xl bg-brand-500 px-4 py-4 text-sm font-black text-white shadow-lg shadow-brand-500/30 hover:bg-brand-600 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 disabled:opacity-50 active:scale-95 transition-all"
              >
                {loading ? 'SAVING...' : 'FINISH SETUP'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default SetupProfile;
