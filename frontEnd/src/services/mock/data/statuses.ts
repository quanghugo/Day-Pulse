import { Status } from '@/types';
import { USERS } from './users';
import { User } from '@/types';

const MOODS = ['😊', '🔥', '☕️', '💻', '🏃‍♀️', '😴', '🎉', '🫠', '🚀', '🌈', '🤔', '🍕', '💪', '📚', '🎵', '✨', '📸', '🎮', '💡', '🌱'];
const TAGS = ['tech', 'fitness', 'vibes', 'coding', 'foodie', 'travel', 'design', 'music', 'weekend', 'productive'];

export const generateStatus = (id: string, index: number, specificUser?: User): Status => {
  const user = specificUser || USERS[index % USERS.length];
  const date = new Date(Date.now() - (index * 3600000));
  const reactionCount = Math.floor(Math.random() * 5) + 1;
  const likedBy = USERS.slice(0, reactionCount);

  return {
    id,
    userId: user.id,
    user,
    content: `Pulse update #${index + 1}: Enjoying a beautiful day and making progress on my latest project. Life is all about the little moments!`,
    mood: MOODS[index % MOODS.length],
    tags: [TAGS[index % TAGS.length], TAGS[(index + 1) % TAGS.length]],
    createdAt: date.toISOString(),
    likes: likedBy.length,
    commentsCount: Math.floor(Math.random() * 5),
    hasLiked: Math.random() > 0.8,
    likedBy: likedBy,
  };
};

export { MOODS, TAGS };
