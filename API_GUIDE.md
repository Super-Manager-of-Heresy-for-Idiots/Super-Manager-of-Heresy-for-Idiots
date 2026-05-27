# D&D Character Management API Guide

## Содержание

- [Запуск](#запуск)
- [Swagger UI](#swagger-ui)
- [Формат ответов](#формат-ответов)
- [Аутентификация](#аутентификация)
- [Контроллеры](#контроллеры)
  - [AuthController](#authcontroller---apiauth)
  - [CharacterController](#charactercontroller---apicharacters)
  - [TeamController](#teamcontroller---apiteams)
  - [AdminController](#admincontroller---apiadmin)
  - [ArtifactController](#artifactcontroller---apiartifacts)
  - [ConditionController](#conditioncontroller---apiconditions)
- [Система эффективных характеристик](#система-эффективных-характеристик)
- [Тестовый скрипт](#тестовый-скрипт)

---

## Запуск

```bash
docker-compose up -d
```

Приложение будет доступно на `http://localhost:8080`.  
PostgreSQL на `localhost:5432` (db: `dndapp`, user: `dnd`, password: `dnd`).

Предустановленный админ: `admin` / `admin123`.

---

## Swagger UI

После запуска приложения Swagger UI доступен по адресу:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON-спецификация:

```
http://localhost:8080/v3/api-docs
```

### Как авторизоваться в Swagger UI

1. Выполните запрос `POST /api/auth/login` с логином и паролем
2. Скопируйте значение `token` из ответа
3. Нажмите кнопку **Authorize** (замок) в правом верхнем углу
4. В поле **Value** введите токен (без префикса `Bearer `, Swagger добавит его сам)
5. Нажмите **Authorize** и закройте диалог
6. Теперь все запросы будут отправляться с JWT-токеном

---

## Формат ответов

Все эндпоинты возвращают единый конверт:

**Успех:**
```json
{
  "success": true,
  "data": { ... },
  "message": "Human readable message"
}
```

**Ошибка:**
```json
{
  "success": false,
  "error": "ERROR_CODE",
  "message": "Human readable message"
}
```

**Ошибка валидации (HTTP 400):**
```json
{
  "success": false,
  "error": "VALIDATION_ERROR",
  "fields": {
    "username": "must be between 3 and 30 characters",
    "email": "Must be a valid email address"
  }
}
```

---

## Аутентификация

Все эндпоинты, кроме `/api/auth/*`, требуют JWT-токен в заголовке:

```
Authorization: Bearer <token>
```

Токен действителен 24 часа.

### Роли

| Роль | Может |
|------|-------|
| **PLAYER** | Создавать/управлять своими персонажами, управлять инвентарём, вступать в команды |
| **GAME_MASTER** | Создавать команды, просматривать персонажей участников, редактировать статы участников, создавать артефакты, управлять состояниями |
| **ADMIN** | Управлять справочниками (классы, расы, типы статов, типы предметов), просматривать всё |

---

## Контроллеры

### AuthController — `/api/auth`

Публичные эндпоинты, не требуют токена.

#### `POST /api/auth/register`

Регистрация нового пользователя. Роль `ADMIN` недоступна для самостоятельной регистрации.

**Тело запроса:**
```json
{
  "username": "hero_player",
  "email": "hero@example.com",
  "password": "mypassword123",
  "role": "PLAYER"
}
```

| Поле | Правила |
|------|---------|
| `username` | 3-30 символов, только `[a-zA-Z0-9_]` |
| `email` | Валидный email |
| `password` | Минимум 8 символов |
| `role` | `PLAYER` или `GAME_MASTER` |

**Ответ (201):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "username": "hero_player",
    "email": "hero@example.com",
    "role": "PLAYER",
    "createdAt": "2025-01-01T00:00:00Z"
  },
  "message": "Registration successful"
}
```

#### `POST /api/auth/login`

**Тело запроса:**
```json
{
  "username": "hero_player",
  "password": "mypassword123"
}
```

**Ответ (200):**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400000,
    "user": {
      "id": "uuid",
      "username": "hero_player",
      "email": "hero@example.com",
      "role": "PLAYER",
      "createdAt": "2025-01-01T00:00:00Z"
    }
  },
  "message": "Login successful"
}
```

---

### CharacterController — `/api/characters`

Требует JWT. Доступ определяется ролью.

#### `POST /api/characters`

Создать персонажа. Только `PLAYER`.  
Автоматически создаёт все статы (значение 10) и пустые слоты инвентаря.

**Тело запроса:**
```json
{
  "name": "Gandalf the Grey",
  "level": 5,
  "classId": "b0000000-0000-0000-0000-000000000002",
  "raceId": "c0000000-0000-0000-0000-000000000001"
}
```

| Поле | Правила |
|------|---------|
| `name` | Обязательное, макс. 100 символов |
| `level` | 1-20, по умолчанию 1 |
| `classId` | UUID существующего класса |
| `raceId` | UUID существующей расы |

**Предустановленные UUID классов:**

| Класс | UUID |
|-------|------|
| Fighter | `b0000000-0000-0000-0000-000000000001` |
| Wizard | `b0000000-0000-0000-0000-000000000002` |
| Rogue | `b0000000-0000-0000-0000-000000000003` |
| Cleric | `b0000000-0000-0000-0000-000000000004` |
| Ranger | `b0000000-0000-0000-0000-000000000005` |
| Paladin | `b0000000-0000-0000-0000-000000000006` |
| Bard | `b0000000-0000-0000-0000-000000000007` |
| Druid | `b0000000-0000-0000-0000-000000000008` |

**Предустановленные UUID рас:**

| Раса | UUID |
|------|------|
| Human | `c0000000-0000-0000-0000-000000000001` |
| Elf | `c0000000-0000-0000-0000-000000000002` |
| Dwarf | `c0000000-0000-0000-0000-000000000003` |
| Halfling | `c0000000-0000-0000-0000-000000000004` |
| Gnome | `c0000000-0000-0000-0000-000000000005` |
| Half-Elf | `c0000000-0000-0000-0000-000000000006` |
| Tiefling | `c0000000-0000-0000-0000-000000000007` |
| Dragonborn | `c0000000-0000-0000-0000-000000000008` |

#### `GET /api/characters`

Список персонажей:
- `PLAYER` — только свои
- `GAME_MASTER` — персонажи игроков из своих команд
- `ADMIN` — все

#### `GET /api/characters/{id}`

Получить персонажа по ID. Доступ по роли (см. выше).

#### `PUT /api/characters/{id}`

Обновить персонажа. Только `PLAYER` (свой). Можно менять `name`, `level`, `classId`, `raceId` — все поля опциональны.

```json
{
  "name": "Gandalf the White",
  "level": 20
}
```

#### `DELETE /api/characters/{id}`

Удалить персонажа. Только `PLAYER` (свой).

#### `GET /api/characters/{id}/stats`

Список статов персонажа. Если на персонажа наложены активные состояния (conditions), ответ включает вычисленное `effectiveValue` и список модификаторов.

**Пример ответа (с активным состоянием):**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "statTypeId": "uuid",
      "statTypeName": "STR",
      "value": 18,
      "effectiveValue": 15,
      "activeModifiers": [
        {
          "source": "Poisoned",
          "modifierValue": -3
        }
      ]
    }
  ]
}
```

Если модификаторов нет, поля `effectiveValue` и `activeModifiers` отсутствуют (null, скрыты `@JsonInclude(NON_NULL)`).

#### `PUT /api/characters/{id}/stats/{statId}`

Изменить значение стата. `PLAYER` (свой) или `GAME_MASTER` (участник его команды).

```json
{
  "value": 18
}
```

| Поле | Правила |
|------|---------|
| `value` | 1-30 |

#### `GET /api/characters/{id}/inventory`

Список слотов инвентаря.

#### `PUT /api/characters/{id}/inventory/{slot}`

Обновить слот инвентаря. Только `PLAYER` (свой).

`{slot}` — одно из: `HEAD`, `CHEST`, `LEGS`, `FEET`, `MAIN_HAND`, `OFF_HAND`, `RING_LEFT`, `RING_RIGHT`, `NECK`, `CLOAK`.

```json
{
  "itemTypeId": "uuid-item-type-or-null",
  "quantity": 1,
  "notes": "Enchanted +2"
}
```

Если `itemTypeId` = `null`, слот очищается. Тип слота предмета должен совпадать со слотом инвентаря.

> **Примечание:** Если в слот помещён артефакт (через `PUT /api/artifacts/place`), ответ будет содержать дополнительные поля: `artifactId`, `artifactName`, `artifactRarity`.

---

### TeamController — `/api/teams`

#### `POST /api/teams`

Создать команду. Только `GAME_MASTER`. Автоматически генерирует `inviteCode`.

```json
{
  "name": "The Fellowship"
}
```

#### `GET /api/teams`

- `GAME_MASTER` — свои команды
- `ADMIN` — все

#### `GET /api/teams/{id}`

Детали команды со списком участников. `GAME_MASTER` (своя) или `ADMIN`.

#### `PUT /api/teams/{id}`

Переименовать команду. Только `GAME_MASTER` (своя).

```json
{
  "name": "New Name"
}
```

#### `DELETE /api/teams/{id}`

Удалить команду. Только `GAME_MASTER` (своя).

#### `POST /api/teams/{id}/regenerate-invite`

Перегенерировать invite-код. Только `GAME_MASTER` (своя). Старый код перестаёт работать.

#### `GET /api/teams/{id}/invite-code`

Получить текущий invite-код. Только `GAME_MASTER` (своя).

#### `POST /api/teams/join`

Вступить в команду по invite-коду. Только `PLAYER`.

```json
{
  "inviteCode": "Ab3kX9mQ"
}
```

---

### AdminController — `/api/admin`

Все эндпоинты требуют роль `ADMIN`.

#### Stat Types — `/api/admin/stat-types`

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/admin/stat-types` | Список всех типов статов |
| `POST` | `/api/admin/stat-types` | Создать тип стата |
| `GET` | `/api/admin/stat-types/{id}` | Получить тип стата |
| `PUT` | `/api/admin/stat-types/{id}` | Обновить тип стата |
| `DELETE` | `/api/admin/stat-types/{id}` | Удалить тип стата |

```json
{
  "name": "LUCK",
  "description": "Measure of fortune and fate"
}
```

#### Item Types — `/api/admin/item-types`

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/admin/item-types` | Список всех типов предметов |
| `POST` | `/api/admin/item-types` | Создать тип предмета |
| `GET` | `/api/admin/item-types/{id}` | Получить тип предмета |
| `PUT` | `/api/admin/item-types/{id}` | Обновить тип предмета |
| `DELETE` | `/api/admin/item-types/{id}` | Удалить тип предмета |

```json
{
  "name": "Longsword",
  "description": "A versatile martial weapon",
  "slot": "MAIN_HAND"
}
```

Допустимые значения `slot`: `HEAD`, `CHEST`, `LEGS`, `FEET`, `MAIN_HAND`, `OFF_HAND`, `RING_LEFT`, `RING_RIGHT`, `NECK`, `CLOAK`.

#### Character Classes — `/api/admin/character-classes`

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/admin/character-classes` | Список классов |
| `POST` | `/api/admin/character-classes` | Создать класс |
| `GET` | `/api/admin/character-classes/{id}` | Получить класс |
| `PUT` | `/api/admin/character-classes/{id}` | Обновить класс |
| `DELETE` | `/api/admin/character-classes/{id}` | Удалить класс |

```json
{
  "name": "Warlock",
  "description": "A wielder of magic derived from a pact with an extraplanar entity"
}
```

#### Character Races — `/api/admin/character-races`

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/admin/character-races` | Список рас |
| `POST` | `/api/admin/character-races` | Создать расу |
| `GET` | `/api/admin/character-races/{id}` | Получить расу |
| `PUT` | `/api/admin/character-races/{id}` | Обновить расу |
| `DELETE` | `/api/admin/character-races/{id}` | Удалить расу |

```json
{
  "name": "Orc",
  "description": "Savage and fearless warriors"
}
```

#### Users & Teams (read-only)

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/admin/users` | Список всех пользователей |
| `GET` | `/api/admin/teams` | Список всех команд |

---

### ArtifactController — `/api/artifacts`

Управление артефактами (уникальными предметами). Только `GAME_MASTER`.

#### `POST /api/artifacts`

Создать артефакт.

**Тело запроса:**
```json
{
  "name": "Flame Tongue",
  "description": "A sword wreathed in fire",
  "itemTypeId": "uuid-of-item-type",
  "rarity": "RARE",
  "properties": "+2 to attack rolls",
  "specialAbilities": "Deals 2d6 extra fire damage"
}
```

| Поле | Правила |
|------|---------|
| `name` | Обязательное, макс. 100 символов |
| `description` | Опциональное |
| `itemTypeId` | UUID существующего типа предмета (определяет слот) |
| `rarity` | `COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE`, `LEGENDARY` (по умолчанию `COMMON`) |
| `properties` | Текст свойств, опциональное |
| `specialAbilities` | Текст особых умений, опциональное |

**Ответ (201):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Flame Tongue",
    "description": "A sword wreathed in fire",
    "itemTypeId": "uuid",
    "itemTypeName": "Longsword",
    "itemTypeSlot": "MAIN_HAND",
    "rarity": "RARE",
    "properties": "+2 to attack rolls",
    "specialAbilities": "Deals 2d6 extra fire damage",
    "createdById": "uuid",
    "createdAt": "2025-01-01T00:00:00Z"
  },
  "message": "Artifact created"
}
```

#### `GET /api/artifacts`

Список артефактов:
- `GAME_MASTER` — только созданные им
- `ADMIN` — все

#### `GET /api/artifacts/{id}`

Получить артефакт. `GAME_MASTER` видит только свои.

#### `PUT /api/artifacts/{id}`

Обновить артефакт. Только создатель.

#### `DELETE /api/artifacts/{id}`

Удалить артефакт. Только создатель.

#### `PUT /api/artifacts/place/{characterId}/{slot}`

Поместить артефакт в инвентарь персонажа. Только `GAME_MASTER`, персонаж должен принадлежать игроку из команды ГМ.

`{slot}` — одно из: `HEAD`, `CHEST`, `LEGS`, `FEET`, `MAIN_HAND`, `OFF_HAND`, `RING_LEFT`, `RING_RIGHT`, `NECK`, `CLOAK`.

Слот артефакта (`itemType.slot`) должен совпадать с целевым слотом инвентаря.

**Тело запроса:**
```json
{
  "artifactId": "uuid-of-artifact"
}
```

**Ответ (200):**
```json
{
  "success": true,
  "data": {
    "slot": "MAIN_HAND",
    "itemTypeId": "uuid",
    "itemTypeName": "Longsword",
    "artifactId": "uuid",
    "artifactName": "Flame Tongue",
    "artifactRarity": "RARE",
    "quantity": 1,
    "notes": "Flame Tongue [RARE]"
  },
  "message": "Artifact placed in inventory"
}
```

---

### ConditionController — `/api/conditions`

Управление состояниями (баффы/дебаффы). Только `GAME_MASTER`.

#### Workflow

1. **Создать состояние** (`POST /api/conditions`) — например, «Отравление»
2. **Добавить модификаторы** (`POST /api/conditions/{id}/modifiers`) — бонусы/штрафы к характеристикам
3. **Наложить на персонажа** (`POST /api/conditions/apply/{characterId}`) — состояние становится активным
4. **Снять с персонажа** (`DELETE /api/conditions/character/{characterId}/{charConditionId}`)

Пока состояние активно, `GET /api/characters/{id}/stats` возвращает `effectiveValue` с учётом модификаторов.

#### `POST /api/conditions`

Создать состояние.

**Тело запроса:**
```json
{
  "name": "Poisoned",
  "description": "The creature is weakened by poison"
}
```

| Поле | Правила |
|------|---------|
| `name` | Обязательное, макс. 50 символов, уникальное |
| `description` | Опциональное |

**Ответ (201):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Poisoned",
    "description": "The creature is weakened by poison",
    "modifiers": [],
    "createdById": "uuid",
    "createdAt": "2025-01-01T00:00:00Z"
  },
  "message": "Condition created"
}
```

#### `GET /api/conditions`

Список состояний:
- `GAME_MASTER` — только созданные им
- `ADMIN` — все

#### `GET /api/conditions/{id}`

Получить состояние с модификаторами.

#### `PUT /api/conditions/{id}`

Обновить название/описание. Только создатель.

#### `DELETE /api/conditions/{id}`

Удалить состояние. Только создатель.

#### `POST /api/conditions/{id}/modifiers`

Добавить модификатор к состоянию. Один тип стата — один модификатор.

**Тело запроса:**
```json
{
  "statTypeId": "a0000000-0000-0000-0000-000000000001",
  "modifierValue": -3
}
```

| Поле | Правила |
|------|---------|
| `statTypeId` | UUID существующего типа стата |
| `modifierValue` | Целое число (положительное = бонус, отрицательное = штраф) |

**Ответ (201):** полный объект состояния с обновлённым списком модификаторов.

**Предустановленные UUID типов статов:**

| Стат | UUID |
|------|------|
| STR | `a0000000-0000-0000-0000-000000000001` |
| DEX | `a0000000-0000-0000-0000-000000000002` |
| CON | `a0000000-0000-0000-0000-000000000003` |
| INT | `a0000000-0000-0000-0000-000000000004` |
| WIS | `a0000000-0000-0000-0000-000000000005` |
| CHA | `a0000000-0000-0000-0000-000000000006` |

#### `DELETE /api/conditions/{condId}/modifiers/{modId}`

Удалить модификатор. Только создатель состояния.

#### `POST /api/conditions/apply/{characterId}`

Наложить состояние на персонажа. Персонаж должен принадлежать игроку из команды ГМ.

**Тело запроса:**
```json
{
  "conditionId": "uuid-of-condition"
}
```

**Ответ (201):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "conditionId": "uuid",
    "conditionName": "Poisoned",
    "conditionDescription": "The creature is weakened by poison",
    "modifiers": [
      {
        "id": "uuid",
        "statTypeId": "a0000000-0000-0000-0000-000000000001",
        "statTypeName": "STR",
        "modifierValue": -3
      }
    ],
    "appliedById": "uuid",
    "appliedAt": "2025-01-01T00:00:00Z",
    "active": true
  },
  "message": "Condition applied"
}
```

#### `GET /api/conditions/character/{characterId}`

Список активных состояний на персонаже.

#### `DELETE /api/conditions/character/{characterId}/{charConditionId}`

Снять состояние с персонажа (помечает `active = false`). Только ГМ, персонаж из его команды.

---

## Система эффективных характеристик

Когда на персонажа наложены активные состояния с модификаторами, `GET /api/characters/{id}/stats` вычисляет **эффективное значение** каждого стата:

```
effectiveValue = baseValue + Σ(модификаторы от активных состояний)
```

**Пример:**
- Персонаж имеет STR = 18 (базовое)
- На него наложено состояние «Poisoned» с модификатором STR = -3
- На него наложено состояние «Blessed» с модификатором STR = +2
- `effectiveValue` STR = 18 + (-3) + 2 = **17**

Снятие состояния (`DELETE /api/conditions/character/...`) отключает все его модификаторы.

---

## Тестовый скрипт

В директории `src/test/` находится bash-скрипт `api-test.sh`, который прогоняет полный сценарий:

1. Логин админа
2. Создание кастомного класса и расы (админ)
3. Создание типа предмета (админ)
4. Регистрация игрока и гейм-мастера
5. Создание персонажа (игрок)
6. Просмотр статов и инвентаря
7. Изменение стата
8. Создание команды (ГМ)
9. Вступление в команду (игрок)
10. Просмотр персонажей через ГМ
11. Изменение стата через ГМ
12. Создание артефакта (ГМ)
13. Размещение артефакта в инвентаре персонажа (ГМ)
14. Создание состояния с модификаторами (ГМ)
15. Наложение состояния на персонажа (ГМ)
16. Проверка эффективных характеристик
17. Снятие состояния с персонажа (ГМ)
18. Негативные тесты контроля доступа

### Запуск

```bash
# Убедитесь, что приложение запущено
docker-compose up -d

# Дайте приложению 15 секунд на старт, затем:
chmod +x src/test/api-test.sh
./src/test/api-test.sh
```

Для Windows (Git Bash / WSL):
```bash
bash src/test/api-test.sh
```
