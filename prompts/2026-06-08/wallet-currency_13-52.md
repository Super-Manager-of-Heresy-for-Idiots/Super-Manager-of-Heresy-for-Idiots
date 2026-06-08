# Кошелёк персонажа: начисление / списание валюты + WS-событие

_Сгенерировано: 2026-06-08 13-52_

Чинили баг **«Не получается добавить персонажу валюту»** (POST `…/wallet` → 404
`Wallet entry not found for this currency type`). Бэкенд исправлен и передеплоен. Ниже —
что изменилось и что нужно сделать на фронте, чтобы реализовать UI: dropdown доступных
валют → ввод суммы → кнопки «Начислить» / «Списать», плюс live-обновление по WebSocket.

---

## 1. Что было не так и что изменилось на бэкенде

**Причина 404:** `WalletService.modifyCurrency` требовал, чтобы строка кошелька для выбранной
валюты **уже существовала**. При создании персонажа автоматически заводится только `Gold`,
поэтому первое начисление любой другой валюты падало с 404.

**Исправлено:**
- POST `…/wallet` теперь **создаёт** строку кошелька при **первом начислении** валюты (upsert).
- Это **один** эндпоинт и для начисления, и для списания: знак `amount` решает направление
  (`> 0` — начислить, `< 0` — списать).
- Списание ниже нуля (или списание валюты, которой у персонажа ещё нет) → **400**
  `Insufficient funds for this operation`.
- При успешном изменении бэкенд **шлёт WebSocket-событие `WALLET_CHANGED`** (раньше не слал
  вообще — `WalletService` не был подключён к `WebSocketEventService`).

Изменение баланса атомарно на уровне БД (`amount = amount + delta where … and amount + delta >= 0`),
гонок между двумя одновременными операциями нет.

---

## 2. Эндпоинты (всё уже существует, новых маршрутов добавлять не нужно)

База: `/api/campaigns/{campaignId}/characters/{characterId}`. Всё обёрнуто в `ApiResponse<T>`.

| Метод | Путь | Тело | Ответ | Назначение |
|---|---|---|---|---|
| GET | `/api/campaigns/{campaignId}/reference/currencies` | — | `List<CurrencyTypeResponse>` | Список доступных типов валют для dropdown |
| GET | `…/wallet` | — | `List<WalletEntryResponse>` | Текущие балансы персонажа |
| POST | `…/wallet` | `ModifyCurrencyRequest` | `WalletEntryResponse` | Начислить / списать (один эндпоинт) |
| GET | `…/wallet/history` | — (пагинация) | `Page<WalletHistoryEntryResponse>` | История операций |

### DTO

```jsonc
// CurrencyTypeResponse  (для dropdown)
{
  "id": "uuid",
  "name": "Gold",
  "exchangeRateToGold": 1.0,   // number | null
  "isDefault": true
}

// ModifyCurrencyRequest  (тело POST …/wallet)
{
  "currencyTypeId": "uuid",    // @NotNull — что выбрали в dropdown
  "amount": 25                 // @NotNull, BigDecimal. > 0 начислить, < 0 списать
}

// WalletEntryResponse  (ответ POST и элемент GET …/wallet)
{
  "currencyTypeId": "uuid",
  "currencyName": "Gold",
  "amount": 125,               // НОВЫЙ баланс после операции
  "goldEquivalent": 125        // number | null (amount × exchangeRateToGold)
}
```

> Доступ: начислять/списывать может **владелец персонажа, GM кампании или ADMIN**
> (на бэке — `enforceOwnerOrGmOrAdmin`). Если нет прав — 403.

---

## 3. UI-флоу (то, что просили)

1. При открытии формы кошелька запросить `GET …/reference/currencies` и заполнить **dropdown**
   списком валют (`name`, value = `id`). Параллельно — `GET …/wallet`, чтобы показать текущие
   балансы.
2. Пользователь выбирает **тип валюты** в dropdown и вводит **положительное** число в поле суммы.
3. Две кнопки (или переключатель направления):
   - **«Начислить»** → POST `…/wallet` с `amount = +введённое`.
   - **«Списать»** → POST `…/wallet` с `amount = -введённое` (отправляем отрицательное значение).
4. Поле ввода — только положительное число; знак задаёт выбранная кнопка. Так пользователю не
   нужно думать про минус.
5. На успехе обновить отображаемый баланс. Можно взять `amount` прямо из ответа
   `WalletEntryResponse`, но надёжнее инвалидировать query кошелька (см. п.4 про WS — обновление
   всё равно прилетит).

### Обработка ошибок
- **400** `Insufficient funds for this operation` — недостаточно средств для списания (или
  списание валюты, которой нет). Показать понятный тост, **не** уводить с формы.
- **403** — нет прав (не владелец / не GM / не ADMIN).
- Сумма `0` или пустая — не отправлять, валидировать на клиенте (`amount` обязателен).

---

## 4. WebSocket: live-обновление кошелька

Канал — уже существующий **`/topic/campaign.{campaignId}`** (фан-аут всем в кампании; FE на него
уже подписан в `src/lib/websocket.ts`, **новых подписок не нужно**). Обратите внимание на **точку**
перед `{campaignId}` — не слэш (требование RabbitMQ STOMP-relay).

Новый тип события в `WebSocketEventType`: **`WALLET_CHANGED`**.

Payload (`WebSocketEventPayload`, как у остальных событий):

```jsonc
{
  "type": "WALLET_CHANGED",
  "campaignId": "uuid",
  "characterId": "uuid",          // персонаж, у которого изменился кошелёк
  "data": {                       // WalletEntryResponse — НОВЫЙ баланс изменённой валюты
    "currencyTypeId": "uuid",
    "currencyName": "Gold",
    "amount": 125,
    "goldEquivalent": 125
  },
  "timestamp": "2026-06-08T13:52:00Z",
  "triggeredBy": "uuid"           // userId инициатора
}
```

### TODO в диспетчере событий (`src/lib/websocket.ts` → обработка по `payload.type`)
- Добавить ветку `WALLET_CHANGED`: инвалидировать query кошелька персонажа `characterId`
  (`GET …/wallet`) и историю (`GET …/wallet/history`).
- Можно сделать оптимистичный апдейт строки баланса из `data` (мгновенно), но инвалидация
  query — источник истины.
- Событие приходит **всем** в кампании, поэтому игрок видит, как GM начисляет/списывает ему
  валюту, без перезагрузки. И наоборот.
- `triggeredBy` = userId инициатора: можно глушить собственный тост, если
  `triggeredBy === currentUserId`.

---

## 5. Как протестировать с интерфейса

1. Два пользователя в одной кампании (GM и игрок), две вкладки.
2. Игрок открыл своего персонажа (подписка на `/topic/campaign.{id}` поднимается сама).
3. GM в форме кошелька выбирает валюту (например, не-Gold, которой раньше не было), вводит сумму,
   жмёт **«Начислить»** → у персонажа появляется новая строка валюты; у игрока баланс обновляется
   **без перезагрузки**.
4. GM жмёт **«Списать»** больше, чем есть → **400** `Insufficient funds`, баланс не меняется.
5. История (`…/wallet/history`) пополняется записями `delta` / `balanceAfter` на каждую операцию.

---

## 6. Затронутые файлы бэкенда (для ревью)

- `domain/enums/WebSocketEventType.java` — добавлен `WALLET_CHANGED`.
- `service/WalletService.java` — `modifyCurrency` переписан на upsert (создаёт строку при первом
  начислении; 400 на недостаток средств) + инжектится `WebSocketEventService` и эмитится
  `WALLET_CHANGED` после коммита.
- `CONTRACT_FRONT.md` — обновлены enum, описание POST `…/wallet` и раздел WebSocket.

Компиляция `./gradlew compileJava` — **OK**; образ `dnd-app:local` пересобран, контейнеры
`backend` (×2) и `ws` пересозданы и подняты (на 2026-06-08 13:52).
