# Character Sheet — уточнение контракта и добавление `tempHp`

Ответ на обзор FE-команды от 2026-06-06 («лист персонажа показывает «—» в большинстве полей, нет skills/spells/wallet/attacks»). Прошёл по каждому пункту обзора и сверил с реальным бэкендом.

**TL;DR:** большинство «отсутствующих» эндпоинтов и полей уже есть — просто называются иначе или возвращаются inline в `CharacterResponse`, а не отдельными подресурсами. Реальный пробел был один — `tempHp`, реализован.

---

## 1. `POST /characters/full` — есть, фолбэк не нужен

В `characters-full.api.ts:65` (`isEndpointMissing` → откат на минимальный `POST /characters`) — можно убирать. Эндпоинт существует и персистит весь payload:

- В кампанию: **`POST /api/campaigns/{campaignId}/characters/full`** (`CampaignCharacterController.java:33`)
- Шаблон (вне кампании): **`POST /api/characters/full`** (`CharacterTemplateController.java:29`)

Сервис `CharacterWizardService.createFullCharacter` сохраняет полный `CreateFullCharacterRequest`:

```ts
{
  name: string,
  alignment?: string,
  avatar?: string,
  raceId: UUID,
  subraceId?: UUID,
  classId: UUID,
  level: 1..20,
  abilityScores: { statId: UUID, baseValue: 1..30 }[],   // ровно 6
  scoreMethod: "STANDARD_ARRAY" | "POINT_BUY" | "ROLL" | "MANUAL",
  backgroundId: UUID,
  chosenSkillProficiencyIds: UUID[],
  cantripIds?: UUID[],
  spellIds?: UUID[],
  biography?: { personalityTraits, ideals, bonds, flaws },
  startingCoins?: { currencyTypeId: UUID, amount: number }[]
}
```

Если ловите 404/405 — это URL/baseURL ошибка (нужен префикс `/api`), а не отсутствие эндпоинта. Проверьте сериализацию `UUID`-полей.

## 2. `CharacterResponse` уже возвращает почти всё — сверьте имена полей

Полная актуальная схема — `CharacterResponse.java`. Маппинг ваших «нужно добавить» → реальное имя поля:

| Что вы ожидаете | Что реально приходит | Тип |
|---|---|---|
| `backgroundId + backgroundName` | `background: { id, name, description, skillProficiencyNames, grantedExtras }` | `BackgroundResponse` |
| `alignment` | `alignment` | `string` |
| `armorClass` | `armorClass` | `number` |
| `hitDice` | `hitDiceType` + `hitDiceTotal` | `string` («d8») + `string` («3d8») |
| `inspiration` | `inspiration` | `boolean` |
| `deathSaves` | `deathSaveSuccesses` + `deathSaveFailures` | `number` + `number` |
| `playerName` | `ownerUsername` (логин владельца персонажа) | `string` |
| `avatar` (portrait) | `avatarUrl` | `string` |
| `Saving throws` | `savingThrowProficiencyStatNames` | `string[]` (имена статов: `"Strength"`, `"Dexterity"`, ...) |
| `Biography` | `biography: { personalityTraits, ideals, bonds, flaws }` | `BiographyResponse` |
| `tempHp` | **`tempHp`** | `number` *(новое — см. п.5)* |
| `Initiative` | НЕ возвращается — считается на клиенте: DEX-мод (+ proficiency, если вы реализуете соответствующие фичи классов) | — |
| `Passive Perception` | НЕ возвращается — считается на клиенте: `10 + WIS-мод + proficiency (если Perception в skillProficiencies)` | — |
| `Proficiencies` (armor/weapon, текст) | НЕ на персонаже. Берите из `/api/reference/classes` → `armorWeaponProficiencies` по `classLevels[i].classId` | — |

Остальные поля (`currentHp`, `maxHp`, `speed`, `experience`, `totalLevel`, `classLevels`, `race`, `raceSnapshot`, `selectedLineageId`, `status`, `campaignId`, `createdAt`, `updatedAt`) — без изменений.

## 3. «Отсутствующие подресурсы» — они уже inline в `CharacterResponse`

| Что просили | Где реально лежит | Структура |
|---|---|---|
| `GET /characters/{id}/skills` | `CharacterResponse.skillProficiencies[]` | `{ skillId, skillName, source: "CLASS" \| "BACKGROUND" \| "RACE" \| "FEAT" \| "OTHER" }` |
| `GET /characters/{id}/spells` | `CharacterResponse.knownSpells[]` | `{ spellId, name, level, school }` |
| Hit/Damage на инвентаре | НЕ на `ItemInstanceResponse`. Боевые данные — в **`CharacterResponse.attacks[]`** | `{ name, attackBonus: string, damage: string, damageType: string }` |
| `/wallet` | **`GET /api/campaigns/{campaignId}/characters/{characterId}/wallet`** (`CampaignCharacterController.java:273`) → `List<WalletEntryResponse>` | `{ currencyTypeId, currencyName, amount, goldEquivalent }` |
| Модификация валюты | `POST /api/campaigns/{campaignId}/characters/{characterId}/wallet` с `{ currencyTypeId, amount }` | — |
| Features (текст из мастера) | `CharacterResponse.features` | `string` |
| Granted class features | **`GET /api/characters/{characterId}/rewards`** (`LevelUpController.java:37`) → `CharacterRewardsResponse` | — |

**Бонусы навыков считаем на клиенте:**

```
modifier = governingStat.modifier
         + (skillProficiencies.includes(skillId) ? proficiencyBonus : 0)
proficiencyBonus = floor((totalLevel - 1) / 4) + 2
```

`governingStatName` для каждого скилла — из `/api/reference/skills` (или `/api/campaigns/{id}/reference/skills` внутри кампании) → `ProficiencySkillResponse.governingStatName`.

## 4. Шаблоны вне кампании — все реализованы

`CharacterTemplateController.java`:

| Метод | URL | Делает |
|---|---|---|
| POST | `/api/characters/full` | Создать шаблон через wizard (тот же `CreateFullCharacterRequest`) |
| GET | `/api/characters/my` | Список моих шаблонов (`campaignId == null`) |
| GET | `/api/characters/{characterId}` | Один шаблон |
| DELETE | `/api/characters/{characterId}` | Удалить шаблон |
| POST | `/api/campaigns/{campaignId}/characters/from-template/{templateId}` | Клонировать шаблон в кампанию |

> Флаг `?clone=false` **не поддерживается** — всегда полный клон. Оригинал остаётся как шаблон, копия идёт в кампанию с `currentHp=maxHp`, `tempHp=0`, `deathSaves=0/0`, `inspiration=false`. Клонируются: статы, навыки, известные заклинания, кошелёк, ресурсы, биография, features, атаки, classLevels.

## 5. Что добавил по вашему обзору: `tempHp`

Единственный реальный пробел в данных, который вы выявили. Реализовано полностью.

### Изменения в схеме БД

Миграция `035-add-temp-hp.xml` — колонка `characters.temp_hp INT NOT NULL DEFAULT 0`.

### Изменения в `CharacterResponse`

Добавлено поле:

```ts
tempHp: number   // временные HP, поглощают урон до currentHp
```

### Изменения в `ModifyHpRequest`

```ts
POST /api/campaigns/{campaignId}/characters/{characterId}/hp
{
  amount: number,        // delta к HP.
                         // > 0 = лечение (только currentHp, до maxHp). tempHp не растёт.
                         // < 0 = урон. Поглощается tempHp в первую очередь.
                         // = 0 = только применить setTempHp (если задан).
  setTempHp?: number     // опционально: установить новое значение temp HP пула. >= 0.
                         // Применяется ДО amount.
}
```

### Логика урона/лечения (`CharacterService.modifyHp`)

1. Если в запросе есть `setTempHp` → `character.tempHp = setTempHp` (применяется первым: соответствует 5e-механике «получили временные хиты» отдельным шагом).
2. Если `amount < 0` (урон):
   - Урон сначала поглощается `tempHp`: `absorbed = min(tempHp, |amount|)`.
   - Остаток снимается с `currentHp`, не уходя ниже 0.
3. Если `amount > 0` (лечение):
   - `currentHp = min(currentHp + amount, maxHp)`.
   - `tempHp` не меняется (правило 5e: лечение не восполняет temp HP).
4. Если `amount == 0` — только применяется `setTempHp` (если задан).

Возврат: обычный `CharacterResponse` с обновлёнными `currentHp` и `tempHp`.

### Примеры запросов

```ts
// Жрец накладывает Aid → персонажу +5 temp HP
POST /hp { amount: 0, setTempHp: 5 }

// Прилетел Fireball на 18: 5 temp HP сгорают, currentHp -= 13
POST /hp { amount: -18 }

// Лечение на 8 — currentHp растёт, tempHp не трогается
POST /hp { amount: 8 }

// Снять Aid (например, истёк) и одновременно полечить
POST /hp { amount: 6, setTempHp: 0 }
```

## 6. Что НЕ делать на FE

- ❌ Не откатываться на «минимальный POST /characters» при 404/405. Если падает — это URL/baseURL ошибка, а не отсутствие эндпоинта.
- ❌ Не ходить на `GET /characters/{id}/skills` / `/spells` / `/attacks` — их нет, всё уже в `CharacterResponse`.
- ❌ Не использовать `homebrewId`/`source`-фильтрацию для vanilla-формы — для шаблонов есть `/api/reference/*` (без campaignId), они уже возвращают только системный контент.
- ❌ Не ждать поле `playerName` — используйте `ownerUsername`. Если нужен display name, отличный от логина, скажите — заведём отдельное поле на `User`.

## 7. Что осталось обсудить

- **Armor/Weapon proficiencies на персонаже** — сейчас только на `CharacterClass`. Если хотите готовую строку прямо в `CharacterResponse` (агрегированную по классам), скажите — добавлю поле `proficiencies: string`.
- **Initiative bonus / Passive Perception** — можно посчитать на бэке и отдать готовыми, если не хотите дублировать логику на клиенте.

---

## Файлы, которые менялись на бэке

- `src/main/resources/db/changelog/035-add-temp-hp.xml` (новая миграция)
- `src/main/resources/db/changelog/master.xml` (подключение миграции)
- `src/main/java/com/dnd/app/domain/PlayerCharacter.java` (поле `tempHp`)
- `src/main/java/com/dnd/app/dto/response/CharacterResponse.java` (поле `tempHp`)
- `src/main/java/com/dnd/app/dto/request/ModifyHpRequest.java` (поле `setTempHp`)
- `src/main/java/com/dnd/app/service/CharacterService.java` (логика поглощения урона, проброс в `toResponse`, сброс `tempHp=0` при клонировании шаблона)
- `CONTRACT_FRONT.md` (обновлённая карта DTO)
