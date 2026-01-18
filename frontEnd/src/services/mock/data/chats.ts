import { Chat } from '@/types';
import { MOCK_USER, USERS } from './users';

export const generateChats = (count: number): Chat[] => {
  return Array.from({ length: count }, (_, i) => {
    const partner = USERS[i % USERS.length];
    const isUnread = i < 5;
    const timeOffset = i * 1000 * 60 * 30;
    
    return {
      id: `c${i}`,
      participants: [MOCK_USER, partner],
      unreadCount: isUnread ? Math.floor(Math.random() * 3) + 1 : 0,
      updatedAt: new Date(Date.now() - timeOffset).toISOString(),
      lastMessage: {
        id: `m${i}`,
        chatId: `c${i}`,
        senderId: Math.random() > 0.5 ? MOCK_USER.id : partner.id,
        text: Math.random() > 0.5 ? 'Hey, how are you doing?' : 'Did you see the latest post?',
        type: 'text',
        createdAt: new Date(Date.now() - timeOffset).toISOString()
      }
    };
  });
};

export const MOCK_CHATS = generateChats(20);
