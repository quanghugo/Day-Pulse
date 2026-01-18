import { User } from './user';

export interface Status {
  id: string;
  userId: string;
  user: Partial<User>;
  content: string;
  mood?: string;
  tags: string[];
  createdAt: string;
  likes: number;
  commentsCount: number;
  hasLiked: boolean;
  likedBy?: Partial<User>[];
}

export interface Reaction {
  emoji: string;
  count: number;
  userIds: string[];
}

export interface Comment {
  id: string;
  userId: string;
  userName: string;
  content: string;
  createdAt: string;
}
