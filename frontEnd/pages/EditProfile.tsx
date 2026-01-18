
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore, useUIStore } from '../store';
import { useTranslation } from '../i18n';
import { mockService } from '../services/mock';
import { BackIcon } from '../components/Icons';
import Avatar from '../components/Avatar';

const EditProfile: React.FC = () => {
  const navigate = useNavigate();
  const { user, updateUser } = useAuthStore();
  const { language, addToast } = useUIStore();
  const t = useTranslation(language);

  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    username: '',
    bio: '',
    timezone: 'UTC+7',
  });

  useEffect(() => {
    if (user) {
      setFormData({
        name: user.name,
        username: user.username,
        bio: user.bio || '',
        timezone: user.timezone,
      });
    } else {
        navigate('/login');
    }
  }, [user, navigate]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    
    if (formData.username.length < 3) {
      addToast('Username must be at least 3 characters', 'error');
      return;
    }

    setLoading(true);
    try {
      // Reusing mock service's setup method as it handles user data persistence in mock
      const updatedUser = await mockService.completeSetup(user.id, formData);
      updateUser(updatedUser);
      addToast('Profile updated successfully! ✨', 'success');
      navigate('/profile');
    } catch (err) {
      addToast('Failed to save profile changes.', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-8 pb-10 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex items-center gap-4 px-2">
        <button 
          onClick={() => navigate(-1)} 
          className="p-2 -ml-2 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors text-slate-600 dark:text-slate-300"
        >
          <BackIcon className="w-6 h-6" />
        </button>
        <h1 className="text-2xl font-black text-slate-900 dark:text-white">
          {t('edit_profile')}
        </h1>
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] p-8 shadow-sm border border-slate-100 dark:border-slate-800/50">
        
        {/* Avatar Selection Section */}
        <div className="flex flex-col items-center mb-10">
          <div className="relative group">
            <Avatar src={user?.avatar} name={user?.name} size="xl" />
            <div className="absolute inset-0 bg-black/40 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
               <span className="text-white font-bold text-xs">Change</span>
            </div>
            <button className="absolute bottom-1 right-1 bg-brand-500 text-white p-2 rounded-full border-4 border-white dark:border-slate-900 shadow-lg active:scale-90 transition-transform">
              <span className="text-xs">📸</span>
            </button>
          </div>
          <p className="mt-4 text-[10px] font-black uppercase text-slate-400 tracking-widest">Update your pulse avatar</p>
        </div>

        <form className="space-y-6" onSubmit={handleSave}>
          <div>
            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 mb-2 block ml-1">Display Name</label>
            <input 
              required
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="w-full bg-slate-50 dark:bg-slate-800/50 border-none rounded-2xl px-5 py-4 focus:ring-2 focus:ring-brand-500 text-base text-slate-900 dark:text-white shadow-inner"
              placeholder="Your full name"
            />
          </div>

          <div>
            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 mb-2 block ml-1">Username</label>
            <div className="relative">
              <span className="absolute left-5 top-1/2 -translate-y-1/2 text-slate-400 font-bold">@</span>
              <input 
                required
                value={formData.username}
                onChange={(e) => setFormData({ ...formData, username: e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '') })}
                className="w-full bg-slate-50 dark:bg-slate-800/50 border-none rounded-2xl pl-10 pr-5 py-4 focus:ring-2 focus:ring-brand-500 text-base text-slate-900 dark:text-white shadow-inner"
                placeholder="unique_handle"
              />
            </div>
          </div>

          <div>
            <label className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 mb-2 block ml-1">Bio</label>
            <textarea 
              value={formData.bio}
              onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
              className="w-full bg-slate-50 dark:bg-slate-800/50 border-none rounded-2xl px-5 py-4 focus:ring-2 focus:ring-brand-500 text-base text-slate-900 dark:text-white shadow-inner resize-none"
              rows={4}
              placeholder="Tell the world your pulse..."
            />
          </div>

          <div className="pt-4 flex flex-col sm:flex-row gap-4">
            <button 
              type="submit"
              disabled={loading}
              className="flex-1 bg-brand-500 text-white py-4 rounded-2xl font-black shadow-lg shadow-brand-500/25 active:scale-95 transition-transform disabled:opacity-50"
            >
              {loading ? 'SAVING...' : t('save')}
            </button>
            <button 
              type="button"
              onClick={() => navigate(-1)}
              className="flex-1 bg-slate-100 dark:bg-slate-800 py-4 rounded-2xl font-black text-slate-600 dark:text-slate-400 active:scale-95 transition-transform"
            >
              {t('cancel')}
            </button>
          </div>
        </form>
      </div>

      {/* Account Preferences (Simulated) */}
      <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] p-8 shadow-sm border border-slate-100 dark:border-slate-800/50">
        <h3 className="text-sm font-black uppercase tracking-widest text-slate-400 mb-6">Account Sync</h3>
        <div className="space-y-4">
          <div className="flex items-center justify-between p-4 bg-slate-50 dark:bg-slate-800/30 rounded-2xl">
            <div className="flex items-center gap-3">
              <span className="text-xl">🌐</span>
              <div className="flex flex-col">
                <span className="text-sm font-black text-slate-900 dark:text-white">Timezone</span>
                <span className="text-xs text-slate-500">{formData.timezone}</span>
              </div>
            </div>
            <button className="text-xs font-bold text-brand-500">Update</button>
          </div>
          <div className="flex items-center justify-between p-4 bg-slate-50 dark:bg-slate-800/30 rounded-2xl">
            <div className="flex items-center gap-3">
              <span className="text-xl">🔐</span>
              <div className="flex flex-col">
                <span className="text-sm font-black text-slate-900 dark:text-white">Two-Factor Auth</span>
                <span className="text-xs text-slate-500">Disabled</span>
              </div>
            </div>
            <button className="text-xs font-bold text-brand-500">Enable</button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EditProfile;
