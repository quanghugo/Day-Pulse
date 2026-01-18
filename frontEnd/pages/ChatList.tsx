
import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { mockService } from '../services/mock';
import { useAuthStore, useUIStore } from '../store';
import { useTranslation } from '../i18n';
import Avatar from '../components/Avatar';
import { SearchIcon, CloseIcon, PlusIcon, ChatIcon } from '../components/Icons';

const ChatList: React.FC = () => {
  const { user } = useAuthStore();
  const { language } = useUIStore();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [searchQuery, setSearchQuery] = useState('');

  const { data: chats, isLoading: chatsLoading } = useQuery({
    queryKey: ['chats'],
    queryFn: mockService.getChats,
  });

  const { data: contacts, isLoading: contactsLoading } = useQuery({
    queryKey: ['contacts'],
    queryFn: mockService.getAvailableUsers,
  });

  const createChatMutation = useMutation({
    mutationFn: (userId: string) => mockService.createChat(userId),
    onSuccess: (newChat) => {
      // Optimistically add chat if it's new
      queryClient.setQueryData(['chats'], (old: any) => {
        if (!old) return [newChat];
        if (old.some((c: any) => c.id === newChat.id)) return old;
        return [newChat, ...old];
      });
      setSearchQuery('');
      navigate(`/chat/${newChat.id}`);
    }
  });

  const filteredChats = chats?.filter(chat => {
    const partner = chat.participants.find(p => p.id !== user?.id);
    const matchesName = partner?.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesUsername = partner?.username.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesMessage = chat.lastMessage?.text.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesName || matchesUsername || matchesMessage;
  });

  const filteredContacts = searchQuery ? contacts?.filter(c => {
    // Check if user matches search
    const matches = c.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                   c.username.toLowerCase().includes(searchQuery.toLowerCase());
    
    // Check if we already have a chat with them
    const hasChat = chats?.some(chat => chat.participants.some(p => p.id === c.id));
    
    // Filter out self and existing chats (optional, but cleaner for "Start New")
    return matches && !hasChat && c.id !== user?.id;
  }) : [];

  return (
    <div className="max-w-4xl mx-auto space-y-4 min-h-[80vh]">
      {/* Search Bar - Sticky Header */}
      <div className="sticky top-14 md:top-16 z-20 bg-slate-50/95 dark:bg-slate-950/95 backdrop-blur-sm pt-2 pb-2 -mx-4 px-4 md:mx-0 md:px-0">
        <div className="bg-slate-200/50 dark:bg-slate-800 rounded-2xl px-4 py-2 flex items-center gap-3 border-none focus-within:ring-2 focus-within:ring-brand-500 focus-within:bg-white dark:focus-within:bg-slate-900 transition-all">
          <SearchIcon className="w-5 h-5 text-slate-400" />
          <input 
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search messages or start new..."
            className="flex-1 bg-transparent border-none focus:ring-0 text-slate-900 dark:text-white placeholder:text-slate-500 text-base p-0"
          />
          {searchQuery && (
            <button 
              onClick={() => setSearchQuery('')}
              className="w-6 h-6 flex items-center justify-center rounded-full bg-slate-100 dark:bg-slate-700 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
            >
              <CloseIcon className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-2xl overflow-hidden border border-slate-200 dark:border-slate-800 shadow-sm min-h-[60vh]">
        {chatsLoading ? (
          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {[1, 2, 3, 4].map(i => (
              <div key={i} className="p-5 flex items-center gap-4">
                 <div className="w-14 h-14 rounded-full bg-slate-200 dark:bg-slate-800 animate-pulse flex-shrink-0"></div>
                 <div className="flex-1 space-y-3">
                    <div className="flex justify-between items-center">
                      <div className="h-4 w-32 bg-slate-200 dark:bg-slate-800 rounded-full animate-pulse"></div>
                      <div className="h-3 w-12 bg-slate-100 dark:bg-slate-800/50 rounded-full animate-pulse"></div>
                    </div>
                    <div className="h-3 w-48 bg-slate-100 dark:bg-slate-800/50 rounded-full animate-pulse"></div>
                 </div>
              </div>
            ))}
          </div>
        ) : (
          <>
            {/* New People Results */}
            {filteredContacts && filteredContacts.length > 0 && (
              <div className="border-b-4 border-slate-50 dark:border-slate-800/50">
                <div className="px-5 py-3 bg-slate-50/50 dark:bg-slate-800/30">
                  <h3 className="text-xs font-black text-brand-500 uppercase tracking-widest">Start New Chat</h3>
                </div>
                {filteredContacts.map(contact => (
                  <button
                    key={contact.id}
                    onClick={() => createChatMutation.mutate(contact.id)}
                    disabled={createChatMutation.isPending}
                    className="w-full p-5 flex items-center gap-4 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer border-b border-slate-100 dark:border-slate-800 transition-colors text-left group"
                  >
                    <div className="relative">
                      <Avatar src={contact.avatar} name={contact.name} size="lg" isOnline={contact.isOnline} />
                      <div className="absolute -bottom-1 -right-1 bg-brand-500 text-white rounded-full w-5 h-5 flex items-center justify-center border-2 border-white dark:border-slate-900 shadow-sm">
                        <PlusIcon className="w-3 h-3" />
                      </div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className="font-bold text-slate-900 dark:text-slate-100 group-hover:text-brand-500 transition-colors">{contact.name}</h3>
                      <p className="text-sm font-medium text-slate-500 dark:text-slate-400">@{contact.username}</p>
                    </div>
                  </button>
                ))}
              </div>
            )}

            {/* Existing Chats */}
            {searchQuery && filteredChats && filteredChats.length > 0 && (
               <div className="px-5 py-3 bg-slate-50/50 dark:bg-slate-800/30 border-b border-slate-100 dark:border-slate-800">
                  <h3 className="text-xs font-black text-slate-400 uppercase tracking-widest">Conversations</h3>
               </div>
            )}

            {filteredChats?.length ? (
              filteredChats.map(chat => {
                const partner = chat.participants.find(p => p.id !== user?.id);
                return (
                  <div 
                    key={chat.id}
                    onClick={() => navigate(`/chat/${chat.id}`)}
                    className="p-5 flex items-center gap-4 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer border-b last:border-0 border-slate-100 dark:border-slate-800 transition-colors group"
                  >
                    <Avatar src={partner?.avatar} name={partner?.name} size="lg" isOnline={partner?.isOnline} />
                    <div className="flex-1 min-w-0">
                      <div className="flex justify-between items-center mb-1">
                        <h3 className="font-black text-slate-900 dark:text-slate-100 truncate group-hover:text-brand-500 transition-colors">{partner?.name}</h3>
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-tighter">
                          {new Date(chat.updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                      <div className="flex justify-between items-center">
                        <p className="text-sm font-medium text-slate-500 dark:text-slate-400 truncate max-w-[85%]">
                          {chat.lastMessage?.senderId === user?.id ? 'You: ' : ''}{chat.lastMessage?.text || 'Start a conversation...'}
                        </p>
                        {chat.unreadCount > 0 && (
                          <span className="bg-brand-500 text-white text-[10px] font-black w-6 h-6 rounded-full flex items-center justify-center shadow-lg shadow-brand-500/20">
                            {chat.unreadCount}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })
            ) : (
              // Empty State
              <div className="p-12 text-center">
                {searchQuery ? (
                  <>
                     <div className="flex justify-center mb-2 opacity-50"><SearchIcon className="w-10 h-10 text-slate-400" /></div>
                     <p className="text-slate-400 italic">No conversations found.</p>
                     {!filteredContacts?.length && (
                        <p className="text-xs text-slate-500 mt-2">No new people found either.</p>
                     )}
                  </>
                ) : (
                  <>
                    <div className="flex justify-center mb-2 opacity-50"><ChatIcon className="w-10 h-10 text-slate-400" /></div>
                    <p className="text-slate-400 italic">No chats yet. Search to start one!</p>
                  </>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default ChatList;
