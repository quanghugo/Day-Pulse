
import React from 'react';

interface AvatarProps {
  src?: string;
  name?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  isOnline?: boolean;
  className?: string;
}

const Avatar: React.FC<AvatarProps> = ({ src, name, size = 'md', isOnline, className = '' }) => {
  const sizeClasses = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-10 h-10 text-sm',
    lg: 'w-14 h-14 text-base',
    xl: 'w-24 h-24 text-2xl',
  };

  const statusSize = size === 'sm' ? 'w-2.5 h-2.5' : 'w-3 h-3';

  return (
    <div className={`relative inline-block ${className}`}>
      {src ? (
        <img 
          src={src} 
          alt={name} 
          className={`${sizeClasses[size]} rounded-full object-cover border-2 border-white dark:border-slate-800 shadow-sm`} 
        />
      ) : (
        <div className={`${sizeClasses[size]} rounded-full bg-brand-100 text-brand-600 flex items-center justify-center font-bold uppercase border-2 border-white dark:border-slate-800 shadow-sm`}>
          {name?.charAt(0) || '?'}
        </div>
      )}
      {isOnline !== undefined && (
        <span className={`absolute bottom-0 right-0 ${statusSize} rounded-full border-2 border-white dark:border-slate-800 ${isOnline ? 'bg-green-500' : 'bg-slate-300'}`}></span>
      )}
    </div>
  );
};

export default Avatar;
