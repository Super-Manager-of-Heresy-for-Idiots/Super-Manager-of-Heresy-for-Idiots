# Контракты API: бестиарий (монстры + справочники)

Сгенерировано: 2026-06-10 16:18. Обновлено: 2026-06-12 16:23 — размер/характеристика/тип урона
переведены из enum-строк в справочники (передаются UUID, разворачиваются в `DictionaryRef`).
Теперь ВСЕ справочники homebrew-дружелюбны: `homebrewId == null` = CORE (системная строка),
`homebrewId != null` = строка homebrew-модификации.

Бэкенд завершён и компилируется. Ниже — полный контракт новых эндпоинтов бестиария
для фронтенда: маршруты, роли, тела запросов/ответов, перечисления и сценарии.
Источник истины в коде:
- Контроллеры: `src/main/java/com/dnd/app/controller/{AdminBestiary,Bestiary,HomebrewBestiary,CampaignMonster}Controller.java`
- Сервисы (где живут проверки прав): `service/MonsterService.java`, `service/BestiaryDictionaryService.java`
- DTO: `dto/request/{MonsterRequest,DictionaryEntryRequest}.java`, `dto/response/{MonsterResponse,MonsterSummaryResponse,DictionaryEntryResponse}.java`

---

## 0. Общие правила

### 0.1 Конверт ответа `ApiResponse<T>`
Все эндпоинты возвращают единый конверт (поля с `null` опускаются — `@JsonInclude(NON_NULL)`):
```json
{
  "success": true,
  "data": { ... },        // payload эндпоинта (T), либо null для delete/void
  "message": "Monster created",  // человекочитаемое сообщение (не всегда)
  "error": "VALIDATION_ERROR",   // только при success=false
  "fields": { "sizeId": "sizeId is required" }  // только при ошибке валидации
}
```

### 0.2 Аутентификация и роли
- Все маршруты требуют JWT (`Authorization: Bearer <token>`), кроме публичных
  (`/api/auth/**`, swagger, actuator/health).
- Жёсткая URL-проверка только для `/api/admin/**` → роль `ADMIN` (`SecurityConfig`).
  Остальные проверки прав (GAME_MASTER, член кампании, GM кампании, видимость игроку)
  выполняются в сервисном слое и при нарушении возвращают `403`/`404`.
- Роли пользователя: `ADMIN`, `GAME_MASTER`, `PLAYER`.

### 0.3 Три области видимости монстра (`scope`)
Поле `scope` присутствует в ответах и определяется так:
- `SYSTEM` — `homebrewId == null && campaignId == null`. Системные монстры (управляет ADMIN).
- `HOMEBREW` — задан `homebrewId`. Монстры внутри homebrew-пакета (управляет GAME_MASTER).
- `CAMPAIGN` — задан `campaignId`. Монстры в кампании (управляет мастер кампании).

### 0.4 Коды ошибок
- `400` — невалидное тело/перечисление/нарушение бизнес-правила (напр. редактирование
  не-DRAFT пакета, дубликат slug/code).
- `403` — нет прав (не та роль / не владелец пакета / не GM кампании).
- `404` — сущность не найдена ИЛИ монстр кампании скрыт от игрока (намеренно 404, не 403).
- `409`-семантика отдаётся как `400` с типом `DUPLICATE...` в сообщении.

### 0.5 Бывшие перечисления — теперь справочники (передаются UUID)
Размер, характеристика и тип урона раньше были enum-строками — теперь это полноценные
homebrew-дружелюбные справочники. В запросе передаётся `*Id` (UUID), в ответе разворачивается
в `DictionaryRef`. Опции грузятся из соответствующих `GET /dictionaries/{kind}`, НЕ хардкодить.
- `sizeId`, `sizeSecondaryId`, `swarmSizeId` → справочник `sizes`
  (системные `code`: `TINY | SMALL | MEDIUM | LARGE | HUGE | GARGANTUAN`)
- `abilityId` (saving throws), `saveAbilityId` (features) → справочник `abilities`
  (системные `code`: `STRENGTH | DEXTERITY | CONSTITUTION | INTELLIGENCE | WISDOM | CHARISMA`)
- `damageTypeId` → справочник `damage-types`
  (системные `code`: `SLASHING | PIERCING | BLUDGEONING | FIRE | COLD | LIGHTNING | POISON | NECROTIC | RADIANT | PSYCHIC | FORCE | THUNDER | ACID`)
- `section`, `kind`, `attackType` — по-прежнему свободные строки (в БД нижний регистр), enum НЕ применять.

### 0.6 Виды справочников (`{kind}` в пути = slug)
| slug | смысл |
|---|---|
| `creature-types` | типы существ |
| `alignments` | мировоззрения |
| `languages` | языки |
| `sense-types` | типы чувств (тёмное зрение и т.п.) |
| `movement-types` | типы перемещения |
| `habitats` | места обитания |
| `treasure-tags` | теги сокровищ |
| `conditions` | состояния (для иммунитетов) |
| `gear-items` | снаряжение монстров |
| `sources` | источники (книги); только здесь используется `bookCode` |
| `sizes` | размеры существ (бывший enum `CreatureSize`) |
| `abilities` | характеристики (бывший enum `Ability`) |
| `damage-types` | типы урона (бывший enum `DamageType`) |

---

## 1. Справочник: тело запроса/ответа

### `DictionaryEntryRequest`
```json
{
  "code": "dragon",            // required, ≤60, уникален в пределах области
  "nameRusloc": "Дракон",      // required
  "nameEngloc": "Dragon",      // optional
  "bookCode": "MM",            // только для kind=sources, ≤20; иначе игнорируется
  "isUnique": false            // optional, default false
}
```

### `DictionaryEntryResponse`
```json
{
  "id": "uuid",
  "code": "dragon",
  "nameRusloc": "Дракон",
  "nameEngloc": "Dragon",
  "bookCode": "MM",            // null кроме sources
  "homebrewId": "uuid|null",   // null = системная строка
  "isUnique": false,
  "createdAt": "2026-06-10T...Z",
  "updatedAt": "2026-06-10T...Z"
}
```

---

## 2. Монстр: тело запроса/ответа

### `MonsterRequest` (полный вложенный граф)
Скаляры:
```json
{
  "slug": "goblin",            // optional; если пусто — генерируется из имени, уникален
  "nameRusloc": "Гоблин",      // REQUIRED
  "nameEngloc": "Goblin",
  "alignmentId": "uuid|null",  // ссылка на справочник alignments
  "sizeId": "uuid",            // REQUIRED, ссылка на справочник sizes
  "sizeSecondaryId": null,     // uuid|null (sizes)
  "isSwarm": false,
  "swarmSizeId": null,         // uuid|null (sizes)
  "armorClass": 15,            // REQUIRED (Short)
  "armorClassText": "кожаный доспех, щит",
  "initiativeBonus": 2,
  "initiativeScore": 12,
  "hpAverage": 7,
  "hpDiceCount": 2,
  "hpDiceSides": 6,
  "hpDiceModifier": 0,
  "hpFormula": "2d6",
  "strScore": 8,               // REQUIRED все шесть характеристик (Short)
  "dexScore": 14,
  "conScore": 10,
  "intScore": 10,
  "wisScore": 8,
  "chaScore": 8,
  "passivePerception": 9,
  "telepathyFt": null,
  "crRating": "1/4",           // REQUIRED (строка)
  "crValue": 0.25,             // REQUIRED (BigDecimal, numeric(5,3))
  "xpBase": 50,
  "xpLair": null,
  "proficiencyBonus": 2,
  "legendaryUsesBase": null,
  "legendaryUsesLair": null,
  "legendaryText": null,
  "loreText": "...",
  "isVisibleToPlayers": false, // учитывается только для CAMPAIGN-области
  "isActive": true             // учитывается только для SYSTEM-области
}
```

Наборы ссылок на справочники (`Set<UUID>`, могут быть пустыми/отсутствовать):
`creatureTypeIds`, `languageIds`, `conditionImmunityIds`, `habitatIds`, `treasureTagIds`, `sourceIds`.

Вложенные списки (полностью заменяются при каждом PUT — rebuild-семантика):
```json
{
  "speeds":   [{ "movementTypeId": "uuid", "ft": 30, "hover": false }],
  "senses":   [{ "senseTypeId": "uuid", "ft": 60 }],
  "savingThrows": [{ "abilityId": "uuid", "bonus": 4 }],
  "skillProficiencies": [{ "proficiencySkillId": "uuid", "bonus": 6 }],
  "damageResistances":   [{ "damageTypeId": "uuid", "note": null }],
  "damageImmunities":    [{ "damageTypeId": "uuid", "note": null }],
  "damageVulnerabilities":[{ "damageTypeId": "uuid", "note": null }],
  "gear": [{ "itemId": "uuid", "qty": 1 }],
  "features": [{
    "section": "actions",          // REQUIRED, свободная строка
    "sortOrder": 0,                // REQUIRED
    "nameRusloc": "Удар скимитаром",
    "nameEngloc": "Scimitar",
    "kind": "melee_weapon",        // REQUIRED, свободная строка
    "rechargeMin": null,
    "rechargeMax": null,
    "descriptionRusloc": "...",    // REQUIRED
    "descriptionEngloc": "...",
    "attackType": "melee",         // свободная строка|null
    "attackBonus": 4,
    "reachFt": 5,
    "rangeFt": null,
    "rangeLongFt": null,
    "saveAbilityId": null,         // uuid|null (abilities)
    "saveDc": null,
    "damages": [{
      "sortOrder": 0,              // REQUIRED
      "average": 5,
      "dice": "1d6+2",
      "damageTypeId": "uuid",      // uuid|null (damage-types)
      "note": null
    }]
  }]
}
```
Примечания:
- `damageTypeId` в `DamageEntry`/`FeatureDamageEntry` может быть `null` (тогда тип урона не задан, остаётся только `note`).
- `qty` по умолчанию `1`, `hover` по умолчанию `false`.

### `MonsterResponse`
Возвращает весь граф. Ключевые отличия от запроса:
- Справочные ссылки разворачиваются в объекты `DictionaryRef { id, code, nameRusloc, nameEngloc, homebrewId }`
  (поля `alignment`, `size`, `sizeSecondary`, `swarmSize`, `creatureTypes[]`, `languages[]`,
  `conditionImmunities[]`, `habitats[]`, `treasureTags[]`, `sources[]`, а также
  `movementType`/`senseType`/`item` внутри строк).
- `skillProficiencies[]` → `{ id, proficiencySkillId, skillName, bonus }`.
- `savingThrows[].ability`, `damage*[].damageType`, `features[].saveAbility`,
  `features[].damages[].damageType` теперь тоже `DictionaryRef` (раньше — enum строка).
- Метаданные: `id`, `sourceExternalId`, `scope`, `homebrewId`, `campaignId`,
  `sourceMonsterId` (если монстр склонирован — id источника), `isVisibleToPlayers`,
  `isActive`, `createdBy`/`createdByUsername`, `updatedBy`/`updatedByUsername`,
  `createdAt`, `updatedAt`.
- У каждой вложенной строки есть свой `id` (для редактирования на FE удобно сопоставлять).

### `MonsterSummaryResponse` (списки)
```json
{
  "id": "uuid", "slug": "goblin",
  "nameRusloc": "Гоблин", "nameEngloc": "Goblin",
  "size": { "id": "uuid", "code": "SMALL", "nameRusloc": "Маленький", "nameEngloc": "Small", "homebrewId": null },
  "crRating": "1/4", "crValue": 0.25,
  "scope": "SYSTEM", "homebrewId": null, "campaignId": null,
  "isVisibleToPlayers": false, "isActive": true
}
```

---

## 3. Эндпоинты

### 3.1 ADMIN — системный бестиарий (`/api/admin/bestiary`, требует роль ADMIN)
Справочники:
- `GET    /dictionaries/{kind}` → `List<DictionaryEntryResponse>` (системные строки, homebrew=null)
- `POST   /dictionaries/{kind}` (`DictionaryEntryRequest`) → `201` `DictionaryEntryResponse`
- `PUT    /dictionaries/{kind}/{id}` (`DictionaryEntryRequest`) → `DictionaryEntryResponse`
- `DELETE /dictionaries/{kind}/{id}` → `200` (data=null)

Системные монстры:
- `GET    /monsters` → `List<MonsterSummaryResponse>` (ADMIN видит и неактивных)
- `GET    /monsters/{id}` → `MonsterResponse`
- `POST   /monsters` (`MonsterRequest`) → `201` `MonsterResponse`
- `PUT    /monsters/{id}` (`MonsterRequest`) → `MonsterResponse`
- `POST   /monsters/{id}/active?active=true|false` → `MonsterResponse` (вкл/выкл публикацию)
- `DELETE /monsters/{id}` → `200`

### 3.2 Любой авторизованный — просмотр (`/api/bestiary`)
- `GET /dictionaries/{kind}` → системные справочники (для выпадающих списков на FE)
- `GET /monsters` → системные монстры (НЕ-ADMIN видит только `isActive=true`)
- `GET /monsters/{id}` → `MonsterResponse` (видимость кампанийных монстров проверяется в сервисе)

### 3.3 GAME_MASTER — homebrew (`/api/homebrew/{packageId}/bestiary`)
Все мутации требуют: роль GAME_MASTER (или ADMIN), владение пакетом, статус пакета `DRAFT`.
Справочники homebrew:
- `GET    /dictionaries/{kind}` → строки этого пакета
- `POST   /dictionaries/{kind}` → `201`
- `PUT    /dictionaries/{kind}/{id}`
- `DELETE /dictionaries/{kind}/{id}`

Монстры homebrew:
- `GET    /monsters` → `List<MonsterSummaryResponse>` пакета
- `GET    /monsters/{id}` → `MonsterResponse`
- `POST   /monsters` (`MonsterRequest`) → `201` — новый монстр «с нуля» по справочным данным
- `POST   /monsters/duplicate/{sourceId}` → `201` — форк системного/homebrew монстра
  в этот пакет (глубокая копия, новый уникальный slug, `sourceMonsterId` = исходный)
- `PUT    /monsters/{id}` (`MonsterRequest`) → `MonsterResponse`
- `DELETE /monsters/{id}` → `200`

### 3.4 Мастер кампании — кампанийные монстры (`/api/campaigns/{campaignId}/monsters`)
Чтение — любой член кампании; мутации — только GM кампании (или ADMIN).
- `GET    ` → `List<MonsterSummaryResponse>`: GM видит всех, игрок — только `isVisibleToPlayers=true`
- `GET    /{id}` → `MonsterResponse` (скрытый монстр для игрока → `404`)
- `POST   ` (`MonsterRequest`) → `201` — создать монстра в кампании
- `POST   /clone/{sourceId}` → `201` — клонировать системного/homebrew/кампанийного монстра
  в кампанию (homebrew-источник допускается, только если его пакет подключён к кампании;
  кампанийный источник — только из той же кампании). Клон создаётся скрытым.
- `PUT    /{id}` (`MonsterRequest`) → `MonsterResponse`
- `POST   /{id}/toggle-visibility` → `MonsterResponse` — показать/скрыть монстра игрокам
- `DELETE /{id}` → `200` — удалить (после победы игроков)

---

## 4. TODO на фронтенде

1. **Сервисный слой API**: 4 группы клиентов под разделы 3.1–3.4. Учесть конверт `ApiResponse`
   (разворачивать `data`, показывать `message`, на `error/fields` — тосты/подсветка полей формы).
2. **Справочники как источники для селектов**: грузить `GET /api/bestiary/dictionaries/{kind}`
   (системные) и при работе в homebrew-пакете дополнительно мёржить
   `GET /api/homebrew/{packageId}/bestiary/dictionaries/{kind}`. В селектах хранить `id`, показывать `nameRusloc`.
   Это относится и к `sizes` / `abilities` / `damage-types` — больше НЕ enum.
3. **Бывшие перечисления — теперь справочники**: для `sizeId` / `abilityId` / `damageTypeId`
   грузить опции из `GET /dictionaries/{sizes|abilities|damage-types}` (как любой другой селект,
   с homebrew-мёржем). НЕ хардкодить значения. `section`/`kind`/`attackType` остаются
   свободным вводом/пресет-подсказками, без жёсткого enum.
4. **Форма монстра**: одна большая форма строит `MonsterRequest`. Обязательные поля:
   `nameRusloc`, `sizeId`, `armorClass`, все шесть `*Score`, `crRating`, `crValue`. Вложенные
   секции (speeds/senses/saves/skills/3×damage/gear/features→damages) — динамические списки
   с add/remove. Помнить: PUT перезаписывает вложенные списки целиком (rebuild), частичный
   PATCH не поддерживается — отправлять полный набор.
5. **Ролевые экраны**:
   - ADMIN: каталог системных монстров + toggle `active`, CRUD системных справочников.
   - GAME_MASTER: внутри DRAFT-пакета — CRUD монстров и справочников, кнопка «дублировать»
     из системного/homebrew.
   - Мастер кампании: список монстров кампании, создание/клон, переключатель видимости
     (скрыт/показан игрокам), удаление; заранее готовить скрытых монстров.
   - PLAYER: видит только видимых кампанийных монстров и системный/homebrew каталог (read-only).
6. **Обработка `404` для скрытых кампанийных монстров**: для игрока скрытый монстр
   неотличим от отсутствующего — не показывать «нет доступа», трактовать как «не найдено».
7. **slug**: можно не отправлять при создании (сгенерируется). При ручном вводе — показывать
   ошибку дубликата (`400`).
