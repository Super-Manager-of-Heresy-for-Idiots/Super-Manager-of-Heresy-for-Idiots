# Content Catalog API — новые read-only эндпоинты (PHB 2024)

Документ для FE-разработчика. Все эндпоинты ниже — **только чтение** (GET). Это нормализованная
модель контента D&D 2024: feats, spells, backgrounds, equipment, magic items. CRUD/редактирование
сюда не входит — оно остаётся в homebrew-authoring и здесь не описано.

---

## 1. Общие правила (читать обязательно)

### Конверт ответа
Любой ответ обёрнут в `ApiResponse<T>`:
```jsonc
{
  "success": true,
  "data":  /* T — объект или массив */,
  "message": null,   // присутствует не всегда
  "error":   null,   // заполняется только при ошибке
  "fields":  null    // только для VALIDATION_ERROR
}
```
Поля со значением `null` могут отсутствовать в JSON (сериализация `NON_NULL`). Полезную нагрузку
всегда брать из `response.data`.

### Локализация
Все эндпоинты принимают query-параметр `?lang=ru|en` (по умолчанию `en`).
- `name` в ответе — уже выбранная под локаль строка.
- `nameRu` / `nameEn` отдаются дополнительно, если нужно показать оба варианта.

### Два варианта каждого ресурса
| Вариант | Путь | Что видит | Аутентификация |
|---|---|---|---|
| **Vanilla (core)** | `/api/reference/...` | только базовый контент (`homebrew_id IS NULL`) | не требуется* |
| **Campaign-aware** | `/api/campaigns/{campaignId}/reference/...` | базовый контент + активные homebrew-пакеты кампании | требуется (член кампании или админ) |

\* по фактическим настройкам Security; если глобально стоит auth — заголовок всё равно нужен.

У каждого пути есть алиас с `/content/` в середине — они эквивалентны, используйте любой:
`/api/reference/feats` ≡ `/api/reference/content/feats`.

### Поле `packageId`
В каждом detail-объекте есть `packageId`:
- `null` → это базовый (core) контент;
- UUID → контент принадлежит homebrew-пакету.

### Поведение 404
Если объект существует, но **не виден** в данном контексте (например, запросили homebrew-сущность
через vanilla-эндпоинт, или пакет не активирован в кампании) — вернётся **404**, как будто объекта нет.
Это by design.

### Базовый URL
Все пути относительны к корню API. Заголовок аутентификации (для campaign-вариантов) подставляйте
своим обычным механизмом (Bearer / cookie).

---

## 2. Сводная таблица эндпоинтов

Для каждого ресурса 4 эндпоинта. `{...}` — path-параметры. Везде доступен `?lang=`.

### Feats
| Метод | Путь | Возвращает |
|---|---|---|
| GET | `/api/reference/feats` | `FeatDetail[]` |
| GET | `/api/reference/feats/{featId}` | `FeatDetail` |
| GET | `/api/campaigns/{campaignId}/reference/feats` | `FeatDetail[]` |
| GET | `/api/campaigns/{campaignId}/reference/feats/{featId}` | `FeatDetail` |

### Spells
| Метод | Путь | Возвращает |
|---|---|---|
| GET | `/api/reference/spells` | `SpellDetail[]` |
| GET | `/api/reference/spells/{spellId}` | `SpellDetail` |
| GET | `/api/campaigns/{campaignId}/reference/spells` | `SpellDetail[]` |
| GET | `/api/campaigns/{campaignId}/reference/spells/{spellId}` | `SpellDetail` |

### Backgrounds
| Метод | Путь | Возвращает |
|---|---|---|
| GET | `/api/reference/backgrounds` | `BackgroundDetail[]` |
| GET | `/api/reference/backgrounds/{backgroundId}` | `BackgroundDetail` |
| GET | `/api/campaigns/{campaignId}/reference/backgrounds` | `BackgroundDetail[]` |
| GET | `/api/campaigns/{campaignId}/reference/backgrounds/{backgroundId}` | `BackgroundDetail` |

### Equipment (обычные предметы: оружие, броня, снаряжение)
| Метод | Путь | Возвращает |
|---|---|---|
| GET | `/api/reference/equipment` | `EquipmentItemDetail[]` |
| GET | `/api/reference/equipment/{equipmentItemId}` | `EquipmentItemDetail` |
| GET | `/api/campaigns/{campaignId}/reference/equipment` | `EquipmentItemDetail[]` |
| GET | `/api/campaigns/{campaignId}/reference/equipment/{equipmentItemId}` | `EquipmentItemDetail` |

### Magic Items
| Метод | Путь | Возвращает |
|---|---|---|
| GET | `/api/reference/magic-items` | `MagicItemDetail[]` |
| GET | `/api/reference/magic-items/{magicItemId}` | `MagicItemDetail` |
| GET | `/api/campaigns/{campaignId}/reference/magic-items` | `MagicItemDetail[]` |
| GET | `/api/campaigns/{campaignId}/reference/magic-items/{magicItemId}` | `MagicItemDetail` |

---

## 3. TypeScript-типы

```ts
// ---------- общее ----------
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string | null;
  error?: string | null;
  fields?: Record<string, string> | null;
}

/** Лёгкая ссылка-ярлык на любую справочную сущность (для дропдаунов, inline-ссылок). */
export interface ContentLabel {
  id: string;          // UUID
  slug: string;
  name: string;        // выбран под ?lang=
  nameRu: string;
  nameEn: string | null;
}

// ---------- Feat ----------
export interface FeatPrerequisite {
  type: string | null;          // напр. "ability_score", "level"
  levelRequired: number | null;
  abilityScore: ContentLabel | null;
  minimumScore: number | null;
  groupKey: string | null;      // для "одно из" групп
  rawText: string | null;
}
export interface FeatSection {
  title: string | null;
  body: string | null;
}
export interface FeatDetail {
  id: string;
  slug: string;
  name: string;
  nameRu: string;
  nameEn: string | null;
  description: string | null;
  repeatable: boolean | null;
  packageId: string | null;
  category: ContentLabel | null;       // origin / general / fighting-style / ...
  prerequisites: FeatPrerequisite[];
  sections: FeatSection[];
}

// ---------- Spell ----------
export interface SpellComponent {
  component: string | null;   // "verbal" | "somatic" | "material"
  materialText: string | null;
  consumed: boolean | null;
}
export interface SpellDetail {
  id: string;
  slug: string;
  name: string;
  nameRu: string;
  nameEn: string | null;
  level: number | null;       // 0 = заговор (cantrip)
  school: ContentLabel | null;
  castingTimeRaw: string | null;
  castingActionSlug: string | null;
  ritual: boolean | null;
  rangeType: string | null;
  rangeDistance: number | null;
  rangeUnit: string | null;
  durationRaw: string | null;
  durationType: string | null;
  durationAmount: number | null;
  durationUnit: string | null;
  concentration: boolean | null;
  description: string | null;
  higherLevels: string | null;   // "На более высоких уровнях"
  packageId: string | null;
  components: SpellComponent[];
  classes: ContentLabel[];        // классы, у которых заклинание в списке
  subclasses: ContentLabel[];     // подклассы, дающие заклинание
}

// ---------- Background ----------
export interface BackgroundFeatOption {
  feat: ContentLabel | null;
  featCategory: ContentLabel | null;
  chooseCount: number | null;
  selectedOptionRaw: string | null;
  recommendedFeat: ContentLabel | null;
  rawText: string | null;
}
export interface BackgroundToolProficiency {
  equipmentItemId: string | null;  // UUID, ссылка на equipment-предмет (инструмент)
  chooseCount: number | null;
  choiceGroupSlug: string | null;
  rawText: string | null;
}
export interface BackgroundLanguageProficiency {
  languageSlug: string | null;
  chooseCount: number | null;
  rawText: string | null;
}
export interface BackgroundEquipmentEntry {
  entryType: string | null;        // напр. "item" | "money" | "choice"
  equipmentItemId: string | null;  // UUID
  moneyValueId: string | null;     // UUID
  quantity: string | null;         // BigDecimal сериализуется строкой
  quantityUnitRaw: string | null;
  variantNote: string | null;
  choiceRef: string | null;
  rawText: string | null;
}
export interface BackgroundEquipmentOption {
  optionCode: string | null;       // "A" / "B" ...
  sortOrder: number | null;
  rawText: string | null;
  entries: BackgroundEquipmentEntry[];
}
export interface BackgroundEquipmentGroup {
  groupSlug: string | null;
  chooseCount: number | null;
  rawText: string | null;
  options: BackgroundEquipmentOption[];
}
export interface BackgroundDetail {
  id: string;
  slug: string;
  name: string;
  nameRu: string;
  nameEn: string | null;
  description: string | null;
  url: string | null;
  packageId: string | null;
  grantedFeat: ContentLabel | null;       // origin feat 2024
  abilityOptions: ContentLabel[];         // характеристики для +ASI
  skillProficiencies: ContentLabel[];
  featOptions: BackgroundFeatOption[];
  toolProficiencies: BackgroundToolProficiency[];
  languageProficiencies: BackgroundLanguageProficiency[];
  equipmentChoiceGroups: BackgroundEquipmentGroup[];
}

// ---------- Equipment Item ----------
export interface DiceFormula {
  diceCount: number | null;   // 2 в "2d6+1"
  dieSize: number | null;     // 6
  bonus: number | null;       // 1
  rawText: string | null;     // "2d6+1"
}
export interface EquipmentCost {
  amount: string | null;          // BigDecimal -> строка
  currency: ContentLabel | null;
  copperValue: string | null;     // нормализованная стоимость в медяках
  rawText: string | null;
}
export interface WeaponStat {
  damageDice: DiceFormula | null;
  damageType: ContentLabel | null;
  flatDamage: number | null;
  mastery: ContentLabel | null;   // weapon mastery 2024
}
export interface ArmorStat {
  baseAc: number | null;
  dexBonusAllowed: boolean | null;
  maxDexBonus: number | null;
  strengthRequired: number | null;
  stealthDisadvantage: boolean | null;
  armorClassRaw: string | null;   // "11 + Dex (max 2)"
}
export interface WeaponItemProperty {
  property: ContentLabel | null;       // finesse / thrown / versatile / ...
  normalRangeFt: number | null;
  longRangeFt: number | null;
  versatileDice: DiceFormula | null;
  ammunitionEquipmentItemId: string | null;  // UUID
  rawText: string | null;
}
export interface EquipmentItemDetail {
  id: string;
  slug: string;
  name: string;
  nameRu: string;
  nameEn: string | null;
  kind: string;                   // "weapon" | "armor" | "gear" | "tool" | ...
  category: ContentLabel | null;
  cost: EquipmentCost | null;
  weightLb: string | null;        // BigDecimal -> строка
  propertiesText: string | null;
  url: string | null;
  packageId: string | null;
  weaponStat: WeaponStat | null;  // присутствует только для оружия
  armorStat: ArmorStat | null;    // присутствует только для брони
  weaponProperties: WeaponItemProperty[];
}

// ---------- Magic Item ----------
export interface MagicItemCost {
  amount: string | null;
  currency: ContentLabel | null;
  copperValue: string | null;
  rawText: string | null;
}
export interface MagicItemAllowedEquipment {
  equipment: ContentLabel | null;  // базовый предмет, на который накладывается
  rawText: string | null;
}
export interface MagicItemDetail {
  id: string;
  slug: string;
  name: string;
  nameRu: string;
  nameEn: string | null;
  type: ContentLabel | null;          // armor / weapon / ring / wondrous ...
  typeRestrictionRaw: string | null;
  rarity: ContentLabel | null;
  variableRarity: boolean | null;
  attunementRequired: boolean | null;
  attunementRequirement: string | null;
  cost: MagicItemCost | null;
  description: string | null;
  embeddedTablesDetected: boolean | null;
  url: string | null;
  packageId: string | null;
  allowedEquipment: MagicItemAllowedEquipment[];
}
```

---

## 4. Примеры использования (React + fetch + TypeScript)

### 4.1 Тонкий клиент-обёртка
```ts
const BASE = ""; // тот же origin

async function apiGet<T>(path: string, lang: "ru" | "en" = "ru", token?: string): Promise<T> {
  const url = `${BASE}${path}${path.includes("?") ? "&" : "?"}lang=${lang}`;
  const res = await fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (res.status === 404) throw new Error("NOT_FOUND");
  const json: ApiResponse<T> = await res.json();
  if (!json.success) throw new Error(json.error ?? "API_ERROR");
  return json.data;
}
```

### 4.2 Список core-фитов (без кампании)
```ts
const feats = await apiGet<FeatDetail[]>("/api/reference/feats", "ru");
// feats[0].name, feats[0].category?.name, feats[0].prerequisites
```

### 4.3 Один фит
```ts
const feat = await apiGet<FeatDetail>(`/api/reference/feats/${featId}`, "ru");
```

### 4.4 Заклинания, видимые в кампании (core + активные homebrew)
```ts
const spells = await apiGet<SpellDetail[]>(
  `/api/campaigns/${campaignId}/reference/spells`,
  "ru",
  token,
);
const cantrips = spells.filter(s => s.level === 0);
const concentration = spells.filter(s => s.concentration);
```

### 4.5 Background со стартовым снаряжением
```ts
const bg = await apiGet<BackgroundDetail>(
  `/api/reference/backgrounds/${backgroundId}`,
  "ru",
);

// origin feat:
const featName = bg.grantedFeat?.name;

// варианты выбора характеристик для ASI:
const abilities = bg.abilityOptions.map(a => a.name);

// группы выбора снаряжения:
bg.equipmentChoiceGroups.forEach(group => {
  // выбрать group.chooseCount из group.options
  group.options.forEach(opt => {
    // opt.optionCode = "A"/"B"; opt.entries — что входит в вариант
  });
});
```

### 4.6 Предмет снаряжения: различать оружие/броню
```ts
const item = await apiGet<EquipmentItemDetail>(
  `/api/reference/equipment/${equipmentItemId}`,
  "ru",
);

if (item.weaponStat) {
  // оружие
  const dmg = item.weaponStat.damageDice?.rawText;       // "1d8"
  const dmgType = item.weaponStat.damageType?.name;       // "Рубящий"
  const mastery = item.weaponStat.mastery?.name;          // "Sap" / ...
}
if (item.armorStat) {
  // броня
  const ac = item.armorStat.armorClassRaw;                // "11 + Dex (max 2)"
}
// стоимость:
const price = item.cost ? `${item.cost.amount} ${item.cost.currency?.name}` : "—";
```

### 4.7 Магический предмет
```ts
const magic = await apiGet<MagicItemDetail>(
  `/api/reference/magic-items/${magicItemId}`,
  "ru",
);
const needsAttunement = magic.attunementRequired;
const rarity = magic.rarity?.name;          // "Необычный" / "Редкий" / ...
const baseItems = magic.allowedEquipment.map(a => a.equipment?.name); // на какие базы кладётся
```

---

## 5. Подсказки по UI

- **Дропдауны / списки:** используйте `slug` как стабильный ключ, `name` — как подпись, `id` —
  для запросов detail.
- **`BigDecimal` поля** (`amount`, `copperValue`, `weightLb`, `quantity`) приходят **строками** —
  парсить через `Number()` при вычислениях, не полагаться на то, что это уже number.
- **Пустые коллекции** всегда приходят как `[]`, не `null`.
- **Single-FK объекты** (`category`, `school`, `cost`, `weaponStat`, ...) могут быть `null` —
  проверять перед обращением к полям.
- **Сортировка:** `prerequisites`/`sections` (feat), `options` внутри equipment-группы и т.п.
  уже отсортированы сервером по своему sort_order — дополнительно сортировать не нужно.
- **Кэширование:** core-эндпоинты (`/api/reference/...`) безопасно кэшировать надолго (контент
  меняется только при импорте). Campaign-эндпоинты кэшировать по `campaignId`, инвалидировать
  при изменении набора активных homebrew-пакетов.
