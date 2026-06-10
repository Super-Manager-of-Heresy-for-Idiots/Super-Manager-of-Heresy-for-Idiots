# Инструкция: подготовка миграции бестиария (601 монстр)

Цель: на основе `DB_SCHEMA_PROPOSAL.md` и датасета (601 монстр) сгенерировать
Liquibase-миграцию для приложения. Документ-предложение писался «в вакууме» под
абстрактный PostgreSQL и **конфликтует с реальной схемой приложения по ряду пунктов**.
Ниже — что нужно поправить ПЕРЕД генерацией и как именно готовить миграцию.

Источник истины по существующей схеме: `DB_SCHEMA_AND_SEED.md` и
`src/main/resources/db/changelog/*.xml` (миграции 001–039). JPA-сущности —
`src/main/java/com/dnd/app/domain/`.

---

## 1. Конфликты с приложением (обязательно учесть)

### 1.1 Типы PK: UUID, а не BIGINT  ❗главный конфликт
Всё приложение использует `uuid` PK с `gen_random_uuid()` (расширение pgcrypto уже
подключено). Предложение везде пишет `BIGINT` и «id из источника».

**Решение:**
- Все PK новых таблиц — `type="uuid"`, значение `gen_random_uuid()`.
- Числовой id монстра из датасета НЕ делать первичным ключом. Хранить его как
  `source_external_id BIGINT` (UNIQUE, nullable) в `monsters` — для идемпотентного
  повторного импорта и отладки.
- `campaign_id` и `homebrew_id` ОБЯЗАНЫ быть `uuid` (FK на `campaigns(id)` и
  `homebrew_packages(id)`), иначе FK не соберётся. В предложении они BIGINT — это ошибка.

### 1.2 Только Liquibase XML, не сырой SQL
Миграции — это XML changeSet'ы в `src/main/resources/db/changelog/`, подключаемые в
`master.xml`. Последняя занятая — `039`. Новые файлы начинать с **040**.
- Один логичный шаг — один файл: например `040-create-bestiary-schema.xml`
  (DDL — createTable) и `041-seed-bestiary.xml` (данные). Можно дробить мельче.
- В конец `master.xml` добавить `<include file="db/changelog/040-...xml"/>` и т.д.
- Внутри changeSet данные лить через `<sql> INSERT ... SELECT ... FROM (VALUES ...) </sql>`
  по образцу `030-seed-wizard-data.xml`.
- Идемпотентность: `WHERE NOT EXISTS (SELECT 1 FROM <table> WHERE ...)`.
- FK по значению искать подзапросом: `(SELECT id FROM creature_types WHERE code = 'dragon')`.
- `created_at`/`updated_at` Liquibase НЕ проставит сам (Hibernate-аннотации не работают
  на сыром INSERT) — задавать явно `now()` или `defaultValueComputed="now()"` в колонке.

### 1.3 Коллизии имён таблиц и дублирование справочников
Несколько справочников из предложения УЖЕ существуют — нельзя создавать заново,
нужно переиспользовать:

| В предложении | Реальность в приложении | Что делать |
|---|---|---|
| `skills` (Восприятие, Скрытность…) | Таблица `skills` УЖЕ занята — это активные способности классов (Rage, Action Surge). Навыки-профессии лежат в `proficiency_skills` (18 шт.) | НЕ создавать `skills`. Использовать существующую `proficiency_skills`. Связку назвать `monster_skill_proficiencies` (FK→`proficiency_skills`). 17 навыков монстров = подмножество 18 PHB. |
| `conditions` (15 состояний) | Таблица `conditions` УЖЕ существует (миграция 003) | НЕ создавать. Переиспользовать. Внимание: у неё `created_by uuid NOT NULL` и нет `homebrew_id` — посмотреть, как 003 сеет системные строки, и какие из 15 уже есть (по `name`). Связку `monster_condition_immunities` вешать FK→существующей `conditions`. |
| `items` (снаряжение) | Есть `item_types` и `item_templates` | По возможности переиспользовать `item_templates` (или `item_types`) для снаряжения монстров вместо новой таблицы `items`. Если решено вводить отдельный лёгкий справочник — назвать иначе (напр. `monster_gear_items`), чтобы не путать. |
| `damage_types` (~14) | Это enum `DamageType` (13 значений: SLASHING, PIERCING, BLUDGEONING, FIRE, COLD, LIGHTNING, POISON, NECROTIC, RADIANT, PSYCHIC, FORCE, THUNDER, ACID) | НЕ создавать справочник. В связках урона/сопротивлений хранить `damage_type varchar(20)` со значением enum-константы. «Звук»→`THUNDER`, «Силовое»→`FORCE`. 4 мусорных значения (заметка 2) — в `note`, не в палитру. |
| `sizes` (6) | Это enum `CreatureSize` (TINY/SMALL/MEDIUM/LARGE/HUGE/GARGANTUAN) | Размер монстра — колонка `size varchar(20)` (enum-константа). Двойной размер у 77 монстров — либо вторая nullable-колонка `size_secondary`, либо связка `monster_sizes` с `size varchar(20)`. Отдельную таблицу-справочник `sizes` не плодить. |
| `abilities` (6) | Есть enum `Ability` (STRENGTH…CHARISMA) и таблица `stat_types` (6 строк) | Для `save_ability`, спасбросков и т.п. использовать `ability varchar(20)` (enum `Ability`) ИЛИ FK→`stat_types`. Выбрать одно; рекомендация — varchar(`Ability`), это легче и совпадает с тем, как монстр оперирует характеристиками. Сами 6 значений str/dex/... монстра — это скаляры `*_score` в `monsters`, не справочник. |
| `alignments` (~10) | На персонаже `alignment varchar(40)` (свободная строка), отдельной таблицы нет | Можно завести нормализованный `alignments` (10 канонических, заметка 1: свернуть 26 склоняемых форм→10) — конфликта нет, но это новый паттерн. Альтернатива в духе приложения — `monsters.alignment varchar(40)`. Решение за продуктом (см. §2). |

Справочники, которых в приложении НЕТ и которые создаём как новые (конфликта нет):
`creature_types`, `languages`, `sense_types`, `movement_types`, `habitats`,
`treasure_tags`, `sources`, `feature_sections` (последний лучше как `varchar`-enum,
а не таблица). Плюс все `monsters*`, `monster_features`, `feature_damages`.

### 1.4 Модель владения (scope) расходится с приложением
Предложение вводит триаду `scope` = core/campaign/homebrew + `campaign_id` + `homebrew_id`
на КАЖДОМ справочнике. В приложении этого нет:
- Справочный/системный контент маркируется `homebrew_id uuid NULL` (NULL = системный/«core»),
  где-то дополнительно `is_default bool` или `source_type = 'SYSTEM'`.
- **`campaign_id` на справочниках в приложении не используется вообще.**
- Homebrew-контент дополнительно трекается через `homebrew_content_items
  (content_type ContentType enum, content_id uuid)` и enum `ContentType`.

**Рекомендация (минимальные расхождения с приложением):**
- Импортируемые 601 монстр и сиды справочников = «core» → `homebrew_id NULL`.
  Колонку `scope`/`campaign_id` НЕ добавлять, если продукту реально не нужны
  монстры, локальные для одной кампании.
- Если homebrew-монстры планируются — добавить значение `MONSTER` (и при нужде
  `MONSTER_*` для справочников) в enum `ContentType`, а на `monsters` — `homebrew_id uuid NULL`
  как на остальном контенте. Это требует правки Java-enum, не только миграции.
- Поля `usage_count` / `is_unique` / `hidden_in_constructor` (раздел 0 предложения) —
  чисто под UI-конструктор; в приложении такого механизма нет. Либо отложить, либо
  заводить осознанно (тогда нужен и код поддержки счётчика). По умолчанию — НЕ тащить
  в первую миграцию.

➡️ Этот пункт требует продуктового решения — см. §2, вопрос 1.

---

## 2. Что нужно решить ДО генерации (передать вместе с датасетом)

1. **Нужны ли монстры/справочники, привязанные к кампании или homebrew?**
   - Вариант A (просто): только системные (`homebrew_id NULL`), без `campaign_id`/`scope`.
   - Вариант B (полноценно): `homebrew_id uuid NULL` на монстрах + `MONSTER` в `ContentType`.
   - Вариант C: плюс `campaign_id` (новый паттерн, больше всего работы).
   Рекомендация: A для первой итерации, заложить B на будущее.
2. **`alignment`**: нормализованный справочник `alignments` или `varchar(40)` на `monsters`?
   (Рекомендация — справочник, раз конструктор строится на палитрах.)
3. **Снаряжение**: переиспользовать `item_templates` или новый `monster_gear_items`?
4. **`feature_sections`**: enum-строка `varchar` (рекомендуется) или таблица-справочник?
5. **`usage_count`/`is_unique`**: включаем сейчас или откладываем? (Рекомендация — отложить.)

---

## 3. Как готовить датасет к миграции (нормализация)

Перед генерацией INSERT'ов привести данные в порядок (заметки E из предложения):

1. **Alignment**: свернуть 26 склоняемых русских форм → 10 канонических кодов
   (`lawful_good`, …, `unaligned`). Род существа отбросить.
2. **Типы урона**: канонизировать к enum `DamageType`. «Силовой»→`FORCE`, «Звук»→`THUNDER`
   и т.д. 4 мусорных значения («тип урона», «см. Живая тень»,
   «определяемый Драконьим происхождением», «Колющий от оружия…») — в `note` связки
   или отбросить, НЕ в палитру.
3. **Тип существа**: 15 канонических + `is_swarm`/`swarm_size`. Рой («рой …») →
   `is_swarm=true`. Редкий «X или Y» → 2 строки в `monster_creature_types`.
4. **Размер**: двойной размер (77 монстров) → 2 строки в `monster_sizes` (или
   `size`+`size_secondary`).
5. **Сокровища**: составные строки («Личные , Инструментальные») разбить на 7 тегов
   → `treasure_tags` + `monster_treasures`.
6. **Сопротивления/иммунитеты/уязвимости**: исключения-оговорки писать в `note` на связке.
7. **Числовые поля HP/CR**: `cr_value NUMERIC(5,3)` для сортировки (1/8→0.125),
   `cr_rating` — текст оригинала. HP разбить на average/dice/formula как в предложении.
8. **Действия** (`monster_features`): сохранить `section`, `sort_order`, `kind`,
   recharge_min/max, attack/save поля; урон действия → `feature_damages` (среднее+кубики+тип).
   `save_ability` хранить как `varchar`(`Ability`) либо FK→`stat_types` (см. §1.3).
9. Технический мусор (`url`, `_source_file`, `_parse_errors`) НЕ импортировать.
10. Заполнить `source_external_id` числовым id монстра из датасета.

---

## 4. Порядок генерации (FK-зависимости)

1. `040-create-bestiary-schema.xml` — DDL всех НОВЫХ таблиц (creature_types, languages,
   sense_types, movement_types, habitats, treasure_tags, sources, alignments [если выбран],
   monsters, все monster_* связки, monster_features, feature_damages). PK uuid.
   FK связок → `monsters(id) ON DELETE CASCADE`. Переиспользуемые таблицы
   (`proficiency_skills`, `conditions`, `item_templates`) НЕ создавать — только FK на них.
2. `041-seed-bestiary-dictionaries.xml` — сиды новых справочников (alignments 10,
   languages 96, habitats 31, sources 14, creature_types 15, treasure_tags 7,
   sense_types 4, movement_types 5). Идемпотентно по `code`/`name`.
3. `042-seed-bestiary-monsters.xml` — 601 монстр + все связки + features + damages.
   FK резолвить подзапросами по `code`/`name`/enum-значению.
4. Добавить все три `<include>` в `master.xml` в порядке номеров.
5. (Если выбран homebrew-вариант B) — отдельным шагом правка Java-enum `ContentType`
   (+`MONSTER`) и соответствующей сущности `Monster`. Это вне Liquibase.

После прогона проверить, что приложение стартует (Liquibase катит миграции на старте)
и данные доступны.

---

## 5. Чек-лист «не сломать»
- [ ] Все PK — `uuid`/`gen_random_uuid()`, не BIGINT.
- [ ] `campaign_id`/`homebrew_id` — `uuid`, FK на реальные таблицы.
- [ ] Не пересоздаются `skills`, `conditions`, `item_templates`, `proficiency_skills`.
- [ ] `damage_types`/`sizes`/`abilities` НЕ заводятся как таблицы — это enum-varchar.
- [ ] Файлы начинаются с 040, добавлены в `master.xml`.
- [ ] `created_at`/`updated_at` заданы явно (`now()`).
- [ ] Все INSERT идемпотентны (`WHERE NOT EXISTS`).
- [ ] Технический мусор и 4 мусорных типа урона не попали в палитры.
