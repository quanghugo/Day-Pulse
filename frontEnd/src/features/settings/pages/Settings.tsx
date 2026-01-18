import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useUIStore, useAuthStore } from '@/store';
import { useTranslation } from '@/i18n';
import { SunIcon, MoonIcon, LogoutIcon } from '@/components/icons';

const Settings: React.FC = () => {
  const { theme, setTheme, language, setLanguage } = useUIStore();
  const { logout } = useAuthStore();
  const t = useTranslation(language);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const sections = [
    {
      title: t('theme'),
      options: [
        { label: t('light'), value: 'light', Icon: SunIcon },
        { label: t('dark'), value: 'dark', Icon: MoonIcon },
      ],
      current: theme,
      onChange: setTheme,
    },
    {
      title: t('language'),
      options: [
        { label: 'English', value: 'en', icon: '🇺🇸' },
        { label: 'Tiếng Việt', value: 'vi', icon: '🇻🇳' },
      ],
      current: language,
      onChange: setLanguage,
    },
  ];

  return (
    <div className="max-w-2xl mx-auto space-y-8 pb-8">
      <h1 className="text-2xl font-black text-slate-900 dark:text-white">{t('settings')}</h1>

      {sections.map((section) => (
        <div key={section.title} className="bg-white dark:bg-slate-900 rounded-[2rem] p-6 shadow-sm border border-slate-100 dark:border-slate-800">
          <h2 className="text-sm font-black uppercase tracking-wider text-slate-400 mb-4">{section.title}</h2>
          <div className="grid grid-cols-2 gap-4">
            {section.options.map((opt) => (
              <button
                key={opt.value}
                onClick={() => section.onChange(opt.value as any)}
                className={`p-4 rounded-2xl flex flex-col items-center gap-2 border-2 transition-all ${
                  section.current === opt.value 
                    ? 'border-brand-500 bg-brand-50 dark:bg-brand-500/10 text-brand-600 dark:text-brand-400' 
                    : 'border-transparent bg-slate-50 dark:bg-slate-800 hover:border-slate-200 dark:hover:border-slate-700 text-slate-600 dark:text-slate-300'
                }`}
              >
                {opt.Icon ? <opt.Icon className="w-8 h-8" /> : <span className="text-2xl">{opt.icon}</span>}
                <span className="font-bold text-sm">{opt.label}</span>
              </button>
            ))}
          </div>
        </div>
      ))}

      <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-6 shadow-sm border border-slate-100 dark:border-slate-800">
         <h2 className="text-sm font-black uppercase tracking-wider text-slate-400 mb-4">Notifications</h2>
         <div className="space-y-4">
            <div className="flex justify-between items-center text-slate-900 dark:text-white">
              <span className="font-bold">Status Reminders</span>
              <button className="w-12 h-6 bg-brand-500 rounded-full flex items-center justify-end px-1 transition-colors">
                <div className="w-4 h-4 bg-white rounded-full shadow-sm"></div>
              </button>
            </div>
            <div className="flex justify-between items-center text-slate-900 dark:text-white">
              <span className="font-bold">Chat Messages</span>
              <button className="w-12 h-6 bg-brand-500 rounded-full flex items-center justify-end px-1 transition-colors">
                <div className="w-4 h-4 bg-white rounded-full shadow-sm"></div>
              </button>
            </div>
         </div>
      </div>

      <div className="pt-4">
        <button 
          onClick={handleLogout}
          className="w-full bg-red-50 dark:bg-red-500/10 text-red-500 border-2 border-transparent hover:border-red-200 dark:hover:border-red-500/30 p-4 rounded-[2rem] font-black uppercase tracking-widest transition-all active:scale-95 flex items-center justify-center gap-2"
        >
          <LogoutIcon className="w-5 h-5" />
          {t('logout')}
        </button>
        <p className="text-center text-xs text-slate-400 font-bold mt-4 uppercase tracking-widest">
          DayPulse v1.0.2
        </p>
      </div>
    </div>
  );
};

export default Settings;