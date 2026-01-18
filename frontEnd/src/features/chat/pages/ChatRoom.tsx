import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { mockService } from '@/services/mock';
import { useAuthStore, useUIStore } from '@/store';
import { useTranslation } from '@/i18n';
import Avatar from '@/components/ui/Avatar';
import { Message, ReminderJob } from '@/types';
import { BackIcon, ClockIcon, AttachmentIcon, SendIcon, CloseIcon } from '@/components/icons';

const ChatRoom: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { language, addToast } = useUIStore();
  const t = useTranslation(language);
  const queryClient = useQueryClient();
  const scrollRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const [inputText, setInputText] = useState('');
  const [showReminders, setShowReminders] = useState(false);
  const [isTyping, setIsTyping] = useState(false);

  // Reminder Form State
  const [isAddingReminder, setIsAddingReminder] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
  const [newReminder, setNewReminder] = useState({
    time: '09:00',
    content: 'Update your DayPulse status!',
    target: 'both' as ReminderJob['target'],
    type: 'daily' as ReminderJob['type']
  });

  const { data: chat, isLoading: chatLoading } = useQuery({
    queryKey: ['chat', id],
    queryFn: () => mockService.getChat(id!),
  });

  const { data: messages, isLoading: messagesLoading } = useQuery({
    queryKey: ['messages', id],
    queryFn: () => mockService.getMessages(id!),
  });

  const { data: reminders } = useQuery({
    queryKey: ['reminders', id],
    queryFn: () => mockService.getReminders(id!),
  });

  const partner = chat?.participants.find(p => p.id !== user?.id);

  const sendMutation = useMutation({
    mutationFn: (text: string) => Promise.resolve({
      id: Math.random().toString(),
      chatId: id!,
      senderId: user?.id!,
      text,
      type: 'text',
      createdAt: new Date().toISOString()
    } as Message),
    onSuccess: (newMsg) => {
      queryClient.setQueryData(['messages', id], (old: any) => [...(old || []), newMsg]);
      setInputText('');
    }
  });

  const saveReminderMutation = useMutation({
    mutationFn: (reminder: any) => {
      if (editingId) {
        return Promise.resolve({ ...reminder, id: editingId, enabled: true });
      }
      return Promise.resolve({ ...reminder, id: Math.random().toString(), enabled: true });
    },
    onSuccess: (savedRem) => {
      queryClient.setQueryData(['reminders', id], (old: any) => {
        if (editingId) {
          return old.map((r: any) => r.id === editingId ? savedRem : r);
        }
        return [...(old || []), savedRem];
      });
      setIsAddingReminder(false);
      setEditingId(null);
      addToast(editingId ? 'Reminder updated! ⏰' : 'Reminder scheduled! ⏰', 'success');
    }
  });

  const deleteReminderMutation = useMutation({
    mutationFn: (reminderId: string) => Promise.resolve(reminderId),
    onSuccess: (deletedId) => {
      queryClient.setQueryData(['reminders', id], (old: any) => 
        (old || []).filter((r: any) => r.id !== deletedId)
      );
      setConfirmDeleteId(null);
      addToast('Reminder deleted 🗑️', 'info');
    }
  });

  const handleSend = () => {
    if (!inputText.trim()) return;
    sendMutation.mutate(inputText);
    // Keep focus after sending for rapid messaging
    inputRef.current?.focus();
  };

  const handleEditClick = (reminder: ReminderJob) => {
    setNewReminder({
      time: reminder.time,
      content: reminder.content,
      target: reminder.target,
      type: reminder.type
    });
    setEditingId(reminder.id);
    setIsAddingReminder(true);
  };

  const handleAddClick = () => {
    setNewReminder({
      time: '09:00',
      content: 'Update your DayPulse status!',
      target: 'both',
      type: 'daily'
    });
    setEditingId(null);
    setIsAddingReminder(true);
  };

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, isTyping]);

  useEffect(() => {
    const timer = setTimeout(() => setIsTyping(true), 2000);
    const stopTimer = setTimeout(() => setIsTyping(false), 5000);
    return () => { clearTimeout(timer); clearTimeout(stopTimer); };
  }, []);

  if (messagesLoading || chatLoading) return (
    <div className="flex flex-col h-[100dvh] bg-white dark:bg-slate-900 animate-pulse">
      <div className="h-16 w-full bg-slate-100 dark:bg-slate-800 mb-4 flex-shrink-0"></div>
      <div className="flex-1 p-4 space-y-6 overflow-hidden">
         <div className="flex justify-start">
             <div className="h-10 w-32 bg-slate-100 dark:bg-slate-800 rounded-2xl rounded-bl-none"></div>
         </div>
         <div className="flex justify-end">
             <div className="h-14 w-48 bg-slate-200 dark:bg-slate-800 rounded-2xl rounded-br-none"></div>
         </div>
         <div className="flex justify-start">
             <div className="h-20 w-56 bg-slate-100 dark:bg-slate-800 rounded-2xl rounded-bl-none"></div>
         </div>
         <div className="flex justify-end">
             <div className="h-8 w-24 bg-slate-200 dark:bg-slate-800 rounded-2xl rounded-br-none"></div>
         </div>
      </div>
      <div className="h-20 w-full bg-slate-100 dark:bg-slate-800 mt-auto flex-shrink-0"></div>
    </div>
  );

  return (
    <div className={`
      /* Mobile: Fullscreen using dynamic viewport height */
      fixed inset-0 z-[100] bg-white dark:bg-slate-900 flex flex-col h-dvh
      /* Desktop: Static card layout */
      md:static md:z-auto md:h-[calc(100vh-theme(spacing.16)-theme(spacing.8))] md:rounded-[2.5rem] md:overflow-hidden md:border md:border-slate-200 md:dark:border-slate-800 md:shadow-2xl
      animate-in slide-in-from-right duration-300 md:animate-none
    `}>
      
      {/* Header */}
      <div className="flex-shrink-0 pt-safe bg-white/95 dark:bg-slate-900/95 backdrop-blur-md border-b border-slate-100 dark:border-slate-800 z-30">
        <div className="flex items-center justify-between p-3 md:p-5">
          <div className="flex items-center gap-2">
            <button 
              onClick={() => navigate('/chat')} 
              className="p-2 -ml-2 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-full transition-colors text-slate-600 dark:text-slate-300"
            >
              <BackIcon className="w-6 h-6" />
            </button>
            <div className="flex items-center gap-3 cursor-pointer">
              <Avatar src={partner?.avatar} name={partner?.name} size="md" isOnline={partner?.isOnline} />
              <div className="flex flex-col">
                <h3 className="font-bold text-base text-slate-900 dark:text-white leading-tight">{partner?.name}</h3>
                <span className="text-[10px] text-slate-400 font-bold hidden md:block">{partner?.isOnline ? t('online') : t('offline')}</span>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-1 md:gap-2">
            <button 
              onClick={() => setShowReminders(true)}
              className={`p-2 rounded-full transition-all ${showReminders ? 'bg-brand-500 text-white' : 'text-slate-400 hover:text-brand-500 hover:bg-slate-50 dark:hover:bg-slate-800'}`}
            >
              <ClockIcon className="w-6 h-6" />
            </button>
            <button className="p-2 rounded-full text-brand-500 bg-brand-50 dark:bg-slate-800">
              <span className="text-xl">📞</span>
            </button>
          </div>
        </div>
      </div>

      {/* Messages Area */}
      <div 
        ref={scrollRef} 
        className="flex-1 overflow-y-auto no-scrollbar bg-slate-50/50 dark:bg-slate-950/50 p-4 md:p-8 space-y-4 overscroll-contain"
      >
        <div className="py-8 text-center opacity-60">
          <Avatar src={partner?.avatar} name={partner?.name} size="xl" className="mx-auto mb-3" />
          <p className="text-xs text-slate-400 font-medium">You've been connected for 8 months</p>
        </div>

        {messages?.map((msg) => {
          const isMe = msg.senderId === user?.id;
          return (
            <div key={msg.id} className={`flex ${isMe ? 'justify-end' : 'justify-start'} animate-in slide-in-from-bottom-2 duration-300`}>
              <div className={`max-w-[80%] md:max-w-[70%] px-4 py-2.5 rounded-[1.2rem] text-[15px] shadow-sm ${
                isMe 
                  ? 'bg-brand-500 text-white rounded-br-none' 
                  : 'bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 rounded-bl-none border border-slate-100 dark:border-slate-700/50'
              }`}>
                {msg.text}
              </div>
            </div>
          );
        })}
        
        {isTyping && (
          <div className="flex justify-start">
             <div className="bg-slate-200 dark:bg-slate-800 text-slate-500 text-xs px-4 py-2 rounded-full rounded-bl-none italic animate-pulse font-medium">
               Typing...
             </div>
          </div>
        )}
        <div className="h-2"></div>
      </div>

      {/* Input Area */}
      <div className="flex-shrink-0 p-3 md:p-6 bg-white dark:bg-slate-900 border-t border-slate-100 dark:border-slate-800 flex items-end gap-2 z-30 pb-safe">
        <button className="w-10 h-10 mb-1 flex items-center justify-center text-slate-400 hover:text-brand-500 transition-colors">
          <AttachmentIcon className="w-6 h-6" />
        </button>
        <div className="flex-1 bg-slate-100 dark:bg-slate-800 rounded-[1.5rem] px-4 py-2 flex items-center focus-within:ring-2 focus-within:ring-brand-500/50 transition-all min-h-[48px]">
          <input
            ref={inputRef}
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            placeholder="Message..."
            className="flex-1 bg-transparent border-none focus:ring-0 text-base py-1 text-slate-900 dark:text-white placeholder:text-slate-500"
          />
          <button className="p-1 text-xl opacity-60 hover:opacity-100 transition-opacity">😊</button>
        </div>
        {inputText.trim() ? (
          <button 
            onClick={handleSend}
            className="w-10 h-10 mb-1 rounded-full bg-brand-500 text-white flex items-center justify-center shadow-lg active:scale-90 transition-transform"
          >
            <SendIcon className="w-5 h-5" />
          </button>
        ) : (
          <button className="w-10 h-10 mb-1 flex items-center justify-center text-2xl text-slate-400 hover:text-brand-500 transition-colors">
            👍
          </button>
        )}
      </div>

      {/* Modern Reminder Bottom Sheet */}
      {showReminders && (
        <div className="fixed inset-0 z-[110] flex flex-col justify-end md:justify-center md:items-center">
          {/* Backdrop */}
          <div 
            className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-300"
            onClick={() => setShowReminders(false)}
          />
          
          {/* Sheet Container */}
          <div className="relative w-full h-[85dvh] md:h-auto md:max-h-[80vh] md:max-w-md bg-white dark:bg-slate-900 rounded-t-[2.5rem] md:rounded-[2.5rem] shadow-2xl flex flex-col overflow-hidden animate-in slide-in-from-bottom duration-300 md:zoom-in-95">
            
            {/* Mobile Drag Handle Visual */}
            <div className="w-full flex justify-center pt-3 pb-1 md:hidden flex-shrink-0 cursor-pointer" onClick={() => setShowReminders(false)}>
              <div className="w-12 h-1.5 bg-slate-200 dark:bg-slate-700 rounded-full"></div>
            </div>

            {/* Header */}
            <div className="px-6 py-4 flex items-center justify-between flex-shrink-0 border-b border-slate-50 dark:border-slate-800">
              <div>
                <h3 className="text-xl font-black text-slate-900 dark:text-white flex items-center gap-2">
                  <ClockIcon className="w-6 h-6 text-brand-500" />
                  {isAddingReminder ? (editingId ? 'Edit Reminder' : 'New Reminder') : 'Reminders'}
                </h3>
              </div>
              {!isAddingReminder && (
                <button 
                  onClick={() => setShowReminders(false)}
                  className="w-8 h-8 flex items-center justify-center bg-slate-100 dark:bg-slate-800 rounded-full text-slate-500 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors"
                >
                  <CloseIcon className="w-5 h-5" />
                </button>
              )}
            </div>

            {/* Main Content Scroll Area */}
            <div className="flex-1 overflow-y-auto p-4 md:p-6 no-scrollbar relative">
              
              {/* Delete Confirmation Overlay */}
              {confirmDeleteId && (
                <div className="absolute inset-0 z-20 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md flex items-center justify-center p-6 animate-in fade-in duration-200">
                  <div className="bg-white dark:bg-slate-950 p-6 rounded-3xl shadow-xl border border-slate-100 dark:border-slate-800 w-full text-center">
                    <div className="w-16 h-16 bg-red-100 text-red-500 rounded-full flex items-center justify-center text-3xl mx-auto mb-4">
                      🗑️
                    </div>
                    <h4 className="text-lg font-black text-slate-900 dark:text-white mb-2">Delete this reminder?</h4>
                    <p className="text-sm text-slate-500 mb-6">This action cannot be undone.</p>
                    <div className="flex gap-3">
                      <button 
                        onClick={() => setConfirmDeleteId(null)} 
                        className="flex-1 py-3 rounded-xl font-bold bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300"
                      >
                        Cancel
                      </button>
                      <button 
                        onClick={() => deleteReminderMutation.mutate(confirmDeleteId)} 
                        className="flex-1 py-3 rounded-xl font-bold bg-red-500 text-white shadow-lg shadow-red-500/20"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {isAddingReminder ? (
                /* Edit/Add Form */
                <div className="space-y-6 animate-in slide-in-from-right duration-300">
                  <div className="bg-slate-50 dark:bg-slate-800/50 p-1 rounded-3xl">
                    <div className="bg-white dark:bg-slate-900 rounded-[1.2rem] p-4 mb-1 shadow-sm border border-slate-100 dark:border-slate-800">
                       <label className="text-[10px] font-black uppercase text-slate-400 tracking-widest mb-1 block">Time</label>
                       <div className="flex items-center gap-3">
                         <input 
                           type="time" 
                           value={newReminder.time} 
                           onChange={(e) => setNewReminder({ ...newReminder, time: e.target.value })} 
                           className="flex-1 bg-transparent text-3xl font-black text-slate-900 dark:text-white border-none p-0 focus:ring-0"
                         />
                       </div>
                    </div>
                    <div className="bg-white dark:bg-slate-900 rounded-[1.2rem] p-4 shadow-sm border border-slate-100 dark:border-slate-800">
                       <label className="text-[10px] font-black uppercase text-slate-400 tracking-widest mb-2 block">Frequency</label>
                       <div className="flex gap-2">
                         {['once', 'daily'].map((t) => (
                           <button
                             key={t}
                             onClick={() => setNewReminder({ ...newReminder, type: t as any })}
                             className={`flex-1 py-2 rounded-xl text-sm font-bold capitalize transition-all ${
                               newReminder.type === t 
                                 ? 'bg-brand-500 text-white shadow-md' 
                                 : 'bg-slate-100 dark:bg-slate-800 text-slate-500'
                             }`}
                           >
                             {t}
                           </button>
                         ))}
                       </div>
                    </div>
                  </div>

                  <div className="bg-white dark:bg-slate-900 rounded-[1.5rem] p-4 shadow-sm border border-slate-100 dark:border-slate-800">
                     <label className="text-[10px] font-black uppercase text-slate-400 tracking-widest mb-2 block">Reminder Message</label>
                     <textarea 
                       value={newReminder.content} 
                       onChange={(e) => setNewReminder({ ...newReminder, content: e.target.value })} 
                       className="w-full bg-slate-50 dark:bg-slate-800/50 border-none rounded-xl p-3 text-base font-medium resize-none focus:ring-2 focus:ring-brand-500/50"
                       rows={3}
                       placeholder="What needs to be done?"
                     />
                  </div>
                </div>
              ) : (
                /* List View */
                <div className="space-y-4">
                  <div 
                    onClick={handleAddClick}
                    className="bg-brand-50 dark:bg-brand-500/10 border border-brand-200 dark:border-brand-500/20 p-4 rounded-2xl flex items-center gap-3 cursor-pointer hover:bg-brand-100 dark:hover:bg-brand-500/20 transition-colors active:scale-[0.99] group"
                  >
                     <div className="w-10 h-10 bg-brand-500 text-white rounded-full flex items-center justify-center text-xl shadow-lg shadow-brand-500/30 group-hover:scale-110 transition-transform">
                       +
                     </div>
                     <div>
                       <h4 className="font-bold text-brand-700 dark:text-brand-300">Create New Reminder</h4>
                       <p className="text-xs text-brand-600/70 dark:text-brand-400/70 font-medium">Schedule a ping for you or your partner</p>
                     </div>
                  </div>

                  <div className="space-y-3">
                    {reminders?.map(r => (
                      <div key={r.id} className="bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800 p-4 rounded-2xl shadow-sm flex items-start justify-between group">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-lg font-black text-slate-900 dark:text-white">{r.time}</span>
                            <span className="text-[10px] font-bold uppercase bg-slate-100 dark:bg-slate-800 text-slate-500 px-2 py-0.5 rounded-md">{r.type}</span>
                          </div>
                          <p className="text-sm font-medium text-slate-500 dark:text-slate-400 leading-snug">{r.content}</p>
                        </div>
                        <div className="flex gap-2 pl-2">
                           <button onClick={() => handleEditClick(r)} className="w-9 h-9 flex items-center justify-center bg-slate-50 dark:bg-slate-800 rounded-full text-slate-400 hover:text-brand-500 transition-colors">
                             ✏️
                           </button>
                           <button onClick={() => setConfirmDeleteId(r.id)} className="w-9 h-9 flex items-center justify-center bg-slate-50 dark:bg-slate-800 rounded-full text-slate-400 hover:text-red-500 transition-colors">
                             🗑️
                           </button>
                        </div>
                      </div>
                    ))}
                    {reminders?.length === 0 && (
                      <div className="text-center py-8">
                        <p className="text-slate-400 text-sm font-medium">No reminders active</p>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Footer Actions */}
            <div className="p-4 md:p-6 bg-white dark:bg-slate-900 border-t border-slate-50 dark:border-slate-800 pb-safe">
              {isAddingReminder ? (
                <div className="flex gap-3">
                  <button 
                    onClick={() => { setIsAddingReminder(false); setEditingId(null); }} 
                    className="flex-1 py-4 rounded-2xl font-bold bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 active:scale-95 transition-transform"
                  >
                    Cancel
                  </button>
                  <button 
                    onClick={() => saveReminderMutation.mutate(newReminder)} 
                    className="flex-1 py-4 rounded-2xl font-black bg-brand-500 text-white shadow-lg shadow-brand-500/25 active:scale-95 transition-transform"
                  >
                    Save Reminder
                  </button>
                </div>
              ) : (
                <button 
                  onClick={() => setShowReminders(false)}
                  className="w-full py-4 rounded-2xl font-bold bg-slate-100 dark:bg-slate-800 text-slate-500 hover:bg-slate-200 dark:hover:bg-slate-700 active:scale-95 transition-transform"
                >
                  Close
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ChatRoom;