# Аудит покрытия сущностей API и доработка эндпоинтов — 2026-06-18

Цель: устранить два класса проблем, найденных при ревью контроллеров.
1. Контроллер требует лишний контекст (например `campaignId` там, где он не нужен).
2. Сущность БД нужна фронту, но её негде взять (нет ни отдельного, ни вложенного эндпоинта).

Важный принцип (от заказчика): **не каждую сущность нужно отдавать отдельным эндпоинтом.**
Часть данных правильнее отдавать вложенно внутри родителя (комплексный ответ), часть —
отдельным справочным списком, а часть — и так, и так. Ниже зафиксирована конкретная
форма для каждого случая.

---

## 0. Терминология

- **Standalone (отдельный)** — `GET /…/<dictionary>` возвращает список справочника
  (для наполнения выпадашек в формах создания/редактирования).
- **Composite (комплексный/вложенный)** — справочник приезжает уже внутри родителя
  (например предмет вместе со своим `rarity`), отдельный запрос не нужен.
- Многие словари нужны в **обеих** формах: standalone — для формы выбора, composite —
  для отображения готовой сущности.

Форма standalone-ответа для словарей: `ContentLabelDto { id, slug, name, nameRu, nameEn }`
(уже используется для abilities/feats).

---

## 1. `/backgrounds` требует campaignId — это НЕ баг бэкенда

Reference-данные продублированы для двух режимов, и обе ветки полные:

| Режим | Базовый путь |
|---|---|
| Вне кампании (шаблоны) | `GET /api/reference/*` — races, backgrounds, skills, stat-types, currencies, spells, abilities, feats; классы `GET /api/reference/classes`; создание `POST /api/characters/full`; левел-ап `…/characters/{id}/level-up*` |
| Внутри кампании | `GET /api/campaigns/{id}/reference/*` + `POST /api/campaigns/{id}/characters/full` |

`GET /api/reference/backgrounds` без campaignId **существует** (`VanillaReferenceController`).
**Действие:** правка фронта — при создании шаблона звать `/api/reference/*`, а не
campaign-scoped вариант. Бэкенд менять не нужно.

---

## 2. Справочники-словари без чтения по API (главная дыра)

Это реальные FK-справочники: в запросах авторинга задаются по slug/имени, но списка
допустимых значений фронту негде взять. Composite-форма (slug строкой внутри родителя)
уже есть — не хватает именно standalone-списка для выпадашек.

| Сущность / таблица | Используется в | Composite (уже есть) | Standalone | Решение |
|---|---|---|---|---|
| `Rarity` / `magic_item_rarity` | создание `ItemTemplate` | `ItemTemplateResponse.rarity` (slug) | **нет** | добавить `GET /api/reference/rarities` |
| `DamageType` / `damage_type` (PHB) | ItemTemplate, ItemType, Skill, Spell, EnchantmentType | `*.damageType` (slug) | **нет**¹ | добавить `GET /api/reference/damage-types` |
| `SpellSchool` / `spell_school` | Spell, ClassLevelRewardGrantSpell | `SpellResponse.school` (slug) | **нет**² | добавить `GET /api/reference/spell-schools` |
| `CreatureSize` / `character_size` | размеры рас/персонажа | — | **нет**³ | добавить `GET /api/reference/sizes` |

¹ `GET /api/bestiary/dictionaries/damage-types` отдаёт ДРУГУЮ таблицу `bestiary_damage_types`, не PHB-словарь.
² фильтр `GET …/spells?school=` принимает свободную строку; списка школ нет.
³ `GET /api/bestiary/dictionaries/sizes` отдаёт `bestiary_sizes`, не `character_size`.

---

## 3. Отдаётся, но через «чужой» путь (работает, низкий приоритет)

| Сущность | Текущий доступ | Замечание |
|---|---|---|
| `EquipmentSlot` / `equipment_slots` | `GET /api/bestiary/dictionaries/equipment-slots` | нужен предметам, а лежит под бестиарием — фронту неочевидно. Опционально: алиас `GET /api/reference/equipment-slots`. |
| `Alignment` / `alignments` | `GET /api/bestiary/dictionaries/alignments` | используется только монстрами — оставляем как есть. |

---

## 4. Сущности, которым отдельный эндпоинт НЕ нужен (только composite)

Подтверждаем сознательно, чтобы не плодить лишние маршруты:
- Под-таблицы наград класса (`class_level_reward_*`, `character_reward_*`) — отдаются
  вложенно в `level-up-options` / detail класса.
- Под-таблицы монстров (`monster_*`) — отдаются внутри ответа монстра.
- Кошелёк/ресурсы/эффекты/инвентарь персонажа — отдаются под `…/characters/{id}/…`.

---

## 5. План доработки (инкрементально)

- [x] **Шаг 1.** Standalone reference-эндпоинты для 4 непокрытых словарей:
      `rarities`, `damage-types`, `spell-schools`, `sizes` под `/api/reference/*`,
      форма `List<ContentLabelDto>`.
- [ ] **Шаг 2.** (опц.) Алиас `GET /api/reference/equipment-slots` для предметного авторинга.
- [ ] **Шаг 3.** (опц.) Campaign-scoped варианты тех же словарей (если homebrew-rarity/damage станут нужны в кампании).

---

## 6. Журнал выполненного

### Шаг 1 — standalone-словари (2026-06-18) — ГОТОВО (компиляция зелёная)
Добавлены 4 read-only эндпоинта, форма `List<ContentLabelDto>`:
- `GET /api/reference/rarities`
- `GET /api/reference/damage-types`
- `GET /api/reference/spell-schools`
- `GET /api/reference/sizes`

Изменения:
- `RarityRepository`, `DamageTypeRepository`, `CreatureSizeRepository` — добавлен list-метод vanilla (`findByHomebrewIsNull…OrderBy…`).
- `SpellSchoolRepository` — создан (раньше отсутствовал), `findAllByOrderByNameRuAsc()`.
- `ReferenceDataService` — 4 метода `getRarities/getDamageTypes/getSpellSchools/getSizes` + хелпер `label(...)`.
- `VanillaReferenceController` — 4 GET-маршрута.

Ничего не ломает: только чтение системных (homebrew IS NULL) словарей; composite-формы внутри предметов/заклинаний не тронуты.
