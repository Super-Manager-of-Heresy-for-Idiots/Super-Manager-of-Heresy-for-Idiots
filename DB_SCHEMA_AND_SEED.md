# Структура БД и шаблоны для заполнения из Player's Handbook

Полная карта таблиц БД (Liquibase migrations 001–035) с JSON-шаблонами для каждой таблицы, которые можно заполнить контентом из PHB 5e и затем превратить в SQL-вставки / Liquibase migration / admin POST.

> Источник истины — JPA-сущности в `src/main/java/com/dnd/app/domain/`. Все имена колонок указаны так, как они реально лежат в БД (PostgreSQL).

---

## Содержание

- [Как использовать шаблоны](#как-использовать-шаблоны)
- [Порядок заполнения (FK-зависимости)](#порядок-заполнения)
- [Перечисления (enums)](#перечисления-enums)
- **Справочники для PHB-сидинга:**
  - [`stat_types`](#stat_types) — 6 характеристик
  - [`proficiency_skills`](#proficiency_skills) — 18 навыков
  - [`currency_types`](#currency_types) — CP/SP/EP/GP/PP
  - [`backgrounds`](#backgrounds) — 13 предысторий
  - [`character_races`](#character_races) — расы + сабрейсы
  - [`character_classes`](#character_classes) — 12 классов
  - [`subclasses`](#subclasses) — подклассы
  - [`spells`](#spells) — заклинания (~300+)
  - [`feats`](#feats) — фиты
  - [`item_types`](#item_types) — категории предметов
  - [`item_templates`](#item_templates) — конкретные предметы из PHB
  - [`enchantment_types`](#enchantment_types) — магические эффекты
  - [`buffs_debuffs`](#buffs_debuffs) — состояния и баффы
  - [`skills`](#skills) — активные способности классов
  - [`skill_effects`](#skill_effects) — связь skill→buff
  - [`custom_resource_types`](#custom_resource_types) — Ki, Sorcery Points и т.п.
  - [`class_level_rewards`](#class_level_rewards) — что выдаётся на каждом уровне
  - [`item_template_buffs`](#item_template_buffs) — встроенные баффы предметов
- **Рантайм-таблицы (не для PHB-сидинга, краткое описание):**
  - [Пользователи и кампании](#пользователи-и-кампании)
  - [Персонажи и их подресурсы](#персонажи-и-их-подресурсы)
  - [Инвентарь](#инвентарь)
  - [Контент кампании](#контент-кампании)
  - [Homebrew-маркетплейс](#homebrew)

---

## Как использовать шаблоны

Каждая таблица описана **схемой** (имя колонки → тип → ограничения) и **JSON-шаблоном** для одной записи. Заполняете JSON для всех записей PHB → передаёте бэкенду тремя способами:

**Вариант 1 — Liquibase migration (рекомендуется для статики).** Пишете SQL `INSERT ... FROM (VALUES ...)` по образцу `030-seed-wizard-data.xml`. UUID генерируется через `gen_random_uuid()`, ссылки на справочники — через подзапрос `(SELECT id FROM stat_types WHERE name = 'Strength')`. Это то, как уже залит существующий vanilla-сид.

**Вариант 2 — Admin POST endpoints.** Часть таблиц можно заполнять через `/api/admin/...` (см. `AdminController.java`). Удобно для итеративной правки. Не покрывает все таблицы.

**Вариант 3 — Прямой SQL** через `psql`. Быстро, но не воспроизводимо между средами.

**Конвенции в JSON-шаблонах ниже:**

- `null` — поле опциональное (nullable column).
- `"<...>"` — placeholder, нужно заполнить.
- `"<FK: tableName.column>"` — UUID, который нужно подставить из соответствующей таблицы.
- Поля с суффиксом `_json` в БД хранят сериализованную JSON-строку — в шаблоне указан **внутренний** формат (что должно быть после `JSON.parse`).
- Timestamps (`created_at`, `updated_at`) проставляются автоматически (`@CreationTimestamp`/`@UpdateTimestamp`), в шаблонах опущены.
- `id` (UUID) — при INSERT через Liquibase используйте `gen_random_uuid()`; в JSON-шаблоне опущен.

## Порядок заполнения

Из-за FK-зависимостей вставлять надо в этом порядке (внутри слоя — любой порядок):

```
1. stat_types
2. proficiency_skills, currency_types, buffs_debuffs, custom_resource_types,
   enchantment_types, feats, backgrounds, character_races
3. character_classes  (FK→stat_types)
4. subclasses, item_types, spells, skills  (FK→character_classes/skill/etc.)
5. skill_effects, item_templates, custom_resource_types(.classBound)
6. item_template_buffs, class_level_rewards
```

## Перечисления (enums)

Хранятся как `VARCHAR` колонки с `@Enumerated(EnumType.STRING)`. Значения должны точно совпадать с константой Java:

| Enum | Значения |
|---|---|
| `Role` | `PLAYER`, `GAME_MASTER`, `ADMIN` |
| `CampaignRole` | `GM`, `PLAYER` |
| `CampaignStatus` | `ACTIVE`, `PAUSED`, `COMPLETED` |
| `CharacterStatus` | `ACTIVE`, `DEAD`, `RESERVE` |
| `ContentType` | `ITEM_TYPE`, `CHARACTER_CLASS`, `SKILL`, `FEAT`, `SUBCLASS`, `RACE`, `STAT_TYPE`, `BUFF_DEBUFF`, `ENCHANTMENT_TYPE`, `CURRENCY`, `CUSTOM_RESOURCE`, `ITEM_TEMPLATE`, `BACKGROUND`, `SPELL`, `PROFICIENCY_SKILL` |
| `EffectRole` | `BUFF`, `DEBUFF` |
| `HomebrewStatus` | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| `QuestStatus` | `ACTIVE`, `COMPLETED`, `FAILED`, `HIDDEN`, `ARCHIVED` |
| `RaceAbilityBonusMode` | `FIXED`, `CHOICE` |
| `RaceSourceType` | `SYSTEM`, `HOMEBREW` |
| `RaceTraitActionType` | `PASSIVE`, `ACTION`, `BONUS_ACTION`, `REACTION`, `PART_OF_ATTACK_ACTION` |
| `RaceTraitRecharge` | `NONE`, `SHORT_REST`, `LONG_REST`, `PROFICIENCY_BONUS_PER_LONG_REST`, `CUSTOM` |
| `RaceTraitUseType` | `PASSIVE`, `LIMITED`, `ACTION`, `BONUS_ACTION`, `REACTION` |
| `RewardType` | `SKILL`, `SUBCLASS`, `FEAT`, `BUFF_DEBUFF`, `ABILITY_SCORE_IMPROVEMENT` |
| `ScoreMethod` | `STANDARD_ARRAY`, `POINT_BUY`, `ROLL` |
| `SkillActivation` | `PASSIVE`, `ACTIVE` |
| `SkillProficiencySource` | `CLASS`, `BACKGROUND`, `RACE`, `MANUAL` |

### Контентные перечисления → homebrew-дружелюбные справочники

Бывшие enum-ы `CreatureSize`, `DamageType`, `EquipmentSlot`, `Rarity` и `Ability`
больше **не** фиксированные перечисления. Они хранятся как самостоятельные справочные
таблицы (миграция `045`) с колонкой `homebrew_id` (`null` = системная/ванильная строка,
иначе принадлежит homebrew-пакету) и доступны через `GET/POST/PUT/DELETE /dictionaries/{kind}`:

| Справочник | Таблица | `kind` | Системные `code` |
|---|---|---|---|
| Тип урона | `damage_types` | `content-damage-types` | `SLASHING`, `PIERCING`, `BLUDGEONING`, `FIRE`, `COLD`, `LIGHTNING`, `POISON`, `NECROTIC`, `RADIANT`, `PSYCHIC`, `FORCE`, `THUNDER`, `ACID` |
| Редкость | `item_rarities` | `item-rarities` | `COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE`, `LEGENDARY` |
| Слот экипировки | `equipment_slots` | `equipment-slots` | `HEAD`, `CHEST`, `LEGS`, `FEET`, `MAIN_HAND`, `OFF_HAND`, `RING_LEFT`, `RING_RIGHT`, `NECK`, `CLOAK` |
| Размер существа | `creature_sizes` | `creature-sizes` | `TINY`, `SMALL`, `MEDIUM`, `LARGE`, `HUGE`, `GARGANTUAN` |
| Характеристика (бывш. `Ability`) | `stat_types` (колонка `code`) | — | `STRENGTH`, `DEXTERITY`, `CONSTITUTION`, `INTELLIGENCE`, `WISDOM`, `CHARISMA` |

На уровне БД и сущностей это FK (`slot_id`, `damage_type_id`, `rarity_id` и т.п.).
**API/DTO по-прежнему принимают и отдают строковый `code`** (`"MAIN_HAND"`, `"COMMON"`,
`"SLASHING"`) — сервис разрешает код в строку справочника (сначала строка пакета, затем
системная). Поэтому JSON-примеры ниже не изменились.

---

# Справочники для PHB-сидинга

## stat_types

6 характеристик. **Уже засеяно** (`002-seed-data.xml`), повторно не вставлять — приведено для справки.

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | PK |
| `name` | varchar(50) UNIQUE | ✓ | `Strength`, `Dexterity`, `Constitution`, `Intelligence`, `Wisdom`, `Charisma` |
| `description` | text |  | Описание характеристики |
| `is_default` | bool | ✓ | `true` для системных |
| `homebrew_id` | uuid → homebrew_packages |  | `null` для системных |

```json
{
  "name": "Strength",
  "description": "Physical power, athletic training.",
  "isDefault": true,
  "homebrewId": null
}
```

## proficiency_skills

18 навыков. **Уже засеяно** (`030-seed-wizard-data.xml`).

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(60) UNIQUE | ✓ | `Athletics`, `Stealth`, `Perception` и т.д. |
| `governing_stat_id` | uuid → stat_types | ✓ | Какая характеристика управляет навыком |

```json
{
  "name": "Athletics",
  "governingStat": "Strength"
}
```

Полный список (18): Athletics (STR); Acrobatics, Sleight of Hand, Stealth (DEX); Arcana, History, Investigation, Nature, Religion (INT); Animal Handling, Insight, Medicine, Perception, Survival (WIS); Deception, Intimidation, Performance, Persuasion (CHA).

## currency_types

5 валют PHB. Засеяно в `022-character-wallet-resources.xml`.

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(50) | ✓ | `Copper Pieces`, `Silver Pieces`, … |
| `exchange_rate_to_gold` | numeric(10,4) |  | Сколько монет = 1 GP. Для GP — 1, для SP — 10, для CP — 100 |
| `is_default` | bool | ✓ | |
| `homebrew_id` | uuid |  | `null` для PHB |

```json
{
  "name": "Gold Pieces",
  "exchangeRateToGold": 1.0,
  "isDefault": true,
  "homebrewId": null
}
```

PHB: Copper (100), Silver (10), Electrum (2), Gold (1), Platinum (0.1).

## backgrounds

13 предысторий PHB. **Уже засеяно** (`030-seed-wizard-data.xml`), но можно дополнить.

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(100) UNIQUE | ✓ | |
| `description` | text |  | Лор предыстории |
| `skill_proficiency_ids_json` | text (JSON) |  | **JSON-список имён навыков** (не UUID!), например `["Insight","Religion"]` |
| `granted_extras` | text |  | Текст — инструменты/языки одной строкой |
| `homebrew_id` | uuid |  | `null` для PHB |

```json
{
  "name": "Acolyte",
  "description": "You have spent your life in the service of a temple.",
  "skillProficiencyNames": ["Insight", "Religion"],
  "grantedExtras": "Two languages of your choice; Holy symbol",
  "homebrewId": null
}
```

PHB-список: Acolyte, Charlatan, Criminal, Entertainer, Folk Hero, Guild Artisan, Hermit, Noble, Outlander, Sage, Sailor, Soldier, Urchin.

## character_races

Расы. Очень богатая модель (28 колонок, 11 JSON-полей для трейтов/lineages/языков/иммунитетов). Частично засеяно в `028-race-species-evolution.xml`.

### Колонки

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(50) UNIQUE | ✓ | `Human`, `Elf`, `Dwarf`… |
| `slug` | varchar(80) |  | URL-friendly: `human`, `elf-high` |
| `description` | text |  | Короткое описание |
| `lore_description` | text |  | Расширенный лор |
| `source_type` | varchar(20) — `RaceSourceType` | ✓ | `SYSTEM` для PHB |
| `source_name` | varchar(120) |  | `"Player's Handbook"` |
| `active` | bool | ✓ | `true` |
| `created_by`, `updated_by` | uuid → users |  | `null` для PHB-сида |
| `homebrew_id` | uuid |  | `null` для PHB |
| `creature_type` | varchar(40) | ✓ | `HUMANOID`, `DRAGONBORN` и т.п. (строка, не enum) |
| `size_options_json` | text (JSON) |  | Список `code` справочника `creature-sizes`: `["MEDIUM"]` или `["SMALL","MEDIUM"]` (валидируется по справочнику) |
| `default_size` | varchar(20) |  | `MEDIUM` |
| `speed_json` | text (JSON) |  | `{"walk":30,"fly":0,"swim":0,"climb":0,"burrow":0}` |
| `darkvision_range` | int |  | Футы (`60`, `120`); `null` если нет |
| `traits_json` | text (JSON) |  | Список трейтов, см. ниже |
| `lineages_json` | text (JSON) |  | Список сабрейсов (subraces) |
| `lineage_required` | bool | ✓ | `true` если у расы обязательно надо выбрать сабрейс (Elf → High/Wood/Drow) |
| `languages_json` | text (JSON) |  | Список фиксированных: `["Common","Elvish"]` |
| `language_options_json` | text (JSON) |  | Кол-во + пул для выбора: `{"count":1,"pool":"any"}` |
| `proficiencies_json` | text (JSON) |  | Список владений (Dwarven combat training и т.п.) |
| `resistances_json` | text (JSON) |  | Список `code` справочника `content-damage-types`: `["POISON"]` |
| `vulnerabilities_json` | text (JSON) |  | Список `code` справочника `content-damage-types` |
| `immunities_json` | text (JSON) |  | Список `code` справочника `content-damage-types` |
| `condition_resistances_json` | text (JSON) |  | Состояния (Charmed и т.п.) |
| `condition_advantages_json` | text (JSON) |  | Преимущество против состояний |
| `innate_spells_json` | text (JSON) |  | Расовые заклинания |
| `allow_ability_score_bonuses` | bool | ✓ | Разрешены ли ASI бонусы для этой расы |
| `ability_score_bonuses_json` | text (JSON) |  | `[{"ability":"DEXTERITY","value":2,"mode":"FIXED"}]` |
| `metadata_json` | text (JSON) |  | Свободные доп. поля |

### JSON-шаблон расы

```json
{
  "name": "Elf",
  "slug": "elf",
  "description": "Magical people of otherworldly grace.",
  "loreDescription": "Long-lived, slender, and connected to nature...",
  "sourceType": "SYSTEM",
  "sourceName": "Player's Handbook",
  "active": true,
  "homebrewId": null,
  "creatureType": "HUMANOID",
  "sizeOptions": ["MEDIUM"],
  "defaultSize": "MEDIUM",
  "speed": { "walk": 30 },
  "darkvisionRange": 60,
  "traits": [
    {
      "name": "Keen Senses",
      "description": "You have proficiency in the Perception skill.",
      "actionType": "PASSIVE",
      "useType": "PASSIVE",
      "recharge": "NONE",
      "uses": null
    },
    {
      "name": "Fey Ancestry",
      "description": "You have advantage on saving throws against being charmed, and magic can't put you to sleep.",
      "actionType": "PASSIVE",
      "useType": "PASSIVE",
      "recharge": "NONE",
      "uses": null
    },
    {
      "name": "Trance",
      "description": "Elves meditate 4 hours instead of sleeping 8.",
      "actionType": "PASSIVE",
      "useType": "PASSIVE",
      "recharge": "NONE",
      "uses": null
    }
  ],
  "lineages": [
    { "id": "<generate uuid>", "name": "High Elf", "description": "..." },
    { "id": "<generate uuid>", "name": "Wood Elf", "description": "..." },
    { "id": "<generate uuid>", "name": "Drow", "description": "..." }
  ],
  "lineageRequired": true,
  "languages": ["Common", "Elvish"],
  "languageOptions": null,
  "proficiencies": ["Perception"],
  "resistances": [],
  "vulnerabilities": [],
  "immunities": [],
  "conditionResistances": [],
  "conditionAdvantages": ["CHARMED"],
  "innateSpells": [],
  "allowAbilityScoreBonuses": false,
  "abilityScoreBonuses": [
    { "ability": "DEXTERITY", "value": 2, "mode": "FIXED" }
  ],
  "metadata": null
}
```

**Внутренний формат `traits[]`:**
```json
{
  "name": "string",
  "description": "string",
  "actionType": "PASSIVE | ACTION | BONUS_ACTION | REACTION | PART_OF_ATTACK_ACTION",
  "useType": "PASSIVE | LIMITED | ACTION | BONUS_ACTION | REACTION",
  "recharge": "NONE | SHORT_REST | LONG_REST | PROFICIENCY_BONUS_PER_LONG_REST | CUSTOM",
  "uses": null  // или число (для LIMITED)
}
```

**Внутренний формат `lineages[]`:** `{"id": "<uuid>", "name": "string", "description": "string"}`. Этот UUID используется в `characters.selected_lineage_id` — сгенерируйте и сохраните.

**Внутренний формат `abilityScoreBonuses[]`:** `{"ability": "<stat_types.code>", "value": int, "mode": "FIXED" | "CHOICE"}` — `ability` валидируется по справочнику характеристик (`stat_types.code`, бывший enum `Ability`).

PHB-список рас: Dwarf, Elf, Halfling, Human, Dragonborn, Gnome, Half-Elf, Half-Orc, Tiefling.

## character_classes

12 классов PHB. Частично засеяно (название/описание) в `002-seed-data.xml`, метаданные — в `030-seed-wizard-data.xml`.

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(50) UNIQUE | ✓ | `Barbarian`, `Wizard`… |
| `description` | text |  | |
| `homebrew_id` | uuid |  | `null` для PHB |
| `hit_die` | int |  | Размер хитовой кости: 6 / 8 / 10 / 12 |
| `primary_ability_stat_id` | uuid → stat_types |  | Основная характеристика класса |
| `saving_throw_stat_ids_json` | text (JSON) |  | **JSON-список имён статов**, например `["Strength","Constitution"]` |
| `skill_choice_count` | int |  | Сколько навыков выбирает игрок при создании (обычно 2, у барда 3) |
| `skill_choice_option_ids_json` | text (JSON) |  | **JSON-список имён навыков**-кандидатов |
| `armor_weapon_proficiencies` | text |  | Свободный текст: «Light armor, simple weapons, …» |
| `is_spellcaster` | bool | ✓ | Является ли класс заклинателем |
| `spellcasting_stat_id` | uuid → stat_types |  | INT (Wizard), WIS (Cleric, Druid), CHA (Bard, Sorcerer, Warlock, Paladin) |
| `has_cantrips` | bool | ✓ | Доступны ли кантрипы |
| `is_half_caster` | bool | ✓ | Полузаклинатель (Paladin, Ranger) |

```json
{
  "name": "Wizard",
  "description": "A scholarly magic-user capable of manipulating the structures of reality.",
  "homebrewId": null,
  "hitDie": 6,
  "primaryAbilityStat": "Intelligence",
  "savingThrowStatNames": ["Intelligence", "Wisdom"],
  "skillChoiceCount": 2,
  "skillChoiceOptionNames": ["Arcana","History","Insight","Investigation","Medicine","Religion"],
  "armorWeaponProficiencies": "Daggers, darts, slings, quarterstaffs, light crossbows",
  "isSpellcaster": true,
  "spellcastingStat": "Intelligence",
  "hasCantrips": true,
  "isHalfCaster": false
}
```

PHB-классы: Barbarian, Bard, Cleric, Druid, Fighter, Monk, Paladin, Ranger, Rogue, Sorcerer, Warlock, Wizard.

## subclasses

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(100) UNIQUE | ✓ | `Berserker`, `College of Lore`, `Champion`… |
| `class_id` | uuid → character_classes | ✓ | Родительский класс |
| `description` | text |  | |
| `homebrew_id` | uuid |  | `null` для PHB |

```json
{
  "name": "School of Evocation",
  "parentClassName": "Wizard",
  "description": "You focus your study on magic that creates powerful elemental effects.",
  "homebrewId": null
}
```

## spells

Заклинания. Колонка `available_to_class_ids_json` — это JSON-список **строк-UUID** классов (а не имён).

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(120) UNIQUE | ✓ | `Fireball`, `Magic Missile`… |
| `level` | int | ✓ | 0 = cantrip, 1–9 |
| `school` | varchar(30) | ✓ | `Abjuration`, `Evocation`, `Necromancy`, `Conjuration`, `Divination`, `Enchantment`, `Illusion`, `Transmutation` |
| `description` | text |  | |
| `available_to_class_ids_json` | text (JSON) |  | **JSON-список UUID** классов-кастеров (как строки) |
| `homebrew_id` | uuid |  | `null` для PHB |

```json
{
  "name": "Fireball",
  "level": 3,
  "school": "Evocation",
  "description": "A bright streak flashes from your pointing finger... 8d6 fire damage on failed save.",
  "availableToClassNames": ["Wizard", "Sorcerer"],
  "homebrewId": null
}
```

> При генерации SQL: подзапросом получите UUID классов и сериализуйте в JSON-массив. Пример: `(SELECT json_agg(id::text) FROM character_classes WHERE name IN ('Wizard','Sorcerer'))`.

## feats

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(100) UNIQUE | ✓ | `Alert`, `Lucky`, `Great Weapon Master`… |
| `description` | text |  | |
| `prerequisites` | text |  | Текст требований (или `null`) |
| `homebrew_id` | uuid |  | `null` для PHB |

```json
{
  "name": "Alert",
  "description": "+5 initiative; you can't be surprised while conscious; creatures don't gain advantage on attacks against you from being unseen.",
  "prerequisites": null,
  "homebrewId": null
}
```

PHB-фиты (42): Alert, Athlete, Actor, Charger, Crossbow Expert, Defensive Duelist, Dual Wielder, Dungeon Delver, Durable, Elemental Adept, Grappler, Great Weapon Master, Healer, Heavily Armored, Heavy Armor Master, Inspiring Leader, Keen Mind, Lightly Armored, Linguist, Lucky, Mage Slayer, Magic Initiate, Martial Adept, Medium Armor Master, Mobile, Moderately Armored, Mounted Combatant, Observant, Polearm Master, Resilient, Ritual Caster, Savage Attacker, Sentinel, Sharpshooter, Shield Master, Skilled, Skulker, Spell Sniper, Tavern Brawler, Tough, War Caster, Weapon Master.

## item_types

Категории/типы предметов (мечи, луки, лёгкая броня и т.п.). Связаны со слотом экипировки и базовым уроном.

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(50) UNIQUE | ✓ | `Longsword`, `Light Crossbow`, `Chain Mail`, `Shield`… |
| `description` | text |  | |
| `slot_id` | uuid → equipment_slots | ✓ | Справочник слотов; API отдаёт `code` |
| `damage_dice` | varchar(10) |  | `"1d8"`, `"2d6"`; `null` для брони/щита |
| `damage_bonus` | int | ✓ | По умолчанию 0 |
| `damage_type_id` | uuid → damage_types |  | Справочник; `null` для не-оружия; API отдаёт `code` |
| `skill_id` | uuid → skills |  | Если предмет даёт активную способность |
| `skill_activation` | varchar(10) — `SkillActivation` |  | `PASSIVE` / `ACTIVE` |
| `homebrew_id` | uuid |  | `null` для PHB |

```json
{
  "name": "Longsword",
  "description": "Versatile martial weapon.",
  "slot": "MAIN_HAND",
  "damageDice": "1d8",
  "damageBonus": 0,
  "damageType": "SLASHING",
  "skillName": null,
  "skillActivation": null,
  "homebrewId": null
}
```

## item_templates

Конкретные предметы из PHB-главы Equipment (зелья, верёвки, магические штуки и т.д.). Расширяет `item_types` для уникальных предметов.

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(100) | ✓ | |
| `description` | text |  | |
| `item_type_id` | uuid → item_types |  | К какому типу относится |
| `rarity_id` | uuid → item_rarities |  | Справочник редкостей; API отдаёт `code` |
| `damage_dice` | varchar(10) |  | Можно переопределить базовый урон item_type |
| `damage_bonus` | int |  | |
| `damage_type_id` | uuid → damage_types |  | Справочник; API отдаёт `code` |
| `is_stackable` | bool | ✓ | Стопками или уникально (зелья → true, мечи → false) |
| `skill_id` | uuid → skills |  | Активная способность предмета |
| `skill_activation` | varchar(10) — `SkillActivation` |  | |
| `homebrew_id` | uuid |  | `null` для PHB |

```json
{
  "name": "Potion of Healing",
  "description": "You regain 2d4+2 hit points when you drink this potion.",
  "itemTypeName": "Potion",
  "rarity": "COMMON",
  "damageDice": null,
  "damageBonus": 0,
  "damageType": null,
  "isStackable": true,
  "skillName": "Drink Healing Potion",
  "skillActivation": "ACTIVE",
  "homebrewId": null
}
```

## enchantment_types

Магические эффекты для предметов (например, +1, Flaming, Vorpal).

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(100) UNIQUE | ✓ | `+1 Weapon`, `Flaming`, `Vicious`… |
| `description` | text |  | |
| `damage_dice` | varchar(10) |  | Доп. урон от чара |
| `damage_bonus` | int | ✓ | По умолчанию 0 |
| `damage_type_id` | uuid → damage_types |  | Справочник; API отдаёт `code` |
| `buff_debuff_id` | uuid → buffs_debuffs |  | Связанный бафф |

```json
{
  "name": "Flaming",
  "description": "When you hit, deal extra 1d6 fire damage.",
  "damageDice": "1d6",
  "damageBonus": 0,
  "damageType": "FIRE",
  "buffDebuffName": null
}
```

## buffs_debuffs

Состояния (Blinded, Charmed, Frightened…) и баффы (Bless, Bardic Inspiration). Засеяно базово в `009-seed-buffs-debuffs.xml`.

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(100) UNIQUE | ✓ | |
| `description` | text |  | |
| `effect_type` | varchar(30) | ✓ | Свободная строка-категория: `STAT_MODIFIER`, `CONDITION`, `ADVANTAGE`, `RESISTANCE`… |
| `target_stat_id` | uuid → stat_types |  | Если бафф модифицирует характеристику |
| `modifier_value` | int |  | Сколько добавить к стату |
| `duration_rounds` | int |  | `null` = постоянный/до диспелла |
| `is_buff` | bool | ✓ | `true` — бафф, `false` — дебафф |
| `homebrew_id` | uuid |  | `null` для PHB |

```json
{
  "name": "Bless",
  "description": "Roll a d4 and add to attack rolls and saving throws.",
  "effectType": "STAT_MODIFIER",
  "targetStatName": null,
  "modifierValue": null,
  "durationRounds": 10,
  "isBuff": true,
  "homebrewId": null
}
```

PHB-состояния: Blinded, Charmed, Deafened, Frightened, Grappled, Incapacitated, Invisible, Paralyzed, Petrified, Poisoned, Prone, Restrained, Stunned, Unconscious, Exhaustion (6 уровней).

## skills

**Активные** способности классов/предметов (Action Surge, Second Wind, Rage и т.д.). Не путать с `proficiency_skills`.

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(100) UNIQUE | ✓ | `Action Surge`, `Rage`, `Bardic Inspiration`… |
| `description` | text |  | |
| `skill_type` | varchar(50) |  | Категория: `CLASS_FEATURE`, `RACIAL`, `ITEM_ACTIVATION` |
| `damage_dice` | varchar(10) |  | Если способность наносит урон |
| `damage_bonus` | int | ✓ | По умолчанию 0 |
| `damage_type_id` | uuid → damage_types |  | Справочник; API отдаёт `code` |
| `homebrew_id` | uuid |  | `null` для PHB |

```json
{
  "name": "Second Wind",
  "description": "You have a limited well of stamina. As a bonus action, regain 1d10+fighter_level HP. Once per short rest.",
  "skillType": "CLASS_FEATURE",
  "damageDice": null,
  "damageBonus": 0,
  "damageType": null,
  "homebrewId": null
}
```

## skill_effects

Связь N×N между `skills` и `buffs_debuffs` — какой бафф/дебафф накладывает скилл (с шансом срабатывания).

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `skill_id` | uuid → skills | ✓ | |
| `buff_debuff_id` | uuid → buffs_debuffs | ✓ | |
| `effect_role` | varchar(10) — `EffectRole` | ✓ | `BUFF` или `DEBUFF` |
| `chance_percent` | int | ✓ | 0..100, по умолчанию 100 |

Уникальность: `(skill_id, buff_debuff_id)`.

```json
{
  "skillName": "Rage",
  "buffDebuffName": "Raging",
  "effectRole": "BUFF",
  "chancePercent": 100
}
```

## custom_resource_types

Ki Points (Monk), Sorcery Points (Sorcerer), Bardic Inspiration uses и т.п.

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `name` | varchar(100) | ✓ | |
| `description` | text |  | |
| `max_value` | int |  | Базовый максимум (часто увеличивается с уровнем) |
| `class_bound_id` | uuid → character_classes |  | Привязан к классу |
| `homebrew_id` | uuid |  | `null` для PHB |

```json
{
  "name": "Ki Points",
  "description": "Mystical energy of the Monk, regained on short rest.",
  "maxValue": 2,
  "classBoundName": "Monk",
  "homebrewId": null
}
```

## class_level_rewards

Что выдаётся персонажу на каждом уровне класса. Дженерик-таблица: `reward_type` (строка) + `reward_id` (UUID соответствующей таблицы).

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `class_id` | uuid → character_classes | ✓ | |
| `required_level` | int | ✓ | На каком уровне доступна награда |
| `reward_type` | varchar(30) | ✓ | `SKILL`, `SUBCLASS`, `FEAT`, `BUFF_DEBUFF`, `ABILITY_SCORE_IMPROVEMENT` (`RewardType` enum) |
| `reward_id` | uuid | ✓ | UUID из соответствующей таблицы: skills / subclasses / feats / buffs_debuffs. Для ASI — любой не-null UUID (обычно генерится новый) |
| `is_choice` | bool | ✓ | `true` если игрок выбирает из нескольких (например, ASI vs feat); `false` если получает автоматически |

Уникальность: `(class_id, required_level, reward_type, reward_id)`.

```json
{
  "className": "Fighter",
  "requiredLevel": 1,
  "rewardType": "SKILL",
  "rewardName": "Second Wind",
  "isChoice": false
}
```

```json
{
  "className": "Fighter",
  "requiredLevel": 3,
  "rewardType": "SUBCLASS",
  "rewardName": "Champion",
  "isChoice": true
}
```

```json
{
  "className": "Wizard",
  "requiredLevel": 4,
  "rewardType": "ABILITY_SCORE_IMPROVEMENT",
  "rewardName": "ASI Level 4",
  "isChoice": false
}
```

## item_template_buffs

Связь N×N: какие баффы автоматически активируются при экипировке предмета (например, +2 к AC от плащ-кольцо защиты).

| Колонка | Тип | NN | Описание |
|---|---|---|---|
| `id` | uuid | ✓ | |
| `template_id` | uuid → item_templates | ✓ | |
| `buff_debuff_id` | uuid → buffs_debuffs | ✓ | |

Уникальность: `(template_id, buff_debuff_id)`.

```json
{
  "templateName": "Ring of Protection",
  "buffDebuffName": "+1 AC and Saves"
}
```

---

# Рантайм-таблицы (не для PHB-сидинга)

Эти таблицы наполняются в процессе игры через API. Шаблоны не требуются — структура для справки.

## Пользователи и кампании

- **`users`** — `id, username (uniq), email (uniq), password_hash, role (Role enum), created_at, updated_at`. Регистрация: `POST /api/auth/register`.
- **`campaigns`** — `id, name, description, status (CampaignStatus), invite_code (uniq, 8 chars), created_at, updated_at`. Создание: `POST /api/campaigns`.
- **`campaign_members`** — N×N user↔campaign: `id, campaign_id, user_id, role_in_campaign (CampaignRole), is_creator, joined_at, kicked`. Уникальность `(campaign_id, user_id)`.
- **`campaign_homebrew`** — какие homebrew-пакеты подключены к кампании: композитный PK `(campaign_id, package_id)`, плюс `pinned_version` для фиксации версии.
- **`gm_session_notes`** — заметки GM по сессиям: `id, campaign_id, author_id, title, content, created_at, updated_at`.

## Персонажи и их подресурсы

- **`characters`** (entity `PlayerCharacter`) — главная таблица. См. `PlayerCharacter.java` для полного списка (40+ колонок: name, totalLevel, experience, race+lineage, currentHp/maxHp/**tempHp**, alignment, background, avatarUrl, armorClass, speed, inspiration, hitDice*, deathSaves, saving_throw_proficiency_stat_ids_json, biography_json, features, attacks_json, scoreMethod, статус, владелец, кампания).
- **`character_class_levels`** — мультикласс: композитный PK `(character_id, class_id)` + `class_level`.
- **`character_stats`** — 6 строк на персонажа (по одной на стат): `id, character_id, stat_type_id, value`. Уник `(character_id, stat_type_id)`.
- **`character_skill_proficiencies`** — `id, character_id, skill_id, source (SkillProficiencySource)`. Уник `(character_id, skill_id)`.
- **`character_known_spells`** — `id, character_id, spell_id`. Уник `(character_id, spell_id)`.
- **`character_resources`** — `id, character_id, resource_type_id, current_value`. Уник `(character_id, resource_type_id)`.
- **`character_wallets`** — `id, character_id, currency_type_id, amount (numeric 15,2)`. Уник `(character_id, currency_type_id)`.
- **`character_active_effects`** — `id, character_id, buff_debuff_id, applied_by, remaining_rounds, applied_at`.
- **`character_acquired_rewards`** — какие награды из `class_level_rewards` игрок реально взял. Уник `(character_id, class_level_reward_id)`.

## Инвентарь

- **`item_instances`** — конкретные экземпляры предметов: `id, template_id, owner_character_id ИЛИ shared_storage_id, custom_name, quantity, is_unique, slot (если экипировано), notes, …`.
- **`item_enchantments`** — N×N item_instance ↔ enchantment_type: `id, item_instance_id, enchantment_type_id, applied_at, notes`. Уник `(item_instance_id, enchantment_type_id)`.
- **`shared_storage`** — общий ящик кампании: `id, name, campaign_id, created_by, created_at`.

## Контент кампании

- **`campaign_npcs`** — `id, campaign_id, name, is_visible_to_players, public_description, private_description, created_by, …`.
- **`npc_notes`** — заметки по NPC: `id, npc_id, author_id, content, …`.
- **`campaign_quests`** — `id, campaign_id, title, description, status (QuestStatus), is_visible_to_players, created_by, …`.
- **`quest_notes`** — заметки по квестам.
- **`quest_npcs`** — N×N `(quest_id, npc_id)`, уник пары.
- **`quest_locations`** — N×N `(quest_id, location_id)`, уник пары.
- **`quest_rewards`** — `id, quest_id, item_template_id?, quantity, currency_type_id?, currency_amount (numeric 15,2)`. Одна награда — это либо предмет, либо валюта.
- **`campaign_locations`** — `id, campaign_id, name, description, is_visible_to_players, created_by, …`.

## Homebrew

- **`homebrew_packages`** — пакеты-публикации: `id, author_id, parent_id (для форков), is_removable, title, description, status (HomebrewStatus), version, download_count, created_at, updated_at, published_at, deleted_at, deleted_by`.
- **`homebrew_package_tags`** — N×N пакет ↔ тег.
- **`homebrew_tags`** — `id, name (uniq)`.
- **`homebrew_ratings`** — композитный PK `(user_id, package_id)`, `rating` (-1/0/+1), `created_at`.
- **`homebrew_content_items`** — что входит в пакет: `id, package_id, content_type (ContentType enum), content_id (UUID соответствующей таблицы)`.
- **`homebrew_content_versions`** — лог изменений в версиях пакета: `id, package_id, version, content_type, content_id, change_type, changed_at`.
- **`gm_homebrew_library`** — какие пакеты добавил себе GM: композитный PK `(gm_user_id, package_id)`, `added_at`.

---

# Рабочий поток для заполнения PHB

1. **Соберите JSON-бандл.** Один большой файл со всеми разделами справа:
   ```json
   {
     "backgrounds": [ /* по шаблону */ ],
     "races": [ /* ... */ ],
     "classes": [ /* ... */ ],
     "subclasses": [ /* ... */ ],
     "spells": [ /* ... */ ],
     "feats": [ /* ... */ ],
     "itemTypes": [ /* ... */ ],
     "itemTemplates": [ /* ... */ ],
     "enchantmentTypes": [ /* ... */ ],
     "buffsDebuffs": [ /* ... */ ],
     "skills": [ /* ... */ ],
     "skillEffects": [ /* ... */ ],
     "customResourceTypes": [ /* ... */ ],
     "classLevelRewards": [ /* ... */ ],
     "itemTemplateBuffs": [ /* ... */ ]
   }
   ```

2. **Сгенерируйте Liquibase migration** (например, `036-seed-phb-full.xml`) по образцу `030-seed-wizard-data.xml`. Используйте идемпотентность: `WHERE NOT EXISTS (SELECT 1 FROM <table> WHERE name = '...')`. Имена в FK ищите подзапросом `(SELECT id FROM stat_types WHERE name = 'Strength')`.

3. **Добавьте в `master.xml`** строку `<include file="db/changelog/036-seed-phb-full.xml"/>`.

4. **Перезапустите приложение** — Liquibase прокатит миграцию автоматически.

5. **Проверьте** через `/api/reference/*` (vanilla endpoints) что данные видны без кампании.

Если предпочитаете не лить через миграцию (нужна правка на лету), часть таблиц (homebrew-контент, item templates, кастомные ресурсы) доступна через `POST /api/admin/...` — см. `AdminController.java` и связанные.
