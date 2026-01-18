
import React, { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { mockService } from '../services/mock';
import { useAuthStore, useUIStore } from '../store';
import Avatar from './Avatar';
import { User } from '../types';
import { CloseIcon, SearchIcon } from './Icons';

interface UserListModalProps {
  isOpen: boolean;
  onClose: () => void;
  userId: string;
  type: 'followers' | 'following';
}

const UserListModal: React.FC<UserListModalProps> = ({ isOpen, onClose, userId, type }) => {
  const navigate = useNavigate();
  const { user: currentUser } = useAuthStore();
  const { addToast } = useUIStore();
  const [searchQuery, setSearchQuery] = useState('');

  // Handle Body Scroll Lock
  useEffect(() => {
    if (isOpen) {
      const scrollY = window.scrollY;
      document.body.style.position = 'fixed';
      document.body.style.top = `-${scrollY}px`;
      document.body.style.width = '100%';
      document.body.style.overflow = 'hidden';
    }

    return () => {
      if (document.body.style.position === 'fixed') {
        const scrollY = document.body.style.top;
        document.body.style.position = '';
        document.body.style.top = '';
        document.body.style.width = '';
        document.body.style.overflow = '';
        window.scrollTo(0, parseInt(scrollY || '0') * -1);
      }
    };
  }, [isOpen]);

  const { data: users, isLoading } = useQuery({
    queryKey: ['user-list', userId, type],
    queryFn: () => type === 'followers' ? mockService.getFollowers(userId) : mockService.getFollowing(userId),
    enabled: isOpen,
  });

  if (!isOpen) return null;

  const filteredUsers = users?.filter(u => 
    u.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
    u.username.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleUserClick = (uId: string) => {
    onClose();
    navigate(`/profile/${uId}`);
  };

  const handleToggleFollow = (e: React.MouseEvent, u: User) => {
    e.stopPropagation();
    addToast(`Action simulated for @${u.username}`, 'info');
  };

  return createPortal(
    <div className="fixed inset-0 z-[100] flex items-end md:items-center justify-center p-0 md:p-4">
      <div 
        className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-300"
        onClick={onClose}
      />
      <div className="relative w-full max-w-md bg-white dark:bg-slate-900 rounded-t-[2.5rem] md:rounded-[2.5rem] shadow-2xl flex flex-col h-[85dvh] md:h-[70vh] animate-in slide-in-from-bottom duration-300 md:zoom-in-95">
        
        {/* Header */}
        <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between sticky top-0 z-10 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md rounded-t-[2.5rem]">
          <div className="w-10"></div>
          <h3 className="text-base font-black text-slate-900 dark:text-white capitalize">
            {type}
          </h3>
          <button 
            onClick={onClose}
            className="w-10 h-10 flex items-center justify-center rounded-full text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <CloseIcon className="w-6 h-6" />
          </button>
        </div>

        {/* Search */}
        <div className="px-6 py-4 border-b border-slate-50 dark:border-slate-800">
          <div className="bg-slate-100 dark:bg-slate-800 rounded-xl px-4 py-2 flex items-center gap-2">
            <SearchIcon className="w-4 h-4 text-slate-400" />
            <input 
              type="text" 
              placeholder="Search" 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="bg-transparent border-none focus:ring-0 text-sm w-full p-0 text-slate-900 dark:text-white"
            />
          </div>
        </div>

        {/* User List */}
        <div className="flex-1 overflow-y-auto p-2 no-scrollbar">
          {isLoading ? (
            <div className="space-y-4 p-4">
              {[1, 2, 3, 4, 5].map(i => (
                <div key={i} className="flex items-center gap-3 animate-pulse">
                  <div className="w-12 h-12 rounded-full bg-slate-100 dark:bg-slate-800"></div>
                  <div className="flex-1 space-y-2">
                    <div className="h-4 w-24 bg-slate-100 dark:bg-slate-800 rounded-full"></div>
                    <div className="h-3 w-32 bg-slate-50 dark:bg-slate-800/50 rounded-full"></div>
                  </div>
                </div>
              ))}
            </div>
          ) : filteredUsers?.length ? (
            <div className="space-y-1">
              {filteredUsers.map(u => (
                <div 
                  key={u.id} 
                  onClick={() => handleUserClick(u.id)}
                  className="flex items-center justify-between p-3 rounded-2xl hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors group"
                >
                  <div className="flex items-center gap-3">
                    <Avatar src={u.avatar} name={u.name} size="lg" isOnline={u.isOnline} />
                    <div className="flex flex-col">
                      <span className="font-bold text-sm text-slate-900 dark:text-white group-hover:text-brand-500 transition-colors">
                        {u.username}
                      </span>
                      <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                        {u.name}
                      </span>
                    </div>
                  </div>
                  
                  {u.id !== currentUser?.id && (
                    <button 
                      onClick={(e) => handleToggleFollow(e, u)}
                      className="px-4 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-black text-slate-900 dark:text-white hover:bg-slate-100 dark:hover:bg-slate-800 active:scale-95 transition-all"
                    >
                      {type === 'following' ? 'Following' : 'Follow'}
                    </button>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="py-20 text-center">
              <span className="text-4xl block mb-2 grayscale opacity-20">👤</span>
              <p className="text-slate-400 text-sm font-bold italic">No users found</p>
            </div>
          )}
        </div>
      </div>
    </div>,
    document.body
  );
};

export default UserListModal;
