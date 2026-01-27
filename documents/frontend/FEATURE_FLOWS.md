# Feature Flows - Visual Guide

## Quick Reference

| Feature | Entry Point | Key Components | Data Source |
|---------|------------|----------------|-------------|
| Authentication | `/login` | Login, Register, VerifyOTP, SetupProfile | `useAuthStore` |
| Feed | `/feed` | Feed, StatusComposer, StatusCard | `useFeed()` hook |
| Chat | `/chat` | ChatList, ChatRoom | `getChats()`, `getMessages()` |
| Profile | `/profile/:id` | Profile, EditProfile | `getUserById()`, `getUserStatuses()` |
| Notifications | `/notifications` | Notifications | `getNotifications()` |
| Search | `/search` | Search | `search()` |
| Settings | `/settings` | Settings | `useUIStore` |

---

## 1. Authentication Flow (Complete Journey)

### Visual Flow

```
┌──────────────┐
│  App Starts  │
└──────┬───────┘
       │
       ▼
┌─────────────────────────────────┐
│ Check: isAuthenticated?         │
│ Location: src/app/routes.tsx    │
│ Uses: useAuthStore()             │
└──────┬──────────────────┬───────┘
       │                  │
    NO │                  │ YES
       │                  │
       ▼                  ▼
┌──────────────┐    ┌──────────────────┐
│ /login       │    │ Check: Profile   │
│ Login.tsx    │    │ Complete?        │
└──────┬───────┘    └─────┬────────┬───┘
       │                  │        │
       │               NO │        │ YES
       │                  │        │
       ▼                  ▼        ▼
┌──────────────┐    ┌─────────┐  ┌──────┐
│ Enter Email  │    │ /setup  │  │ /feed│
│ & Password   │    │ -profile│  │      │
└──────┬───────┘    └─────────┘  └──────┘
       │
       ▼
┌──────────────────────────────────┐
│ handleLogin()                     │
│ 1. Call mockService.login()      │
│ 2. Get user + tokens              │
│ 3. setAuth(user, tokens)          │
│ 4. Save to localStorage           │
└──────┬───────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│ Navigate to /feed or /setup      │
└──────────────────────────────────┘
```

### Code Flow

**Step 1: User Opens App**
```typescript
// src/app/routes.tsx
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, user } = useAuthStore();
  
  // Check 1: Is user logged in?
  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }
  
  // Check 2: Has user completed profile?
  if (user && !user.isSetupComplete) {
    return <Navigate to="/setup-profile" />;
  }
  
  return children; // Show protected content
};
```

**Step 2: Login Process**
```typescript
// src/features/auth/pages/Login.tsx
const Login = () => {
  const { setAuth } = useAuthStore();
  const navigate = useNavigate();
  
  const handleLogin = async (e) => {
    e.preventDefault();
    
    // 1. Call API
    const data = await mockService.login();
    // Returns: { user: {...}, tokens: {...} }
    
    // 2. Store authentication
    setAuth(data.user, data.tokens);
    // This updates Zustand store and saves to localStorage
    
    // 3. Navigate to app
    navigate('/feed');
  };
  
  return (
    <form onSubmit={handleLogin}>
      <input type="text" name="username" />
      <input type="password" name="password" />
      <button type="submit">Sign In</button>
    </form>
  );
};
```

**Step 3: Store Updates**
```typescript
// src/store/authStore.ts
export const useAuthStore = create(
  persist(
    (set) => ({
      user: null,
      tokens: null,
      isAuthenticated: false,
      
      setAuth: (user, tokens) => set({ 
        user, 
        tokens, 
        isAuthenticated: true 
      }),
      // This triggers re-render in all components using useAuthStore
    }),
    { name: 'daypulse-auth' } // Saves to localStorage
  )
);
```

---

## 2. Feed Flow (Creating & Viewing Posts)

### Visual Flow

```
┌─────────────────────────────────────────────────────┐
│ User navigates to /feed                              │
└─────────────────┬───────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────┐
│ Feed.tsx renders                                     │
│ Location: src/features/feed/pages/Feed.tsx          │
└─────────────────┬───────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────┐
│ useFeed() hook executes                              │
│ Location: src/features/feed/hooks/useFeed.ts        │
└─────────────────┬───────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────┐
│ React Query fetches data                             │
│ mockService.getFeed(page, limit)                    │
│ Location: src/services/mock/handlers.ts             │
└─────────────────┬───────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────┐
│ Data returned and cached by React Query              │
│ Cache key: ['feed']                                  │
└─────────────────┬───────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────┐
│ Feed.tsx renders UI:                                 │
│ • StatusComposer (create new post)                  │
│ • StatusCard[] (list of posts)                      │
│ • InfiniteScroll trigger                            │
└─────────────────────────────────────────────────────┘
```

### Creating a Post Flow

```
User types in StatusComposer
         │
         ▼
┌─────────────────────────────┐
│ User clicks "Post" button   │
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│ handlePost() in StatusComposer          │
│ Validates: content not empty            │
│           content <= 280 chars          │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│ createMutation.mutate()                 │
│ Sends: { content, mood, tags }          │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│ mockService.createStatus()              │
│ Simulates API call (500ms delay)        │
│ Returns: new Status object              │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│ onSuccess callback                      │
│ Updates React Query cache               │
│ Prepends new post to feed               │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│ Feed re-renders automatically           │
│ New post appears at top                 │
│ No page reload needed!                  │
└─────────────────────────────────────────┘
```

### Code Example

```typescript
// src/features/feed/components/StatusComposer.tsx
const StatusComposer = () => {
  const [content, setContent] = useState('');
  const [mood, setMood] = useState('😊');
  const [tags, setTags] = useState([]);
  
  // Define mutation
  const createMutation = useMutation({
    mutationFn: (vars) => mockService.createStatus(
      vars.content, 
      vars.mood, 
      vars.tags
    ),
    onSuccess: (newPost) => {
      // Update cache optimistically
      queryClient.setQueryData(['feed'], (oldData) => {
        const newPages = [...oldData.pages];
        newPages[0] = [newPost, ...newPages[0]]; // Add to top
        return { ...oldData, pages: newPages };
      });
      
      // Reset form
      setContent('');
      setTags([]);
      setMood('😊');
      
      // Show success message
      addToast('Pulse shared successfully! ✨', 'success');
    },
  });
  
  const handlePost = () => {
    if (!content.trim()) return;
    createMutation.mutate({ content, mood, tags });
  };
  
  return (
    <section>
      <textarea 
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="What's your pulse today?"
      />
      <button onClick={handlePost}>Post</button>
    </section>
  );
};
```

---

## 3. Chat Flow (Real-time Messaging)

### Chat List Flow

```
User clicks "Chat" in navigation
         │
         ▼
┌─────────────────────────────┐
│ Navigate to /chat            │
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│ ChatList.tsx renders                 │
│ Location: src/features/chat/pages/  │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│ useQuery(['chats'])                  │
│ Fetches: mockService.getChats()     │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│ Displays list of chats:              │
│ • Partner avatar & name              │
│ • Last message preview               │
│ • Unread count badge                 │
│ • Timestamp                          │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│ User clicks on a chat                │
│ Navigate to /chat/:id                │
└─────────────────────────────────────┘
```

### Chat Room Flow

```
User opens specific chat (/chat/c1)
         │
         ▼
┌──────────────────────────────────────┐
│ ChatRoom.tsx renders                  │
│ Gets chatId from URL params           │
└─────────────┬────────────────────────┘
              │
              ▼
┌──────────────────────────────────────┐
│ Two queries execute in parallel:      │
│ 1. useQuery(['chat', id])            │
│    → getChat(id)                     │
│    → Returns chat metadata            │
│                                       │
│ 2. useQuery(['messages', id])        │
│    → getMessages(id)                 │
│    → Returns message history          │
└─────────────┬────────────────────────┘
              │
              ▼
┌──────────────────────────────────────┐
│ Renders:                              │
│ ┌──────────────────────────────────┐ │
│ │ Header: Partner info & actions   │ │
│ ├──────────────────────────────────┤ │
│ │ Messages:                        │ │
│ │ • Scrollable list                │ │
│ │ • Sender avatar                  │ │
│ │ • Message bubbles                │ │
│ │ • Timestamps                     │ │
│ ├──────────────────────────────────┤ │
│ │ Input: Type & send messages      │ │
│ └──────────────────────────────────┘ │
└─────────────┬────────────────────────┘
              │
              ▼
┌──────────────────────────────────────┐
│ User types message                    │
│ User clicks send or presses Enter    │
└─────────────┬────────────────────────┘
              │
              ▼
┌──────────────────────────────────────┐
│ sendMutation.mutate(text)            │
│ Creates new message object            │
└─────────────┬────────────────────────┘
              │
              ▼
┌──────────────────────────────────────┐
│ Optimistic update:                    │
│ Message appears immediately           │
│ Shows in UI before API responds       │
└─────────────┬────────────────────────┘
              │
              ▼
┌──────────────────────────────────────┐
│ Auto-scroll to bottom                 │
│ Input field clears                    │
│ Ready for next message                │
└──────────────────────────────────────┘
```

### Code Example

```typescript
// src/features/chat/pages/ChatRoom.tsx
const ChatRoom = () => {
  const { id } = useParams(); // Get chat ID from URL
  const { user } = useAuthStore();
  const [inputText, setInputText] = useState('');
  
  // Fetch chat metadata
  const { data: chat } = useQuery({
    queryKey: ['chat', id],
    queryFn: () => mockService.getChat(id),
  });
  
  // Fetch messages
  const { data: messages } = useQuery({
    queryKey: ['messages', id],
    queryFn: () => mockService.getMessages(id),
  });
  
  // Send message mutation
  const sendMutation = useMutation({
    mutationFn: (text) => Promise.resolve({
      id: Math.random().toString(),
      chatId: id,
      senderId: user.id,
      text,
      type: 'text',
      createdAt: new Date().toISOString()
    }),
    onSuccess: (newMsg) => {
      // Update cache
      queryClient.setQueryData(['messages', id], (old) => 
        [...old, newMsg]
      );
      setInputText('');
    }
  });
  
  const handleSend = () => {
    if (!inputText.trim()) return;
    sendMutation.mutate(inputText);
  };
  
  const partner = chat?.participants.find(p => p.id !== user.id);
  
  return (
    <div className="chat-room">
      {/* Header */}
      <header>
        <Avatar src={partner?.avatar} name={partner?.name} />
        <h3>{partner?.name}</h3>
      </header>
      
      {/* Messages */}
      <div className="messages">
        {messages?.map((msg) => (
          <div key={msg.id} className={msg.senderId === user.id ? 'my-message' : 'their-message'}>
            {msg.text}
          </div>
        ))}
      </div>
      
      {/* Input */}
      <div className="input-area">
        <input
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
          placeholder="Message..."
        />
        <button onClick={handleSend}>Send</button>
      </div>
    </div>
  );
};
```

---

## 4. Profile Flow

### View Profile Flow

```
User navigates to /profile or /profile/:id
         │
         ▼
┌────────────────────────────────────┐
│ Profile.tsx renders                 │
│ Gets id from URL params             │
└─────────────┬──────────────────────┘
              │
              ▼
┌────────────────────────────────────┐
│ Determine profile type:             │
│ • No id → Own profile               │
│ • id matches user.id → Own profile  │
│ • Different id → Other user         │
└─────────────┬──────────────────────┘
              │
              ▼
┌────────────────────────────────────┐
│ Fetch data in parallel:             │
│ 1. getUserById(id)                  │
│    → User profile data              │
│ 2. getUserStatuses(id)              │
│    → User's posts                   │
└─────────────┬──────────────────────┘
              │
              ▼
┌────────────────────────────────────┐
│ Render profile:                     │
│ ┌────────────────────────────────┐ │
│ │ Avatar (large, with online     │ │
│ │ status indicator)              │ │
│ ├────────────────────────────────┤ │
│ │ Name & Username                │ │
│ ├────────────────────────────────┤ │
│ │ Stats Row:                     │ │
│ │ • Followers (clickable)        │ │
│ │ • Following (clickable)        │ │
│ │ • Streak                       │ │
│ │ • Posts count                  │ │
│ ├────────────────────────────────┤ │
│ │ Bio                            │ │
│ ├────────────────────────────────┤ │
│ │ Action Buttons:                │ │
│ │ If own: "Edit Profile"         │ │
│ │ If other: "Follow" & "Message" │ │
│ ├────────────────────────────────┤ │
│ │ Activity Intensity Graph       │ │
│ ├────────────────────────────────┤ │
│ │ Recent Posts                   │ │
│ │ (StatusCard components)        │ │
│ └────────────────────────────────┘ │
└────────────────────────────────────┘
```

### Edit Profile Flow

```
User clicks "Edit Profile"
         │
         ▼
┌────────────────────────────────────┐
│ Navigate to /profile/edit           │
└─────────────┬──────────────────────┘
              │
              ▼
┌────────────────────────────────────┐
│ EditProfile.tsx renders             │
│ Form pre-filled with current data   │
└─────────────┬──────────────────────┘
              │
              ▼
┌────────────────────────────────────┐
│ User edits:                         │
│ • Name                              │
│ • Username                          │
│ • Bio                               │
│ • Avatar (upload)                   │
└─────────────┬──────────────────────┘
              │
              ▼
┌────────────────────────────────────┐
│ User clicks "Save"                  │
│ handleSave() called                 │
└─────────────┬──────────────────────┘
              │
              ▼
┌────────────────────────────────────┐
│ updateUser(changes)                 │
│ Updates Zustand store               │
│ Persists to localStorage            │
└─────────────┬──────────────────────┘
              │
              ▼
┌────────────────────────────────────┐
│ Navigate back to /profile           │
│ Profile shows updated data          │
└────────────────────────────────────┘
```

---

## 5. Data Flow Summary

### Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        USER INTERFACE                        │
│  Components render based on state and props                  │
└────────────────────┬────────────────────────────────────────┘
                     │ ▲
                     │ │ Props & State Updates
                     ▼ │
┌─────────────────────────────────────────────────────────────┐
│                      STATE MANAGEMENT                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Zustand    │  │ React Query  │  │  Component   │      │
│  │   (Global)   │  │  (Server)    │  │   (Local)    │      │
│  │              │  │              │  │              │      │
│  │ • Auth       │  │ • Feed       │  │ • Forms      │      │
│  │ • UI/Theme   │  │ • Chat       │  │ • Modals     │      │
│  │ • Toasts     │  │ • Profile    │  │ • Toggles    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────┬────────────────────────────────────────┘
                     │ ▲
                     │ │ API Calls & Responses
                     ▼ │
┌─────────────────────────────────────────────────────────────┐
│                       DATA LAYER                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Services (src/services/)                             │   │
│  │  • mockService - Simulated API with delays            │   │
│  │  • api client - Axios with interceptors               │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### State Management Decision Tree

```
Need to store data?
         │
         ▼
    ┌────────┐
    │ Where? │
    └───┬────┘
        │
        ├─── Server data (from API)
        │    └─→ Use React Query
        │        • Automatic caching
        │        • Background refetching
        │        • Loading/error states
        │
        ├─── Global app state
        │    └─→ Use Zustand
        │        • Auth state
        │        • Theme/language
        │        • Toasts
        │
        └─── Component-specific
             └─→ Use useState
                 • Form inputs
                 • Modal open/close
                 • Local toggles
```

---

## Common Patterns

### Pattern 1: Fetch and Display

```typescript
// 1. Create custom hook
export const useFeed = () => {
  return useQuery({
    queryKey: ['feed'],
    queryFn: () => mockService.getFeed(),
  });
};

// 2. Use in component
const Feed = () => {
  const { data, isLoading, error } = useFeed();
  
  if (isLoading) return <Loader />;
  if (error) return <Error />;
  
  return (
    <div>
      {data.map(item => <Item key={item.id} {...item} />)}
    </div>
  );
};
```

### Pattern 2: Create and Update

```typescript
const StatusComposer = () => {
  const queryClient = useQueryClient();
  
  const createMutation = useMutation({
    mutationFn: (data) => mockService.createStatus(data),
    onSuccess: (newItem) => {
      // Option 1: Invalidate and refetch
      queryClient.invalidateQueries(['feed']);
      
      // Option 2: Update cache manually (faster)
      queryClient.setQueryData(['feed'], (old) => [newItem, ...old]);
    },
  });
  
  return (
    <form onSubmit={(e) => {
      e.preventDefault();
      createMutation.mutate(formData);
    }}>
      {/* form fields */}
    </form>
  );
};
```

### Pattern 3: Navigate After Action

```typescript
const Login = () => {
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  
  const handleLogin = async () => {
    const data = await mockService.login();
    setAuth(data.user, data.tokens);
    navigate('/feed'); // Redirect after success
  };
  
  return <form onSubmit={handleLogin}>...</form>;
};
```

This documentation should help React beginners understand how data flows through the application and how different features work together!
