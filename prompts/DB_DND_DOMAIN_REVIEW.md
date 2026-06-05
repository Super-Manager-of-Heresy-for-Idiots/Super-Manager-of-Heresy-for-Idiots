# Ревью: Доменная модель БД с точки зрения D&D 5e

## Оглавление
1. [Карта доменных концептов D&D → таблицы БД](#1-карта-доменных-концептов)
2. [SHOW-STOPPER: Сломанный seed — языковой рассинхрон](#2-show-stopper-seed)
3. [Skill vs Spell vs Feat — дубликаты или разные концепты?](#3-skill-vs-spell-vs-feat)
4. [Conditions → BuffDebuff → SkillEffect — эволюция одной и той же сущности](#4-conditions-buffdebuff)
5. [Artifacts → ItemTemplate — успешная консолидация](#5-artifacts-itemtemplate)
6. [Отсутствующие доменные концепты D&D 5e](#6-missing-concepts)
7. [Таблица всех обнаруженных коллизий](#7-collision-table)
8. [Рекомендации](#8-recommendations)

---

## 1. Карта доменных концептов

| Концепт D&D 5e | Правильное англ. название | Таблица в БД | Entity | Совпадает? |
|---|---|---|---|---|
| Ability Scores (STR, DEX, CON, INT, WIS, CHA) | Ability Score | `stat_types` | `StatType` | Да, но общее |
| Skills (Athletics, Stealth, Perception...) | Skill | `proficiency_skills` | `ProficiencySkill` | Да |
| Class Features (Second Wind, Sneak Attack...) | Class Feature | `skills` | `Skill` | **НЕТ — НЕПРАВИЛЬНОЕ НАЗВАНИЕ** |
| Spells (Fireball, Cure Wounds, Shield...) | Spell | `spells` | `Spell` | Да |
| Feats (Lucky, Tough, Alert...) | Feat | `feats` | `Feat` | Да |
| Classes (Fighter, Wizard, Rogue...) | Class | `character_classes` | `CharacterClass` | Да |
| Subclasses (Champion, School of Evocation...) | Subclass | `subclasses` | `Subclass` | Да |
| Races/Species (Human, Elf, Dwarf...) | Race | `character_races` | `CharacterRace` | Да |
| Subraces (High Elf, Wood Elf...) | Subrace/Lineage | JSON внутри `character_races.lineages_json` + `characters.subrace_id` | — | **Двойная реализация** |
| Backgrounds (Acolyte, Criminal...) | Background | `backgrounds` | `Background` | Да |
| Conditions (Blinded, Charmed...) | Condition | `buffs_debuffs` | `BuffDebuff` | Да (после миграции 027) |
| Equipment/Items | Item | `item_templates` / `item_instances` | `ItemTemplate` / `ItemInstance` | Да (после миграции 027) |
| Spell Slots | Spell Slot | **НЕ СУЩЕСТВУЕТ** | — | **Отсутствует** |
| Prepared Spells | Prepared Spell | **НЕ СУЩЕСТВУЕТ** | — | **Отсутствует** |
| Proficiency Bonus | Proficiency Bonus | **НЕ СУЩЕСТВУЕТ** | — | **Отсутствует (деривируется)** |
| Hit Dice (ресурс отдыха) | Hit Die | `characters.hit_dice_type` + `hit_dice_total` | поля в `PlayerCharacter` | Частично |
| Saving Throws | Saving Throw | JSON в `character_classes` + `characters` | — | **JSON, не нормализовано** |

---

## 2. SHOW-STOPPER: Сломанный seed — языковой рассинхрон

### Хронология рассинхрона

```
Миграция 002: seed stat_types — имена на РУС (СИЛ, ЛОВ, ТЕЛ, ИНТ, МДР, ХАР)
              seed character_classes — имена на РУС (Воин, Волшебник, Плут...)
              seed character_races — имена на РУС (Человек, Эльф, Дварф...)

Миграция 006: локализация → остаётся РУС (СИЛ, ЛОВ, Воин, Волшебник...)

Миграция 009: seed buffs_debuffs → ссылается на stat_types WHERE name = 'STR'
              ❌ stat_types уже на РУС → "Blessed" и "Weakened" получают NULL target_stat_id

Миграция 028: переименовывает расы обратно на ENG (Human, Elf, Dwarf...)
              НО stat_types и character_classes ОСТАЮТСЯ на РУС

Миграция 030: seed proficiency_skills → JOIN stat_types ON name = 'Strength'
              ❌ stat_types = 'СИЛ' → NO MATCH → ТАБЛИЦА ПУСТАЯ
              
              seed class metadata → WHERE name = 'Barbarian', 'Fighter'...
              ❌ Классы = 'Воин', + 4 класса вообще не существуют
              → НИ ОДИН UPDATE НЕ СРАБОТАЕТ → hit_die, saving throws, skill choices = дефолты
              
              seed spells → cc.name = ANY(ARRAY['Sorcerer','Wizard'])
              ❌ Классы на РУС → available_to_class_ids_json = NULL для всех спеллов
```

### Результат

| Данные | Ожидание | Реальность |
|---|---|---|
| `proficiency_skills` | 18 записей | **0 записей** |
| `character_classes.hit_die` | 6-12 по классу | **8 (дефолт) у всех** |
| `character_classes.saving_throw_stat_ids_json` | JSON с ID | **NULL у всех** |
| `character_classes.skill_choice_option_ids_json` | JSON с ID навыков | **NULL у всех** |
| `character_classes.is_spellcaster` | true для кастеров | **false у всех** |
| `spells.available_to_class_ids_json` | JSON с ID классов | **NULL у всех** |
| `buffs_debuffs` "Blessed".target_stat_id | ID стата СИЛ | **NULL** |
| `buffs_debuffs` "Weakened".target_stat_id | ID стата СИЛ | **NULL** |

### Недостающие классы

В seed-данных (030) обновляются 12 классов, но в БД существуют только 8:

| Класс в seed 030 | Класс в БД | Матч? |
|---|---|---|
| Barbarian | — | **НЕ СУЩЕСТВУЕТ** |
| Bard | Бард | **Имя не совпадает** |
| Cleric | Жрец | **Имя не совпадает** |
| Druid | Друид | **Имя не совпадает** |
| Fighter | Воин | **Имя не совпадает** |
| Monk | — | **НЕ СУЩЕСТВУЕТ** |
| Paladin | Паладин | **Имя не совпадает** |
| Ranger | Следопыт | **Имя не совпадает** |
| Rogue | Плут | **Имя не совпадает** |
| Sorcerer | — | **НЕ СУЩЕСТВУЕТ** |
| Warlock | — | **НЕ СУЩЕСТВУЕТ** |
| Wizard | Волшебник | **Имя не совпадает** |

**Вывод:** Весь Character Wizard feature неработоспособен на уровне данных. UI-формы будут получать пустые списки навыков, спеллов без классовых привязок, классы без 5e-метаданных.

---

## 3. Skill vs Spell vs Feat — дубликаты или разные концепты?

### Ответ: это РАЗНЫЕ доменные концепты, но `Skill` неправильно назван

#### `skills` таблица (entity: `Skill`) — это Class Features (способности класса)

Seed-данные из миграции 004:

```
"Второе дыхание"      (Second Wind)       — Воин, уровень 1, auto
"Всплеск действий"    (Action Surge)      — Воин, уровень 2, auto
"Дополнительная атака" (Extra Attack)     — Воин, уровень 5, auto
"Неукротимость"       (Indomitable)       — Воин, уровень 9, auto
"Магическое восстановление" (Arcane Recovery) — Волшебник, уровень 1, auto
"Скрытая атака"       (Sneak Attack)      — Плут, уровень 1, auto
"Хитрое действие"     (Cunning Action)    — Плут, уровень 2, auto
"Божественный канал"  (Channel Divinity)  — Жрец, уровень 2, auto
```

Это **Class Features** в терминологии D&D 5e — способности, автоматически получаемые при повышении уровня.
Они привязаны к классам через `class_level_rewards` (reward_type = 'SKILL').

#### `spells` таблица (entity: `Spell`) — это Spells (заклинания)

Seed-данные из миграции 030: Fire Bolt, Magic Missile, Fireball, Cure Wounds, etc.
Имеют уровень (0-9), школу (Evocation, Necromancy...), привязку к классам.
Это **Spells** — магические эффекты, использующие ячейки заклинаний.

#### Почему они кажутся дублями?

| Свойство | `Skill` (Class Feature) | `Spell` |
|---|---|---|
| Имя | Да | Да |
| Описание | Да | Да |
| Урон (damageDice, damageType) | Да (доб. в миграции 011) | Нет (только текст в description) |
| Эффекты (buffs/debuffs) | Да (через SkillEffect) | Нет |
| Уровень (0-9) | Нет | Да |
| Школа магии | Нет | Да |
| Привязка к классам | Через class_level_rewards | Через availableToClassIdsJson |
| homebrew_id | Да | Да |
| skillType (COMBAT/PASSIVE/UTILITY) | Да | Нет |

**Они НЕ дубликаты**, потому что моделируют разные механики:
- Class Feature = "что ты получаешь на уровне X" (progression reward)
- Spell = "что ты можешь кастовать, тратя слоты" (castable resource)

**Но проблемы есть:**

1. **Название `skills`** — в D&D "Skill" это Athletics, Perception (→ `proficiency_skills`). То, что хранится в `skills` — это Features/Abilities, не Skills.

2. **Spell не имеет механики** — `Spell` хранит только справочные данные (уровень, школа, описание). У спеллов нет damageDice, effects, или иной игромеханики. Если Fireball наносит 8d6 fire, этого нет в структуре данных.

3. **Skill имеет неиспользуемую механику** — damageDice/damageType были добавлены в миграции 011, но большинство seeded features не наносят урон. Только у "Скрытая атака" потенциально есть damage dice.

4. **Нет связи Spell ↔ Skill** — Заклинание "Haste" (из таблицы `spells`) и бафф "Hasted" (из таблицы `buffs_debuffs`) — один и тот же эффект в D&D, но они не связаны. Нет FK или ссылки между ними.

#### `feats` таблица — это Feats (черты)

Feats в D&D — это ВЫБИРАЕМЫЕ способности (вместо повышения характеристик). `Feat` entity корректен, но минималистичен (нет механики — только описание и prerequisites).

Feats привязаны к классам через `class_level_rewards` (reward_type = 'FEAT', is_choice = true).

**Feat и Skill — разные вещи:**
- Feat = выбирается на определённых уровнях (4, 8, 12, 16, 19...)
- Skill (Class Feature) = получается автоматически на определённых уровнях

Это корректное разделение в терминах D&D.

---

## 4. Conditions → BuffDebuff → SkillEffect — эволюция одной и той же сущности

### Хронология

```
Миграция 003: conditions (id, name, description)
              condition_modifiers (condition_id → stat_type_id, modifier_value)
              character_conditions (character_id, condition_id, applied_by, active)

Миграция 008: buffs_debuffs (id, name, effect_type, target_stat_id, modifier_value, 
                              duration_rounds, is_buff)
              → объединяет conditions + modifiers В ОДНУ таблицу
              → добавляет effect_type (STAT_MODIFIER, CONDITION, DAMAGE_OVER_TIME, HEAL_OVER_TIME)
              → добавляет duration_rounds и is_buff

Миграция 012: skill_effects (skill_id → buff_debuff_id, effect_role, chance_percent)
              → связывает Skill (class feature) с BuffDebuff

Миграция 021: character_active_effects
              → замена character_conditions

Миграция 027: DROP TABLE conditions, condition_modifiers, character_conditions
```

**Вывод:** `conditions` → `buffs_debuffs` — это успешная эволюция. Старые таблицы удалены, функциональность перенесена в `buffs_debuffs`. Это НЕ дублирование — это рефакторинг.

**НО:** seed в миграции 009 ссылается на `WHERE name = 'STR'` для target_stat_id, а stat_types уже переименованы в 'СИЛ'. Поэтому `Blessed.target_stat_id = NULL` и `Weakened.target_stat_id = NULL`.

---

## 5. Artifacts → ItemTemplate — успешная консолидация

### Хронология

```
Миграция 003: artifacts (name, description, item_type_id, rarity, properties, special_abilities)
              + artifact_id в inventory_slots

Миграция 019: item_templates (name, description, item_type_id, rarity, damage_dice, 
                               damage_bonus, damage_type, skill_id, is_stackable, homebrew_id)
              item_instances (template_id, owner_character_id, shared_storage_id, 
                              custom_name, quantity, slot)
              item_template_buffs (template_id → buff_debuff_id)

Миграция 027: DROP TABLE artifacts, inventory_slots
```

**Вывод:** `artifacts` → `item_templates` + `item_instances` — это успешная эволюция:
- `artifacts.properties` (text) → `item_templates` с типизированными полями damage + buffs
- `inventory_slots` → `item_instances` с flexible ownership (character или shared_storage)

**НЕ дублирование**, а замена.

---

## 6. Отсутствующие доменные концепты D&D 5e

### 6.1. Spell Slots (ячейки заклинаний) — КРИТИЧНО

В D&D 5e заклинатели имеют конечное количество ячеек заклинаний каждого уровня. Например, Волшебник 5-го уровня имеет: 4 слота 1-го, 3 слота 2-го, 2 слота 3-го уровня.

**В схеме нет:**
- Таблицы `character_spell_slots` (character_id, spell_level, total, remaining)
- Или поля в `PlayerCharacter` для хранения текущих слотов
- Логики определения количества слотов по классу и уровню

Персонаж может "знать" заклинания (через `character_known_spells`), но не может их "кастовать" — нет ресурса для траты.

### 6.2. Prepared Spells (подготовленные заклинания) — ВАЖНО

В D&D 5e классы-кастеры делятся на:
- **Known casters** (Bard, Sorcerer, Warlock, Ranger): знают фиксированный набор, всегда доступны
- **Prepared casters** (Cleric, Druid, Paladin, Wizard): знают больше, но ежедневно готовят подмножество

`character_known_spells` моделирует только "known". Нет механизма для "prepared today".

### 6.3. Ability Score Improvements (ASI) — ВАЖНО

В D&D 5e на определённых уровнях (4, 8, 12, 16, 19) персонаж может ПОВЫСИТЬ характеристики (+2 к одной или +1 к двум) ИЛИ взять feat.

В `class_level_rewards` на этих уровнях предлагаются только FEAT. Опции повышения характеристик нет. Это неполная реализация — ASI в D&D это не только feat choice.

### 6.4. Cantrip Scaling — МИНОРНО

Заговоры (cantrips) в D&D усиливаются с общим уровнем персонажа (Fire Bolt: 1d10 → 2d10 → 3d10 → 4d10). Схема не моделирует scaling.

### 6.5. Multiclass Spellcasting — МИНОРНО

При мультиклассировании количество ячеек определяется особой таблицей, а не суммой классов. Текущая схема поддерживает мультикласс (`character_class_levels`), но нет логики для расчёта ячеек мультикласса.

### 6.6. Proficiency Bonus — ДЕРИВИРУЕТСЯ

Proficiency Bonus в D&D = `floor((level - 1) / 4) + 2`. Не хранится в БД — это нормально, если вычисляется в сервисном слое. Но в текущем коде `CharacterWizardService` его нет. Он нужен для:
- Saving throws (d20 + ability mod + proficiency bonus IF proficient)
- Skill checks (d20 + ability mod + proficiency bonus IF proficient)
- Attack rolls
- Spell save DC

---

## 7. Таблица всех обнаруженных коллизий

### Концептуальные дубликаты (ОДНО И ТО ЖЕ в D&D)

| Пара | Статус | Действие |
|---|---|---|
| `conditions` (003) → `buffs_debuffs` (008) | ✅ Успешно объединены, старое удалено (027) | Ничего |
| `artifacts` (003) → `item_templates` (019) | ✅ Успешно заменены, старое удалено (027) | Ничего |
| `inventory_slots` (001) → `item_instances` (019) | ✅ Успешно заменены, старое удалено (027) | Ничего |
| `teams` (001) → `campaigns` (017) | ✅ Успешно заменены, старое удалено (027) | Ничего |

### НЕ-дубликаты, но путающие имена

| Пара | Что есть на самом деле в D&D | Действие |
|---|---|---|
| `skills` vs `proficiency_skills` | `skills` = Class Features, `proficiency_skills` = Skills | **Переименовать `skills` → `class_features`** |
| `skills` vs `spells` | `skills` = Class Features, `spells` = Spells | **НЕ объединять, это разные вещи** |
| `skills` vs `feats` | `skills` = auto-gained features, `feats` = chosen features | **НЕ объединять** |

### Перекрывающиеся, но не идентичные

| Пара | Пересечение | Рекомендация |
|---|---|---|
| `Spell` (spells) ↔ `BuffDebuff` (buffs_debuffs) | Spell "Haste" и BuffDebuff "Hasted" — один эффект | Добавить FK `spells.effect_buff_debuff_id → buffs_debuffs` |
| `Skill` (skills) ↔ `BuffDebuff` (buffs_debuffs) | Связаны через `skill_effects` | ✅ Корректно |
| `ItemType.damage*` ↔ `ItemTemplate.damage*` | Damage на двух уровнях иерархии | Документировать приоритет или убрать из одной таблицы |

### Дублирование полей/концепций ВНУТРИ одной сущности

| Сущность | Дублирование | В D&D |
|---|---|---|
| `PlayerCharacter.selectedLineageId` ↔ `PlayerCharacter.subraceId` | Две реализации подрасы | В 5e это одна вещь — выбрать подрасу. **Убрать `subrace_id`, оставить lineage** |
| `CharacterClass.savingThrowStatIdsJson` (хранит имена) ↔ `PlayerCharacter.savingThrowProficiencyStatIdsJson` (хранит имена) | Данные дублируются из класса в персонажа | В 5e saving throws определяются классом. Дублирование оправдано только для snapshot (мультикласс) |

---

## 8. Рекомендации

### ЭТАП 0 — Исправление seed-данных (БЛОКЕР)

Без этого Character Wizard не работает.

1. **Определиться с языком данных:** Либо ВСЁ на английском, либо ВСЁ на русском.

   Рекомендация: **английский** для ВСЕХ справочных данных. Локализация — на уровне фронтенда.
   - Переименовать `stat_types`: СИЛ→Strength, ЛОВ→Dexterity, ТЕЛ→Constitution, ИНТ→Intelligence, МДР→Wisdom, ХАР→Charisma
   - Переименовать `character_classes`: Воин→Fighter, Волшебник→Wizard, Плут→Rogue, Жрец→Cleric, Следопыт→Ranger, Паладин→Paladin, Бард→Bard, Друид→Druid
   - Добавить недостающие классы: Barbarian, Monk, Sorcerer, Warlock
   - Переименовать `skills` (class features): на английские названия
   - Исправить seed 009 (buffs_debuffs target_stat ссылки)

2. **Пересоздать seed 030** после языкового фикса, чтобы JOINs и WHERE clauses совпадали.

3. **Конвертировать JSON-колонки** `savingThrowStatIdsJson` и `skillChoiceOptionIdsJson` на хранение UUID (а не строковых имён), для устойчивости к переименованиям.

### ЭТАП 1 — Переименование и нормализация

1. `skills` → `class_features` (таблица, entity, ContentType, RewardType)
2. Убрать `PlayerCharacter.subrace_id` — оставить `selectedLineageId` + `raceSnapshotJson`
3. Добавить `BACKGROUND`, `SPELL`, `PROFICIENCY_SKILL` в `ContentType` enum

### ЭТАП 2 — Недостающие механики D&D

1. Создать таблицу `character_spell_slots`:
   ```
   character_id UUID FK, spell_level INT (1-9), total INT, remaining INT
   UNIQUE(character_id, spell_level)
   ```

2. Добавить `is_prepared BOOLEAN DEFAULT false` в `character_known_spells` (для prepared casters)

3. Добавить обработку ASI (Ability Score Improvement) в `class_level_rewards`:
   - Новый `RewardType.ABILITY_SCORE_IMPROVEMENT`
   - Или хранить ASI как выбор между feat и +2/+1 к характеристикам

### ЭТАП 3 — Связи между сущностями

1. Добавить FK `spells → buffs_debuffs` для связи заклинания с его механическим эффектом
2. Нормализовать `Spell.availableToClassIdsJson` → join-таблица `spell_class_availability`
3. Нормализовать `CharacterClass.savingThrowStatIdsJson` → join-таблица
4. Нормализовать `CharacterClass.skillChoiceOptionIdsJson` → join-таблица
5. Нормализовать `Background.skillProficiencyIdsJson` → join-таблица

---

## Приложение A: Хронология таблиц

```
001  users, character_classes, character_races, characters, stat_types,
     character_stats, item_types, inventory_slots, teams, team_members

003  artifacts ❌, conditions ❌, condition_modifiers ❌, character_conditions ❌

004  character_class_levels, skills, subclasses, feats,
     class_level_rewards, character_acquired_rewards

005  homebrew_packages, homebrew_content_items, homebrew_tags,
     homebrew_package_tags, homebrew_installations ❌

008  buffs_debuffs (замена conditions + condition_modifiers)

012  skill_effects

013  enchantment_types

015  inventory_enchantments ❌

016  (role rework)

017  campaigns, campaign_members

018  (migrate team → campaign)

019  shared_storage, item_templates (замена artifacts), item_instances (замена inventory_slots),
     item_template_buffs

020  item_enchantments

021  character_active_effects (замена character_conditions)

022  character_wallets, character_resources, custom_resource_types, currency_types

023  (character status, HP)

024  campaign_npcs, npc_notes, campaign_quests, quest_notes, quest_rewards,
     quest_npcs, quest_locations, campaign_locations, gm_session_notes

025  homebrew_content_versions, homebrew_ratings, gm_homebrew_library,
     campaign_homebrew

027  DROP: inventory_enchantments, bag_slots, character_conditions,
     condition_modifiers, inventory_slots, artifacts, conditions,
     team_homebrew_activations, team_members, homebrew_installations, teams

028  (extend character_races with rich fields, rename to English)

029  proficiency_skills, backgrounds, spells,
     character_skill_proficiencies, character_known_spells
     (extend character_classes with 5e metadata)
     (extend characters with wizard fields)

030  seed: proficiency_skills ❌, backgrounds, class metadata ❌, spells (partial ❌)
```

Таблицы, помеченные ❌ — удалены в последующих миграциях или не заполнены корректно из-за рассинхрона имён.
