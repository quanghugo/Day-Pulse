import { Notification } from '@/types';
import { USERS } from './users';

export const generateNotifications = (count: number): Notification[] => {
  const types: Notification['type'][] = ['like', 'comment', 'follow'];
  const notifications: Notification[] = [];
  const now = Date.now();

  for (let i = 0; i < count; i++) {
    const type = types[Math.floor(Math.random() * types.length)];
    const actor = USERS[Math.floor(Math.random() * USERS.length)];
    const timeOffset = Math.floor(Math.pow(Math.random(), 3) * 1000 * 60 * 60 * 24 * 7); 
    
    notifications.push({
      id: `n-${i}`,
      type,
      actor,
      pulseId: type !== 'follow' ? `p-${i}` : undefined,
      content: type === 'comment' ? ['Love this!', 'Amazing!', 'Great shot', 'Wow 🚀'][Math.floor(Math.random() * 4)] : undefined,
      createdAt: new Date(now - timeOffset).toISOString(),
      isRead: timeOffset > 1000 * 60 * 60 * 2,
    });
  }
  
  return notifications.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
};

export const MOCK_NOTIFICATIONS = generateNotifications(30);
