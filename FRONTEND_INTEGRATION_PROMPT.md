# Backend API Integration Guide for Frontend Claude Code

> **Назначение**: Этот файл — справочник для Claude Code, подключённого к фронтенд-проекту.
> Каждый раздел — самодостаточный блок, который можно скопировать и вставить как промпт.
> Бэкенд: Spring Boot 3 (Java 21), PostgreSQL 16, JWT auth, REST API на порту 8080.

---

## БЛОК 0 — Общая архитектура (вставь первым при старте работы)

```
Ты работаешь над фронтендом для D&D Character Management приложения.

Бэкенд — Spring Boot REST API, работает на http://localhost:8080.
Swagger UI доступен по http://localhost:8080/swagger-ui.html
OpenAPI JSON — http://localhost:8080/v3/api-docs

Все ответы бэкенда обёрнуты в единую оболочку:

{
  "success": true,
  "data": { ... },       // полезная нагрузка (null при ошибке)
  "message": "...",       // опционально
  "error": "ERROR_CODE"  // только при success: false
}

Поля со значением null не приходят в JSON (используется @JsonInclude(NON_NULL)).

При ошибках валидации (400) формат:
{
  "success": false,
  "error": "VALIDATION_ERROR",
  "message": "Validation failed",
  "fields": {
    "fieldName": "сообщение об ошибке"
  }
}

Коды ошибок HTTP:
- 400 — невалидный запрос / VALIDATION_ERROR
- 401 — невалидный или отсутствующий токен / BAD_CREDENTIALS
- 403 — нет прав (роль не подходит) / ACCESS_DENIED
- 404 — ресурс не найден / NOT_FOUND
- 409 — дубликат (username, email и т.д.) / DUPLICATE_RESOURCE
- 422 — бизнес-логика не позволяет / UNPROCESSABLE_ENTITY
- 500 — серверная ошибка / INTERNAL_ERROR

Все ID — UUID (строки формата "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx").
Все даты — ISO-8601 строки (Instant): "2025-01-15T12:30:00Z".
CORS на бэкенде не настроен — если фронтенд запущен на другом порту, используй прокси в dev-сервере.
```

---

## БЛОК 1 — Аутентификация и управление токеном

```
Аутентификация — JWT Bearer Token.

РЕГИСТРАЦИЯ:
POST /api/auth/register
Content-Type: application/json

Request body:
{
  "username": string,     // 3-30 символов, только [a-zA-Z0-9_]
  "email": string,        // валидный email
  "password": string,     // минимум 8 символов
  "role": string          // только "PLAYER" или "GAME_MASTER"
}

Response (201 Created):
{
  "success": true,
  "data": {
    "id": "uuid",
    "username": "string",
    "email": "string",
    "role": "PLAYER",
    "createdAt": "2025-01-15T12:30:00Z"
  }
}

Ошибки:
- 409 если username или email уже заняты


ЛОГИН:
POST /api/auth/login
Content-Type: application/json

Request body:
{
  "username": string,
  "password": string
}

Response (200 OK):
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400000,
    "user": {
      "id": "uuid",
      "username": "string",
      "email": "string",
      "role": "PLAYER",
      "createdAt": "2025-01-15T12:30:00Z"
    }
  }
}

Ошибки:
- 401 если неправильный логин/пароль


КАК ИСПОЛЬЗОВАТЬ ТОКЕН:
Все запросы кроме /api/auth/* требуют заголовок:
Authorization: Bearer <token>

Токен живёт 24 часа (86400000 мс). expiresIn приходит в ответе логина.
При 401 на любом запросе — токен истёк, нужен повторный логин.

Роль ADMIN нельзя выбрать при регистрации — она назначается вручную в БД.

Реализуй:
- Хранение токена (localStorage или httpOnly cookie через прокси)
- Автоматическое добавление Authorization header ко всем запросам
- Перенаправление на логин при получении 401
- Хранение данных пользователя (id, username, role) для UI-логики
```

---

## БЛОК 2 — Роли и доступ (логика отображения UI)

```
В системе 3 роли. Фронтенд должен показывать/скрывать UI-элементы по роли.

PLAYER может:
- Создавать и удалять своих персонажей
- Редактировать имя и расу своих персонажей
- Управлять инвентарём своих персонажей
- Просматривать свои статы (редактировать только свои)
- Вступать в команды по invite-коду
- Видеть только своих персонажей в списке
- Просматривать опции level-up и повышать уровень

GAME_MASTER может:
- Создавать, редактировать, удалять команды
- Генерировать и просматривать invite-код команды
- Видеть персонажей участников своих команд
- Редактировать статы персонажей участников своих команд
- Создавать, редактировать, удалять артефакты
- Размещать артефакты в инвентаре персонажей участников
- Создавать, редактировать, удалять состояния (conditions)
- Добавлять модификаторы к состояниям
- Применять и снимать состояния с персонажей участников
- Управлять справочными данными (классы, расы, предметы, типы статов, навыки, подклассы, черты)
- Настраивать награды за уровни классов

ADMIN может:
- Всё то же что GAME_MASTER в части справочников
- Видеть всех пользователей (GET /api/admin/users)
- Видеть все команды (GET /api/admin/teams)
- Видеть всех персонажей, все артефакты, все состояния

При построении навигации и маршрутов:
- Роль приходит в поле user.role при логине
- Показывай только доступные разделы для текущей роли
- Бэкенд всё равно проверяет права — но лучше не показывать кнопки, которые вернут 403
```

---

## БЛОК 3 — Персонажи (CRUD + статы + инвентарь)

```
СОЗДАНИЕ ПЕРСОНАЖА (только PLAYER):
POST /api/characters
Authorization: Bearer <token>

Request:
{
  "name": string,       // обязательно, до 100 символов
  "classId": "uuid",    // обязательно, ID класса из справочника
  "raceId": "uuid"      // обязательно, ID расы из справочника
}

Response (201):
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "string",
    "totalLevel": 1,
    "experience": 0,
    "classLevels": [
      {
        "classId": "uuid",
        "className": "Fighter",
        "classLevel": 1
      }
    ],
    "race": {
      "id": "uuid",
      "name": "Human",
      "description": "..."
    },
    "ownerId": "uuid",
    "ownerUsername": "string",
    "stats": [
      {
        "id": "uuid",
        "statTypeId": "uuid",
        "statTypeName": "STR",
        "value": 10,
        "effectiveValue": null,
        "activeModifiers": null
      }
    ],
    "inventorySlots": [
      {
        "id": "uuid",
        "slot": "HEAD",
        "itemTypeId": null,
        "itemTypeName": null,
        "quantity": 0,
        "notes": null
      }
    ],
    "createdAt": "...",
    "updatedAt": "..."
  }
}

При создании бэкенд автоматически:
- Создаёт статы для всех default stat types (STR, DEX, CON, INT, WIS, CHA) со значением 10
- Создаёт 10 пустых слотов инвентаря: HEAD, CHEST, LEGS, FEET, MAIN_HAND, OFF_HAND, RING_LEFT, RING_RIGHT, NECK, CLOAK
- Устанавливает totalLevel = 1 и experience = 0


СПИСОК ПЕРСОНАЖЕЙ:
GET /api/characters
Authorization: Bearer <token>

PLAYER видит только своих, GAME_MASTER видит персонажей участников своих команд, ADMIN видит всех.

Response (200):
{
  "success": true,
  "data": [ ...массив CharacterResponse... ]
}


ПОЛУЧИТЬ ОДНОГО:
GET /api/characters/{id}
Authorization: Bearer <token>


ОБНОВИТЬ (владелец PLAYER или GAME_MASTER для участника команды):
PUT /api/characters/{id}
Authorization: Bearer <token>

Request:
{
  "name": string,     // опционально, до 100 символов
  "raceId": "uuid"    // опционально
}


УДАЛИТЬ (только владелец PLAYER):
DELETE /api/characters/{id}
Authorization: Bearer <token>

Response (200):
{
  "success": true,
  "message": "Character deleted successfully"
}


СТАТЫ ПЕРСОНАЖА:
GET /api/characters/{id}/stats

Response (200):
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "statTypeId": "uuid",
      "statTypeName": "STR",
      "value": 14,
      "effectiveValue": 16,        // появляется только если есть активные модификаторы
      "activeModifiers": [          // появляется только если есть активные модификаторы
        {
          "source": "Blessed",
          "modifierValue": 2
        }
      ]
    }
  ]
}

Поля effectiveValue и activeModifiers приходят только когда на персонажа наложены условия (conditions) с модификаторами. Если модификаторов нет — эти поля отсутствуют в JSON.


ОБНОВИТЬ СТАТ:
PUT /api/characters/{id}/stats/{statId}
Authorization: Bearer <token>

Request:
{
  "value": integer  // от 1 до 30
}

statId — это id из CharacterStatResponse, НЕ statTypeId.


ИНВЕНТАРЬ ПЕРСОНАЖА:
GET /api/characters/{id}/inventory

Response (200):
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "slot": "MAIN_HAND",
      "itemTypeId": "uuid",
      "itemTypeName": "Sword",
      "artifactId": "uuid",
      "artifactName": "Excalibur",
      "artifactRarity": "LEGENDARY",
      "quantity": 1,
      "notes": "Found in dungeon"
    }
  ]
}

artifactId/artifactName/artifactRarity — опциональные поля, приходят только если в слоте размещён артефакт.


ОБНОВИТЬ СЛОТ ИНВЕНТАРЯ (только владелец PLAYER):
PUT /api/characters/{id}/inventory/{slot}
Authorization: Bearer <token>

slot — одно из: HEAD, CHEST, LEGS, FEET, MAIN_HAND, OFF_HAND, RING_LEFT, RING_RIGHT, NECK, CLOAK

Request:
{
  "itemTypeId": "uuid",  // null чтобы очистить слот
  "quantity": integer,    // минимум 1, по умолчанию 1
  "notes": string         // опционально
}

Бэкенд проверяет что slot типа предмета совпадает со слотом инвентаря.
```

---

## БЛОК 4 — Система повышения уровня (Level-Up)

```
ПОЛУЧИТЬ ОПЦИИ LEVEL-UP:
GET /api/characters/{id}/level-up-options
Authorization: Bearer <token>

Возвращает доступные классы для повышения и награды за каждый.

Response (200):
{
  "success": true,
  "data": {
    "currentTotalLevel": 3,
    "xpToNextLevel": 900,
    "availableClasses": [
      {
        "classId": "uuid",
        "className": "Fighter",
        "currentLevelInClass": 2,
        "newLevelInClass": 3,
        "rewardGroups": [
          {
            "rewardType": "SKILL",
            "isChoice": true,
            "rewards": [
              {
                "rewardEntryId": "uuid",
                "rewardId": "uuid",
                "name": "Athletics",
                "description": "...",
                "alreadyAcquired": false
              }
            ]
          }
        ]
      }
    ]
  }
}

Логика UI:
- Покажи все availableClasses — игрок выбирает один для повышения
- Для выбранного класса покажи rewardGroups
- Если isChoice: true — игрок должен выбрать ОДНУ награду из списка
- Если isChoice: false — все награды выдаются автоматически
- Награды с alreadyAcquired: true — уже есть у персонажа, покажи это визуально


ВЫПОЛНИТЬ LEVEL-UP (только владелец PLAYER):
POST /api/characters/{id}/level-up
Authorization: Bearer <token>

Request:
{
  "classId": "uuid",
  "selections": [
    {
      "rewardType": "SKILL",
      "rewardEntryId": "uuid"
    }
  ]
}

selections — массив только для reward groups где isChoice: true.
rewardEntryId берётся из поля rewardEntryId (не rewardId!) в ответе level-up-options.

Response (200):
{
  "success": true,
  "data": {
    "newTotalLevel": 4,
    "classLeveled": "Fighter",
    "newClassLevel": 3,
    "rewardsAcquired": [
      {
        "rewardType": "SKILL",
        "name": "Athletics"
      }
    ]
  }
}


ПОЛУЧИТЬ ВСЕ НАГРАДЫ ПЕРСОНАЖА:
GET /api/characters/{id}/rewards
Authorization: Bearer <token>

Response (200):
{
  "success": true,
  "data": {
    "characterId": "uuid",
    "totalLevel": 4,
    "classBreakdown": [
      {
        "classId": "uuid",
        "className": "Fighter",
        "classLevel": 3,
        "subclass": {                    // null если не выбран
          "name": "Champion",
          "description": "..."
        },
        "rewardsByType": {
          "SKILL": [
            {
              "name": "Athletics",
              "acquiredAt": "2025-01-15T12:30:00Z"
            }
          ],
          "FEAT": []
        }
      }
    ]
  }
}
```

---

## БЛОК 5 — Команды

```
СОЗДАТЬ КОМАНДУ (только GAME_MASTER):
POST /api/teams
Authorization: Bearer <token>

Request:
{
  "name": string  // обязательно, до 80 символов
}

Response (201):
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "string",
    "gameMasterId": "uuid",
    "gameMasterUsername": "string",
    "members": [],
    "createdAt": "...",
    "updatedAt": "..."
  }
}


СПИСОК КОМАНД:
GET /api/teams
Authorization: Bearer <token>

GAME_MASTER видит свои команды, ADMIN — все.


ДЕТАЛИ КОМАНДЫ (GAME_MASTER владелец или ADMIN):
GET /api/teams/{id}

Response:
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Heroes",
    "gameMasterId": "uuid",
    "gameMasterUsername": "dm_john",
    "members": [
      {
        "playerId": "uuid",
        "playerUsername": "player1",
        "joinedAt": "..."
      }
    ],
    "createdAt": "...",
    "updatedAt": "..."
  }
}


ОБНОВИТЬ КОМАНДУ:
PUT /api/teams/{id}
Request: { "name": "New Name" }


УДАЛИТЬ КОМАНДУ:
DELETE /api/teams/{id}


ПОЛУЧИТЬ INVITE-КОД (GAME_MASTER владелец):
GET /api/teams/{id}/invite-code

Response:
{
  "success": true,
  "data": {
    "inviteCode": "A1B2C3D4"
  }
}


ПЕРЕГЕНЕРИРОВАТЬ INVITE-КОД:
POST /api/teams/{id}/regenerate-invite

Response: InviteCodeResponse с новым кодом.


ВСТУПИТЬ В КОМАНДУ (только PLAYER):
POST /api/teams/join
Authorization: Bearer <token>

Request:
{
  "inviteCode": "A1B2C3D4"
}

Response: TeamResponse с данными команды, куда вступил.

Invite-код — 8 символов, буквенно-цифровой. Показывай его GAME_MASTER-у для передачи игрокам.
```

---

## БЛОК 6 — Артефакты (GAME_MASTER)

```
СОЗДАТЬ АРТЕФАКТ:
POST /api/artifacts
Authorization: Bearer <token>

Request:
{
  "name": string,           // обязательно, до 100 символов
  "description": string,    // опционально
  "itemTypeId": "uuid",     // обязательно, определяет в какой слот можно надеть
  "rarity": string,         // COMMON | UNCOMMON | RARE | VERY_RARE | LEGENDARY
  "properties": string,     // опционально, текстовое описание свойств
  "specialAbilities": string // опционально
}

Response (201):
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Excalibur",
    "description": "Legendary sword",
    "itemTypeId": "uuid",
    "itemTypeName": "Sword",
    "itemTypeSlot": "MAIN_HAND",
    "rarity": "LEGENDARY",
    "properties": "...",
    "specialAbilities": "...",
    "createdById": "uuid",
    "createdAt": "..."
  }
}


СПИСОК АРТЕФАКТОВ:
GET /api/artifacts
GAME_MASTER видит свои, ADMIN — все.


ПОЛУЧИТЬ / ОБНОВИТЬ / УДАЛИТЬ:
GET    /api/artifacts/{id}
PUT    /api/artifacts/{id}   (тело как при создании)
DELETE /api/artifacts/{id}


РАЗМЕСТИТЬ АРТЕФАКТ В ИНВЕНТАРЕ ПЕРСОНАЖА (GAME_MASTER):
PUT /api/artifacts/place/{characterId}/{slot}
Authorization: Bearer <token>

Request:
{
  "artifactId": "uuid"
}

slot — один из: HEAD, CHEST, LEGS, FEET, MAIN_HAND, OFF_HAND, RING_LEFT, RING_RIGHT, NECK, CLOAK

Бэкенд проверяет:
- Персонаж принадлежит участнику команды текущего GM
- Слот артефакта (itemType.slot) совпадает со слотом инвентаря

Response: InventorySlotResponse с заполненными полями artifactId, artifactName, artifactRarity.

Возможные значения rarity для UI (цветовая схема):
- COMMON — серый/белый
- UNCOMMON — зелёный
- RARE — синий
- VERY_RARE — фиолетовый
- LEGENDARY — оранжевый/золотой
```

---

## БЛОК 7 — Состояния и модификаторы (GAME_MASTER)

```
Система условий (conditions) позволяет GM накладывать баффы/дебаффы на персонажей.
Каждое условие имеет модификаторы, которые изменяют effective value статов.

СОЗДАТЬ УСЛОВИЕ:
POST /api/conditions
Authorization: Bearer <token>

Request:
{
  "name": string,        // обязательно, до 50 символов, уникальное
  "description": string  // опционально
}

Response (201):
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Blessed",
    "description": "Divine blessing",
    "modifiers": [],
    "createdById": "uuid",
    "createdAt": "..."
  }
}


СПИСОК / ПОЛУЧИТЬ / ОБНОВИТЬ / УДАЛИТЬ:
GET    /api/conditions
GET    /api/conditions/{id}
PUT    /api/conditions/{id}
DELETE /api/conditions/{id}


ДОБАВИТЬ МОДИФИКАТОР К УСЛОВИЮ:
POST /api/conditions/{conditionId}/modifiers
Authorization: Bearer <token>

Request:
{
  "statTypeId": "uuid",     // ID типа стата (STR, DEX и т.д.)
  "modifierValue": integer  // положительное = бонус, отрицательное = штраф
}

Один модификатор на один тип стата на одно условие.

Response: обновлённый ConditionResponse с массивом modifiers.


УДАЛИТЬ МОДИФИКАТОР:
DELETE /api/conditions/{conditionId}/modifiers/{modifierId}


НАЛОЖИТЬ УСЛОВИЕ НА ПЕРСОНАЖА (GAME_MASTER):
POST /api/conditions/apply/{characterId}
Authorization: Bearer <token>

Request:
{
  "conditionId": "uuid"
}

Персонаж должен быть из команды текущего GM.

Response:
{
  "success": true,
  "data": {
    "id": "uuid",
    "conditionId": "uuid",
    "conditionName": "Blessed",
    "conditionDescription": "Divine blessing",
    "modifiers": [
      {
        "id": "uuid",
        "statTypeId": "uuid",
        "statTypeName": "STR",
        "modifierValue": 2
      }
    ],
    "appliedById": "uuid",
    "appliedAt": "...",
    "active": true
  }
}


ПОСМОТРЕТЬ АКТИВНЫЕ УСЛОВИЯ ПЕРСОНАЖА:
GET /api/conditions/character/{characterId}

Response: массив CharacterConditionResponse.


СНЯТЬ УСЛОВИЕ:
DELETE /api/conditions/character/{characterId}/{characterConditionId}

characterConditionId — это id из CharacterConditionResponse, НЕ conditionId.

Когда условие активно, в статах персонажа (GET /api/characters/{id}/stats):
- effectiveValue = value + сумма всех modifierValue от активных условий
- activeModifiers показывает источник каждого модификатора

UI должен показывать:
- Базовое значение стата (value)
- Эффективное значение (effectiveValue) если отличается
- Список активных модификаторов с источниками
- Визуальную индикацию баффов (зелёный/+) и дебаффов (красный/-)
```

---

## БЛОК 8 — Справочные данные (Admin / GM панель)

```
Справочные данные — это базовые сущности, которые используются при создании персонажей,
предметов, артефактов и т.д. Управляют ими ADMIN и GAME_MASTER.

Все CRUD-эндпоинты имеют одинаковую структуру.


ТИПЫ СТАТОВ:
GET    /api/admin/stat-types           — список
POST   /api/admin/stat-types           — создать { name, description }
GET    /api/admin/stat-types/{id}      — получить
PUT    /api/admin/stat-types/{id}      — обновить { name, description }
DELETE /api/admin/stat-types/{id}      — удалить

Response: { id, name, description, isDefault }
Предустановленные: STR, DEX, CON, INT, WIS, CHA (isDefault: true).


ТИПЫ ПРЕДМЕТОВ:
GET    /api/admin/item-types           — список
POST   /api/admin/item-types           — создать { name, description, slot }
GET    /api/admin/item-types/{id}      — получить
PUT    /api/admin/item-types/{id}      — обновить
DELETE /api/admin/item-types/{id}      — удалить

slot — один из: HEAD, CHEST, LEGS, FEET, MAIN_HAND, OFF_HAND, RING_LEFT, RING_RIGHT, NECK, CLOAK
Response: { id, name, description, slot }


КЛАССЫ ПЕРСОНАЖЕЙ:
GET    /api/admin/character-classes
POST   /api/admin/character-classes     — { name, description }
GET    /api/admin/character-classes/{id}
PUT    /api/admin/character-classes/{id}
DELETE /api/admin/character-classes/{id}

Response: { id, name, description }
Предустановленные: Fighter, Wizard, Rogue, Cleric, Ranger, Bard, Paladin, Warlock.


РАСЫ ПЕРСОНАЖЕЙ:
GET    /api/admin/character-races
POST   /api/admin/character-races       — { name, description }
GET    /api/admin/character-races/{id}
PUT    /api/admin/character-races/{id}
DELETE /api/admin/character-races/{id}

Response: { id, name, description }
Предустановленные: Human, Elf, Dwarf, Halfling, Gnome, Half-Orc, Tiefling, Dragonborn.


НАВЫКИ (Skills):
GET    /api/admin/skills
POST   /api/admin/skills                — { name, description, skillType }
GET    /api/admin/skills/{id}
PUT    /api/admin/skills/{id}
DELETE /api/admin/skills/{id}

Response: { id, name, description, skillType, createdAt, updatedAt }


ПОДКЛАССЫ (Subclasses):
GET    /api/admin/subclasses
POST   /api/admin/subclasses            — { name, classId, description }
GET    /api/admin/subclasses/{id}
PUT    /api/admin/subclasses/{id}
DELETE /api/admin/subclasses/{id}

Response: { id, name, classId, className, description, createdAt, updatedAt }


ЧЕРТЫ (Feats):
GET    /api/admin/feats
POST   /api/admin/feats                 — { name, description, prerequisites }
GET    /api/admin/feats/{id}
PUT    /api/admin/feats/{id}
DELETE /api/admin/feats/{id}

Response: { id, name, description, prerequisites, createdAt, updatedAt }


НАГРАДЫ ЗА УРОВНИ КЛАССОВ:
GET    /api/admin/classes/{classId}/level-rewards
POST   /api/admin/classes/{classId}/level-rewards
DELETE /api/admin/classes/{classId}/level-rewards/{rewardEntryId}

Request для POST:
{
  "requiredLevel": integer,   // 1-20
  "rewardType": string,       // "SKILL" | "SUBCLASS" | "FEAT"
  "rewardId": "uuid",         // ID навыка/подкласса/черты
  "isChoice": boolean         // true = игрок выбирает одну из, false = автоматически
}

Response: { id, classId, requiredLevel, rewardType, rewardId, rewardName, isChoice }


ПРОСМОТР ПОЛЬЗОВАТЕЛЕЙ (только ADMIN):
GET /api/admin/users
Response: массив UserResponse { id, username, email, role, createdAt }

ПРОСМОТР ВСЕХ КОМАНД (только ADMIN):
GET /api/admin/teams
Response: массив TeamResponse
```

---

## БЛОК 9 — TypeScript типы (скопируй в проект)

```
Сгенерируй TypeScript типы для работы с API на основе следующих контрактов.

// === Обёртка ответов ===
interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

interface ValidationErrorResponse {
  success: false;
  error: "VALIDATION_ERROR";
  message: string;
  fields: Record<string, string>;
}

// === Auth ===
interface RegisterRequest {
  username: string;  // 3-30, [a-zA-Z0-9_]
  email: string;
  password: string;  // min 8
  role: "PLAYER" | "GAME_MASTER";
}

interface LoginRequest {
  username: string;
  password: string;
}

interface UserResponse {
  id: string;       // UUID
  username: string;
  email: string;
  role: "PLAYER" | "GAME_MASTER" | "ADMIN";
  createdAt: string; // ISO-8601
}

interface AuthResponse {
  token: string;
  expiresIn: number;
  user: UserResponse;
}

// === Characters ===
interface CreateCharacterRequest {
  name: string;     // max 100
  classId: string;  // UUID
  raceId: string;   // UUID
}

interface UpdateCharacterRequest {
  name?: string;    // max 100
  raceId?: string;  // UUID
}

interface CharacterResponse {
  id: string;
  name: string;
  totalLevel: number;
  experience: number;
  classLevels: ClassLevelResponse[];
  race: CharacterRaceResponse;
  ownerId: string;
  ownerUsername: string;
  stats: CharacterStatResponse[];
  inventorySlots: InventorySlotResponse[];
  createdAt: string;
  updatedAt: string;
}

interface ClassLevelResponse {
  classId: string;
  className: string;
  classLevel: number;
}

interface CharacterStatResponse {
  id: string;
  statTypeId: string;
  statTypeName: string;
  value: number;
  effectiveValue?: number;           // только при наличии модификаторов
  activeModifiers?: StatModifierDetail[];  // только при наличии модификаторов
}

interface StatModifierDetail {
  source: string;
  modifierValue: number;
}

interface InventorySlotResponse {
  id: string;
  slot: EquipmentSlot;
  itemTypeId?: string;
  itemTypeName?: string;
  artifactId?: string;
  artifactName?: string;
  artifactRarity?: Rarity;
  quantity?: number;
  notes?: string;
}

type EquipmentSlot = "HEAD" | "CHEST" | "LEGS" | "FEET" | "MAIN_HAND" | "OFF_HAND" | "RING_LEFT" | "RING_RIGHT" | "NECK" | "CLOAK";

type Rarity = "COMMON" | "UNCOMMON" | "RARE" | "VERY_RARE" | "LEGENDARY";

// === Teams ===
interface CreateTeamRequest {
  name: string;  // max 80
}

interface JoinTeamRequest {
  inviteCode: string;
}

interface TeamResponse {
  id: string;
  name: string;
  gameMasterId: string;
  gameMasterUsername: string;
  members: TeamMemberResponse[];
  createdAt: string;
  updatedAt: string;
}

interface TeamMemberResponse {
  playerId: string;
  playerUsername: string;
  joinedAt: string;
}

interface InviteCodeResponse {
  inviteCode: string;
}

// === Artifacts ===
interface CreateArtifactRequest {
  name: string;        // max 100
  description?: string;
  itemTypeId: string;  // UUID
  rarity?: Rarity;
  properties?: string;
  specialAbilities?: string;
}

interface PlaceArtifactRequest {
  artifactId: string;
}

interface ArtifactResponse {
  id: string;
  name: string;
  description?: string;
  itemTypeId: string;
  itemTypeName: string;
  itemTypeSlot: string;
  rarity?: string;
  properties?: string;
  specialAbilities?: string;
  createdById: string;
  createdAt: string;
}

// === Conditions ===
interface CreateConditionRequest {
  name: string;        // max 50, уникальное
  description?: string;
}

interface AddConditionModifierRequest {
  statTypeId: string;
  modifierValue: number;  // + бонус, - штраф
}

interface ApplyConditionRequest {
  conditionId: string;
}

interface ConditionResponse {
  id: string;
  name: string;
  description?: string;
  modifiers: ConditionModifierResponse[];
  createdById: string;
  createdAt: string;
}

interface ConditionModifierResponse {
  id: string;
  statTypeId: string;
  statTypeName: string;
  modifierValue: number;
}

interface CharacterConditionResponse {
  id: string;
  conditionId: string;
  conditionName: string;
  conditionDescription?: string;
  modifiers: ConditionModifierResponse[];
  appliedById: string;
  appliedAt: string;
  active: boolean;
}

// === Level-Up ===
interface LevelUpRequest {
  classId: string;
  selections: RewardSelection[];
}

interface RewardSelection {
  rewardType: string;
  rewardEntryId: string;
}

interface LevelUpOptionsResponse {
  currentTotalLevel: number;
  xpToNextLevel: number;
  availableClasses: AvailableClassOption[];
}

interface AvailableClassOption {
  classId: string;
  className: string;
  currentLevelInClass: number;
  newLevelInClass: number;
  rewardGroups: RewardGroup[];
}

interface RewardGroup {
  rewardType: string;
  isChoice: boolean;
  rewards: RewardEntry[];
}

interface RewardEntry {
  rewardEntryId: string;
  rewardId: string;
  name: string;
  description?: string;
  alreadyAcquired: boolean;
}

interface LevelUpResultResponse {
  newTotalLevel: number;
  classLeveled: string;
  newClassLevel: number;
  rewardsAcquired: AcquiredRewardSummary[];
}

interface AcquiredRewardSummary {
  rewardType: string;
  name: string;
}

interface CharacterRewardsResponse {
  characterId: string;
  totalLevel: number;
  classBreakdown: ClassBreakdown[];
}

interface ClassBreakdown {
  classId: string;
  className: string;
  classLevel: number;
  subclass?: { name: string; description: string };
  rewardsByType: Record<string, AcquiredReward[]>;
}

interface AcquiredReward {
  name: string;
  acquiredAt: string;
}

// === Inventory ===
interface UpdateInventorySlotRequest {
  itemTypeId?: string;   // null чтобы очистить
  quantity?: number;     // min 1, default 1
  notes?: string;
}

interface UpdateStatRequest {
  value: number;  // 1-30
}

// === Admin / Reference Data ===
interface CreateStatTypeRequest { name: string; description?: string; }
interface StatTypeResponse { id: string; name: string; description?: string; isDefault: boolean; }

interface CreateItemTypeRequest { name: string; description?: string; slot: EquipmentSlot; }
interface ItemTypeResponse { id: string; name: string; description?: string; slot: string; }

interface CreateCharacterClassRequest { name: string; description?: string; }
interface CharacterClassResponse { id: string; name: string; description?: string; }

interface CreateCharacterRaceRequest { name: string; description?: string; }
interface CharacterRaceResponse { id: string; name: string; description?: string; }

interface CreateSkillRequest { name: string; description?: string; skillType?: string; }
interface SkillResponse { id: string; name: string; description?: string; skillType?: string; createdAt: string; updatedAt: string; }

interface CreateSubclassRequest { name: string; classId: string; description?: string; }
interface SubclassResponse { id: string; name: string; classId: string; className: string; description?: string; createdAt: string; updatedAt: string; }

interface CreateFeatRequest { name: string; description?: string; prerequisites?: string; }
interface FeatResponse { id: string; name: string; description?: string; prerequisites?: string; createdAt: string; updatedAt: string; }

interface CreateClassLevelRewardRequest { requiredLevel: number; rewardType: "SKILL" | "SUBCLASS" | "FEAT"; rewardId: string; isChoice: boolean; }
interface ClassLevelRewardResponse { id: string; classId: string; requiredLevel: number; rewardType: string; rewardId: string; rewardName: string; isChoice: boolean; }
```

---

## БЛОК 10 — Рекомендации по HTTP-клиенту

```
Рекомендации по настройке HTTP-клиента для работы с этим бэкендом.

1. BASE_URL = "http://localhost:8080" (или через прокси если dev-сервер на другом порту)

2. Все запросы Content-Type: application/json

3. Interceptor для токена:
   - Если есть сохранённый токен — добавляй заголовок Authorization: Bearer <token>
   - Исключения: POST /api/auth/login и POST /api/auth/register

4. Interceptor для ответов:
   - Проверяй response.data.success
   - При success: false — бросай ошибку с error и message
   - При HTTP 401 — чисти токен, редиректь на /login

5. Типизируй ответы: каждый API-метод возвращает ApiResponse<T> где T — конкретный тип

6. Обработка ошибок валидации:
   - При error === "VALIDATION_ERROR" есть поле fields
   - Маппи fields на соответствующие поля формы

7. Прокси для dev-сервера (обход CORS):
   Для Vite (vite.config.ts):
   server: {
     proxy: {
       '/api': 'http://localhost:8080'
     }
   }

   Для Next.js (next.config.js):
   rewrites: [{ source: '/api/:path*', destination: 'http://localhost:8080/api/:path*' }]

   Для CRA (package.json):
   "proxy": "http://localhost:8080"

8. WebSocket нет — всё через REST. Для "реального времени" используй polling.
```

---

## БЛОК 11 — Сценарии использования (для планирования страниц)

```
Основные пользовательские сценарии для планирования структуры страниц фронтенда:

СЦЕНАРИЙ: Новый игрок (PLAYER)
1. Регистрация → POST /api/auth/register (role: "PLAYER")
2. Логин → POST /api/auth/login → сохранить токен
3. Загрузить справочники для формы:
   - GET /api/admin/character-classes (список классов)
   - GET /api/admin/character-races (список рас)
4. Создать персонажа → POST /api/characters
5. Настроить статы → PUT /api/characters/{id}/stats/{statId} (для каждого стата)
6. Вступить в команду → POST /api/teams/join (ввести invite-код от GM)

СЦЕНАРИЙ: Game Master
1. Регистрация → POST /api/auth/register (role: "GAME_MASTER")
2. Логин
3. Создать команду → POST /api/teams
4. Получить invite-код → GET /api/teams/{id}/invite-code → передать игрокам
5. Ждать пока игроки вступят
6. Смотреть персонажей → GET /api/characters (показывает персонажей участников)
7. Создать артефакт → POST /api/artifacts
8. Выдать артефакт игроку → PUT /api/artifacts/place/{characterId}/{slot}
9. Создать условие → POST /api/conditions + модификаторы
10. Наложить условие → POST /api/conditions/apply/{characterId}

СЦЕНАРИЙ: Настройка Level-Up (GM/ADMIN)
1. Создать навыки → POST /api/admin/skills
2. Создать подклассы → POST /api/admin/subclasses
3. Создать черты → POST /api/admin/feats
4. Назначить награды на уровни → POST /api/admin/classes/{classId}/level-rewards
5. Теперь игроки могут повышать уровень с выбором наград

СЦЕНАРИЙ: Игрок повышает уровень
1. GET /api/characters/{id}/level-up-options — посмотреть доступные опции
2. Выбрать класс и награды в UI
3. POST /api/characters/{id}/level-up — подтвердить
4. GET /api/characters/{id} — обновить данные персонажа

Рекомендуемая структура страниц:
- /login — логин
- /register — регистрация
- /characters — список персонажей (PLAYER)
- /characters/:id — карточка персонажа (статы, инвентарь, условия, награды)
- /characters/:id/level-up — интерфейс повышения уровня
- /teams — список команд (GAME_MASTER)
- /teams/:id — детали команды с участниками
- /artifacts — список артефактов (GAME_MASTER)
- /conditions — список условий (GAME_MASTER)
- /admin — панель справочных данных (ADMIN / GAME_MASTER)
  - /admin/classes, /admin/races, /admin/stats, /admin/items
  - /admin/skills, /admin/subclasses, /admin/feats
  - /admin/classes/:id/rewards — настройка наград за уровни
- /admin/users — список пользователей (ADMIN)
- /homebrew/my — мои homebrew-пакеты (GAME_MASTER)
- /homebrew/my/:id — редактор пакета
- /homebrew/marketplace — маркетплейс (GAME_MASTER)
- /homebrew/marketplace/:id — страница пакета в маркетплейсе
- /homebrew/installed — установленные пакеты
- /admin/homebrew — управление homebrew (ADMIN)
```

---

## БЛОК 12 — Homebrew Marketplace: Концепция и роли

```
В приложение добавлена система Homebrew Marketplace.
"Homebrew" — именованный пакет пользовательского игрового контента,
созданный Game Master-ом и опционально опубликованный в маркетплейсе,
где другие GM могут находить и устанавливать его.

КЛЮЧЕВЫЕ ПРИНЦИПЫ:

1. SHARED REFERENCE, NOT COPY:
   Установка homebrew НЕ копирует данные. Создаётся только строка в homebrew_installations,
   которая ссылается на оригинальный пакет. Все GM, установившие пакет, разделяют одни данные.
   Контент (классы, навыки, предметы, черты) из пакета становится доступен
   в игровом контексте установившего GM.

2. SOFT DELETE ONLY:
   Автор никогда не удаляет пакет физически. Удаление ставит deleted_at.
   GM, которые уже установили пакет, сохраняют доступ.
   При отображении удалённого пакета в UI к title добавляется префикс "[DELETED] "
   (бэкенд уже возвращает title с этим префиксом).

3. ЖИЗНЕННЫЙ ЦИКЛ: DRAFT → PUBLISHED → (опционально) UNPUBLISHED
   Редактировать контент можно только в статусе DRAFT.
   Publish можно повторно из UNPUBLISHED (version инкрементируется).

4. ТИПЫ КОНТЕНТА в пакете:
   ITEM_TYPE, CHARACTER_CLASS, SKILL, FEAT
   Каждый элемент контента — ссылка на существующую запись в справочнике.

ДОСТУП ПО РОЛЯМ:
- GAME_MASTER: создание пакетов, публикация, маркетплейс, установка/удаление
- ADMIN: модерация — просмотр всех пакетов, хард-делит, управление тегами
- PLAYER: НЕ имеет доступа к homebrew
```

---

## БЛОК 13 — Homebrew: Авторство пакетов (GAME_MASTER)

```
Все эндпоинты авторства требуют JWT и роль GAME_MASTER.
Все ответы обёрнуты в стандартный ApiResponse<T>.


СОЗДАТЬ ПАКЕТ (DRAFT):
POST /api/homebrew
Authorization: Bearer <token>

Request:
{
  "title": string,            // обязательно, до 120 символов
  "description": string,      // опционально, до 2000 символов
  "tagNames": ["tag1", "tag2"] // опционально, до 10 тегов
}

Теги автоматически нормализуются: trim, lowercase, пробелы→дефисы.
Паттерн: ^[a-z0-9-]{1,50}$
Несуществующие теги создаются автоматически.

Response (201):
{
  "success": true,
  "data": {
    "id": "uuid",
    "title": "Dark Fantasy Pack",
    "description": "Custom content...",
    "status": "DRAFT",
    "version": 1,
    "downloadCount": 0,
    "authorUsername": "gm_john",
    "tags": ["dark-fantasy", "horror"],
    "contentSummary": {
      "itemTypeCount": 0,
      "classCount": 0,
      "skillCount": 0,
      "featCount": 0
    },
    "contentByType": {},
    "publishedAt": null,
    "createdAt": "2025-06-01T12:00:00Z",
    "isDeleted": false
  },
  "message": "Package created"
}


СПИСОК МОИХ ПАКЕТОВ:
GET /api/homebrew/my?status=DRAFT&page=0&size=20
Authorization: Bearer <token>

Query params:
  status — DRAFT | PUBLISHED | UNPUBLISHED | DELETED (опционально, без фильтра — все)
  page   — номер страницы (с 0)
  size   — размер страницы

Response (200): пагинированный список HomebrewPackageResponse
{
  "success": true,
  "data": {
    "content": [ ...массив HomebrewPackageResponse... ],
    "totalElements": 15,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}

Все статусы включая DELETED видны автору.


ДЕТАЛИ МОЕГО ПАКЕТА:
GET /api/homebrew/my/{id}
Authorization: Bearer <token>

Response (200): HomebrewDetailResponse (включает contentByType)


ОБНОВИТЬ ПАКЕТ (только DRAFT):
PUT /api/homebrew/my/{id}
Authorization: Bearer <token>

Request:
{
  "title": string,           // опционально
  "description": string,     // опционально
  "tagNames": ["new-tag"]    // опционально, заменяет все теги
}

Ошибки:
- 409 если статус != DRAFT


ДОБАВИТЬ КОНТЕНТ В ПАКЕТ (только DRAFT):
POST /api/homebrew/my/{id}/content
Authorization: Bearer <token>

Request:
{
  "contentType": "SKILL",    // ITEM_TYPE | CHARACTER_CLASS | SKILL | FEAT
  "contentId": "uuid"        // ID существующей записи в справочнике
}

Валидация бэкенда:
- Пакет должен быть в DRAFT
- contentType должен быть известным типом
- contentId должен существовать в соответствующей таблице
- Контент должен принадлежать текущему GM (owner_id = caller) ИЛИ быть глобальным (owner_id = null)
- Такая пара (contentType, contentId) не должна уже быть в пакете

Ошибки:
- 409 если статус != DRAFT, дублирование, неизвестный тип
- 403 если контент не принадлежит текущему GM
- 404 если contentId не найден

Response (201): обновлённый HomebrewDetailResponse


УДАЛИТЬ КОНТЕНТ ИЗ ПАКЕТА (только DRAFT):
DELETE /api/homebrew/my/{id}/content/{contentItemId}
Authorization: Bearer <token>

contentItemId — это UUID конкретного элемента контента В ПАКЕТЕ
(НЕ contentId из справочника, а id из homebrew_content_items).
Этот id можно взять из contentByType в HomebrewDetailResponse.

Ошибки:
- 409 если статус != DRAFT
- 404 если contentItemId не найден


ОПУБЛИКОВАТЬ ПАКЕТ:
POST /api/homebrew/my/{id}/publish
Authorization: Bearer <token>

Допустимые переходы: DRAFT → PUBLISHED, UNPUBLISHED → PUBLISHED.
Инкрементирует version. При первой публикации ставит publishedAt.

Валидация:
- Минимум 1 элемент контента
- Непустой title

Ошибки:
- 409 если текущий статус не DRAFT и не UNPUBLISHED
- 422 если нет контента или пустой title


СНЯТЬ С ПУБЛИКАЦИИ:
POST /api/homebrew/my/{id}/unpublish
Authorization: Bearer <token>

Переход: PUBLISHED → UNPUBLISHED.
Скрывает из маркетплейса. Установки не затрагиваются.

Ошибки:
- 409 если статус != PUBLISHED


УДАЛИТЬ ПАКЕТ (soft delete):
DELETE /api/homebrew/my/{id}
Authorization: Bearer <token>

Работает для любого статуса (DRAFT, PUBLISHED, UNPUBLISHED).
Ставит deleted_at, скрывает из маркетплейса. Установки сохраняются.

Response (200):
{
  "success": true,
  "data": {
    "message": "Package deleted successfully",
    "installationCount": 5
  }
}

installationCount — сколько GM установили этот пакет.
```

---

## БЛОК 14 — Homebrew: Маркетплейс (GAME_MASTER)

```
Маркетплейс — витрина опубликованных homebrew-пакетов.
Все эндпоинты требуют JWT и роль GAME_MASTER.


ОБЗОР МАРКЕТПЛЕЙСА:
GET /api/homebrew/marketplace?search=pirates&tags=dark-fantasy,pirates&sort=downloads&page=0&size=20
Authorization: Bearer <token>

Query params:
  search — поиск по title И description (ILIKE, регистронезависимый)
  tags   — фильтр по тегам через запятую; пакет должен иметь ВСЕ перечисленные теги (AND логика)
  sort   — downloads | newest | oldest (по умолчанию newest)
  page   — номер страницы (с 0)
  size   — размер страницы (по умолчанию 20)

Все параметры опциональны. Без параметров — все опубликованные, отсортированы по дате (newest).
Маркетплейс НИКОГДА не показывает удалённые и неопубликованные пакеты.

Response (200):
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "title": "Dark Fantasy Pack",
        "description": "...",
        "status": "PUBLISHED",
        "version": 3,
        "downloadCount": 42,
        "authorUsername": "gm_john",
        "tags": ["dark-fantasy", "horror"],
        "contentSummary": {
          "itemTypeCount": 5,
          "classCount": 2,
          "skillCount": 8,
          "featCount": 3
        },
        "publishedAt": "2025-05-15T10:00:00Z",
        "createdAt": "2025-05-01T08:00:00Z",
        "isDeleted": false
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}


ДЕТАЛИ ПАКЕТА В МАРКЕТПЛЕЙСЕ:
GET /api/homebrew/marketplace/{id}
Authorization: Bearer <token>

Только для PUBLISHED и не-deleted пакетов.
Ошибки: 404 если пакет удалён или не опубликован.

Response (200): HomebrewDetailResponse с contentByType:
{
  "success": true,
  "data": {
    "id": "uuid",
    "title": "Dark Fantasy Pack",
    "description": "...",
    "status": "PUBLISHED",
    "version": 3,
    "downloadCount": 42,
    "authorUsername": "gm_john",
    "tags": ["dark-fantasy", "horror"],
    "contentSummary": {
      "itemTypeCount": 5,
      "classCount": 2,
      "skillCount": 8,
      "featCount": 3
    },
    "contentByType": {
      "SKILL": [
        {
          "id": "uuid",
          "name": "Dark Strike",
          "description": "Deal necrotic damage...",
          "skillType": "COMBAT"
        }
      ],
      "FEAT": [
        {
          "id": "uuid",
          "name": "Shadow Resilience",
          "description": "Resist shadow damage",
          "prerequisites": "Level 5+"
        }
      ],
      "ITEM_TYPE": [
        {
          "id": "uuid",
          "name": "Shadow Blade",
          "description": "A blade forged in darkness",
          "slot": "MAIN_HAND"
        }
      ],
      "CHARACTER_CLASS": [
        {
          "id": "uuid",
          "name": "Shadow Knight",
          "description": "A warrior of darkness"
        }
      ]
    },
    "publishedAt": "2025-05-15T10:00:00Z",
    "createdAt": "2025-05-01T08:00:00Z",
    "isDeleted": false
  }
}

Поля ContentSummaryDto зависят от типа контента:
- ITEM_TYPE: id, name, description, slot
- CHARACTER_CLASS: id, name, description
- SKILL: id, name, description, skillType
- FEAT: id, name, description, prerequisites
Поля, которые не применимы к типу — отсутствуют (NON_NULL).


УСТАНОВИТЬ ПАКЕТ:
POST /api/homebrew/marketplace/{id}/install
Authorization: Bearer <token>

Без тела запроса.

Валидация:
- Пакет должен быть PUBLISHED и не удалён
- GM не должен уже иметь установку этого пакета

Ошибки:
- 404 если пакет не опубликован или удалён
- 409 если уже установлен

Response (201):
{
  "success": true,
  "data": {
    "installedAt": "2025-06-01T14:00:00Z",
    "sourceVersion": 3,
    "contentCount": 18
  },
  "message": "Package installed"
}

contentCount — общее количество элементов контента в пакете.
При установке download_count пакета увеличивается на 1.
```

---

## БЛОК 15 — Homebrew: Установленные пакеты (GAME_MASTER)

```
СПИСОК УСТАНОВЛЕННЫХ:
GET /api/homebrew/installed?page=0&size=20
Authorization: Bearer <token>

Показывает ВСЕ установленные пакеты, включая удалённые автором.
Удалённые пакеты приходят с title = "[DELETED] Original Title".

Response (200):
{
  "success": true,
  "data": {
    "content": [
      {
        "installationId": "uuid",
        "packageId": "uuid",
        "title": "Dark Fantasy Pack",
        "authorUsername": "gm_john",
        "isDeleted": false,
        "installedAt": "2025-06-01T14:00:00Z",
        "sourceVersion": 3,
        "contentSummary": {
          "itemTypeCount": 5,
          "classCount": 2,
          "skillCount": 8,
          "featCount": 3
        }
      },
      {
        "installationId": "uuid",
        "packageId": "uuid",
        "title": "[DELETED] Old Pack",
        "authorUsername": "gm_jane",
        "isDeleted": true,
        "installedAt": "2025-04-10T09:00:00Z",
        "sourceVersion": 1,
        "contentSummary": {
          "itemTypeCount": 2,
          "classCount": 0,
          "skillCount": 3,
          "featCount": 1
        }
      }
    ],
    "totalElements": 2,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}

UI рекомендации:
- Для isDeleted: true — показывай визуальный индикатор (серый фон, иконка, бейдж)
- Кнопка "Uninstall" доступна для всех, включая удалённые
- sourceVersion позволяет показать: "Installed at version 3"


УДАЛИТЬ УСТАНОВКУ (Uninstall):
DELETE /api/homebrew/installed/{installationId}
Authorization: Bearer <token>

installationId — UUID из поля installationId в InstalledHomebrewResponse.
НЕ packageId!

Response (200):
{
  "success": true,
  "message": "Package uninstalled"
}

Деинсталляция удаляет только строку установки.
Контент в справочниках остаётся (глобальный контент доступен всем).
```

---

## БЛОК 16 — Homebrew: Админ-модерация (ADMIN)

```
Эндпоинты модерации доступны только для роли ADMIN.


СПИСОК ВСЕХ ПАКЕТОВ:
GET /api/admin/homebrew?status=PUBLISHED&authorId=uuid&page=0&size=20
Authorization: Bearer <token>

Query params:
  status   — DRAFT | PUBLISHED | UNPUBLISHED (опционально)
  authorId — UUID автора (опционально)
  page, size — пагинация

Показывает ВСЕ пакеты всех авторов, всех статусов.

Response (200): Page<HomebrewPackageResponse>


ХАРД-ДЕЛИТ ПАКЕТА (модерация):
DELETE /api/admin/homebrew/{id}
Authorization: Bearer <token>

Физически удаляет пакет, его контент-элементы и теги-связи.
Установки других GM также удаляются.

Response (200):
{
  "success": true,
  "data": {
    "deletedPackageId": "uuid",
    "affectedInstallations": 12
  }
}


СПИСОК ТЕГОВ С КОЛИЧЕСТВОМ ИСПОЛЬЗОВАНИЙ:
GET /api/admin/homebrew/tags
Authorization: Bearer <token>

Response (200):
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "dark-fantasy",
      "usageCount": 15
    },
    {
      "id": "uuid",
      "name": "horror",
      "usageCount": 3
    }
  ]
}


УДАЛИТЬ ТЕГ:
DELETE /api/admin/homebrew/tags/{id}
Authorization: Bearer <token>

Ошибки:
- 409 если тег используется хотя бы одним пакетом
- 404 если тег не найден

Response (200):
{
  "success": true,
  "message": "Tag deleted"
}
```

---

## БЛОК 17 — Homebrew: TypeScript типы

```
TypeScript типы для Homebrew Marketplace API.
Добавь их к существующим типам проекта.

// === Homebrew Types ===

type HomebrewStatus = "DRAFT" | "PUBLISHED" | "UNPUBLISHED";

type ContentType = "ITEM_TYPE" | "CHARACTER_CLASS" | "SKILL" | "FEAT";

// --- Requests ---

interface CreateHomebrewRequest {
  title: string;            // обязательно, max 120
  description?: string;     // max 2000
  tagNames?: string[];      // max 10 тегов, каждый: lowercase, [a-z0-9-], max 50
}

interface UpdateHomebrewRequest {
  title?: string;           // max 120
  description?: string;     // max 2000
  tagNames?: string[];      // max 10, если передан — заменяет все теги
}

interface AddContentRequest {
  contentType: ContentType; // "ITEM_TYPE" | "CHARACTER_CLASS" | "SKILL" | "FEAT"
  contentId: string;        // UUID существующей записи в справочнике
}

// --- Responses ---

interface HomebrewContentSummary {
  itemTypeCount: number;
  classCount: number;
  skillCount: number;
  featCount: number;
}

interface ContentSummaryDto {
  id: string;               // UUID
  name: string;
  description?: string;
  slot?: string;            // только для ITEM_TYPE
  skillType?: string;       // только для SKILL
  prerequisites?: string;   // только для FEAT
}

interface HomebrewPackageResponse {
  id: string;
  title: string;            // "[DELETED] " prefix если удалён
  description?: string;
  status: HomebrewStatus;
  version: number;
  downloadCount: number;
  authorUsername: string;
  tags: string[];
  contentSummary: HomebrewContentSummary;
  publishedAt?: string;     // ISO-8601
  createdAt: string;
  isDeleted: boolean;
}

interface HomebrewDetailResponse extends HomebrewPackageResponse {
  contentByType: Record<ContentType, ContentSummaryDto[]>;
}

interface InstalledHomebrewResponse {
  installationId: string;   // UUID — использовать для uninstall
  packageId: string;
  title: string;            // "[DELETED] " prefix если удалён
  authorUsername: string;
  isDeleted: boolean;
  installedAt: string;      // ISO-8601
  sourceVersion: number;
  contentSummary: HomebrewContentSummary;
}

interface InstallResponse {
  installedAt: string;
  sourceVersion: number;
  contentCount: number;
}

interface SoftDeleteResponse {
  message: string;
  installationCount: number;
}

interface HardDeleteResponse {
  deletedPackageId: string;
  affectedInstallations: number;
}

interface HomebrewTagResponse {
  id: string;
  name: string;
  usageCount: number;
}

// --- Paginated response wrapper ---
// Все списочные эндпоинты homebrew возвращают Spring Page:
interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;           // текущая страница (с 0)
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
// Пример: ApiResponse<Page<HomebrewPackageResponse>>
```

---

## БЛОК 18 — Homebrew: Пользовательские сценарии

```
СЦЕНАРИЙ: GM создаёт и публикует homebrew-пакет

1. Создать контент в справочниках (если ещё нет):
   - POST /api/admin/skills        → получить skillId
   - POST /api/admin/feats         → получить featId
   - POST /api/admin/item-types    → получить itemTypeId
   - POST /api/admin/character-classes → получить classId

2. Создать DRAFT пакет:
   POST /api/homebrew
   { title: "My Pack", description: "...", tagNames: ["dark-fantasy"] }
   → получить packageId

3. Наполнить контентом (повторять для каждого элемента):
   POST /api/homebrew/my/{packageId}/content
   { contentType: "SKILL", contentId: "uuid" }

4. Просмотреть что получилось:
   GET /api/homebrew/my/{packageId}
   → contentByType показывает весь контент по типам

5. Опубликовать:
   POST /api/homebrew/my/{packageId}/publish
   → статус меняется на PUBLISHED, version инкрементируется

6. (Опционально) Снять с публикации и доработать:
   POST /api/homebrew/my/{packageId}/unpublish → UNPUBLISHED
   — Чтобы снова редактировать контент, нужно вернуть в DRAFT?
   — Нет! Редактирование title/description/tags доступно только в DRAFT.
   — Добавление/удаление контента — тоже только DRAFT.
   — Для повторной правки: создай новый пакет или удали (soft) и создай заново.


СЦЕНАРИЙ: GM находит и устанавливает пакет из маркетплейса

1. Открыть маркетплейс:
   GET /api/homebrew/marketplace?sort=downloads
   → список популярных пакетов

2. Поиск по тегам/тексту:
   GET /api/homebrew/marketplace?search=pirates&tags=adventure

3. Посмотреть детали:
   GET /api/homebrew/marketplace/{id}
   → contentByType показывает что внутри

4. Установить:
   POST /api/homebrew/marketplace/{id}/install
   → контент пакета теперь доступен в играх этого GM

5. Посмотреть установленные:
   GET /api/homebrew/installed


СЦЕНАРИЙ: GM удаляет установленный пакет

1. GET /api/homebrew/installed → найти installationId
2. DELETE /api/homebrew/installed/{installationId}
   Глобальный контент (owner_id = null) остаётся доступен.


СЦЕНАРИЙ: Автор удаляет свой пакет

1. DELETE /api/homebrew/my/{id}
   → Soft delete. Пакет скрыт из маркетплейса.
   → GM, которые установили пакет, сохраняют доступ.
   → В их списке установленных title = "[DELETED] Original Title".


СЦЕНАРИЙ: Админ модерирует маркетплейс

1. GET /api/admin/homebrew → все пакеты всех авторов
2. DELETE /api/admin/homebrew/{id} → хард-делит (физическое удаление)
3. GET /api/admin/homebrew/tags → список тегов с количеством использований
4. DELETE /api/admin/homebrew/tags/{id} → удалить неиспользуемый тег


РЕКОМЕНДАЦИИ ПО UI ДЛЯ HOMEBREW:

Структура страниц:
- /homebrew/my — таблица/список пакетов GM с фильтром по статусу, кнопка "Create"
- /homebrew/my/:id — детальная страница: метаданные + список контента по типам
  - В DRAFT: кнопки "Add Content", "Remove", "Publish"
  - В PUBLISHED: кнопки "Unpublish", "Delete"
  - В UNPUBLISHED: кнопки "Publish", "Delete"
- /homebrew/marketplace — карточки пакетов, поиск, фильтры по тегам, сортировка
- /homebrew/marketplace/:id — страница пакета: описание + контент + кнопка "Install"
- /homebrew/installed — список установленных с кнопкой "Uninstall"

Визуальные индикаторы:
- Статус пакета: DRAFT (серый), PUBLISHED (зелёный), UNPUBLISHED (жёлтый)
- Удалённый пакет: красный бейдж "[DELETED]", серый фон
- downloadCount: иконка скачивания + число
- Теги: chip/badge компоненты, кликабельные для фильтрации
- contentSummary: 4 иконки с числами (меч=items, щит=classes, звезда=skills, свиток=feats)

Форма добавления контента в пакет:
- Dropdown для contentType (4 варианта)
- При выборе типа — загружать список доступных записей из справочника
  (GET /api/admin/item-types, GET /api/admin/character-classes и т.д.)
- Показывать только записи, которые ещё не добавлены в пакет
- Кнопка "Add" → POST /api/homebrew/my/{id}/content

Маркетплейс:
- Строка поиска (search)
- Фильтр тегов: мульти-селект (AND логика)
- Сортировка: "Newest" | "Oldest" | "Most Downloaded"
- Пагинация
- На карточке: title, description (truncated), tags, downloadCount, authorUsername
```

---

## БЛОК 19 — Баффы/дебаффы (справочные данные, только ADMIN)

```
Система баффов и дебаффов — справочная таблица эффектов.
Умения и зачарования ссылаются на записи из этой таблицы.

CRUD ЭНДПОИНТЫ (только ADMIN):

GET    /api/admin/buffs-debuffs
  Query params: ?is_buff=true|false, ?effect_type=STAT_MODIFIER
  Response: массив BuffDebuffResponse

POST   /api/admin/buffs-debuffs
  Body:
  {
    "name": string,           // обязательно, до 100 символов, уникальное
    "description": string,    // опционально
    "effectType": string,     // обязательно: STAT_MODIFIER | CONDITION | DAMAGE_OVER_TIME
                              //   | HEAL_OVER_TIME | IMMUNITY | VULNERABILITY
    "targetStatId": "uuid",   // обязательно при effectType = STAT_MODIFIER
    "modifierValue": integer, // опционально, +2 или -3
    "durationRounds": integer,// опционально, null = перманентный
    "isBuff": boolean         // обязательно: true = бафф, false = дебафф
  }

GET    /api/admin/buffs-debuffs/{id}
PUT    /api/admin/buffs-debuffs/{id}
DELETE /api/admin/buffs-debuffs/{id}
  Возвращает 409 если используется в эффектах умений или типах зачарований.

Response: BuffDebuffResponse
{
  "id": "uuid",
  "name": "Blessed",
  "description": "Божественное благословение",
  "effectType": "STAT_MODIFIER",
  "targetStatId": "uuid",
  "targetStatName": "STR",
  "modifierValue": 2,
  "durationRounds": null,
  "isBuff": true,
  "createdAt": "..."
}

Предустановленные записи:
- Blessed (STAT_MODIFIER, STR +2, бафф)
- Hasted (CONDITION, бафф, 10 раундов)
- Poisoned (CONDITION, дебафф, 5 раундов)
- Weakened (STAT_MODIFIER, STR -3, дебафф)
- Burning (DAMAGE_OVER_TIME, дебафф, 3 раунда)
- Regenerating (HEAL_OVER_TIME, бафф, 5 раундов)
- Stunned (CONDITION, дебафф, 2 раунда)
- Frightened (CONDITION, дебафф, 4 раунда)
```

---

## БЛОК 20 — Урон, умения и эффекты умений

```
Типы предметов и умения теперь поддерживают урон и привязку к умениям/эффектам.

ОБНОВЛЁННЫЕ ТИПЫ ПРЕДМЕТОВ:
POST/PUT /api/admin/item-types теперь принимают дополнительные поля:
{
  "name": string,
  "description": string,
  "slot": string,
  "damageDice": string,       // опционально, формат: "2d6", "1d8", "d4"
  "damageBonus": integer,     // по умолчанию 0
  "damageType": string,       // обязательно если есть damageDice
                              // SLASHING | PIERCING | BLUDGEONING | FIRE | COLD
                              // | LIGHTNING | POISON | NECROTIC | RADIANT
                              // | PSYCHIC | FORCE | THUNDER | ACID
  "skillId": "uuid",          // опционально, ссылка на умение
  "skillActivation": string   // обязательно если есть skillId: PASSIVE | ACTIVE
}

Response ItemTypeResponse теперь включает:
  damageDice, damageBonus, damageType, skillId, skillName, skillActivation


ОБНОВЛЁННЫЕ УМЕНИЯ:
POST/PUT /api/admin/skills теперь принимают:
{
  "name": string,
  "description": string,
  "skillType": string,
  "damageDice": string,       // опционально
  "damageBonus": integer,     // по умолчанию 0
  "damageType": string        // обязательно если есть damageDice
}

Response SkillResponse теперь включает:
  damageDice, damageBonus, damageType,
  effects: SkillEffectResponse[]


ЭФФЕКТЫ УМЕНИЙ (подресурс):
GET /api/admin/skills/{id}/effects
  Response: массив SkillEffectResponse

PUT /api/admin/skills/{id}/effects
  Полная замена всех эффектов (идемпотентная).
  Body:
  {
    "effects": [
      {
        "buffDebuffId": "uuid",
        "effectRole": "BUFF" | "DEBUFF",
        "chancePercent": integer  // 1-100
      }
    ]
  }
  Пустой массив = удалить все эффекты.
  effectRole должен совпадать с isBuff записи:
    BUFF → isBuff=true, DEBUFF → isBuff=false

Response: SkillEffectResponse
{
  "id": "uuid",
  "buffDebuff": { ...BuffDebuffResponse... },
  "effectRole": "DEBUFF",
  "chancePercent": 75
}
```

---

## БЛОК 21 — Зачарования (Enchantments)

```
Зачарования — эффекты, накладываемые игроком на экипированные предметы.

СПРАВОЧНИК ТИПОВ ЗАЧАРОВАНИЙ:

GET /api/enchantment-types
  Доступ: все авторизованные пользователи (PLAYER, GAME_MASTER, ADMIN).
  Каталог доступных зачарований для выбора.

ADMIN CRUD:
GET    /api/admin/enchantment-types
POST   /api/admin/enchantment-types
  Body:
  {
    "name": string,           // обязательно, уникальное
    "description": string,
    "damageDice": string,     // "1d4", "1d6" и т.д.
    "damageBonus": integer,   // по умолчанию 0
    "damageType": string,     // обязательно если есть damageDice
    "buffDebuffId": "uuid"    // опционально
  }
GET    /api/admin/enchantment-types/{id}
PUT    /api/admin/enchantment-types/{id}
DELETE /api/admin/enchantment-types/{id}
  Возвращает 409 если зачарование применено к инвентарю.

Response: EnchantmentTypeResponse
{
  "id": "uuid",
  "name": "Flaming",
  "description": "Огненное зачарование",
  "damageDice": "1d4",
  "damageBonus": 0,
  "damageType": "FIRE",
  "buffDebuff": { ...BuffDebuffResponse или null... }
}

Предустановленные зачарования:
- Flaming (1d4 FIRE, связан с Burning)
- Frost Touch (1d6 COLD, без баффа)
- Holy (1d8 RADIANT, связан с Blessed)
- Venomous (1d4 POISON, связан с Poisoned)
- Thunderous (1d6 THUNDER, связан с Stunned)


ЗАЧАРОВАНИЯ ИНВЕНТАРЯ (PLAYER):

GET /api/characters/{characterId}/inventory/{slotId}/enchantments
  Доступ: PLAYER (свой), GAME_MASTER (участник команды), ADMIN (любой).

POST /api/characters/{characterId}/inventory/{slotId}/enchantments
  Доступ: только PLAYER (свой персонаж) или ADMIN.
  Body:
  {
    "enchantmentTypeId": "uuid",
    "notes": string          // опционально, до 255 символов
  }
  Ошибки:
  - 409 если слот пустой (нет предмета)
  - 409 если зачарование уже наложено на этот слот

DELETE /api/characters/{characterId}/inventory/{slotId}/enchantments/{enchantmentId}
  Доступ: только PLAYER (свой) или ADMIN.

Response: EnchantmentResponse
{
  "id": "uuid",
  "enchantmentType": { ...EnchantmentTypeResponse... },
  "appliedAt": "2026-05-29T12:00:00Z",
  "notes": "Наложено после боя с драконом"
}

slotId — это UUID слота инвентаря (поле id из InventorySlotResponse).
enchantmentId — это UUID конкретного зачарования на слоте.


TypeScript типы:

interface BuffDebuffResponse {
  id: string;
  name: string;
  description?: string;
  effectType: string;
  targetStatId?: string;
  targetStatName?: string;
  modifierValue?: number;
  durationRounds?: number;
  isBuff: boolean;
  createdAt: string;
}

interface EnchantmentTypeResponse {
  id: string;
  name: string;
  description?: string;
  damageDice?: string;
  damageBonus: number;
  damageType?: string;
  buffDebuff?: BuffDebuffResponse;
}

interface EnchantmentResponse {
  id: string;
  enchantmentType: EnchantmentTypeResponse;
  appliedAt: string;
  notes?: string;
}

interface SkillEffectResponse {
  id: string;
  buffDebuff: BuffDebuffResponse;
  effectRole: "BUFF" | "DEBUFF";
  chancePercent: number;
}

// Updated existing types:
interface ItemTypeResponse {
  id: string;
  name: string;
  description?: string;
  slot: string;
  damageDice?: string;
  damageBonus?: number;
  damageType?: string;
  skillId?: string;
  skillName?: string;
  skillActivation?: "PASSIVE" | "ACTIVE";
}

interface SkillResponse {
  id: string;
  name: string;
  description?: string;
  skillType?: string;
  damageDice?: string;
  damageBonus?: number;
  damageType?: string;
  effects?: SkillEffectResponse[];
  createdAt: string;
  updatedAt: string;
}

type DamageType = "SLASHING" | "PIERCING" | "BLUDGEONING" | "FIRE" | "COLD" | "LIGHTNING" | "POISON" | "NECROTIC" | "RADIANT" | "PSYCHIC" | "FORCE" | "THUNDER" | "ACID";
```
