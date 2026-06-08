# WebSocket: взаимодействия GAME_MASTER ↔ PLAYER

_Сгенерировано: 2026-06-08 02:26_

Бэкенд теперь **реально шлёт** WebSocket-уведомления при действиях Мастера игры (GM),
которые затрагивают персонажей/кампанию. Раньше `WebSocketEventService` был мёртвым кодом
(никуда не инжектился, ничего не публиковал) — FE был подписан, но бэк молчал. Теперь это
исправлено.

---

## 1. Что изменилось на бэкенде

### Транзакционно-безопасная доставка
Уведомления больше не отправляются «в моменте». Внутри `@Transactional`-метода сервис
**публикует Spring-событие**, а реальная отправка в STOMP-брокер происходит только
**после коммита** транзакции:

- `WsCampaignBroadcastEvent` / `WsUserBroadcastEvent` — внутренние app-события.
- `WebSocketBroadcastListener` — слушает их через
  `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` и вызывает
  `SimpMessagingTemplate.convertAndSend(...)` / `convertAndSendToUser(...)`.

Эффект для FE: **вы никогда не получите событие про изменение, которое потом откатилось**.
Если транзакция упала — события не будет.

### Где теперь эмитятся события (call-sites)
| Действие GM (сервис.метод) | Событие |
|---|---|
| `ItemInstanceService.grantItem` | `ITEM_GRANTED` |
| `ItemInstanceService.removeItem` | `ITEM_REMOVED` |
| `CharacterEffectService.applyEffect` | `BUFF_APPLIED` |
| `CharacterEffectService.removeEffect` | `BUFF_REMOVED` |
| `XpService.distributeXp` | `XP_GRANTED` |
| `CharacterService.modifyHp` | `HP_CHANGED` |
| `CharacterService.updateCharacter` | `CHARACTER_UPDATED` |
| `NpcService.toggleVisibility` | `NPC_REVEALED` / `NPC_HIDDEN` |
| `QuestService.updateQuest` | `QUEST_UPDATED` (только если квест видим игрокам) |
| `CampaignService.changeCampaignStatus` | `CAMPAIGN_STATUS_CHANGED` |
| `CampaignService.kickMember` | `MEMBER_KICKED` (×2 — см. ниже) |

---

## 2. Каналы (destinations)

- `/topic/campaign/{campaignId}` — фан-аут всем в кампании (GM + игроки).
- `/user/queue/notifications` — адресно одному пользователю (используется для кика).

FE уже подписан на оба канала в `src/lib/websocket.ts` — **новых подписок добавлять не нужно**,
нужно лишь обработать новые `type` в существующем диспетчере.

---

## 3. Форма payload

Каждое сообщение — это `WebSocketEventPayload`:

```jsonc
{
  "type": "HP_CHANGED",                 // string, имя из WebSocketEventType
  "campaignId": "1f2e...-uuid",
  "characterId": "ab12...-uuid | null", // заполнен для событий по персонажу
  "data": { /* зависит от type, см. ниже */ },
  "timestamp": "2026-06-08T02:26:00Z",  // ISO-8601 (Instant)
  "triggeredBy": "c0ff...-uuid"         // userId того, кто инициировал действие
}
```

### Таблица событий и `data`

| type | канал | characterId | data |
|---|---|---|---|
| `ITEM_GRANTED` | topic | да | **полный** `ItemInstanceResponse` |
| `ITEM_REMOVED` | topic | да | `{ "instanceId": uuid }` |
| `BUFF_APPLIED` | topic | да | **полный** `CharacterActiveEffectResponse` |
| `BUFF_REMOVED` | topic | да | `{ "effectId": uuid }` |
| `XP_GRANTED` | topic | нет | `{ "amount": int, "characterIds": [uuid, ...] }` |
| `HP_CHANGED` | topic | да | `{ "currentHp": int, "tempHp": int, "maxHp": int }` |
| `CHARACTER_UPDATED` | topic | да | **полный** `CharacterResponse` |
| `NPC_REVEALED` | topic | нет | `{ "npcId": uuid }` |
| `NPC_HIDDEN` | topic | нет | `{ "npcId": uuid }` |
| `QUEST_UPDATED` | topic | нет | `{ "questId": uuid }` |
| `CAMPAIGN_STATUS_CHANGED` | topic | нет | `{ "status": "ACTIVE" \| "PAUSED" \| "COMPLETED" \| ... }` |
| `MEMBER_KICKED` (кикнутому) | **user queue** | нет | `{ "campaignId": uuid }` |
| `MEMBER_KICKED` (всем) | topic | нет | `{ "userId": uuid }` |

> Философия: payload — это **уведомление**, а не источник истины. Для событий с минимальным
> `data` (`{ ...Id }`) FE должен **перезапросить** актуальное состояние по REST. Для событий,
> где приходит полный DTO (`ITEM_GRANTED`, `BUFF_APPLIED`, `CHARACTER_UPDATED`), можно обновить
> кэш напрямую, но безопаснее всё равно инвалидировать соответствующий query.

---

## 4. Что нужно сделать на фронте (TODO)

В диспетчере событий (`src/lib/websocket.ts` → обработчики по `payload.type`) добавить реакции.
Рекомендуется завязать на React Query: `queryClient.invalidateQueries(...)`.

1. **`HP_CHANGED`** — обновить HP персонажа `characterId`. Можно взять `data.currentHp/tempHp/maxHp`
   напрямую (мгновенный апдейт полоски HP) и/или инвалидировать query персонажа.
2. **`XP_GRANTED`** — для каждого id из `data.characterIds` инвалидировать персонажа; можно показать
   тост «+{amount} XP».
3. **`ITEM_GRANTED` / `ITEM_REMOVED`** — инвалидировать инвентарь персонажа `characterId`.
   На `ITEM_GRANTED` доступен полный `ItemInstanceResponse` в `data`.
4. **`BUFF_APPLIED` / `BUFF_REMOVED`** — инвалидировать активные эффекты персонажа `characterId`
   (и его статы, т.к. бафы влияют на модификаторы).
5. **`CHARACTER_UPDATED`** — обновить карточку персонажа `characterId` (в `data` — полный
   `CharacterResponse`).
6. **`NPC_REVEALED` / `NPC_HIDDEN`** — инвалидировать список NPC кампании (для игроков NPC
   появляется/исчезает; GM видит всех всегда).
7. **`QUEST_UPDATED`** — инвалидировать список/деталь квеста `data.questId`. Приходит только когда
   квест видим игрокам, поэтому это безопасно показывать.
8. **`CAMPAIGN_STATUS_CHANGED`** — обновить статус кампании, при необходимости показать баннер
   (например, «Кампания на паузе»).
9. **`MEMBER_KICKED`**:
   - Если событие пришло в **`/user/queue/notifications`** (значит кикнули **текущего**
     пользователя) — показать тост, увести его из кампании (redirect на список кампаний),
     сбросить связанный кэш. `data.campaignId` подскажет, из какой кампании.
   - Если пришло в **topic** (`data.userId`) — обновить ростер кампании (убрать участника).

### Полезное
- `triggeredBy` = userId инициатора. Можно глушить собственные тосты (если `triggeredBy ===
  currentUserId`), чтобы GM не видел уведомления о собственных действиях.
- Все id — UUID-строки. `timestamp` — ISO-8601.
- Дедуп/идемпотентность: при реконнекте STOMP возможна повторная доставка — обработчики должны
  быть идемпотентны (инвалидация query — безопасна по природе).

---

## 5. Как протестировать с интерфейса

1. Залогиниться двумя пользователями: GM и игрок, оба в одной кампании (две вкладки/браузера).
2. Игрок открывает свою кампанию/персонажа (подписки на `/topic/campaign/{id}` и
   `/user/queue/notifications` поднимаются автоматически).
3. GM выполняет действие (выдать предмет, наложить бафф, выдать XP, изменить HP, показать NPC,
   обновить видимый квест, сменить статус кампании, кикнуть игрока).
4. У игрока соответствующая секция должна обновиться **без перезагрузки страницы**.
5. Проверка транзакционности: если действие GM упало с ошибкой (4xx/5xx) — у игрока **не должно**
   быть никаких изменений (событие не отправляется до коммита).

---

## 6. Затронутые файлы бэкенда (для ревью)

- `event/WsCampaignBroadcastEvent.java`, `event/WsUserBroadcastEvent.java`,
  `event/WebSocketBroadcastListener.java` — инфраструктура after-commit доставки.
- `service/WebSocketEventService.java` — теперь публикует Spring-события.
- `service/ItemInstanceService.java`, `service/CharacterEffectService.java`,
  `service/XpService.java`, `service/CharacterService.java`,
  `service/NpcService.java`, `service/QuestService.java`,
  `service/CampaignService.java` — добавлены вызовы эмита событий.

Компиляция: `./gradlew.bat compileJava` — **OK** (на 2026-06-08 02:26).
