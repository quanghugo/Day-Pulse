import { User, Status, Chat, Message, ReminderJob, Comment } from '@/types';
import { MOCK_USER, USERS, generateStatus, MOCK_CHATS, MOCK_NOTIFICATIONS, TAGS } from './data';

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));
const NETWORK_DELAY = 1000;

export const mockService = {
  login: async () => {
    await sleep(800);
    return { user: MOCK_USER, tokens: { accessToken: 'at', refreshToken: 'rt' } };
  },
  
  register: async (email: string) => {
    await sleep(1000);
    return { success: true, email };
  },

  verifyOtp: async (email: string, code: string) => {
    await sleep(1000);
    if (code !== '123456') throw new Error('Invalid code');
    const incompleteUser: User = {
      ...MOCK_USER,
      id: `u_${Math.random()}`,
      username: '',
      name: '', 
      followersCount: 0,
      followingCount: 0,
      isSetupComplete: false
    };
    return { user: incompleteUser, tokens: { accessToken: 'at', refreshToken: 'rt' } };
  },

  completeSetup: async (userId: string, data: { name: string, username: string, bio: string }) => {
    await sleep(1000);
    return { ...MOCK_USER, id: userId, name: data.name, username: data.username, bio: data.bio, isSetupComplete: true } as User;
  },

  requestPasswordReset: async (email: string) => {
    await sleep(1500);
    return true;
  },

  getFeed: async (page: number = 0, limit: number = 20) => {
    await sleep(NETWORK_DELAY);
    const start = page * limit;
    return Array.from({ length: limit }, (_, i) => generateStatus(`s-${start + i}`, start + i));
  },

  getUserById: async (id: string): Promise<User | null> => {
    await sleep(500);
    if (id === MOCK_USER.id) return MOCK_USER;
    const found = USERS.find(u => u.id === id);
    return found || null;
  },

  getUserStatuses: async (userId: string) => {
    await sleep(800);
    const user = userId === MOCK_USER.id ? MOCK_USER : USERS.find(u => u.id === userId);
    return Array.from({ length: 5 }, (_, i) => {
       const base = generateStatus(`us-${userId}-${i}`, i, user as User);
       return {
         ...base,
         content: `Personal update #${i + 1}: Sharing a moment from my DayPulse journey. ✨ #lifestyle #daily`,
         likes: 10 + i * 2,
       } as Status;
    });
  },

  createStatus: async (content: string, mood: string, tags: string[]) => {
    await sleep(500);
    return { id: Math.random().toString(), content, mood, tags, createdAt: new Date().toISOString(), likes: 0, commentsCount: 0, hasLiked: false, user: MOCK_USER, userId: MOCK_USER.id, likedBy: [] };
  },

  getChats: async () => {
    await sleep(NETWORK_DELAY);
    return MOCK_CHATS;
  },

  getChat: async (id: string) => {
    await sleep(NETWORK_DELAY);
    const found = MOCK_CHATS.find(c => c.id === id);
    if (found) return found;
    if (id.startsWith('c_new_') || id.startsWith('c')) {
       const partner = USERS[Math.floor(Math.random() * USERS.length)];
       return { id, participants: [MOCK_USER, partner as User], unreadCount: 0, updatedAt: new Date().toISOString() } as Chat;
    }
    return null;
  },

  getMessages: async (chatId: string) => {
    await sleep(NETWORK_DELAY);
    return [
      { id: 'm1', chatId, senderId: 'u2', text: 'Hey!', type: 'text', createdAt: new Date(Date.now() - 100000).toISOString() },
      { id: 'm2', chatId, senderId: 'u1', text: 'Hi, hows it going?', type: 'text', createdAt: new Date(Date.now() - 50000).toISOString() },
    ] as Message[];
  },

  getReminders: async (chatId: string) => {
    await sleep(200);
    return [{ id: 'r1', chatId, time: '10:00', type: 'daily', target: 'both', content: 'Update your DayPulse status!', enabled: true, createdBy: 'u1' }] as ReminderJob[];
  },

  getAvailableUsers: async () => {
    await sleep(300);
    return USERS as User[];
  },

  createChat: async (targetUserId: string) => {
    await sleep(500);
    const existing = MOCK_CHATS.find(c => c.participants.some(p => p.id === targetUserId));
    if (existing) return existing;
    const target = USERS.find(u => u.id === targetUserId);
    return { id: `c_new_${Math.random().toString(36).substr(2, 9)}`, participants: [MOCK_USER, target as User], unreadCount: 0, updatedAt: new Date().toISOString() } as Chat;
  },

  getComments: async (statusId: string) => {
    await sleep(400);
    return USERS.slice(0, 3).map((u, i) => ({
      id: `cm-${statusId}-${i}`, userId: u.id, userName: u.name, content: `This is a pulse comment #${i + 1}! Keep up the great vibes.`, createdAt: new Date(Date.now() - i * 600000).toISOString(),
    })) as Comment[];
  },

  addComment: async (statusId: string, content: string) => {
    await sleep(600);
    return { id: Math.random().toString(), userId: MOCK_USER.id, userName: MOCK_USER.name, content, createdAt: new Date().toISOString() } as Comment;
  },
  
  search: async (query: string) => {
    await sleep(NETWORK_DELAY);
    const lowerQuery = query.toLowerCase();
    const matchedUsers = USERS.filter(u => u.name.toLowerCase().includes(lowerQuery) || u.username.toLowerCase().includes(lowerQuery));
    const matchedTags = TAGS.filter(t => t.toLowerCase().includes(lowerQuery));
    return { users: matchedUsers, tags: matchedTags };
  },
  
  getSuggested: async () => {
    await sleep(300);
    return USERS.slice(0, 5); 
  },
  
  getTrendingTags: async () => {
    await sleep(300);
    return [
      { tag: 'tech', count: '12.5k' },
      { tag: 'weekend', count: '8.2k' },
      { tag: 'coding', count: '5.1k' },
      { tag: 'vibes', count: '3.4k' },
      { tag: 'music', count: '2.9k' },
    ];
  },

  getNotifications: async () => {
    await sleep(NETWORK_DELAY);
    return MOCK_NOTIFICATIONS;
  },

  getFollowers: async (userId: string) => {
    await sleep(NETWORK_DELAY);
    // Shuffle and return 10 random users as followers
    return [...USERS].sort(() => 0.5 - Math.random()).slice(0, 10);
  },

  getFollowing: async (userId: string) => {
    await sleep(NETWORK_DELAY);
    // Shuffle and return 8 random users as following
    return [...USERS].sort(() => 0.5 - Math.random()).slice(0, 8);
  }
};
