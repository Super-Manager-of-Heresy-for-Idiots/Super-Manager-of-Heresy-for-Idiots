# API Contract: Campaign Characters

**Base URL:** `/api/campaigns/{campaignId}/characters`

**Авторизация:** Все запросы требуют JWT-токен в заголовке `Authorization: Bearer <token>`

---

## Общая обёртка ответа (ApiResponse)

Все ответы приходят в единой обёртке:

```json
{
  "success": true,
  "data": "<payload>",
  "message": "Сообщение (опционально)",
  "error": null,
  "fields": null
}
```

При ошибке валидации:
```json
{
  "success": false,
  "data": null,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fields": { "name": "Имя персонажа обязательно" }
}
```

---

## 1. CRUD персонажей

### 1.1 Создать персонажа

**Смысл:** Создание нового персонажа в кампании. Персонаж привязывается к текущему пользователю как владельцу.

```
POST /api/campaigns/{campaignId}/characters
```

**Request:**
```json
{
  "name": "Артас Менетил",
  "classId": "a1b2c3d4-0000-0000-0000-000000000001",
  "raceId": "a1b2c3d4-0000-0000-0000-000000000002",
  "campaignId": "a1b2c3d4-0000-0000-0000-000000000099"
}
```

| Поле | Тип | Обязательное | Валидация |
|------|-----|:---:|-----------|
| name | string | да | max 100 символов |
| classId | UUID | да | — |
| raceId | UUID | да | — |
| campaignId | UUID | да | — |

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "f5e6d7c8-0000-0000-0000-000000000010",
    "name": "Артас Менетил",
    "totalLevel": 1,
    "experience": 0,
    "classLevels": [
      { "classId": "a1b2c3d4-...-01", "className": "Паладин", "classLevel": 1 }
    ],
    "race": { "id": "...", "name": "Человек" },
    "ownerId": "...",
    "ownerUsername": "player1",
    "stats": [],
    "createdAt": "2026-06-01T10:00:00Z",
    "updatedAt": "2026-06-01T10:00:00Z"
  },
  "message": "Персонаж создан"
}
```

---

### 1.2 Список персонажей кампании

**Смысл:** Получить всех персонажей, доступных текущему пользователю в данной кампании (ГМ видит всех, игрок — только своих).

```
GET /api/campaigns/{campaignId}/characters
```

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": "...",
      "name": "Артас Менетил",
      "totalLevel": 3,
      "experience": 900,
      "classLevels": [
        { "classId": "...", "className": "Паладин", "classLevel": 2 },
        { "classId": "...", "className": "Воин", "classLevel": 1 }
      ],
      "race": { "id": "...", "name": "Человек" },
      "ownerId": "...",
      "ownerUsername": "player1",
      "stats": [...],
      "createdAt": "...",
      "updatedAt": "..."
    }
  ]
}
```

---

### 1.3 Получить персонажа по ID

**Смысл:** Детальная информация об одном персонаже.

```
GET /api/campaigns/{campaignId}/characters/{characterId}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "f5e6d7c8-...",
    "name": "Артас Менетил",
    "totalLevel": 3,
    "experience": 900,
    "classLevels": [
      { "classId": "...", "className": "Паладин", "classLevel": 2 },
      { "classId": "...", "className": "Воин", "classLevel": 1 }
    ],
    "race": { "id": "...", "name": "Человек" },
    "ownerId": "...",
    "ownerUsername": "player1",
    "stats": [
      {
        "id": "...",
        "statTypeId": "...",
        "statTypeName": "Сила",
        "value": 16,
        "effectiveValue": 18,
        "activeModifiers": [
          { "source": "Belt of Giant Strength", "modifierValue": 2 }
        ]
      }
    ],
    "createdAt": "...",
    "updatedAt": "..."
  }
}
```

---

### 1.4 Обновить персонажа

**Смысл:** Изменить имя или расу персонажа. Все поля опциональные — передаются только изменённые.

```
PUT /api/campaigns/{campaignId}/characters/{characterId}
```

**Request:**
```json
{
  "name": "Артас Падший",
  "raceId": "a1b2c3d4-0000-0000-0000-000000000003"
}
```

| Поле | Тип | Обязательное | Валидация |
|------|-----|:---:|-----------|
| name | string | нет | max 100 символов |
| raceId | UUID | нет | — |

**Response (200):** аналогична `CharacterResponse` с `message: "Персонаж обновлен"`

---

### 1.5 Удалить персонажа

**Смысл:** Безвозвратное удаление персонажа из кампании.

```
DELETE /api/campaigns/{campaignId}/characters/{characterId}
```

**Response (200):**
```json
{ "success": true, "data": null, "message": "Персонаж удален" }
```

---

## 2. Характеристики (Stats)

### 2.1 Получить характеристики персонажа

**Смысл:** Показать все статы персонажа с учётом активных модификаторов (баффы, экипировка). `value` — базовое значение, `effectiveValue` — итоговое с модификаторами.

```
GET /api/campaigns/{campaignId}/characters/{characterId}/stats
```

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": "stat-uuid-1",
      "statTypeId": "type-uuid-str",
      "statTypeName": "Сила",
      "value": 16,
      "effectiveValue": 18,
      "activeModifiers": [
        { "source": "Gauntlets of Ogre Power", "modifierValue": 2 }
      ]
    },
    {
      "id": "stat-uuid-2",
      "statTypeId": "type-uuid-dex",
      "statTypeName": "Ловкость",
      "value": 14,
      "effectiveValue": 14,
      "activeModifiers": []
    }
  ]
}
```

---

### 2.2 Обновить значение характеристики

**Смысл:** Установить базовое значение конкретного стата (1–30). Используется при создании персонажа или при ручной корректировке ГМом.

```
PUT /api/campaigns/{campaignId}/characters/{characterId}/stats/{statId}
```

**Request:**
```json
{ "value": 18 }
```

| Поле | Тип | Обязательное | Валидация |
|------|-----|:---:|-----------|
| value | integer | да | 1–30 |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "stat-uuid-1",
    "statTypeId": "...",
    "statTypeName": "Сила",
    "value": 18,
    "effectiveValue": 20,
    "activeModifiers": [...]
  },
  "message": "Характеристика обновлена"
}
```

---

## 3. Инвентарь (Inventory)

### 3.1 Получить весь инвентарь

**Смысл:** Полный список предметов персонажа (и экипированные, и в рюкзаке).

```
GET /api/campaigns/{campaignId}/characters/{characterId}/inventory
```

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": "instance-uuid-1",
      "templateId": "template-uuid-sword",
      "templateName": "Длинный меч",
      "displayName": "Клинок Рассвета",
      "customName": "Клинок Рассвета",
      "quantity": 1,
      "isUnique": true,
      "slot": "MAIN_HAND",
      "notes": null,
      "rarity": "RARE",
      "enchantments": [
        {
          "id": "...",
          "enchantmentType": { "id": "...", "name": "Огненный", "description": "..." },
          "appliedAt": "2026-05-20T15:00:00Z",
          "notes": "+1d6 fire"
        }
      ]
    },
    {
      "id": "instance-uuid-2",
      "templateId": "template-uuid-potion",
      "templateName": "Зелье лечения",
      "displayName": "Зелье лечения",
      "customName": null,
      "quantity": 3,
      "isUnique": false,
      "slot": null,
      "notes": null,
      "rarity": "COMMON",
      "enchantments": []
    }
  ]
}
```

---

### 3.2 Получить экипированные предметы

**Смысл:** Только те предметы, которые активно используются (надеты в слоты).

```
GET /api/campaigns/{campaignId}/characters/{characterId}/inventory/equipped
```

**Response:** аналогична 3.1, но только предметы с `slot != null`.

---

### 3.3 Получить рюкзак

**Смысл:** Только неэкипированные предметы (лежат в инвентаре, не задействованы).

```
GET /api/campaigns/{campaignId}/characters/{characterId}/inventory/backpack
```

**Response:** аналогична 3.1, но только предметы с `slot == null`.

---

### 3.4 Выдать предмет персонажу (GM only)

**Смысл:** ГМ выдаёт предмет из шаблона. Можно указать количество, кастомное имя и уникальность.

```
POST /api/campaigns/{campaignId}/characters/{characterId}/inventory
```

**Request:**
```json
{
  "templateId": "template-uuid-sword",
  "quantity": 1,
  "customName": "Клинок Рассвета",
  "isUnique": true
}
```

| Поле | Тип | Обязательное | Валидация |
|------|-----|:---:|-----------|
| templateId | UUID | да | — |
| quantity | integer | нет (default: 1) | min 1 |
| customName | string | нет | — |
| isUnique | boolean | нет | — |

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "new-instance-uuid",
    "templateId": "template-uuid-sword",
    "templateName": "Длинный меч",
    "displayName": "Клинок Рассвета",
    "customName": "Клинок Рассвета",
    "quantity": 1,
    "isUnique": true,
    "slot": null,
    "rarity": "RARE",
    "enchantments": []
  },
  "message": "Item granted"
}
```

---

### 3.5 Экипировать предмет

**Смысл:** Надеть предмет в указанный слот (MAIN_HAND, OFF_HAND, HEAD, CHEST, LEGS, FEET, RING, AMULET и т.д.).

```
POST /api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/equip
```

**Request:**
```json
{ "slot": "MAIN_HAND" }
```

| Поле | Тип | Обязательное | Валидация |
|------|-----|:---:|-----------|
| slot | string | да | не пустая строка |

**Response (200):**
```json
{
  "success": true,
  "data": { "id": "...", "slot": "MAIN_HAND", "..." : "..." },
  "message": "Item equipped"
}
```

---

### 3.6 Снять предмет

**Смысл:** Убрать предмет из слота обратно в рюкзак.

```
POST /api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/unequip
```

**Request:** пустое тело (без body)

**Response (200):**
```json
{
  "success": true,
  "data": { "id": "...", "slot": null, "..." : "..." },
  "message": "Item unequipped"
}
```

---

### 3.7 Удалить предмет (GM only)

**Смысл:** ГМ удаляет предмет из инвентаря персонажа (потерян, уничтожен и т.д.).

```
DELETE /api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}
```

**Response (200):**
```json
{ "success": true, "data": null, "message": "Item removed" }
```

---

### 3.8 Передать предмет другому персонажу

**Смысл:** Передача предмета от одного персонажа к другому в той же кампании (торговля, дарение).

```
POST /api/campaigns/{campaignId}/characters/{fromCharId}/inventory/{instanceId}/transfer
```

**Request:**
```json
{ "toCharacterId": "target-character-uuid" }
```

| Поле | Тип | Обязательное | Валидация |
|------|-----|:---:|-----------|
| toCharacterId | UUID | да | — |

**Response (200):**
```json
{
  "success": true,
  "data": { "id": "...", "..." : "..." },
  "message": "Item transferred"
}
```

---

### 3.9 Переименовать предмет

**Смысл:** Дать предмету пользовательское имя (например, зачарованному мечу — уникальное название).

```
PUT /api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/rename
```

**Request:**
```json
{
  "customName": "Ледяная Скорбь",
  "renameEntireStack": true
}
```

| Поле | Тип | Обязательное | Валидация |
|------|-----|:---:|-----------|
| customName | string | да | не пустая строка |
| renameEntireStack | boolean | нет (default: true) | — |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "...",
    "displayName": "Ледяная Скорбь",
    "customName": "Ледяная Скорбь"
  },
  "message": "Item renamed"
}
```

---

## 4. Эффекты (Buffs / Debuffs)

### 4.1 Получить активные эффекты

**Смысл:** Список всех баффов и дебаффов, наложенных на персонажа. Показывает оставшиеся раунды и кто наложил.

```
GET /api/campaigns/{campaignId}/characters/{characterId}/effects
```

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": "effect-uuid-1",
      "buffDebuffId": "buff-template-uuid",
      "buffDebuffName": "Благословение",
      "isBuff": true,
      "effectType": "FLAT_BONUS",
      "modifierValue": 2,
      "targetStatName": "Сила",
      "remainingRounds": 10,
      "appliedAt": "2026-06-01T12:00:00Z",
      "appliedByUsername": "gm_player"
    }
  ]
}
```

---

### 4.2 Наложить эффект (GM only)

**Смысл:** ГМ накладывает бафф или дебафф на персонажа. `remainingRounds = null` означает бессрочный эффект.

```
POST /api/campaigns/{campaignId}/characters/{characterId}/effects
```

**Request:**
```json
{
  "buffDebuffId": "buff-template-uuid",
  "remainingRounds": 10
}
```

| Поле | Тип | Обязательное | Валидация |
|------|-----|:---:|-----------|
| buffDebuffId | UUID | да | — |
| remainingRounds | integer | нет (null = бессрочный) | — |

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "new-effect-uuid",
    "buffDebuffId": "buff-template-uuid",
    "buffDebuffName": "Благословение",
    "isBuff": true,
    "effectType": "FLAT_BONUS",
    "modifierValue": 2,
    "targetStatName": "Сила",
    "remainingRounds": 10,
    "appliedAt": "2026-06-01T12:05:00Z",
    "appliedByUsername": "gm_player"
  },
  "message": "Effect applied"
}
```

---

### 4.3 Снять эффект (GM only)

**Смысл:** ГМ досрочно снимает эффект с персонажа (развеян, отменён).

```
DELETE /api/campaigns/{campaignId}/characters/{characterId}/effects/{effectId}
```

**Response (200):**
```json
{ "success": true, "data": null, "message": "Effect removed" }
```

---

### 4.4 Рассчитать модификатор проверки характеристики

**Смысл:** Получить итоговый модификатор для броска проверки по характеристике, с учётом базы, баффов и бонусов от экипировки. Полезно для отображения результата перед броском.

```
GET /api/campaigns/{campaignId}/characters/{characterId}/ability-check/{statTypeId}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "statName": "Сила",
    "baseValue": 16,
    "modifier": 3,
    "buffBonus": 2,
    "equipmentBonus": 1,
    "totalModifier": 6
  }
}
```

**Формула:**
- `modifier` = `floor((baseValue - 10) / 2)` — стандартный D&D модификатор
- `totalModifier` = `modifier + buffBonus + equipmentBonus`

---

## 5. Кошелёк (Wallet)

### 5.1 Получить кошелёк

**Смысл:** Баланс всех валют персонажа с эквивалентом в золоте.

```
GET /api/campaigns/{campaignId}/characters/{characterId}/wallet
```

**Response (200):**
```json
{
  "success": true,
  "data": [
    { "currencyTypeId": "cur-gold-uuid", "currencyName": "Золото", "amount": 150.00, "goldEquivalent": 150.00 },
    { "currencyTypeId": "cur-silver-uuid", "currencyName": "Серебро", "amount": 30.00, "goldEquivalent": 3.00 },
    { "currencyTypeId": "cur-copper-uuid", "currencyName": "Медь", "amount": 50.00, "goldEquivalent": 0.50 }
  ]
}
```

---

### 5.2 Изменить баланс валюты

**Смысл:** Добавить или вычесть валюту. Положительное `amount` — начисление, отрицательное — списание (покупка, штраф).

```
POST /api/campaigns/{campaignId}/characters/{characterId}/wallet
```

**Request:**
```json
{
  "currencyTypeId": "cur-gold-uuid",
  "amount": -25.00
}
```

| Поле | Тип | Обязательное | Валидация |
|------|-----|:---:|-----------|
| currencyTypeId | UUID | да | — |
| amount | BigDecimal | да | — |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "currencyTypeId": "cur-gold-uuid",
    "currencyName": "Золото",
    "amount": 125.00,
    "goldEquivalent": 125.00
  },
  "message": "Currency updated"
}
```

---

## 6. Ресурсы (Resources)

### 6.1 Получить ресурсы персонажа

**Смысл:** Список расходуемых ресурсов (слоты заклинаний, ярость, ки-очки и пр.) с текущим и максимальным значениями.

```
GET /api/campaigns/{campaignId}/characters/{characterId}/resources
```

**Response (200):**
```json
{
  "success": true,
  "data": [
    { "resourceTypeId": "res-spell-uuid", "resourceName": "Слоты заклинаний (1 круг)", "currentValue": 2, "maxValue": 4 },
    { "resourceTypeId": "res-rage-uuid", "resourceName": "Ярость", "currentValue": 3, "maxValue": 3 }
  ]
}
```

---

### 6.2 Изменить значение ресурса

**Смысл:** Установить текущее значение ресурса (потратить слот, восстановить при отдыхе).

```
POST /api/campaigns/{campaignId}/characters/{characterId}/resources
```

**Request:**
```json
{
  "resourceTypeId": "res-spell-uuid",
  "currentValue": 1
}
```

| Поле | Тип | Обязательное | Валидация |
|------|-----|:---:|-----------|
| resourceTypeId | UUID | да | — |
| currentValue | integer | да | — |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "resourceTypeId": "res-spell-uuid",
    "resourceName": "Слоты заклинаний (1 круг)",
    "currentValue": 1,
    "maxValue": 4
  },
  "message": "Resource updated"
}
```

---

## 7. Здоровье (HP)

### 7.1 Изменить HP персонажа

**Смысл:** Нанести урон (отрицательное значение) или вылечить (положительное). Сервер сам пересчитывает итоговое HP.

```
POST /api/campaigns/{campaignId}/characters/{characterId}/hp
```

**Request:**
```json
{ "amount": -15 }
```

| Поле | Тип | Обязательное | Валидация |
|------|-----|:---:|-----------|
| amount | integer | да | — |

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "...",
    "name": "Артас Менетил",
    "totalLevel": 3,
    "experience": 900,
    "classLevels": [...],
    "race": {...},
    "ownerId": "...",
    "ownerUsername": "player1",
    "stats": [...],
    "createdAt": "...",
    "updatedAt": "..."
  },
  "message": "HP modified"
}
```

---

## Сводная таблица эндпоинтов

| # | Метод | URL | Доступ | Назначение |
|---|-------|-----|--------|------------|
| 1 | POST | `/characters` | Игрок/ГМ | Создать персонажа |
| 2 | GET | `/characters` | Игрок/ГМ | Список персонажей |
| 3 | GET | `/characters/{id}` | Игрок/ГМ | Детали персонажа |
| 4 | PUT | `/characters/{id}` | Владелец/ГМ | Обновить персонажа |
| 5 | DELETE | `/characters/{id}` | Владелец/ГМ | Удалить персонажа |
| 6 | GET | `/characters/{id}/stats` | Игрок/ГМ | Характеристики |
| 7 | PUT | `/characters/{id}/stats/{statId}` | Владелец/ГМ | Изменить стат |
| 8 | GET | `/characters/{id}/inventory` | Игрок/ГМ | Весь инвентарь |
| 9 | GET | `/characters/{id}/inventory/equipped` | Игрок/ГМ | Экипировка |
| 10 | GET | `/characters/{id}/inventory/backpack` | Игрок/ГМ | Рюкзак |
| 11 | POST | `/characters/{id}/inventory` | **GM only** | Выдать предмет |
| 12 | POST | `/characters/{id}/inventory/{iid}/equip` | Владелец/ГМ | Экипировать |
| 13 | POST | `/characters/{id}/inventory/{iid}/unequip` | Владелец/ГМ | Снять |
| 14 | DELETE | `/characters/{id}/inventory/{iid}` | **GM only** | Убрать предмет |
| 15 | POST | `/characters/{fromId}/inventory/{iid}/transfer` | Владелец/ГМ | Передать предмет |
| 16 | PUT | `/characters/{id}/inventory/{iid}/rename` | Владелец/ГМ | Переименовать |
| 17 | GET | `/characters/{id}/effects` | Игрок/ГМ | Активные эффекты |
| 18 | POST | `/characters/{id}/effects` | **GM only** | Наложить эффект |
| 19 | DELETE | `/characters/{id}/effects/{eid}` | **GM only** | Снять эффект |
| 20 | GET | `/characters/{id}/ability-check/{statTypeId}` | Игрок/ГМ | Расчёт модификатора |
| 21 | GET | `/characters/{id}/wallet` | Игрок/ГМ | Кошелёк |
| 22 | POST | `/characters/{id}/wallet` | Владелец/ГМ | Изменить валюту |
| 23 | GET | `/characters/{id}/resources` | Игрок/ГМ | Ресурсы |
| 24 | POST | `/characters/{id}/resources` | Владелец/ГМ | Изменить ресурс |
| 25 | POST | `/characters/{id}/hp` | Владелец/ГМ | Изменить HP |

Все пути относительно базового: `/api/campaigns/{campaignId}`.
