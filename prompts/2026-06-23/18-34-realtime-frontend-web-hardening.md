# FE/Infra: realtime-reconnect, dev-guard, security-заголовки, уязвимости зависимостей, CSRF

Эти пункты живут вне backend-репозитория (frontend / infra / CI), поэтому оформлены как задача-промпт.
Бэкенд-контракт уже готов под них (см. факты ниже). Делай по порядку, каждый пункт самодостаточен.

Контракт бэкенда, который надо знать:
- Все ответы — `ApiResponse<T>`: `{ success, message, data }`.
- Auth — HttpOnly-cookie `access_token` (path `/`) и `refresh_token` (path `/api/auth`). Тело логина/refresh
  дополнительно отдаёт `data.token` (access) для STOMP-handshake.
- Тихое продление: `POST /api/auth/refresh` (куку refresh браузер шлёт сам) → новый access в `data.token`.
  **Важно (новое):** refresh теперь серверный, с ротацией. Каждый refresh выдаёт НОВЫЙ refresh-токен; старый
  становится недействительным. Нельзя слать два параллельных refresh по одной куке — второй получит 401 и
  «погасит» всю сессию (детект кражи). Делай refresh single-flight (один общий промис на все ждущие запросы).
- WS-эндпоинт: `GET /ws` (SockJS), заголовок `Authorization: Bearer <access>`. Нативный заголовок `token` больше
  не принимается — только `Authorization`.

---

## 1. (#32) Авто-reconnect у WebSocket

Сейчас `reconnectDelay: 0` — после блипа сети/рестарта пода realtime не возвращается. Включить bounded reconnect
с обновлением access-токена перед каждой попыткой.

```ts
this.client = new Client({
  webSocketFactory: () => new SockJS('/ws') as unknown as WebSocket,
  connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
  reconnectDelay: 2000,                 // базовая задержка
  beforeConnect: async () => {
    // single-flight: вернуть свежий access (внутри — общий промис refresh, см. контракт выше)
    const fresh = await ensureFreshAccessToken();
    this.client.connectHeaders = { Authorization: `Bearer ${fresh}` };
  },
  onWebSocketClose: () => useWsStore.getState().setConnectionState('reconnecting'),
  onConnect: () => useWsStore.getState().setConnectionState('connected'),
});
```

- Опционально: экспоненциальный backoff с потолком (2s→4s→…→30s) вместо фиксированных 2s.
- В UI — индикатор состояния соединения (`connected | reconnecting | offline`).
- Проверка: вырубить сеть/перезапустить бэкенд → клиент должен сам переподключиться и довосстановить подписки.

## 2. (#33) Прототипные/dev-роуты не должны попадать в прод

`/combat-preview/*` и `/dev/content-classes` зарегистрированы безусловно. Обернуть в dev-guard, чтобы в
prod-сборке роутов физически не было.

```tsx
...(import.meta.env.DEV ? [
  { path: '/combat-preview', children: [ /* ... */ ] },
  { path: '/dev/content-classes', element: <ContentClassViewerPage /> },
] : []),
```

Проверка: `vite build` (prod) → перейти по `/dev/content-classes` вручную → должен быть 404/redirect, а не страница.

## 3. (#16) Nginx — браузерные security-заголовки  (репозиторий infra / nginx.conf)

В `server` (или `location /`) добавить, `always` обязателен (чтобы заголовки шли и на ошибочных ответах):

```nginx
add_header Content-Security-Policy "default-src 'self'; connect-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; frame-ancestors 'none'" always;
add_header Strict-Transport-Security "max-age=63072000; includeSubDomains" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;
```

- `connect-src` должен включать WS/SockJS-источник. Если SockJS ходит на тот же origin — `'self'` достаточно;
  при отдельном домене API добавь его (и `wss:`-схему) в `connect-src`.
- CSP подогнать под реальные источники шрифтов/картинок. Зачем важно: access-токен живёт в JS-памяти ради WS,
  поэтому XSS-радиус надо резать максимально.
- Проверка: securityheaders.com / DevTools → все заголовки на месте, приложение и realtime работают (CSP ничего
  не ломает в консоли).

## 4. (#15) Уязвимости в prod-зависимостях фронтенда

```bash
npm i react-router@^6.30.4 react-router-dom@^6.30.4   # open redirect GHSA-2j2x-hqr9-3h42
npm audit fix                                          # form-data >=4.0.6 (CRLF, GHSA-hmw2-7cc7-3qxx), транзитивный — поднять родителя
npm audit --omit=dev                                   # должно стать чисто
```

После апдейта прогнать роутинг и auth-флоу (login → refresh → redirect), убедиться, что навигация цела.

---

## 5. (#6) CSRF — КООРДИНИРОВАННОЕ изменение FE+BE, деплоить вместе

REST ходит на cookie с `withCredentials`, серверный CSRF сейчас выключен. Сам по себе включённый на бэке CSRF
**сломает все мутации фронта**, пока FE не начнёт отдавать токен — поэтому это парный деплой, не делать половину.

Текущая защита-минимум уже есть: cookie `SameSite=Lax`, а все мутации — не-GET, поэтому классический
cross-site POST CSRF уже отсекается. Полный double-submit — следующий уровень (защита от компрометации
соседнего сабдомена).

**Backend (применить ОДНОВРЕМЕННО с FE), `SecurityConfig`:**
```java
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
    .ignoringRequestMatchers("/api/auth/login", "/api/auth/register",
                             "/api/auth/refresh", "/ws/**"))
```

**Frontend (`axios.ts`)** — Spring по умолчанию `XSRF-TOKEN` (cookie) / `X-XSRF-TOKEN` (header):
```ts
axios.defaults.xsrfCookieName = 'XSRF-TOKEN';
axios.defaults.xsrfHeaderName = 'X-XSRF-TOKEN';
axios.defaults.withCredentials = true;
```

Порядок выката: выложить FE с отправкой заголовка и BE с включённым CSRF одной волной (или сначала FE — он
безвреден без серверной проверки, потом BE). Проверка: POST/PUT/DELETE из приложения проходят (есть
`X-XSRF-TOKEN`), а тот же запрос без заголовка получает 403.

Альтернатива, если контракт «фронт всегда same-origin за nginx» осознанный: оставить без токена, но зафиксировать
`SameSite=Strict` и задокументировать «API только same-origin» как жёсткий контракт. Рекомендуется всё же
double-submit — он устойчивее к компрометации соседнего сабдомена.
