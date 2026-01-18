import React, { useState, useRef, useEffect } from 'react';

interface EmojiPickerProps {
  selectedEmoji: string;
  onSelect: (emoji: string) => void;
}

const EMOJI_LIST = [
  '😊', '🔥', '☕️', '💻', '🏃‍♀️', '😴', '🎉', '🫠', '🚀', '🌈',
  '🤔', '🍕', '💪', '📚', '🎵', '✨', '📸', '🎮', '💡', '🌱'
];

const EmojiPicker: React.FC<EmojiPickerProps> = ({ selectedEmoji, onSelect }) => {
  const [isOpen, setIsOpen] = useState(false);
  const pickerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (pickerRef.current && !pickerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="relative" ref={pickerRef}>
      <label className="text-[10px] absolute -top-4 left-0 font-black uppercase text-slate-400 tracking-tighter">Mood</label>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="bg-slate-100 dark:bg-slate-800 rounded-xl text-xl p-2 border-none ring-offset-white dark:ring-offset-slate-900 focus:ring-2 focus:ring-brand-500 w-12 h-12 flex items-center justify-center transition-all hover:bg-slate-200 dark:hover:bg-slate-700 active:scale-95"
      >
        {selectedEmoji}
      </button>

      {isOpen && (
        <div className="absolute top-full mt-2 left-0 z-[60] w-64 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl shadow-2xl p-4 animate-in zoom-in slide-in-from-top-2 duration-200">
          <div className="grid grid-cols-5 gap-2">
            {EMOJI_LIST.map((emoji) => (
              <button
                key={emoji}
                type="button"
                onClick={() => {
                  onSelect(emoji);
                  setIsOpen(false);
                }}
                className={`text-2xl p-2 rounded-xl transition-all hover:scale-125 hover:bg-slate-50 dark:hover:bg-slate-800 ${
                  selectedEmoji === emoji ? 'bg-brand-50 dark:bg-brand-500/10 ring-1 ring-brand-500' : ''
                }`}
              >
                {emoji}
              </button>
            ))}
          </div>
          <div className="mt-3 pt-3 border-t border-slate-100 dark:border-slate-800 flex justify-center">
            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Select your vibe</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default EmojiPicker;