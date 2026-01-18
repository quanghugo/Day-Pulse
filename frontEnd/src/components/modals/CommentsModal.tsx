
import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mockService } from '@/services/mock';
import { useUIStore } from '@/store';
import Avatar from '@/components/ui/Avatar';
import { Comment } from '@/types';

interface CommentsModalProps {
  isOpen: boolean;
  onClose: () => void;
  statusId: string;
}

const CommentsModal: React.FC<CommentsModalProps> = ({ isOpen, onClose, statusId }) => {
  const [newCommentText, setNewCommentText] = useState('');
  const queryClient = useQueryClient();
  const { addToast } = useUIStore();
  const scrollRef = useRef<HTMLDivElement>(null);

  // Handle Body Scroll Lock for iOS Safari
  useEffect(() => {
    if (isOpen) {
      const scrollY = window.scrollY;
      document.body.style.position = 'fixed';
      document.body.style.top = `-${scrollY}px`;
      document.body.style.width = '100%';
      document.body.style.overflow = 'hidden';
      document.body.style.paddingRight = 'var(--scrollbar-width)';
    }

    return () => {
      if (document.body.style.position === 'fixed') {
        const scrollY = document.body.style.top;
        document.body.style.position = '';
        document.body.style.top = '';
        document.body.style.width = '';
        document.body.style.overflow = '';
        document.body.style.paddingRight = '';
        window.scrollTo(0, parseInt(scrollY || '0') * -1);
      }
    };
  }, [isOpen]);

  const { data: comments, isLoading, isError } = useQuery({
    queryKey: ['comments', statusId],
    queryFn: () => mockService.getComments(statusId),
    enabled: isOpen,
  });

  const postMutation = useMutation({
    mutationFn: (content: string) => mockService.addComment(statusId, content),
    onSuccess: (newComment) => {
      queryClient.setQueryData(['comments', statusId], (old: any) => [...(old || []), newComment]);
      // Update commentsCount in feed cache
      queryClient.setQueryData(['feed'], (old: any) => {
        if (!old) return old;
        return {
          ...old,
          pages: old.pages.map((page: any) => 
            page.map((pulse: any) => 
              pulse.id === statusId 
                ? { ...pulse, commentsCount: pulse.commentsCount + 1 } 
                : pulse
            )
          )
        };
      });
      setNewCommentText('');
      addToast('Comment posted! 💬', 'success');
    }
  });

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [comments]);

  if (!isOpen) return null;

  const handlePost = () => {
    if (!newCommentText.trim() || postMutation.isPending) return;
    postMutation.mutate(newCommentText);
  };

  return createPortal(
    <div className="fixed inset-0 z-[100] flex items-end md:items-center justify-center p-0 md:p-4">
      <div 
        className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-300"
        onClick={(e) => {
          e.preventDefault();
          onClose();
        }}
        onTouchMove={(e) => e.preventDefault()} // Block touches on backdrop
      />
      <div className="relative w-full max-w-lg bg-white dark:bg-slate-900 rounded-t-[2.5rem] md:rounded-[2.5rem] shadow-2xl flex flex-col h-[85dvh] md:h-[70vh] animate-in slide-in-from-bottom duration-300 md:zoom-in-95">
        
        {/* Header */}
        <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between bg-white/80 dark:bg-slate-900/80 backdrop-blur-md sticky top-0 z-10 rounded-t-[2.5rem]">
          <h3 className="text-xl font-black text-slate-900 dark:text-white">Comments</h3>
          <button 
            onClick={onClose}
            className="w-10 h-10 flex items-center justify-center rounded-full bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
          >
            ✕
          </button>
        </div>

        {/* Comment List */}
        <div ref={scrollRef} className="flex-1 overflow-y-auto p-4 md:p-6 space-y-6 no-scrollbar overscroll-contain">
          {isLoading ? (
            <div className="space-y-4">
              {[1, 2, 3].map(i => (
                <div key={i} className="flex gap-4 animate-pulse">
                  <div className="w-10 h-10 bg-slate-100 dark:bg-slate-800 rounded-full flex-shrink-0"></div>
                  <div className="flex-1 space-y-2">
                    <div className="h-3 w-24 bg-slate-100 dark:bg-slate-800 rounded-full"></div>
                    <div className="h-10 w-full bg-slate-50 dark:bg-slate-900 rounded-2xl"></div>
                  </div>
                </div>
              ))}
            </div>
          ) : comments?.length ? (
            comments.map((comment) => (
              <div key={comment.id} className="flex gap-3 animate-in fade-in slide-in-from-bottom-2 duration-300">
                <Avatar name={comment.userName} size="sm" className="mt-1" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-bold text-sm text-slate-900 dark:text-slate-100">{comment.userName}</span>
                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">
                      {new Date(comment.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                  </div>
                  <div className="bg-slate-50 dark:bg-slate-800/50 p-3.5 rounded-2xl rounded-tl-none border border-slate-100 dark:border-slate-800 text-sm text-slate-700 dark:text-slate-300 leading-relaxed shadow-sm">
                    {comment.content}
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="py-20 text-center text-slate-400">
              <span className="text-5xl block mb-4 opacity-30">💭</span>
              <p className="font-bold text-sm italic">Be the first to sync on this pulse!</p>
            </div>
          )}
        </div>

        {/* Input Footer */}
        <div className="p-4 md:p-6 bg-white dark:bg-slate-900 border-t border-slate-100 dark:border-slate-800 rounded-b-none md:rounded-b-[2.5rem] pb-safe">
          <div className="flex items-center gap-3 bg-slate-100 dark:bg-slate-800 p-2 rounded-3xl focus-within:ring-2 focus-within:ring-brand-500/50 transition-all">
            <input 
              type="text"
              value={newCommentText}
              onChange={(e) => setNewCommentText(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handlePost()}
              placeholder="Add a comment..."
              className="flex-1 bg-transparent border-none focus:ring-0 text-base py-2 px-3 text-slate-900 dark:text-white"
            />
            <button 
              onClick={handlePost}
              disabled={!newCommentText.trim() || postMutation.isPending}
              className="bg-brand-500 hover:bg-brand-600 disabled:opacity-50 text-white font-black text-[10px] uppercase tracking-widest px-6 py-3 rounded-2xl shadow-lg shadow-brand-500/20 transition-all active:scale-95 flex items-center gap-2"
            >
              {postMutation.isPending ? (
                <div className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              ) : 'Post'}
            </button>
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
};

export default CommentsModal;
