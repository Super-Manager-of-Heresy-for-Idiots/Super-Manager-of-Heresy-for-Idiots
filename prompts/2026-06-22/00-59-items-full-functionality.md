# FE: Полный функционал предметов (любого типа)

Реализуй на фронтенде весь функционал работы с предметами. В системе **два независимых пласта** предметов — не путай их:

1. **Справочный каталог (content / reference)** — «энциклопедия» предметов из правил (D&D 2024 PHB) + хоумбрю. Только чтение. Это снаряжение и магические предметы как описания в книге.
2. **Игровой рантайм (runtime)** — шаблоны предметов, конкретные экземпляры в инвентаре персонажа, экипировка, зачарования, общие хранилища. Здесь происходит вся игровая механика (выдача, экипировка, передача и т.д.).

Все ответы обёрнуты в `ApiResponse<T>`: `{ success, message, data }`. Авторизация — Bearer JWT. Везде `?lang=en|ru` (по умолчанию `en`) где указано.

---

## 1. Справочный каталог (read-only)

### 1.1 Снаряжение (EquipmentItem) — оружие, броня, снаряжение, инструменты
Единая сущность с полем `kind` = `weapon | armor | gear | tool | ...`. У оружия заполнен блок `weaponStat` + `weaponProperties`, у брони — `armorStat`.

Эндпоинты:
- `GET /api/reference/equipment` (= `/api/reference/content/equipment`) — список ванильного снаряжения.
- `GET /api/reference/equipment/{equipmentItemId}` — один предмет.
- `GET /api/campaigns/{campaignId}/reference/equipment` — список, видимый в кампании (ядро + активный хоумбрю).
- `GET /api/campaigns/{campaignId}/reference/equipment/{equipmentItemId}` — один предмет в контексте кампании.

`EquipmentItemDetail`:
- `id, slug, name, nameRu, nameEn, kind`
- `category` (`{id,label}`), `cost` (`{amount, currency{label}, copperValue, rawText}`), `weightLb`, `propertiesText`, `url`
- `packageId` — id хоумбрю-пакета (null = ядро). Показывай бейдж «Homebrew».
- `weaponStat` (только оружие): `damageDice{diceCount,dieSize,bonus,rawText}`, `damageType{label}`, `flatDamage`, `mastery{label}`.
- `armorStat` (только броня): `baseAc, dexBonusAllowed, maxDexBonus, strengthRequired, stealthDisadvantage, armorClassRaw`.
- `weaponProperties[]`: `property{label}, normalRangeFt, longRangeFt, versatileDice, ammunitionEquipmentItemId, rawText`.

UI: каталог с фильтрами по `kind`, категории, источнику (ядро/хоумбрю); карточка-деталь с условным рендерингом блоков оружия/брони.

### 1.2 Магические предметы (MagicItem)
- `GET /api/reference/magic-items` (= `/api/reference/content/magic-items`)
- `GET /api/reference/magic-items/{magicItemId}`
- `GET /api/campaigns/{campaignId}/reference/magic-items`
- `GET /api/campaigns/{campaignId}/reference/magic-items/{magicItemId}`

`MagicItemDetail`:
- `id, slug, name, nameRu, nameEn`
- `type{label}`, `typeRestrictionRaw`, `rarity{label}`, `variableRarity`
- `attunementRequired`, `attunementRequirement`
- `cost`, `description`, `embeddedTablesDetected`, `url`, `packageId`
- `allowedEquipment[]`: `{equipment{label}, rawText}` — базовое снаряжение, к которому применим магический предмет.

UI: фильтры по типу, редкости, требованию настройки (attunement); карточка с описанием и редкостью (цветовой бейдж).

### 1.3 Справочники для дропдаунов (используй при создании/фильтрации предметов)
- `GET /api/reference/rarities?lang=` — редкости (`{id,label}`).
- `GET /api/reference/damage-types?lang=` — типы урона.
- `GET /api/reference/currencies?lang=` — валюты.
- Слоты экипировки — фиксированный системный набор кодов (см. ниже).

---

## 2. Игровой рантайм

### 2.1 Типы предметов (ItemType) — словарь
Определяет слот, базовый урон, привязку умения. Используется как «класс» предмета при создании шаблонов.

Admin CRUD:
- `GET /api/admin/item-types`
- `POST /api/admin/item-types` (body `CreateItemTypeRequest`)
- `GET /api/admin/item-types/{id}`
- `PUT /api/admin/item-types/{id}`
- `DELETE /api/admin/item-types/{id}`

Хоумбрю-вариант: `POST /api/homebrew/my/{packageId}/content/item-types`.

`ItemTypeResponse`: `id, name, description, slot, damageDice, damageBonus, damageType, skillId, skillName, skillActivation`.

### 2.2 Шаблоны предметов (ItemTemplate) — «чертёж» конкретного предмета
То, что GM выдаёт игрокам. Создаётся Admin/GM.

- `POST /api/item-templates` (Admin/GM) — body `CreateItemTemplateRequest`.
- `GET /api/item-templates/{id}`
- `GET /api/item-templates/campaign/{campaignId}` — доступные в кампании шаблоны.
- `PUT /api/item-templates/{id}`
- `DELETE /api/item-templates/{id}` (Admin).

`CreateItemTemplateRequest`: `name*` (≤100), `description`, `itemTypeId`, `rarity`, `damageDice`, `damageBonus`, `damageType`, `isStackable`, `skillId`, `skillActivation`.

`ItemTemplateResponse`: `id, name, description, itemTypeName, rarity, damageDice, damageBonus, damageType, isStackable, skillName, skillActivation, sourceHomebrewTitle`.

UI: форма создания/редактирования шаблона (поля выше; itemType/skill/damageType/rarity — из справочников); список шаблонов кампании для выбора при выдаче.

### 2.3 Инвентарь персонажа (ItemInstance) — конкретные экземпляры
Базовый путь: `/api/campaigns/{campaignId}/characters/{characterId}/inventory`.

- `GET .../inventory` — все предметы.
- `GET .../inventory/equipped` — только надетые (есть слот).
- `GET .../inventory/backpack` — только в рюкзаке (без слота).
- `POST .../inventory` (GM) — выдать предмет. Body `GrantItemRequest`: `templateId*`, `quantity` (≥1, по умолч. 1), `customName`, `isUnique`. Стакающиеся предметы (`isStackable`) без кастомного имени складываются в существующий стек.
- `POST .../inventory/{instanceId}/equip` — надеть. Body `EquipItemRequest`: `slot*` (код слота). Ошибка 400, если слот занят.
- `POST .../inventory/{instanceId}/unequip` — снять.
- `DELETE .../inventory/{instanceId}` (GM) — удалить/уменьшить количество (стек уменьшается на 1).
- `POST .../{fromCharId}/inventory/{instanceId}/transfer` — передать другому персонажу той же кампании. Body `TransferItemRequest`: `toCharacterId`. Экипированный предмет передать нельзя (сначала снять).
- `PUT .../inventory/{instanceId}/rename` — переименовать. Body `RenameItemRequest`: `customName`, `renameEntireStack`. Если стек >1 и `renameEntireStack=false` — предмет отделяется в новый экземпляр (qty=1) с кастомным именем.

`ItemInstanceResponse`: `id, templateId, templateName, displayName, customName, quantity, isUnique, slot, notes, rarity, enchantments[]`. `displayName` = `customName ?? templateName`.

Права: просмотр — члены кампании; equip/unequip/rename — владелец/GM/Admin; grant/remove — GM/Admin; transfer — владелец источника/GM/Admin.

**Слоты экипировки (системные коды):** `HEAD, CHEST, LEGS, FEET, MAIN_HAND, OFF_HAND, RING_LEFT, RING_RIGHT, NECK, CLOAK`. API возвращает/принимает `code`.

При надевании предмета автоматически применяются баффы шаблона как активные эффекты персонажа; при снятии — снимаются. Учитывай это при отображении статов.

UI: вкладка инвентаря с разделением «надето / рюкзак», drag-and-drop или кнопки в слоты (paper-doll), действия выдать/удалить/передать/переименовать, индикатор количества для стеков, бейдж редкости и списка зачарований.

Реал-тайм: события по WebSocket — `ITEM_GRANTED`, `ITEM_REMOVED` (обновляй инвентарь у всех в кампании).

### 2.4 Зачарования предметов (Enchantment)
Накладываются на конкретный экземпляр.
Базовый путь: `/api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/enchantments`.

- `GET ...` — зачарования экземпляра.
- `POST ...` — наложить. Body `AddEnchantmentRequest`: `enchantmentTypeId*`, `notes` (≤255).
- `DELETE .../{enchantmentId}` — снять.

`EnchantmentResponse`: `id, enchantmentType{...}, appliedAt, notes`.

Словарь типов зачарований:
- `GET /api/enchantment-types` — публичный список (для выбора при наложении).
- Admin CRUD: `GET/POST /api/admin/enchantment-types`, `GET/PUT/DELETE /api/admin/enchantment-types/{id}`. Body `CreateEnchantmentTypeRequest`.

`EnchantmentTypeResponse`: `id, name, description, damageDice, damageBonus, damageType, buffDebuff{...}`.

UI: на карточке экземпляра — список зачарований + кнопка «Наложить» (выбор типа из `/api/enchantment-types` + заметка), кнопка снять.

### 2.5 Общие хранилища (SharedStorage)
Контейнеры кампании, куда можно складывать/забирать предметы.
Базовый путь: `/api/campaigns/{campaignId}/shared-storage`.

- `POST ...` (GM) — создать. Body `CreateSharedStorageRequest`.
- `GET ...` — список хранилищ.
- `GET .../{storageId}` — хранилище с предметами.
- `DELETE .../{storageId}` (GM) — удалить.
- `POST .../{storageId}/items/{instanceId}/deposit?quantity=` — положить предмет.
- `POST .../{storageId}/items/{instanceId}/take/{characterId}?quantity=` — забрать предмет персонажу.

`SharedStorageResponse`: `id, name, campaignId, items[] (ItemInstanceResponse), createdAt`.

UI: общий «сундук» кампании, перетаскивание между инвентарём персонажа и хранилищем, контроль количества для стеков.

---

## Замечания по реализации
- Различай **каталог** (описания из правил, read-only, `slug`/`url`/`nameRu`/`nameEn`) и **рантайм** (то, чем владеет персонаж). Шаблоны/экземпляры не имеют локализованных name-полей — у них один `name`/`customName`.
- Редкость отображай цветовым бейджем по общему словарю редкостей.
- Бейдж «Homebrew» по `packageId`/`sourceHomebrewTitle`.
- Везде обрабатывай 400/403/404 из `ApiResponse.message` (часть сообщений уже на русском).
