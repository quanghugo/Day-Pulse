import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { mockService } from '@/services/mock';

const PAGE_SIZE = 20;

export const useFeed = () => {
  return useInfiniteQuery({
    queryKey: ['feed'],
    queryFn: ({ pageParam = 0 }) => mockService.getFeed(pageParam as number, PAGE_SIZE),
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) => {
      return allPages.length < 10 ? allPages.length : undefined;
    },
  });
};

export const useSuggestedUsers = () => {
  return useQuery({ 
    queryKey: ['feed_suggested'], 
    queryFn: mockService.getSuggested 
  });
};

export const useTrendingTags = () => {
  return useQuery({ 
    queryKey: ['feed_trending'], 
    queryFn: mockService.getTrendingTags 
  });
};
