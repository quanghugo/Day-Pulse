
import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { mockService } from '@/services/mock';
import { useUIStore } from '@/store';
import { useTranslation } from '@/i18n';
import Avatar from '@/components/ui/Avatar';
import { useNavigate } from 'react-router-dom';
import { SearchIcon } from '@/components/icons';

const Search: React.FC = () => {
  const { language, addToast } = useUIStore();
  const t = useTranslation(language);
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');

  // Debounce search input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(query);
    }, 300);
    return () => clearTimeout(handler);
  }, [query]);

  // Fetch Suggestions (when query is empty)
  const { data: suggestions, isLoading: loadingSuggestions } = useQuery({
    queryKey: ['search_suggested'],
    queryFn: mockService.getSuggested,
    enabled: !debouncedQuery,
  });

  // Fetch Search Results (when query exists)
  const { data: results, isLoading: loadingResults } = useQuery({
    queryKey: ['search', debouncedQuery],
    queryFn: () => mockService.search(debouncedQuery),
    enabled: !!debouncedQuery,
  });

  const handleFollow = (e: React.MouseEvent, username: string) => {
    e.stopPropagation();
    addToast(`Followed @${username}`, 'success');
  };

  const handleTagClick = (tag: string) => {
    addToast(`Filtering by #${tag}`, 'info');
  };

  const isLoading = debouncedQuery ? loadingResults : loadingSuggestions;

  return (
    <div className="max-w-2xl mx-auto min-h-[80vh]">
      {/* Sticky Search Input */}
      <div className="sticky top-14 md:top-0 z-30 bg-slate-50 dark:bg-slate-950 pt-2 pb-4 px-2">
        <h1 className="text-3xl font-black text-slate-900 dark:text-white mb-4 px-2 hidden md:block">
          {t('search')}
        </h1>
        <div className="relative group">
          <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
            <SearchIcon className="w-5 h-5 text-slate-400 group-focus-within:text-brand-500 transition-colors" />
          </div>
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t('search_placeholder')}
            className="block w-full rounded-2xl border-none bg-slate-200/50 dark:bg-slate-800 py-3.5 pl-11 pr-4 text-slate-900 dark:text-white placeholder:text-slate-500 focus:ring-2 focus:ring-brand-500 focus:bg-white dark:focus:bg-slate-900 transition-all text-base"
          />
        </div>
      </div>

      {/* Content Area */}
      <div className="space-y-6 px-2 pb-8 animate-in fade-in duration-500">
        
        {/* Loading State */}
        {isLoading && (
          <div className="space-y-4 pt-2">
            {[1, 2, 3, 4].map(i => (
              <div key={i} className="flex items-center gap-4 p-2">
                 <div className="w-12 h-12 rounded-full bg-slate-200 dark:bg-slate-800 animate-pulse"></div>
                 <div className="space-y-2 flex-1">
                   <div className="h-4 w-32 bg-slate-200 dark:bg-slate-800 rounded-full animate-pulse"></div>
                   <div className="h-3 w-20 bg-slate-200 dark:bg-slate-800 rounded-full animate-pulse"></div>
                 </div>
              </div>
            ))}
          </div>
        )}

        {/* Empty State / Suggestions */}
        {!debouncedQuery && !isLoading && suggestions && (
          <div>
            <h3 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 mb-4 px-2">
              {t('suggested_for_you')}
            </h3>
            <div className="space-y-1">
              {suggestions.map(user => (
                <div 
                  key={user.id} 
                  onClick={() => navigate(`/profile/${user.id}`)}
                  className="flex items-center justify-between p-3.5 rounded-[1.5rem] hover:bg-white dark:hover:bg-slate-900 transition-all cursor-pointer group active:scale-[0.98]"
                >
                  <div className="flex items-center gap-4">
                    <Avatar src={user.avatar} name={user.name} size="lg" />
                    <div className="flex flex-col">
                      <div className="flex items-center gap-1">
                        <span className="font-bold text-slate-900 dark:text-white text-[15px] group-hover:text-brand-500 transition-colors">{user.username}</span>
                        {user.isOnline && <span className="w-2 h-2 rounded-full bg-green-500 mt-0.5"></span>}
                      </div>
                      <span className="text-slate-500 dark:text-slate-400 text-sm leading-none">{user.name}</span>
                      <span className="text-slate-400 text-xs mt-1 font-bold">24k {t('followers')}</span>
                    </div>
                  </div>
                  <button 
                    onClick={(e) => handleFollow(e, user.username)}
                    className="border border-slate-300 dark:border-slate-700 rounded-xl px-5 py-2 text-sm font-black text-slate-900 dark:text-white hover:bg-brand-500 hover:text-white hover:border-brand-500 transition-all active:scale-95"
                  >
                    {t('follow')}
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Search Results */}
        {debouncedQuery && !isLoading && results && (
          <div className="space-y-6">
            
            {/* Users Section */}
            {results.users.length > 0 && (
              <div>
                 <div className="space-y-1">
                    {results.users.map(user => (
                      <div 
                        key={user.id} 
                        onClick={() => navigate(`/profile/${user.id}`)}
                        className="flex items-center justify-between p-3.5 rounded-[1.5rem] hover:bg-white dark:hover:bg-slate-900 transition-all cursor-pointer active:scale-[0.98] group"
                      >
                        <div className="flex items-center gap-4">
                          <Avatar src={user.avatar} name={user.name} size="lg" />
                          <div className="flex flex-col">
                            <span className="font-bold text-slate-900 dark:text-white text-[15px] group-hover:text-brand-500 transition-colors">{user.username}</span>
                            <span className="text-slate-500 dark:text-slate-400 text-sm">{user.name}</span>
                          </div>
                        </div>
                        <button 
                          onClick={(e) => handleFollow(e, user.username)}
                          className="border border-slate-300 dark:border-slate-700 rounded-xl px-5 py-2 text-sm font-black text-slate-900 dark:text-white hover:bg-brand-500 hover:text-white hover:border-brand-500 transition-all"
                        >
                          {t('follow')}
                        </button>
                      </div>
                    ))}
                 </div>
              </div>
            )}

            {/* Tags Section */}
            {results.tags.length > 0 && (
              <div>
                 <h3 className="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 mb-2 px-2 mt-4">{t('tags')}</h3>
                 <div className="space-y-1">
                    {results.tags.map(tag => (
                      <div 
                        key={tag}
                        onClick={() => handleTagClick(tag)}
                        className="flex items-center justify-between p-3.5 rounded-[1.5rem] hover:bg-white dark:hover:bg-slate-900 transition-all cursor-pointer active:scale-[0.98] group"
                      >
                        <div className="flex items-center gap-4">
                           <div className="w-12 h-12 rounded-2xl border border-slate-200 dark:border-slate-800 flex items-center justify-center text-xl text-slate-900 dark:text-white bg-white dark:bg-slate-900 group-hover:scale-110 transition-transform shadow-sm">
                             #
                           </div>
                           <div className="flex flex-col">
                             <span className="font-black text-slate-900 dark:text-white text-[15px]">#{tag}</span>
                             <span className="text-slate-500 dark:text-slate-400 text-sm font-medium">3.5k {t('posts')}</span>
                           </div>
                        </div>
                        <span className="text-slate-300 text-xl">›</span>
                      </div>
                    ))}
                 </div>
              </div>
            )}

            {results.users.length === 0 && results.tags.length === 0 && (
              <div className="text-center py-20 bg-white dark:bg-slate-900 rounded-[2.5rem] border border-slate-100 dark:border-slate-800 border-dashed">
                 <span className="text-5xl block mb-4 opacity-20">🔎</span>
                 <p className="text-slate-400 font-bold italic">No results found for "{debouncedQuery}"</p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Search;
