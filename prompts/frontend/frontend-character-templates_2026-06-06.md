# Frontend Prompt: Character Templates (Vanilla Characters Outside Campaigns)

Нужно реализовать возможность создавать персонажей вне кампании ("шаблоны"), а при присоединении к кампании — клонировать шаблон в кампанию.

## Суть фичи

1. Игрок создаёт "ванильного" персонажа вне кампании — без homebrew-элементов.
2. Персонаж сохраняется как шаблон (template): `campaignId = null`.
3. Когда игрок присоединяется к кампании, он видит список своих шаблонов.
4. Нажимает **"Использовать шаблон"** → создаётся копия персонажа, привязанная к кампании.
5. Оригинал остаётся непривязанным, его можно использовать повторно в других кампаниях.

## Backend Endpoints

### Character Templates (вне кампании)

- `POST /api/characters/full` — создать шаблон через полный wizard
- `GET /api/characters/my` — список шаблонов текущего пользователя
- `GET /api/characters/{characterId}` — получить шаблон по ID
- `DELETE /api/characters/{characterId}` — удалить шаблон

### Клонирование в кампанию

- `POST /api/campaigns/{campaignId}/characters/from-template/{templateId}` — клонировать шаблон в кампанию

### Существующие эндпоинты (без изменений)

- `POST /api/campaigns/{campaignId}/characters/full` — создать персонажа напрямую в кампании (как было)
- `GET /api/campaigns/{campaignId}/characters` — список персонажей кампании
- Все остальные campaign character endpoints работают как раньше

## Request/Response

### POST /api/characters/full

Request body — тот же `CreateFullCharacterRequest`, что используется для `/api/campaigns/{campaignId}/characters/full`:

```json
{
  "name": "Эльдрин Светлый",
  "alignment": "Lawful Good",
  "avatar": "https://example.com/avatar.png",
  "raceId": "uuid-расы",
  "subraceId": "uuid-подрасы-или-null",
  "classId": "uuid-класса",
  "level": 1,
  "abilityScores": [
    { "statId": "uuid-strength", "baseValue": 15 },
    { "statId": "uuid-dexterity", "baseValue": 14 },
    { "statId": "uuid-constitution", "baseValue": 13 },
    { "statId": "uuid-intelligence", "baseValue": 12 },
    { "statId": "uuid-wisdom", "baseValue": 10 },
    { "statId": "uuid-charisma", "baseValue": 8 }
  ],
  "scoreMethod": "STANDARD_ARRAY",
  "backgroundId": "uuid-предыстории",
  "chosenSkillProficiencyIds": ["uuid-skill-1", "uuid-skill-2"],
  "cantripIds": [],
  "spellIds": [],
  "biography": {
    "personalityTraits": "Любопытен и дружелюбен",
    "ideals": "Знание — сила",
    "bonds": "Мой учитель пропал",
    "flaws": "Слишком доверчив"
  },
  "startingCoins": [
    { "currencyTypeId": "uuid-gold", "amount": 50 }
  ]
}
```

**Ограничения создания шаблона:** все элементы должны быть vanilla (не homebrew). Бэкенд отклонит запрос, если:
- Класс имеет `homebrew_id != null`
- Раса имеет `homebrew_id != null`
- Предыстория имеет `homebrew_id != null`
- Заклинание имеет `homebrew_id != null`

### CharacterResponse (обновлён)

В ответ добавлены два новых поля:

```json
{
  "id": "uuid",
  "name": "Эльдрин Светлый",
  "campaignId": null,
  "status": "ACTIVE",
  "totalLevel": 1,
  "classLevels": [...],
  "race": {...},
  "stats": [...],
  ...
}
```

- `campaignId: null` → персонаж-шаблон
- `campaignId: "uuid"` → персонаж привязан к кампании
- `status`: `ACTIVE`, `RESERVE`, `RETIRED`, `DECEASED`

### POST /api/campaigns/{campaignId}/characters/from-template/{templateId}

Request body: пустой (всё берётся из шаблона).

Response: `ApiResponse<CharacterResponse>` — новый персонаж с `campaignId` = id кампании.

Копируется всё: характеристики, навыки, заклинания, предыстория, биография, кошелёк, ресурсы. HP сбрасывается до максимума, death saves — в 0.

## Данные для справочников (vanilla контент)

Для формы создания шаблона (вне кампании) используй **vanilla reference endpoints** — это отдельный набор эндпоинтов без `campaignId` в пути. Они возвращают только системный (vanilla) 5e-контент, без homebrew. Фильтровать на стороне FE не нужно.

- `GET /api/reference/stat-types` — характеристики (Strength, Dexterity, ...) → `List<StatTypeResponse>`
- `GET /api/reference/classes` — классы → `List<CharacterClassDetailResponse>`
- `GET /api/reference/races` — расы с сабрейсами → `List<CharacterRaceDetailResponse>`
- `GET /api/reference/backgrounds` — предыстории → `List<BackgroundResponse>`
- `GET /api/reference/skills` — 18 навыков (Athletics, Stealth, ...) → `List<ProficiencySkillResponse>`
- `GET /api/reference/currencies` — типы валют → `List<CurrencyTypeResponse>`
- `GET /api/reference/spells` — заклинания. Поддерживает query-параметры `classId` (UUID), `level` (int), `school` (string) → `List<SpellResponse>`

Все ответы обёрнуты в `ApiResponse<T>` (как и остальной API). Требуется аутентификация (JWT), как для любого `/api/**` кроме `/api/auth/*`.

> **Важно:** не используй `/api/campaigns/{campaignId}/reference/...` для формы шаблона — эти эндпоинты требуют членства в кампании и вернут 403/404, а шаблоны по дизайну создаются вне кампаний. Для wizard'а персонажа **внутри** кампании по-прежнему дёргай campaign-scoped версию.

> **Не используй** пути `/api/reference/proficiency-skills`, `/api/reference/currency-types`, `/api/reference/available/*` — таких эндпоинтов нет, бэкенд вернёт 500 (Spring трактует их как несуществующий статический ресурс).

## UI/UX Flow

### Страница "Мои шаблоны"

Доступна из главного меню (или профиля). Показывает:

```
┌─────────────────────────────────────────────────────┐
│  Мои шаблоны персонажей                             │
│                                                     │
│  [+ Создать шаблон]                                 │
│                                                     │
│  ┌─────────────────────────────────────────────────┐ │
│  │ 🗡️ Эльдрин Светлый                             │ │
│  │ Воин 3 уровня • Эльф • Lawful Good             │ │
│  │ HP: 28 • AC: 16                                  │ │
│  │                                    [Удалить]     │ │
│  └─────────────────────────────────────────────────┘ │
│                                                     │
│  ┌─────────────────────────────────────────────────┐ │
│  │ 🔮 Мирабелла                                    │ │
│  │ Волшебник 1 уровня • Человек • Neutral          │ │
│  │ HP: 8 • AC: 12                                   │ │
│  │                                    [Удалить]     │ │
│  └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

Карточка шаблона — кликабельна, открывает детальный просмотр (GET `/api/characters/{id}`).

### Wizard создания шаблона

Тот же wizard, что для создания персонажа в кампании, но:

1. **Нет выбора кампании** — шаблон создаётся без привязки
2. **Только vanilla контент** — в выпадающих списках классов, рас, предысторий, заклинаний показываем только контент без `homebrewId`
3. Endpoint: `POST /api/characters/full` (без `campaignId` в URL)

### Выбор шаблона при входе в кампанию

После успешного `POST /api/campaigns/join` (join by invite code) или при переходе в кампанию, в которой у игрока ещё нет персонажа, показать:

```
┌─────────────────────────────────────────────────────┐
│  Добро пожаловать в кампанию "Драконья Погибель"!   │
│                                                     │
│  Выберите способ создания персонажа:                │
│                                                     │
│  ┌───────────────────────┐  ┌─────────────────────┐ │
│  │  📋 Из шаблона        │  │  ✨ Создать нового   │ │
│  │                       │  │                     │ │
│  │  Выберите один из     │  │  Создать персонажа  │ │
│  │  ваших готовых        │  │  с нуля для этой    │ │
│  │  персонажей           │  │  кампании           │ │
│  └───────────────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

**"Из шаблона"** → показать список шаблонов (GET `/api/characters/my`):

```
┌─────────────────────────────────────────────────────┐
│  Выберите шаблон                          [Назад]   │
│                                                     │
│  ┌─────────────────────────────────────────────────┐ │
│  │ Эльдрин Светлый • Воин 3 • Эльф               │ │
│  │                    [Использовать шаблон]         │ │
│  └─────────────────────────────────────────────────┘ │
│                                                     │
│  ┌─────────────────────────────────────────────────┐ │
│  │ Мирабелла • Волшебник 1 • Человек              │ │
│  │                    [Использовать шаблон]         │ │
│  └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

Нажатие **"Использовать шаблон"** → подтверждение:

```
┌────────────────────────────────────────┐
│  Использовать "Эльдрин Светлый"?      │
│                                        │
│  Будет создана копия персонажа для     │
│  этой кампании. Оригинальный шаблон    │
│  останется для использования в         │
│  других кампаниях.                     │
│                                        │
│  [Отмена]          [Подтвердить]       │
└────────────────────────────────────────┘
```

→ `POST /api/campaigns/{campaignId}/characters/from-template/{templateId}`

**"Создать нового"** → стандартный wizard (`POST /api/campaigns/{campaignId}/characters/full`), где доступен и homebrew-контент кампании.

### Определение: есть ли у игрока персонаж в кампании

Используй `GET /api/campaigns/{campaignId}/characters` и фильтруй по `ownerId === currentUserId`. Если список пуст — показывай экран выбора шаблона / создания.

## Роутинг

```
/characters/templates         → Список шаблонов (GET /api/characters/my)
/characters/templates/new     → Wizard создания шаблона (POST /api/characters/full)
/characters/templates/:id     → Просмотр шаблона (GET /api/characters/:id)

/campaigns/:id/select-character → Экран выбора (шаблон или новый)
/campaigns/:id/characters/new   → Wizard в кампании (как было)
```

## Важные моменты

1. **Не показывать homebrew в wizard шаблона.** Шаблоны строго ванильные. Фильтруй справочники.
2. **Шаблон можно использовать многократно.** Оригинал НЕ удаляется и НЕ привязывается после клонирования.
3. **Экран выбора — не обязательный.** Игрок может создать персонажа в кампании напрямую, как раньше.
4. **Поле `campaignId` в ответе** — используй для визуального разделения: `null` = шаблон, `uuid` = кампанийный.
5. **Клонирование не требует request body** — всё копируется из шаблона автоматически.
6. **HP при клонировании** сбрасывается до `maxHp`, death saves — в 0. Остальное копируется as-is.

## Error Handling

| Код | Когда | Сообщение |
|-----|-------|-----------|
| 400 | Homebrew контент в шаблоне | "Homebrew classes/races/spells cannot be used in vanilla characters" |
| 400 | Шаблон уже привязан | "Source character is already bound to a campaign" |
| 403 | Чужой шаблон | "You can only clone/view/delete your own templates" |
| 403 | Не участник кампании | "You are not a member of this campaign" |
| 404 | Шаблон не найден | "Template character not found" |
