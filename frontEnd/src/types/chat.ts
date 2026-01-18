import { User } from './user';

export interface Chat {
  id: string;
  participants: User[];
  lastMessage?: Message;
  unreadCount: number;
  updatedAt: string;
}

export interface Message {
  id: string;
  chatId: string;
  senderId: string;
  text: string;
  type: 'text' | 'reaction' | 'system';
  createdAt: string;
  reaction?: string;
}

export interface ReminderJob {
  id: string;
  chatId: string;
  time: string; // ISO string or HH:mm
  type: 'once' | 'daily';
  target: 'self' | 'partner' | 'both';
  content: string;
  enabled: boolean;
  createdBy: string;
}
