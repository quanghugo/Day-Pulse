import React, { useState, useEffect, useRef } from 'react';
import { useInfiniteQuery, useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { mockService } from '../services/mock';
import { useAuthStore, useUIStore } from '../store';
import { useTranslation } from '../i18n';
import Avatar from '../components/Avatar';
import StatusCard from '../components/StatusCard';
import EmojiPicker from '../components/EmojiPicker';
import { Status } from '../types';

const MAX_CHARS = 280;
const PAGE_SIZE = 20;

const Feed: React.FC = () => {
  const { user } = useAuthStore();
  const { language, addToast } = useUIStore();
  const t = useTranslation(language);
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  
  const [content, setContent] = useState('');
  const [mood, setMood] = useState('😊');
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');

  const observerRef = useRef<HTMLDivElement>(null);

  // Fetch Feed
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    isError
  } = useInfiniteQuery({
    queryKey: ['feed'],
    queryFn: ({ pageParam = 0 }) => mockService.getFeed(pageParam as number, PAGE_SIZE),
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) => {
      return allPages.length < 10 ? allPages.length : undefined;
    },
  });

  // Fetch Suggestions
  const { data: suggestedUsers } = useQuery({ 
    queryKey: ['feed_suggested'], 
    queryFn: mockService.getSuggested 
  });

  // Fetch Trending
  const { data: trendingTags } = useQuery({ 
    queryKey: ['feed_trending'], 
    queryFn: mockService.getTrendingTags 
  });

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

  const createMutation = useMutation({
    mutationFn: (vars: { content: string, mood: string, tags: string[] }) => 
      mockService.createStatus(vars.content, vars.mood, vars.tags),
    onSuccess: (newPulse) => {
      // Manually update the cache to put the new pulse at the very top
      queryClient.setQueryData(['feed'], (oldData: any) => {
        if (!oldData) return oldData;
        const newPages = [...oldData.pages];
        // Prepend to the first page
        newPages[0] = [newPulse as Status, ...newPages[0]];
        return {
          ...oldData,
          pages: newPages,
        };
      });
      
      setContent('');
      setTags([]);
      setTagInput('');
      setMood('😊');
      addToast('Pulse shared successfully! ✨', 'success');
    },
    onError: () => {
      addToast('Failed to post status. Please try again.', 'error');
    }
  });

  const handlePost = () => {
    if (!content.trim() || content.length > MAX_CHARS) return;
    
    // Combine established tags with current input if it's a valid tag
    let finalTags = [...tags];
    const pendingTag = tagInput.trim().replace(/^#/, '');
    if (pendingTag && !finalTags.includes(pendingTag)) {
      finalTags.push(pendingTag);
    }

    createMutation.mutate({ content, mood, tags: finalTags });
  };

  const handleTagKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === ' ' || e.key === 'Enter') {
      e.preventDefault();
      const newTag = tagInput.trim().replace(/^#/, '');
      if (newTag) {
        if (!tags.includes(newTag)) {
          setTags([...tags, newTag]);
        }
        setTagInput('');
      }
    } else if (e.key === 'Backspace' && !tagInput && tags.length > 0) {
      // Remove last tag if backspace pressed while input is empty
      const newTags = [...tags];
      newTags.pop();
      setTags(newTags);
    }
  };

  const removeTag = (tagToRemove: string) => {
    setTags(tags.filter(t => t !== tagToRemove));
  };

  const handleFollow = (username: string) => {
    addToast(`Followed @${username}`, 'success');
  };

  const pulses = data?.pages.flatMap((page) => page) || [];
  const charCount = content.length;
  const isOverLimit = charCount > MAX_CHARS;

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
        {/* Onboarding / Streak Banner - HIDDEN
        <div className="bg-brand-500 text-white p-6 rounded-[2rem] shadow-lg relative overflow-hidden group">
          <div className="relative z-10">
            <h2 className="text-2xl font-black mb-1">Pulse Check, {user?.name?.split(' ')[0]}!</h2>
            <p className="opacity-90 text-sm font-medium">Keep your {user?.streak}-day streak alive by sharing how you feel today.</p>
          </div>
          <div className="absolute top-[-20px] right-[-20px] text-8xl opacity-20 rotate-12 transition-transform group-hover:scale-110 duration-500">🔥</div>
        </div>
        */}

        {/* Share Status Composer */}
        <section className="bg-white dark:bg-slate-900 p-5 rounded-[2rem] shadow-sm border border-slate-200 dark:border-slate-800 transition-all focus-within:ring-2 focus-within:ring-brand-500/20">
          <div className="flex gap-4">
            <Avatar src={user?.avatar} name={user?.name} size="md" className="hidden sm:block" />
            <div className="flex-1">
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder={t('share_status')}
                className="w-full bg-transparent border-none focus:ring-0 text-lg resize-none placeholder:text-slate-400 no-scrollbar"
                rows={3}
              />
              
              <div className="mb-4">
                <div className="flex flex-wrap items-center gap-2 bg-slate-50 dark:bg-slate-800/50 rounded-xl px-3 py-2 border border-slate-100 dark:border-slate-700/50 min-h-[46px] transition-colors focus-within:bg-white dark:focus-within:bg-slate-900 focus-within:ring-1 focus-within:ring-brand-500/30">
                  {tags.map((tag) => (
                    <span key={tag} className="flex items-center gap-1 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-brand-600 dark:text-brand-400 px-2.5 py-1 rounded-lg text-sm font-bold shadow-sm animate-in zoom-in duration-200">
                      #{tag}
                      <button 
                        onClick={() => removeTag(tag)}
                        className="ml-1 text-slate-400 hover:text-red-500 transition-colors w-4 h-4 flex items-center justify-center rounded-full hover:bg-slate-100 dark:hover:bg-slate-700"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                  <div className="flex-1 flex items-center min-w-[120px]">
                    {tags.length === 0 && <span className="text-brand-500 font-bold text-sm mr-1">#</span>}
                    <input
                      type="text"
                      value={tagInput}
                      onChange={(e) => setTagInput(e.target.value)}
                      onKeyDown={handleTagKeyDown}
                      placeholder={tags.length ? "" : "add tags (space separated)"}
                      className="bg-transparent text-base w-full border-none focus:ring-0 placeholder:text-slate-500 p-0"
                    />
                  </div>
                </div>
              </div>

              <div className="flex flex-wrap items-center justify-between pt-4 border-t border-slate-100 dark:border-slate-800 gap-4">
                <div className="flex items-center gap-6">
                  <EmojiPicker selectedEmoji={mood} onSelect={setMood} />
                  <span className={`text-xs font-bold ${isOverLimit ? 'text-red-500' : 'text-slate-400'}`}>
                    {charCount}/{MAX_CHARS}
                  </span>
                </div>

                <button
                  onClick={handlePost}
                  disabled={!content.trim() || isOverLimit || createMutation.isPending}
                  className="bg-brand-500 hover:bg-brand-600 disabled:opacity-30 text-white px-8 py-3 rounded-2xl font-black transition-all active:scale-95 shadow-lg shadow-brand-500/25 flex items-center justify-center gap-2 min-w-[120px]"
                >
                  {createMutation.isPending ? (
                    <>
                      <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>SYNCING</span>
                    </>
                  ) : (
                    t('post')
                  )}
                </button>
              </div>
            </div>
          </div>
        </section>

        {/* Pulse List Feed */}
        <div className="space-y-4">
          {/* Posting Placeholder */}
          {createMutation.isPending && (
            <div className="animate-pulse bg-brand-50/50 dark:bg-brand-500/5 p-5 rounded-[2rem] border border-brand-100 dark:border-brand-500/20 shadow-sm flex flex-col gap-3">
               <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-slate-200 dark:bg-slate-800"></div>
                  <div className="space-y-2">
                     <div className="h-3 w-24 bg-slate-200 dark:bg-slate-800 rounded-full"></div>
                     <div className="h-2 w-12 bg-slate-100 dark:bg-slate-900 rounded-full"></div>
                  </div>
                  <div className="ml-auto flex items-center gap-2 text-[10px] font-black uppercase text-brand-500 tracking-widest animate-bounce">
                     🚀 Syncing...
                  </div>
               </div>
               <div className="h-4 w-full bg-slate-100 dark:bg-slate-900 rounded-full"></div>
               <div className="h-4 w-2/3 bg-slate-100 dark:bg-slate-900 rounded-full"></div>
            </div>
          )}

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