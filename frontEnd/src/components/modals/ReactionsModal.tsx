
import React, { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { User } from '@/types';
import Avatar from '@/components/ui/Avatar';

interface ReactionsModalProps {
  isOpen: boolean;
  onClose: () => void;
  users: Partial<User>[];
}

const ReactionsModal: React.FC<ReactionsModalProps> = ({ isOpen, onClose, users }) => {
  // Handle Body Scroll Lock for iOS Safari
  useEffect(() => {
    if (isOpen) {
      // Record current scroll position
      const scrollY = window.scrollY;
      
      // Apply locks: position fixed forces the body to stay put on iOS
      document.body.style.position = 'fixed';
      document.body.style.top = `-${scrollY}px`;
      document.body.style.width = '100%';
      document.body.style.overflow = 'hidden';
      document.body.style.paddingRight = 'var(--scrollbar-width)'; // Prevent layout shift if scrollbar disappears
    }

    return () => {
      // Only cleanup if we are the ones who locked it
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

  if (!isOpen) return null;

  return createPortal(
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
      <div 
        className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-300"
        onClick={(e) => {
          e.preventDefault();
          onClose();
        }}
        onTouchMove={(e) => e.preventDefault()} // Block touch moves on the backdrop
      />
      <div className="relative w-full max-w-sm bg-white dark:bg-slate-900 rounded-[2.5rem] shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200">
        <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
          <h3 className="text-xl font-black text-slate-900 dark:text-white">Reactions</h3>
          <button 
            onClick={onClose}
            className="w-10 h-10 flex items-center justify-center rounded-full bg-slate-50 dark:bg-slate-800 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
          >
            ✕
          </button>
        </div>
        
        <div className="max-h-[60vh] overflow-y-auto p-4 space-y-2 no-scrollbar overscroll-contain">
          {users.length > 0 ? (
            users.map((u, i) => (
              <div 
                key={u.id || i} 
                className="flex items-center gap-4 p-3 rounded-2xl hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors cursor-pointer group"
              >
                <Avatar src={u.avatar} name={u.name} size="md" />
                <div className="flex-1">
                  <p className="font-bold text-slate-900 dark:text-white group-hover:text-brand-500 transition-colors">
                    {u.name}
                  </p>
                  <p className="text-[10px] font-black uppercase text-slate-400 tracking-widest">
                    Pulse Liked
                  </p>
                </div>
                <span className="text-xl">❤️</span>
              </div>
            ))
          ) : (
            <div className="py-12 text-center opacity-40">
              <span className="text-4xl block mb-2">🏜️</span>
              <p className="text-sm font-bold italic">No reactions yet</p>
            </div>
          )}
        </div>

        <div className="p-4 bg-slate-50 dark:bg-slate-800/30 text-center">
          <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">
            {users.length} {users.length === 1 ? 'Pulse' : 'Pulses'} recorded
          </p>
        </div>
      </div>
    </div>,
    document.body
  );
};

export default ReactionsModal;
