# Getting Started - For React Beginners

## Prerequisites

Before you start, make sure you have:
- Node.js installed (v16 or higher)
- Basic understanding of JavaScript
- Text editor (VS Code recommended)
- Terminal/Command Prompt

## Installation

```bash
# 1. Navigate to project folder
cd daypulse

# 2. Install dependencies
npm install

# 3. Start development server
npm run dev

# 4. Open browser
# Visit: http://localhost:3000
```

## Project Tour

### Step 1: Understanding the Entry Point

**Start here**: `src/index.tsx`

```typescript
// This is where React starts
import ReactDOM from 'react-dom/client';
import App from './app/App';

// Find the HTML element with id="root"
const rootElement = document.getElementById('root');

// Create React app and attach it to that element
const root = ReactDOM.createRoot(rootElement);
root.render(<App />);
```

**What happens**: React takes over the `<div id="root">` in `index.html` and renders your entire app inside it.

### Step 2: The Main App Component

**Next**: `src/app/App.tsx`

```typescript
const App = () => {
  const { theme } = useUIStore(); // Get current theme
  
  // Apply theme to entire page
  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);
  
  return (
    <Providers>           {/* Setup React Query, Router, Toasts */}
      <ScrollToTop />     {/* Scroll to top on route change */}
      <AppRoutes />       {/* All your pages/routes */}
    </Providers>
  );
};
```

**Key concepts**:
- `useUIStore()` - Gets data from global state
- `useEffect()` - Runs code when something changes
- `<Providers>` - Wraps app with necessary tools
- `<AppRoutes>` - Defines all pages

### Step 3: Understanding Routes

**Then**: `src/app/routes.tsx`

```typescript
// Lazy load pages (only load when needed)
const Feed = lazy(() => import('@/features/feed/pages/Feed'));
const Login = lazy(() => import('@/features/auth/pages/Login'));

export const AppRoutes = () => {
  return (
    <Suspense fallback={<PageLoader />}>  {/* Show loader while page loads */}
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<Login />} />
        
        {/* Protected routes (need login) */}
        <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route path="feed" element={<Feed />} />
          <Route path="profile" element={<Profile />} />
          {/* ... more routes */}
        </Route>
      </Routes>
    </Suspense>
  );
};
```

**Key concepts**:
- `lazy()` - Load page only when user visits it
- `<Suspense>` - Show loading while page loads
- `<Route>` - Maps URL to component
- `<ProtectedRoute>` - Checks if user is logged in

## Your First Feature: Login

Let's trace how login works step by step.

### Step 1: User Visits /login

```
Browser URL: http://localhost:3000/#/login
         ↓
React Router matches route
         ↓
Loads: src/features/auth/pages/Login.tsx
         ↓
Login component renders
```

### Step 2: Login Component

**File**: `src/features/auth/pages/Login.tsx`

```typescript
const Login = () => {
  const navigate = useNavigate();        // For navigation
  const { setAuth } = useAuthStore();    // For storing auth
  const [loading, setLoading] = useState(false);
  
  const handleLogin = async (e) => {
    e.preventDefault();                  // Prevent page reload
    setLoading(true);                    // Show loading state
    
    try {
      // Call API
      const data = await mockService.login();
      // Returns: { user: {...}, tokens: {...} }
      
      // Store authentication
      setAuth(data.user, data.tokens);
      
      // Navigate to feed
      navigate('/feed');
    } catch (err) {
      alert('Login failed');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <form onSubmit={handleLogin}>
      <input type="text" name="username" />
      <input type="password" name="password" />
      <button type="submit" disabled={loading}>
        {loading ? 'SYNCING...' : 'SIGN IN'}
      </button>
    </form>
  );
};
```

**What each part does**:

1. **`useNavigate()`** - Hook to change pages programmatically
2. **`useAuthStore()`** - Hook to access/update auth state
3. **`useState(false)`** - Creates local state for loading
4. **`async/await`** - Waits for API call to finish
5. **`mockService.login()`** - Simulates API call
6. **`setAuth()`** - Saves user data globally
7. **`navigate('/feed')`** - Changes URL to /feed

### Step 3: What Happens After Login?

```
1. setAuth() called
         ↓
2. Zustand store updates
         ↓
3. Data saved to localStorage
         ↓
4. All components using useAuthStore() re-render
         ↓
5. navigate('/feed') called
         ↓
6. URL changes to /feed
         ↓
7. ProtectedRoute checks isAuthenticated
         ↓
8. User is authenticated ✓
         ↓
9. Feed page loads
```

## Understanding State Management

### Three Types of State

#### 1. Local State (useState)

**Use for**: Component-specific data

```typescript
const MyComponent = () => {
  const [count, setCount] = useState(0);  // Only this component knows about count
  
  return (
    <button onClick={() => setCount(count + 1)}>
      Clicked {count} times
    </button>
  );
};
```

#### 2. Global State (Zustand)

**Use for**: App-wide data (auth, theme, etc.)

```typescript
// Define store (src/store/authStore.ts)
export const useAuthStore = create((set) => ({
  user: null,
  isAuthenticated: false,
  setAuth: (user, tokens) => set({ user, tokens, isAuthenticated: true }),
}));

// Use in any component
const Header = () => {
  const { user, isAuthenticated } = useAuthStore();
  
  return <div>{isAuthenticated ? `Hello ${user.name}` : 'Please login'}</div>;
};
```

#### 3. Server State (React Query)

**Use for**: Data from API

```typescript
// Define hook (src/features/feed/hooks/useFeed.ts)
export const useFeed = () => {
  return useQuery({
    queryKey: ['feed'],
    queryFn: () => mockService.getFeed(),
  });
};

// Use in component
const Feed = () => {
  const { data, isLoading, error } = useFeed();
  
  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error!</div>;
  
  return (
    <div>
      {data.map(post => <Post key={post.id} {...post} />)}
    </div>
  );
};
```

## Common Tasks

### Task 1: Add a New Page

**Step 1**: Create page file
```typescript
// src/features/myfeature/pages/MyPage.tsx
const MyPage = () => {
  return <div>My New Page</div>;
};

export default MyPage;
```

**Step 2**: Add to routes
```typescript
// src/app/routes.tsx
const MyPage = lazy(() => import('@/features/myfeature/pages/MyPage'));

// Inside <Routes>:
<Route path="/mypage" element={<MyPage />} />
```

**Step 3**: Add navigation link
```typescript
// In any component:
<Link to="/mypage">Go to My Page</Link>
```

### Task 2: Fetch Data from API

**Step 1**: Create custom hook
```typescript
// src/features/myfeature/hooks/useMyData.ts
export const useMyData = () => {
  return useQuery({
    queryKey: ['mydata'],
    queryFn: () => mockService.getMyData(),
  });
};
```

**Step 2**: Use in component
```typescript
const MyComponent = () => {
  const { data, isLoading } = useMyData();
  
  if (isLoading) return <div>Loading...</div>;
  
  return <div>{data.map(item => <div key={item.id}>{item.name}</div>)}</div>;
};
```

### Task 3: Create a Form

```typescript
const MyForm = () => {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  
  const handleSubmit = (e) => {
    e.preventDefault();  // Important! Prevents page reload
    
    console.log('Submitted:', { name, email });
    
    // Clear form
    setName('');
    setEmail('');
  };
  
  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        value={name}
        onChange={(e) => setName(e.target.value)}
        placeholder="Name"
      />
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="Email"
      />
      <button type="submit">Submit</button>
    </form>
  );
};
```

### Task 4: Navigate Programmatically

```typescript
const MyComponent = () => {
  const navigate = useNavigate();
  
  const handleClick = () => {
    // Do something...
    
    // Then navigate
    navigate('/feed');
  };
  
  return <button onClick={handleClick}>Go to Feed</button>;
};
```

## Debugging Tips

### 1. Check Console

Open browser DevTools (F12) and look at Console tab for errors.

### 2. Use React DevTools

Install React DevTools extension to inspect component state.

### 3. Add Console Logs

```typescript
const MyComponent = () => {
  const { data } = useFeed();
  
  console.log('Feed data:', data);  // See what data looks like
  
  return <div>...</div>;
};
```

### 4. Check Network Tab

Open DevTools → Network tab to see API calls.

## File Structure Quick Reference

```
src/
├── app/              → App setup (start here)
├── features/         → All features (login, feed, chat, etc.)
│   └── feed/
│       ├── pages/    → Full pages (Feed.tsx)
│       ├── components/ → Reusable parts (StatusCard.tsx)
│       └── hooks/    → Data fetching (useFeed.ts)
├── components/       → Shared components (Avatar, Layout)
├── store/           → Global state (auth, theme)
├── services/        → API calls
└── types/           → TypeScript types
```

## Next Steps

1. **Read**: `docs/ARCHITECTURE.md` - Understand overall structure
2. **Read**: `docs/FEATURE_FLOWS.md` - See how features work
3. **Explore**: Start with `src/features/auth/pages/Login.tsx`
4. **Try**: Make a small change and see it update
5. **Build**: Create your own feature following the patterns

## Common Errors & Solutions

### Error: "Cannot find module '@/...'"

**Problem**: Path alias not recognized

**Solution**: Restart dev server (`npm run dev`)

### Error: "Hooks can only be called inside function components"

**Problem**: Using hooks outside component or in wrong place

**Solution**: Make sure hooks are at top level of component:

```typescript
// ✅ Correct
const MyComponent = () => {
  const { data } = useFeed();  // Top level
  return <div>...</div>;
};

// ❌ Wrong
const MyComponent = () => {
  if (condition) {
    const { data } = useFeed();  // Inside condition
  }
  return <div>...</div>;
};
```

### Error: "Too many re-renders"

**Problem**: Infinite loop in useEffect or state update

**Solution**: Add dependency array to useEffect:

```typescript
// ✅ Correct
useEffect(() => {
  fetchData();
}, []);  // Empty array = run once

// ❌ Wrong
useEffect(() => {
  fetchData();
});  // No array = run every render
```

## Resources

- [React Docs](https://react.dev)
- [React Query Docs](https://tanstack.com/query/latest)
- [Zustand Docs](https://github.com/pmndrs/zustand)
- [React Router Docs](https://reactrouter.com)

Happy coding! 🚀
