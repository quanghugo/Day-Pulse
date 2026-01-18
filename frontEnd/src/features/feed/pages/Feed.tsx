import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useUIStore } from '@/store';
import { useTranslation } from '@/i18n';
import { useFeed, useSuggestedUsers, useTrendingTags } from '../hooks/useFeed';
import Avatar from '@/components/ui/Avatar';
import StatusCard from '../components/StatusCard';
import StatusComposer from '../components/StatusComposer';

const Feed: React.FC = () => {
  const { language, addToast } = useUIStore();
  const t = useTranslation(language);
  const navigate = useNavigate();
  
  const observerRef = useRef<HTMLDivElement>(null);

  // Fetch Feed
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError
  } = useFeed();

  // Fetch Suggestions
  const { data: suggestedUsers } = useSuggestedUsers();

  // Fetch Trending
  const { data: trendingTags } = useTrendingTags();

  // Infinite Scroll Intersection Observer
  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasNextPage && !isFetchingNextPage) {
          fetchNextPage();
        }
      },
      { threshold: 0.5 }
    );

    if (observerRef.current) {
      observer.observe(observerRef.current);
    }

    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const handleFollow = (username: string) => {
    addToast(`Followed @${username}`, 'success');
  };

  const pulses = data?.pages.flatMap((page) => page) || [];

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 animate-in fade-in duration-500">
      
      {/* Left Sidebar: Suggested Users (Desktop Only) */}
      <div className="hidden lg:block lg:col-span-3">
        <div className="sticky top-24 space-y-6">
          <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-5 border border-slate-100 dark:border-slate-800 shadow-sm">
             <h3 className="font-black text-slate-900 dark:text-white mb-4 px-1">Who to follow</h3>
             <div className="space-y-4">
                {suggestedUsers?.map((u) => (
                  <div key={u.id} className="flex items-center justify-between group cursor-pointer" onClick={() => navigate('/profile')}>
                    <div className="flex items-center gap-3">
                       <Avatar src={u.avatar} name={u.name} size="md" />
                       <div className="flex flex-col">
                          <span className="font-bold text-sm text-slate-900 dark:text-white leading-tight">{u.name}</span>
                          <span className="text-xs text-slate-500 dark:text-slate-400">@{u.username}</span>
                       </div>
                    </div>
                    <button 
                      onClick={(e) => { e.stopPropagation(); handleFollow(u.username); }}
                      className="text-xs font-bold bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-900 dark:text-white px-3 py-1.5 rounded-full transition-colors"
                    >
                      Follow
                    </button>
                  </div>
                ))}
             </div>
             <div className="mt-4 pt-3 border-t border-slate-50 dark:border-slate-800">
               <button className="text-xs font-bold text-brand-500 hover:text-brand-600">Show more</button>
             </div>
          </div>
          
          <div className="text-xs text-slate-400 font-medium px-4">
             <p>© 2024 DayPulse • Privacy • Terms</p>
          </div>
        </div>
      </div>

      {/* Center Feed */}
      <div className="lg:col-span-6 space-y-6 max-w-2xl mx-auto w-full lg:max-w-none">
        {/* Share Status Composer */}
        <StatusComposer />

        {/* Pulse List Feed */}
        <div className="space-y-4">
          {isLoading && !pulses.length ? (
            <div className="space-y-4">
              {[1, 2, 3].map(i => (
                <div key={i} className="bg-white dark:bg-slate-900 p-5 rounded-[2rem] border border-slate-100 dark:border-slate-800 shadow-sm">
                  <div className="flex items-center gap-3 mb-4">
                    <div className="w-10 h-10 rounded-full bg-slate-200 dark:bg-slate-800 animate-pulse"></div>
                    <div className="space-y-2">
                      <div className="h-3 w-32 bg-slate-200 dark:bg-slate-800 rounded-full animate-pulse"></div>
                      <div className="h-2 w-16 bg-slate-100 dark:bg-slate-800/50 rounded-full animate-pulse"></div>
                    </div>
                  </div>
                  <div className="space-y-2 mb-4">
                    <div className="h-3 w-full bg-slate-100 dark:bg-slate-800/50 rounded-full animate-pulse"></div>
                    <div className="h-3 w-5/6 bg-slate-100 dark:bg-slate-800/50 rounded-full animate-pulse"></div>
                    <div className="h-3 w-4/6 bg-slate-100 dark:bg-slate-800/50 rounded-full animate-pulse"></div>
                  </div>
                   <div className="flex gap-4 pt-2 border-t border-slate-50 dark:border-slate-800/50 mt-4">
                      <div className="h-8 w-12 bg-slate-50 dark:bg-slate-800 rounded-lg animate-pulse"></div>
                      <div className="h-8 w-12 bg-slate-50 dark:bg-slate-800 rounded-lg animate-pulse"></div>
                   </div>
                </div>
              ))}
            </div>
          ) : isError ? (
            <div className="bg-red-50 dark:bg-red-900/10 p-8 rounded-[2rem] text-center border border-red-100 dark:border-red-900/20">
              <p className="text-red-600 dark:text-red-400 font-bold mb-2">Oops! Something went wrong.</p>
            </div>
          ) : pulses.length ? (
            <>
              {pulses.map((pulse, index) => (
                <StatusCard key={`${pulse.id}-${index}`} pulse={pulse} />
              ))}
              
              {/* Infinite Scroll Trigger */}
              <div ref={observerRef} className="py-8 flex justify-center">
                {isFetchingNextPage ? (
                  <div className="flex items-center gap-3 text-slate-400 font-bold text-sm animate-pulse">
                    <div className="w-5 h-5 border-2 border-brand-500 border-t-transparent rounded-full animate-spin"></div>
                    Loading more pulses...
                  </div>
                ) : hasNextPage ? (
                  <div className="text-slate-400 text-sm font-medium">Scroll for more vibes</div>
                ) : (
                  <div className="text-slate-300 dark:text-slate-600 text-sm font-medium italic">You've reached the end of the pulse</div>
                )}
              </div>
            </>
          ) : (
            <div className="bg-white dark:bg-slate-900 p-12 rounded-[2rem] text-center border border-slate-100 dark:border-slate-800 border-dashed">
              <span className="text-6xl block mb-4 grayscale opacity-50">🏜️</span>
              <p className="text-slate-500 dark:text-slate-400 font-medium mb-1">{t('no_posts')}</p>
            </div>
          )}
        </div>
      </div>

      {/* Right Sidebar: Trending Topics (Desktop Only) */}
      <div className="hidden lg:block lg:col-span-3">
        <div className="sticky top-24">
           <div className="bg-white dark:bg-slate-900 rounded-[2rem] p-5 border border-slate-100 dark:border-slate-800 shadow-sm">
             <h3 className="font-black text-slate-900 dark:text-white mb-4 px-1">Trending Topics</h3>
             <div className="space-y-1">
               {trendingTags?.map((item, i) => (
                 <div key={i} className="flex items-center justify-between p-2 rounded-xl hover:bg-slate-50 dark:hover:bg-slate-800 cursor-pointer transition-colors group">
                   <div className="flex flex-col">
                     <span className="font-bold text-slate-900 dark:text-white group-hover:text-brand-500 transition-colors">#{item.tag}</span>
                     <span className="text-xs text-slate-500 dark:text-slate-400">{item.count} pulses</span>
                   </div>
                   <span className="text-slate-300 text-lg">›</span>
                 </div>
               ))}
             </div>
           </div>
        </div>
      </div>

    </div>
  );
};

export default Feed;
