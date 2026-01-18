
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Status } from '@/types';
import Avatar from '@/components/ui/Avatar';
import { useUIStore } from '@/store';
import { ReactionsModal, CommentsModal } from '@/components/modals';

interface StatusCardProps {
  pulse: Status;
}

const StatusCard: React.FC<StatusCardProps> = ({ pulse }) => {
  const navigate = useNavigate();
  const [liked, setLiked] = useState(pulse.hasLiked);
  const [count, setCount] = useState(pulse.likes);
  const [isReactionsOpen, setIsReactionsOpen] = useState(false);
  const [isCommentsOpen, setIsCommentsOpen] = useState(false);
  const { addToast } = useUIStore();

  const toggleLike = () => {
    setLiked(!liked);
    setCount(prev => liked ? prev - 1 : prev + 1);
  };

  const handleTagClick = (tag: string) => {
    addToast(`Filtering by pulse: #${tag}`, 'info');
  };

  const handleProfileClick = () => {
    if (pulse.userId) {
      navigate(`/profile/${pulse.userId}`);
    }
  };

  const formattedTime = new Date(pulse.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  return (
    <div className="bg-white dark:bg-slate-900 p-6 rounded-[2.5rem] shadow-sm border border-slate-100 dark:border-slate-800/50 group transition-all hover:shadow-md relative overflow-hidden">
      <div className="flex justify-between items-start mb-5">
        <div className="flex gap-4 cursor-pointer" onClick={handleProfileClick}>
          <Avatar src={pulse.user?.avatar} name={pulse.user?.name} size="md" />
          <div>
            <h4 className="font-bold text-slate-900 dark:text-slate-100 hover:text-brand-500 transition-colors text-base">{pulse.user?.name}</h4>
            <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{formattedTime}</p>
          </div>
        </div>
        {pulse.mood && (
          <div className="w-11 h-11 bg-slate-50 dark:bg-slate-800/50 rounded-2xl flex items-center justify-center text-2xl shadow-inner border border-slate-100/50 dark:border-slate-700/50" title="Current Mood">
            {pulse.mood}
          </div>
        )}
      </div>

      <p className="text-slate-700 dark:text-slate-300 leading-relaxed mb-5 text-base md:text-[15px] font-medium">
        {pulse.content}
      </p>

      {pulse.tags && pulse.tags.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-5">
          {pulse.tags.map(tag => (
            <button
              key={tag}
              onClick={() => handleTagClick(tag)}
              className="group/tag flex items-center gap-0.5 text-[10px] font-black uppercase tracking-widest text-brand-600 dark:text-brand-400 bg-brand-50 dark:bg-brand-500/10 px-3 py-1.5 rounded-xl hover:bg-brand-500 hover:text-white dark:hover:bg-brand-500 transition-all active:scale-95 border border-brand-100/50 dark:border-brand-500/20 shadow-sm"
            >
              <span className="text-brand-300 dark:text-brand-700 group-hover/tag:text-white/50 transition-colors">#</span>
              {tag}
            </button>
          ))}
        </div>
      )}

      <div className="flex items-center gap-6 pt-4 border-t border-slate-50 dark:border-slate-800/50">
        <div className="flex items-center gap-2.5">
          <button 
            onClick={toggleLike}
            className={`flex items-center transition-all active:scale-90 ${liked ? 'text-pink-500' : 'text-slate-400 hover:text-pink-500'}`}
          >
            <span className="text-2xl">{liked ? '❤️' : '🤍'}</span>
          </button>
          
          <button 
            onClick={() => setIsReactionsOpen(true)}
            disabled={count === 0}
            className="text-lg font-black text-slate-500 hover:text-brand-500 dark:text-slate-400 dark:hover:text-brand-400 transition-all active:scale-95 disabled:opacity-30 disabled:pointer-events-none"
          >
            {count}
          </button>
        </div>

        <button 
          onClick={() => setIsCommentsOpen(true)}
          className="flex items-center gap-2.5 transition-all active:scale-90 text-slate-400 hover:text-brand-500 group/btn"
        >
          <span className="text-2xl group-hover/btn:scale-125 transition-transform duration-200">💬</span>
          <span className="text-lg font-black">{pulse.commentsCount}</span>
        </button>
        
        <button className="flex items-center transition-all active:scale-90 ml-auto text-slate-400 hover:text-slate-900 dark:hover:text-slate-100">
          <span className="text-2xl">🔗</span>
        </button>
      </div>

      {/* Modals */}
      <ReactionsModal 
        isOpen={isReactionsOpen} 
        onClose={() => setIsReactionsOpen(false)} 
        users={pulse.likedBy || []} 
      />
      <CommentsModal 
        isOpen={isCommentsOpen} 
        onClose={() => setIsCommentsOpen(false)} 
        statusId={pulse.id} 
      />
    </div>
  );
};

export default StatusCard;
