import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuthStore, useUIStore } from '@/store';
import { useTranslation } from '@/i18n';
import { mockService } from '@/services/mock';
import Avatar from '@/components/ui/Avatar';
import EmojiPicker from '@/components/ui/EmojiPicker';
import { Status } from '@/types';

const MAX_CHARS = 280;

const StatusComposer: React.FC = () => {
  const { user } = useAuthStore();
  const { language, addToast } = useUIStore();
  const t = useTranslation(language);
  const queryClient = useQueryClient();
  
  const [content, setContent] = useState('');
  const [mood, setMood] = useState('😊');
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState('');

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

  const charCount = content.length;
  const isOverLimit = charCount > MAX_CHARS;

  return (
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
  );
};

export default StatusComposer;
