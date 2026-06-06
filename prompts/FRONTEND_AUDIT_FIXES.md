# Frontend audit fixes prompt

Apply the fixes below in the frontend repo at
`C:\SuperHerecy\Super-Manager-of-Heresy-for-Idiots-frontend`. All paths in this
document are relative to that repo unless stated otherwise. Tackle them in the
order shown — security first, then performance, then UX. Add deps with
`npm install <pkg>` and update `package-lock.json`.

The backend has been hardened in parallel. Notable backend changes that the
frontend must align with:

- Public auth routes are now explicit: `POST /api/auth/login`,
  `POST /api/auth/register`. Anything else under `/api/auth/*` requires JWT.
- `/actuator/**` is now ADMIN-only except for `/actuator/health` and
  `/actuator/info`.
- WebSocket CORS is locked to `APP_CORS_ALLOWED_ORIGINS`. The frontend dev
  origin (`http://localhost:5173`) is included by default.
- `/auth/login` and `/auth/register` enforce per-IP rate limits and reply with
  HTTP 429 once exceeded — show a friendly error.
- Username/email enumeration was unified — on duplicate registration the
  backend now returns "Имя пользователя или email уже используются".
- ADMIN role can no longer be obtained via the public register endpoint.

---

## 1. CRITICAL — Security

### 1.1 JWT stored in localStorage (XSS = full session theft)
`src/store/authStore.ts:14,35,36`

Goal: stop persisting the raw JWT and the full user object in `localStorage`.

Two acceptable approaches:

1. **Preferred:** switch to HttpOnly cookie auth. This requires a small backend
   change (issue cookie on login, read cookie in `JwtAuthenticationFilter`).
   Coordinate with backend before doing this.
2. **Pragmatic short-term:** keep the access token in memory only (Zustand
   non-persisted slice); persist only a refresh hint (e.g. `"remember": true`).
   Re-login is required after a tab close, which is acceptable until refresh
   tokens land. Do NOT keep `user` in storage — derive role from the decoded
   JWT payload via `jwt-decode`.

Also:

- Decode JWT `exp` on app boot (`jwt-decode`). If expired → call `logout()` and
  navigate to `/login` before any protected query fires.
- `ProtectedRoute` (`src/components/layout/ProtectedRoute.tsx:10-13`) must check
  a valid in-memory token AND a non-expired `exp`, not just storage presence.
  Treat route guards as UX, not security.
- Remove the persisted `user.role` field. Read role from the JWT claims.

Install: `npm install jwt-decode`.

### 1.2 Hard redirect in axios interceptor
`src/api/axios.ts:31`

Remove `window.location.href = '/login'`. On 401:

1. Call `authStore.logout()`.
2. Let `ProtectedRoute` re-render and `<Navigate to="/login" />` through React
   Router. This preserves React Query cache for public pages and avoids a full
   reload.

Also add `timeout: 15000` on the axios instance (`src/api/axios.ts`) and stop
casting through `any`:

- Replace `(error as any).response = response` with a typed
  `ApiBusinessError extends Error` that carries `response` and `data`.

### 1.3 Password policy on register
`src/pages/auth/RegisterPage.tsx:20`

Tighten the Zod schema: `min(8).regex(/[A-Z]/).regex(/[0-9]/)`. Surface the
specific failed rule under the input.

---

## 2. CRITICAL — Architecture

### 2.1 Code-split all routes
`src/router.tsx:5-66`

Replace every static page import with `React.lazy()` and wrap
`<RouterProvider>` (or each route element) in `<Suspense fallback={<PageSkeleton/>}>`.
Goal: the initial chunk must not contain admin/GM-only pages for plain players.

### 2.2 ErrorBoundary
`src/App.tsx:16-45`

Install `react-error-boundary`. Wrap `<RouterProvider>` in
`<ErrorBoundary FallbackComponent={AppErrorFallback}>` with a fallback that
shows a brief error and a "Reload" button. Reset the boundary on route change.

Install: `npm install react-error-boundary`.

### 2.3 404 page + root redirect
`src/router.tsx:168-169`

- Create `NotFoundPage` and point the catch-all `*` route at it.
- On `/`, if `authStore.token` is valid → `<Navigate to="/characters" />`,
  otherwise `<Navigate to="/login" />`. Eliminate the double redirect.

---

## 3. CRITICAL — Performance

### 3.1 Debounce marketplace search
`src/pages/gm/homebrew/MarketplaceBrowsePage.tsx:62`

Use `useDebouncedValue(search, 300)` (or `useDeferredValue` from React 18) so
typing "dragon" issues one request, not six. Also abort the in-flight request
with React Query's `signal` to dodge race conditions on the response.

---

## 4. ВАЖНО — Quick Wins

### 4.1 Toast spam on stat updates
`src/hooks/useCharacters.ts:43,61,76,132,168`

Remove `toast.success(...)` from `useUpdateStat` and other high-frequency
mutations. Keep error toasts.

### 4.2 useAuth handlers
`src/hooks/useAuth.ts:18,44`

- Drop the `throw error` inside `onError` of `useRegister` (lines ~44). It
  causes unhandled promise rejections. Surface errors via `mutation.error`.
- Replace `response.data!` with a guard: if `!response.data` show a toast and
  bail. Don't lean on non-null assertions for network responses.

### 4.3 Strong typing
- `src/components/layout/AppLayout.tsx:48` — type `roleLabels` as
  `Record<Role, string>`.
- `src/types/index.ts` — split by domain (auth, character, campaign,
  homebrew). Merge `CharacterResponse` / `CharacterV2Response` to a single
  canonical type. Replace `RewardSelection.rewardType: string` with a union of
  the actual reward types.

### 4.4 Stat HP indicator
`src/pages/player/CharactersListPage.tsx:8,26`

- Fix `if (!hp || !hpMax) return null` so `hp === 0` (legitimate dead state)
  doesn't fall through. Use explicit `hp == null || hpMax == null`.
- Pass the character's real `currentHp` / `maxHp` to `StatusGlyph` instead of
  the hard-coded `100`.

### 4.5 staleTime defaults
`src/hooks/useTeams.ts`, `src/hooks/useConditions.ts`

Add `staleTime: 30_000` (and `gcTime` to match) so window focus doesn't trigger
constant refetches.

### 4.6 Pagination on lists
- `src/pages/player/CharactersListPage.tsx:70` — server-side pagination.
- `src/pages/gm/campaigns/CampaignListPage.tsx:30` — wire `page` / `size` into
  `campaignsApi.list()` (the API already supports it).

### 4.7 Login page bugs
`src/pages/auth/LoginPage.tsx:35-37,143,154-156,168-183`

- Either implement "recover" or remove the dangling `<a>` (no href, no
  onClick).
- Convert the "Remember me" `<span>` into a real `<input type="checkbox">`
  with a label-for binding (a11y).
- Make the password-eye icon actually toggle `type` between `password` and
  `text`, or delete it.

### 4.8 Focus-visible
`src/index.css:244-306`

Add `.ao-btn:focus-visible { outline: 2px solid var(--gold); outline-offset: 2px; }`
and equivalent for inputs and links.

### 4.9 Admin dashboard counts
`src/pages/admin/AdminDashboardPage.tsx:19-45`

Today the dashboard fetches *every* user/team/stat-type just to read `.length`.
Coordinate with backend to add `GET /api/admin/stats` returning aggregated
counts; switch the dashboard to that single request.

### 4.10 NotificationsFeed re-renders
`src/components/realtime/NotificationsFeed.tsx:47`

Each `NotificationRow` subscribes to the whole Zustand store. Replace with a
selector:

```ts
const markRead = useWsStore(s => s.markRead);
```

### 4.11 .env hygiene
- Ensure `.env` is in `.gitignore` and remove it from VCS history if it ever
  landed (use `git rm --cached .env` + force-push only with team agreement).
- Keep `.env.example` as the canonical template. Make sure no `VITE_*` prod
  URLs leak into the bundle.

---

## 5. Optional UX/architecture follow-ups

These map to the audit's "important but not critical" items. Pick them up if
time allows after the items above ship.

- Decompose `src/pages/player/CharacterDetailPage.tsx` (525 lines, 9
  `useState`) into `StatsSection`, `EquipmentSection`, `EnchantmentModal`.
- Migrate inline styles in `src/components/layout/AppLayout.tsx:70-261` to
  Tailwind / CSS classes; remove the `onMouseEnter/Leave` DOM-mutation hacks.
- Add an early `if (!id) return <Navigate to="/characters" />` guard in
  `CharacterDetailPage` before calling `useCharacterStats(id!)`.
- Replace optimistic update in `useUpdateStat` with a debounced commit or
  input disable during in-flight mutation.

---

## Done criteria

The PR ships when:

1. No JWT or user object lives in `localStorage`.
2. All routes are `React.lazy`-loaded; initial chunk size drops.
3. `ErrorBoundary` catches render errors with a fallback UI.
4. Marketplace search is debounced; no race conditions.
5. 401 from axios does not call `window.location.href`.
6. Login form is keyboard-accessible (`input[type=checkbox]`, working eye
   icon, focus-visible outlines).
7. `npm run typecheck` and `npm run lint` pass.
8. Manual smoke test: register → login → land on /characters → expired-token
   refresh → logout.
