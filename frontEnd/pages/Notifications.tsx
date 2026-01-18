import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { mockService } from '../services/mock';
import { useUIStore } from '../store';
import { useTranslation } from '../i18n';
import Avatar from '../components/Avatar';
import { Notification } from '../types';

const Notifications: React.FC = () => {
  const { language, addToast } = useUIStore();
  const t = useTranslation(language);
  const navigate = useNavigate();

  const { data: notifications, isLoading } = useQuery<Notification[]>({
    queryKey: ['notifications'],
    queryFn: mockService.getNotifications,
  });

  const handleFollowBack = (e: React.MouseEvent, username: string) => {
    e.stopPropagation();
    addToast(`Followed back @${username}`, 'success');
  };

  // Group notifications by time
  const groupedNotifications: Record<string, Notification[]> = useMemo(() => {
    if (!notifications) return {};

    const groups: Record<string, Notification[]> = {
      'New': [],
      'Today': [],
      'Yesterday': [],
      'This Week': [],
      'Earlier': []
    };

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const lastWeek = new Date(today);
    lastWeek.setDate(lastWeek.getDate() - 7);

    notifications.forEach(notif => {
      const date = new Date(notif.createdAt);
      if (!notif.isRead) {
        groups['New'].push(notif);
      } else if (date >= today) {
        groups['Today'].push(notif);
      } else if (date >= yesterday) {
        groups['Yesterday'].push(notif);
      } else if (date >= lastWeek) {
        groups['This Week'].push(notif);
      } else {
        groups['Earlier'].push(notif);
      }
    });

    // Remove empty groups
    Object.keys(groups).forEach(key => {
      if (groups[key].length === 0) delete groups[key];
    });

    return groups;
  }, [notifications]);

  return (
    <div className="max-w-2xl mx-auto min-h-[80vh] -mt-2">
      <div className="animate-in fade-in duration-500 pb-20 pt-2">
        {isLoading ? (
          <div className="pt-2">
            {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map(i => (
              <div key={i} className="flex items-center justify-between px-4 py-4">
                 <div className="flex items-center gap-4 flex-1 min-w-0 pr-4">
                   <div className="w-12 h-12 rounded-full bg-slate-200 dark:bg-slate-800 animate-pulse flex-shrink-0"></div>
                   <div className="space-y-2 flex-1">
                     <div className={`h-4 bg-slate-200 dark:bg-slate-800 rounded-full animate-pulse ${i % 2 === 0 ? 'w-32' : 'w-48'}`}></div>
                     <div className={`h-3 bg-slate-100 dark:bg-slate-800/50 rounded-full animate-pulse ${i % 3 === 0 ? 'w-20' : 'w-24'}`}></div>
                   </div>
                 </div>
                 <div className={`h-9 rounded-xl bg-slate-200 dark:bg-slate-800 animate-pulse flex-shrink-0 ${i % 2 === 0 ? 'w-16' : 'w-10'}`}></div>
              </div>
            ))}
          </div>
        ) : Object.keys(groupedNotifications).length > 0 ? (
          Object.entries(groupedNotifications).map(([label, group]) => (
            <div key={label}>
              <h3 className="sticky top-14 md:top-16 z-20 px-4 py-3 text-sm font-black uppercase tracking-widest text-slate-400 bg-slate-50/95 dark:bg-slate-950/95 backdrop-blur-sm">
                {label}
              </h3>
              <div className="divide-y divide-slate-100 dark:divide-slate-800/50">
                {group.map(notif => (
                  <div 
                    key={notif.id} 
                    onClick={() => {
                        if (notif.type === 'follow') navigate('/profile');
                        else navigate('/feed'); 
                    }}
                    className={`flex items-center justify-between px-4 py-4 hover:bg-white dark:hover:bg-slate-900 transition-colors cursor-pointer group ${!notif.isRead ? 'bg-brand-50/30 dark:bg-brand-500/5' : ''}`}
                  >
                    <div className="flex items-center gap-4 flex-1 min-w-0 pr-4">
                       <div className="relative">
                         <Avatar src={notif.actor.avatar} name={notif.actor.name} size="lg" />
                         <div className={`absolute -bottom-1 -right-1 w-6 h-6 rounded-full border-2 border-white dark:border-slate-900 flex items-center justify-center text-[11px] ${
                            notif.type === 'like' ? 'bg-pink-500 text-white' :
                            notif.type === 'comment' ? 'bg-blue-500 text-white' :
                            'bg-brand-500 text-white'
                         }`}>
                           {notif.type === 'like' ? '❤️' : notif.type === 'comment' ? '💬' : '👤'}
                         </div>
                       </div>
                       <div className="flex-1 min-w-0">
                         <p className="text-base text-slate-900 dark:text-white leading-snug">
                           <span className="font-bold">{notif.actor.name}</span>{' '}
                           <span className="text-slate-600 dark:text-slate-300">
                             {notif.type === 'like' ? t('liked_pulse') :
                              notif.type === 'comment' ? t('commented_pulse') + `"${notif.content}"` :
                              t('followed_you')}
                           </span>
                         </p>
                         <p className="text-sm text-slate-400 font-medium mt-1">
                           {new Date(notif.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                         </p>
                       </div>
                    </div>
                    
                    {notif.type === 'follow' ? (
                      <button 
                        onClick={(e) => handleFollowBack(e, notif.actor.username)}
                        className="px-5 py-2 rounded-xl bg-brand-500 text-white text-sm font-bold hover:bg-brand-600 transition-colors shadow-sm active:scale-95"
                      >
                        Follow Back
                      </button>
                    ) : (
                       <div className="w-12 h-12 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-xl text-slate-400">
                         📝
                       </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))
        ) : (
          <div className="py-20 text-center">
             <span className="text-7xl block mb-6 grayscale opacity-30">🔔</span>
             <p className="text-slate-500 dark:text-slate-400 font-bold text-lg">{t('no_notifications')}</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Notifications;