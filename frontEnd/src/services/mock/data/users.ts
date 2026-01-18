import { User } from '@/types';

export const MOCK_USER: User = {
  id: 'u1',
  name: 'Alex Rivera',
  username: 'arivera',
  avatar: 'https://picsum.photos/seed/alex/200',
  bio: 'Building the future of social interaction. 🚀',
  timezone: 'UTC+7',
  language: 'en',
  streak: 12,
  followersCount: 1280,
  followingCount: 450,
  lastUpdated: new Date().toISOString(),
  isOnline: true,
  isSetupComplete: true,
};

export const generateUsers = (count: number): User[] => {
  const baseUsers = [
    { id: 'u2', name: 'Sarah Chen', avatar: 'https://picsum.photos/seed/sarah/200', username: 'schen', isOnline: true },
    { id: 'u3', name: 'Jake Kim', avatar: 'https://picsum.photos/seed/jake/200', username: 'jkim', isOnline: false },
    { id: 'u4', name: 'Elena Rossi', avatar: 'https://picsum.photos/seed/elena/200', username: 'erossi', isOnline: true },
    { id: 'u5', name: 'Marcus Thorne', avatar: 'https://picsum.photos/seed/marcus/200', username: 'mthorne', isOnline: false },
    { id: 'u6', name: 'Aria Gupta', avatar: 'https://picsum.photos/seed/aria/200', username: 'agupta', isOnline: true },
  ];
  
  const users: User[] = baseUsers.map(u => ({
    ...u,
    bio: 'Just another DayPulse user exploring the digital pulse.',
    timezone: 'UTC',
    language: 'en',
    streak: Math.floor(Math.random() * 15),
    followersCount: Math.floor(Math.random() * 5000),
    followingCount: Math.floor(Math.random() * 1000),
    lastUpdated: new Date().toISOString(),
    isSetupComplete: true
  }));
  
  for(let i = 0; i < count; i++) {
    users.push({
      id: `u${i + 7}`,
      name: `User ${i + 7}`,
      avatar: `https://picsum.photos/seed/user${i + 7}/200`,
      username: `user_${i + 7}`,
      isOnline: Math.random() > 0.5,
      bio: 'Lover of life and coffee. ☕️',
      timezone: 'UTC',
      language: 'en',
      streak: Math.floor(Math.random() * 10),
      followersCount: Math.floor(Math.random() * 2000),
      followingCount: Math.floor(Math.random() * 500),
      lastUpdated: new Date().toISOString(),
      isSetupComplete: true
    });
  }
  return users;
};

export const USERS = generateUsers(25);
