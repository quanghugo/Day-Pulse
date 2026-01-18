
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthStore, useUIStore } from '@/store';
import { useTranslation } from '@/i18n';
import { mockService } from '@/services/mock';
import Avatar from '@/components/ui/Avatar';
import { StatusCard } from '@/features/feed/components';
import { UserListModal } from '@/components/modals';
import { User } from '@/types';

const Profile: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user: currentUser } = useAuthStore();
  const { language, addToast } = useUIStore();
  const t = useTranslation(language);

  const isOwnProfile = !id || id === currentUser?.id;
  const targetId = isOwnProfile ? currentUser?.id : id;

  const { data: profileUser, isLoading: isLoadingUser, isError: isUserError } = useQuery({
    queryKey: ['user-profile', targetId],
    queryFn: () => targetId ? mockService.getUserById(targetId) : Promise.resolve(null),
    enabled: !!targetId,
  });

  const [isFollowing, setIsFollowing] = useState(false);
  const [userListConfig, setUserListConfig] = useState<{ open: boolean, type: 'followers' | 'following' }>({
    open: false,
    type: 'followers'
  });

  useEffect(() => {
    if (!isOwnProfile && profileUser) {
      setIsFollowing(Math.random() > 0.5);
    }
  }, [profileUser, isOwnProfile]);

  const { data: userPulses, isLoading: isLoadingPulses } = useQuery({
    queryKey: ['user-pulses', targetId],
    queryFn: () => targetId ? mockService.getUserStatuses(targetId) : Promise.resolve([]),
    enabled: !!targetId,
  });

  const handleFollowToggle = () => {
    const nextState = !isFollowing;
    setIsFollowing(nextState);
    addToast(nextState ? `You are now following ${profileUser?.name}` : `Unfollowed ${profileUser?.name}`, nextState ? 'success' : 'info');
  };

  const handleMessage = async () => {
    if (!profileUser) return;
    const chat = await mockService.createChat(profileUser.id);
    navigate(`/chat/${chat.id}`);
  };

  const openUserList = (type: 'followers' | 'following') => {
    setUserListConfig({ open: true, type });
  };

  if (isLoadingUser) return (
    <div className="max-w-2xl mx-auto space-y-6 pt-10">
      <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] p-8 shadow-sm border border-slate-100 dark:border-slate-800 flex flex-col items-center text-center animate-pulse">
         <div className="w-24 h-24 rounded-full bg-slate-200 dark:bg-slate-800 mb-4"></div>
         <div className="h-6 w-48 bg-slate-200 dark:bg-slate-800 rounded-full mb-2"></div>
         <div className="h-4 w-32 bg-slate-100 dark:bg-slate-800 rounded-full mb-6"></div>
         <div className="grid grid-cols-4 gap-4 w-full mb-8">
            <div className="h-10 bg-slate-50 dark:bg-slate-800 rounded-lg"></div>
            <div className="h-10 bg-slate-50 dark:bg-slate-800 rounded-lg"></div>
            <div className="h-10 bg-slate-50 dark:bg-slate-800 rounded-lg"></div>
            <div className="h-10 bg-slate-50 dark:bg-slate-800 rounded-lg"></div>
         </div>
         <div className="h-12 w-full bg-slate-100 dark:bg-slate-800 rounded-2xl"></div>
      </div>
    </div>
  );

  if (isUserError || !profileUser) return (
    <div className="max-w-2xl mx-auto py-20 text-center">
       <span className="text-6xl block mb-4">🔦</span>
       <h2 className="text-2xl font-black text-slate-900 dark:text-white mb-2">User Not Found</h2>
       <p className="text-slate-500 mb-6">The pulse you are looking for has faded away.</p>
       <button onClick={() => navigate('/feed')} className="bg-brand-500 text-white px-6 py-2 rounded-xl font-bold">Back to Feed</button>
    </div>
  );

  return (
    <div className="max-w-2xl mx-auto space-y-6 pb-20">
      {/* Profile Header Card */}
      <div className="bg-white dark:bg-slate-900 rounded-[3rem] p-6 pt-10 pb-10 shadow-sm border border-slate-100 dark:border-slate-800/50 flex flex-col items-center text-center">
        <div className="relative mb-6">
          <Avatar src={profileUser.avatar} name={profileUser.name} size="xl" isOnline={profileUser.isOnline} />
          {isOwnProfile && (
            <button 
              onClick={() => navigate('/profile/edit')}
              className="absolute bottom-1 right-1 bg-brand-500 text-white p-2 rounded-full border-4 border-white dark:border-slate-900 shadow-lg active:scale-90 transition-transform"
            >
              <span className="text-xs">📸</span>
            </button>
          )}
        </div>
        
        <div className="mb-6">
          <h1 className="text-2xl md:text-3xl font-black mb-1 text-slate-900 dark:text-white tracking-tight">{profileUser.name}</h1>
          <p className="text-slate-500 dark:text-slate-400 font-bold text-sm md:text-base">@{profileUser.username}</p>
        </div>
        
        {/* Unified Stats Row: Followers, Following, Streak, Pulses */}
        <div className="grid grid-cols-4 gap-1 w-full max-w-lg mb-10">
          <div className="text-center group cursor-pointer" onClick={() => openUserList('followers')}>
            <span className="block text-xl md:text-2xl font-black text-brand-500 group-hover:scale-110 transition-transform leading-none mb-2">
              {profileUser.followersCount >= 1000 ? (profileUser.followersCount/1000).toFixed(1) + 'k' : profileUser.followersCount}
            </span>
            <span className="text-[9px] md:text-[10px] uppercase font-black tracking-widest text-slate-400 block leading-tight">{t('followers')}</span>
          </div>
          <div className="text-center group cursor-pointer" onClick={() => openUserList('following')}>
            <span className="block text-xl md:text-2xl font-black text-brand-500 group-hover:scale-110 transition-transform leading-none mb-2">
              {profileUser.followingCount}
            </span>
            <span className="text-[9px] md:text-[10px] uppercase font-black tracking-widest text-slate-400 block leading-tight">{t('following')}</span>
          </div>
          <div className="text-center group">
            <span className="block text-xl md:text-2xl font-black text-brand-500 group-hover:scale-110 transition-transform leading-none mb-2">{profileUser.streak}</span>
            <span className="text-[9px] md:text-[10px] uppercase font-black tracking-widest text-slate-400 block leading-tight">Streak</span>
          </div>
          <div className="text-center group">
            <span className="block text-xl md:text-2xl font-black text-brand-500 group-hover:scale-110 transition-transform leading-none mb-2">{userPulses?.length || 0}</span>
            <span className="text-[9px] md:text-[10px] uppercase font-black tracking-widest text-slate-400 block leading-tight">{t('posts')}</span>
          </div>
        </div>

        <p className="text-slate-600 dark:text-slate-300 max-w-sm mb-10 italic leading-relaxed text-sm md:text-base font-medium">
          "{profileUser.bio || 'No bio yet. Pulsing quietly.'}"
        </p>
        
        <div className="w-full max-w-xs flex flex-col gap-3">
          {isOwnProfile ? (
            <button 
              onClick={() => navigate('/profile/edit')}
              className="w-full bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 py-4 rounded-[1.5rem] font-black transition-all active:scale-[0.98] text-slate-900 dark:text-white text-sm md:text-base shadow-sm"
            >
              {t('edit_profile')}
            </button>
          ) : (
            <div className="flex gap-3 w-full">
              {isFollowing ? (
                <button 
                  onClick={handleFollowToggle}
                  className="flex-1 bg-slate-100 dark:bg-slate-800 hover:bg-red-50 dark:hover:bg-red-900/20 hover:text-red-500 py-4 rounded-[1.5rem] font-black transition-all active:scale-[0.98] border border-transparent hover:border-red-200 dark:hover:border-red-500/30 text-slate-600 dark:text-slate-300 group text-sm md:text-base"
                >
                  <span className="group-hover:hidden">{t('following')}</span>
                  <span className="hidden group-hover:inline">Unfollow</span>
                </button>
              ) : (
                <button 
                  onClick={handleFollowToggle}
                  className="flex-1 bg-brand-500 text-white py-4 rounded-[1.5rem] font-black shadow-lg shadow-brand-500/25 transition-all active:scale-[0.98] hover:bg-brand-600 text-sm md:text-base"
                >
                  {t('follow')}
                </button>
              )}
              <button 
                onClick={handleMessage}
                className="flex-1 bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 py-4 rounded-[1.5rem] font-black transition-all active:scale-[0.98] text-slate-900 dark:text-white text-sm md:text-base"
              >
                Message
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Intensity Section */}
      <div className="bg-white dark:bg-slate-900 rounded-[2.5rem] p-8 border border-slate-100 dark:border-slate-800/50 shadow-sm">
        <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-[0.3em] mb-6 text-center">Pulsing Intensity</h3>
        <div className="grid grid-cols-7 gap-3">
          {Array.from({ length: 28 }).map((_, i) => (
            <div 
              key={i} 
              className={`h-4 rounded-full transition-all duration-500 ${
                i % 3 === 0 ? 'bg-brand-500/40 w-full' : 
                i % 5 === 0 ? 'bg-brand-500 w-full' : 
                'bg-slate-100 dark:bg-slate-800/50 w-full'
              }`}
              style={{ transitionDelay: `${i * 10}ms` }}
            ></div>
          ))}
        </div>
      </div>

      {/* Activity Section */}
      <div className="space-y-6 pt-4">
        <div className="flex items-center justify-between px-4">
           <h3 className="text-xl md:text-2xl font-black text-slate-900 dark:text-white">Recent Activity</h3>
           {isOwnProfile && <button className="text-xs font-bold text-brand-500">View All</button>}
        </div>
        
        {isLoadingPulses ? (
          <div className="space-y-6">
              {[1, 2].map(i => (
                <div key={i} className="animate-pulse bg-white dark:bg-slate-900 h-40 rounded-[3rem] border border-slate-100 dark:border-slate-800"></div>
              ))}
          </div>
        ) : userPulses && userPulses.length > 0 ? (
          <div className="space-y-6">
            {userPulses.map(pulse => (
              <StatusCard key={pulse.id} pulse={pulse} />
            ))}
          </div>
        ) : (
          <div className="text-center py-24 bg-white dark:bg-slate-900 rounded-[3rem] border border-slate-100 dark:border-slate-800 border-dashed">
             <span className="text-6xl block mb-4 grayscale opacity-20">📭</span>
             <p className="text-slate-400 font-bold italic text-sm md:text-base">No activity recorded yet.</p>
          </div>
        )}
      </div>

      <UserListModal 
        isOpen={userListConfig.open} 
        onClose={() => setUserListConfig({ ...userListConfig, open: false })}
        userId={targetId!}
        type={userListConfig.type}
      />
    </div>
  );
};

export default Profile;
