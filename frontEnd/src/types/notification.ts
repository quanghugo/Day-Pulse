import { User } from './user';

export interface Notification {
  id: string;
  type: 'like' | 'comment' | 'follow';
  actor: User;
  pulseId?: string;
  content?: string; // For comment preview
  createdAt: string;
  isRead: boolean;
}
