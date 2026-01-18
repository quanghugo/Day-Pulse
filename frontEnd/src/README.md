# DayPulse - Restructured Architecture

## Overview

This project has been restructured from a flat file structure to a scalable, feature-based architecture with proper separation of concerns, code splitting, and performance optimizations.

## Project Structure

```
src/
├── app/                          # Application shell
│   ├── App.tsx                   # Main app component with theme management
│   ├── routes.tsx                # Lazy-loaded route definitions
│   └── providers.tsx             # Combined providers (QueryClient, Router, Toast)
│
├── components/                   # Shared components
│   ├── ui/                       # Base UI components
│   │   ├── Avatar.tsx
│   │   ├── EmojiPicker.tsx
│   │   └── index.ts
│   ├── icons/
│   │   └── index.tsx             # All icon components
│   ├── modals/
│   │   ├── CommentsModal.tsx
│   │   ├── ReactionsModal.tsx
│   │   ├── UserListModal.tsx
│   │   └── index.ts
│   └── layout/
│       ├── Layout.tsx            # Main layout with navigation
│       ├── ToastProvider.tsx
│       └── index.ts
│
├── features/                     # Feature modules (domain-driven)
│   ├── auth/
│   │   └── pages/
│   │       ├── Login.tsx
│   │       ├── Register.tsx
│   │       ├── ForgotPassword.tsx
│   │       ├── VerifyOTP.tsx
│   │       ├── SetupProfile.tsx
│   │       └── index.ts
│   │
│   ├── feed/
│   │   ├── components/
│   │   │   ├── StatusCard.tsx
│   │   │   ├── StatusComposer.tsx  # Extracted from Feed page
│   │   │   └── index.ts
│   │   ├── pages/
│   │   │   ├── Feed.tsx            # Refactored, now 200 lines
│   │   │   └── index.ts
│   │   └── hooks/
│   │       └── useFeed.ts          # Data fetching logic extracted
│   │
│   ├── chat/
│   │   └── pages/
│   │       ├── ChatList.tsx
│   │       ├── ChatRoom.tsx
│   │       └── index.ts
│   │
│   ├── profile/
│   │   └── pages/
│   │       ├── Profile.tsx
│   │       ├── EditProfile.tsx
│   │       └── index.ts
│   │
│   ├── notifications/
│   │   └── pages/
│   │       ├── Notifications.tsx
│   │       └── index.ts
│   │
│   ├── search/
│   │   └── pages/
│   │       ├── Search.tsx
│   │       └── index.ts
│   │
│   └── settings/
│       └── pages/
│           ├── Settings.tsx
│           └── index.ts
│
├── services/
│   ├── api/
│   │   └── client.ts             # Axios instance with interceptors
│   └── mock/
│       ├── data/                 # Separated mock data
│       │   ├── users.ts
│       │   ├── statuses.ts
│       │   ├── chats.ts
│       │   ├── notifications.ts
│       │   └── index.ts
│       ├── handlers.ts           # Service methods only
│       └── index.ts
│
├── store/
│   ├── authStore.ts              # Authentication state
│   ├── uiStore.ts                # UI state (theme, language, toasts)
│   └── index.ts                  # Combined exports
│
├── hooks/                        # Shared custom hooks
│   └── useTranslation.ts
│
├── types/                        # Domain-specific types
│   ├── user.ts
│   ├── status.ts
│   ├── chat.ts
│   ├── notification.ts
│   ├── common.ts
│   └── index.ts
│
├── lib/                          # Utilities (future use)
│   ├── constants.ts
│   └── utils.ts
│
├── i18n/
│   ├── translations/
│   │   ├── en.ts
│   │   └── vi.ts
│   └── index.ts
│
└── index.tsx                     # Application entry point
```

## Key Improvements

### 1. **Route-Based Code Splitting** ⚡
- All pages are lazy-loaded using `React.lazy()`
- Reduces initial bundle size
- Faster initial page load
- Better performance on slower connections

### 2. **Feature-Based Organization** 📁
- Each feature is self-contained
- Easy to locate related code
- Scalable architecture
- Clear domain boundaries

### 3. **Separated Concerns** 🎯
- **Components**: Reusable UI elements
- **Pages**: Route-level components
- **Hooks**: Data fetching and business logic
- **Store**: State management
- **Services**: API and mock data
- **Types**: TypeScript definitions

### 4. **Path Aliases** 🔗
Clean imports using `@/` prefix:
```typescript
// Before
import { useAuthStore } from '../../../store';

// After
import { useAuthStore } from '@/store';
```

### 5. **Mock Data Organization** 📊
- Split 300-line `mock.ts` into:
  - `data/users.ts` - User fixtures
  - `data/statuses.ts` - Status generators
  - `data/chats.ts` - Chat data
  - `data/notifications.ts` - Notification data
  - `handlers.ts` - Service methods only

### 6. **Component Extraction** ✂️
- **Feed.tsx**: 360 lines → 200 lines
  - Extracted `StatusComposer` component
  - Created `useFeed` hook for data logic
- **ChatRoom.tsx**: Ready for future extraction
  - Can be split into `ChatHeader`, `MessageList`, `ChatInput`, `ReminderSheet`

### 7. **Type Safety** 🛡️
- Domain-specific type modules
- Better IntelliSense
- Easier to maintain
- Clear data contracts

## Development

### Running the Project
```bash
npm run dev      # Start development server
npm run build    # Build for production
npm run preview  # Preview production build
```

### Adding a New Feature
1. Create feature folder in `src/features/`
2. Add pages, components, hooks as needed
3. Create index.ts for exports
4. Add lazy-loaded route in `src/app/routes.tsx`

### Adding a New Component
- **Shared UI**: `src/components/ui/`
- **Feature-specific**: `src/features/{feature}/components/`
- **Modals**: `src/components/modals/`

## Performance Benefits

1. **Lazy Loading**: Only load code when needed
2. **Code Splitting**: Smaller initial bundle
3. **Tree Shaking**: Remove unused code
4. **Better Caching**: Separate chunks for features
5. **Faster Builds**: Clearer module boundaries

## Migration Notes

- All old root-level files have been removed
- Duplicate `src/pages/Notifications.tsx` deleted
- Path aliases configured in `vite.config.ts` and `tsconfig.json`
- All imports updated to use `@/` prefix

## Future Enhancements

- [ ] Extract ChatRoom sub-components
- [ ] Add unit tests for hooks
- [ ] Implement error boundaries per feature
- [ ] Add Storybook for component documentation
- [ ] Implement React Query devtools
- [ ] Add performance monitoring
