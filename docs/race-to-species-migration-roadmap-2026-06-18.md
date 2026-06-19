# Миграция Race → Species (+ Background), модель D&D 2024 — роадмап 2026-06-18

## Контекст и почему слой мёртвый

Таблицы `species`, `species_size_option`, `species_speed`, `species_trait`,
`species_trait_effect` засеяны загрузчиком `DndContentLoader.loadSpecies()` из
`species.normalized.json`, но на них **нет ни @Entity, ни репозитория, ни сервиса, ни
API**. Рантайм создания персонажа всё ещё работает на legacy-сущности `CharacterRace`
(`character_races`, JSON-в-тексте) через `RaceService`.

Это последний незакрытый «срез» большой миграции контента: классы уже переведены
(R1–R8), backgrounds — тоже (entity `Background`, repo, используется в создании),
а расы остались на legacy. `species.normalized.json` загрузили заранее, рантайм за ним
не подтянули — отсюда мёртвый слой.

## Ключевой принцип модели D&D 2024 (поправка заказчика)

Legacy-раса 2014 тащила в себе всё: размер/скорость/трейты **и** бонусы характеристик,
владения, языки, сабрейсы (lineages). В модели 2024 это **расщеплено**:

- **Species** — тип существа, размер(ы), скорость, видовые трейты и их механические
  эффекты (сопротивления, дарквижн, врождённые заклинания).
- **Background** — бонусы характеристик (ASI), владения навыками/инструментами, языки,
  origin-feat, стартовое снаряжение.
- **Lineages** (legacy сабрейсы) — в 2024 это либо отдельные species, либо выбор
  background. Отдельной рантайм-сущности «lineage» больше нет.

То есть «недостающие» в species поля legacy-расы не потеряны — они **переехали в
Background** (который уже мигрирован).

## Маппинг поле-в-поле: legacy `CharacterRace` → новая модель

| legacy-поле | новый дом | примечание |
|---|---|---|
| `id` | `species.species_id` | |
| `name` (uniq 50) / `nameEngloc` / `nameRusloc` | `species.name_en` / `name_ru` | |
| `slug` | `species.slug` | |
| `description` (+engloc/rusloc) | `species.description` | новая модель — одно поле описания (норм. JSON — источник истины) |
| `loreDescription` (+loc) | свернуть в `species.description` или `species_trait` | отдельного lore в 2024 нет; данные не жалко |
| `sourceType` SYSTEM/HOMEBREW | производное: `homebrew_id IS NULL` = system | |
| `sourceName` | `species.source_id` → `source_book` | богаче legacy-строки |
| `active` | **нет эквивалента** | видимость = владение mod/homebrew-пакетом + publish; как и в миграции классов |
| `createdBy`/`updatedBy` | нет в species | автор — через `homebrew_packages` |
| `homebrew` | `species.homebrew_id` | подтверждено changelog 055 |
| `creatureType` (String) | `species.creature_type_id` → `creature_type` | |
| `sizeOptionsJson` | `species_size_option` (M:N → `character_size`) | |
| `defaultSize` | единственный/выбранный `species_size_option` | |
| `speedJson` | `species_speed` (`speed_type_slug`, `amount_ft`) | |
| `darkvisionRange` | `species_trait_effect` (`effect_type`=senses/darkvision, `range_ft`) | |
| `traitsJson` | `species_trait` (+ `species_trait_effect` для механики) | |
| `lineagesJson` / `lineageRequired` | **→ отдельные species / Background** | в 2024 сабрейсов нет; рантайм-выбор lineage уходит |
| `languagesJson` / `languageOptionsJson` | **→ `background_language_proficiency`** | 2024: языки из background |
| `proficienciesJson` | **→ `background_skill_proficiency` / `background_tool_proficiency`** | 2024 |
| `resistances/vulnerabilities/immunities/conditionResistances/conditionAdvantages Json` | `species_trait_effect` (`effect_type` + `damage_type_id`) | |
| `innateSpellsJson` | `species_trait_effect` (`effect_type`=innate_spell, `spell_id`, `range_ft`) | |
| `allowAbilityScoreBonuses` / `abilityScoreBonusesJson` | **→ `background_ability_option`** | 2024: ASI из background |
| `metadataJson` | drop / `species.url` | |

**Осознанные упрощения модели (данные не жалко, кроме бестиария):** в species нет
отдельного локализованного lore, нет флага `active`, нет createdBy/updatedBy. Это
ровно то же, что приняли при миграции классов.

## Поэтапный план (по образцу R1–R8; между шагами приложение может не билдиться)

- [x] **S1. Read-модель species (exposure).** `Species` + 4 дочерние @Entity, репозитории
      (vanilla `homebrew_id IS NULL` + homebrew-by-package), `ContentReferenceService`
      (vanilla + campaign), эндпоинты в `ContentReferenceController`
      (`/api/reference/species`, `/{id}`, campaign-scoped), DTO детали species.
      Закрывает дыру «сущность засеяна, но не отдаётся по API».
- [x] **S2. Переключение создания персонажа на 2024-сплит.**
      `ContentCharacterCreationService`/`CharacterService` берут вид из `Species`,
      а бонусы характеристик/владения/языки — из выбранного `Background`
        КОММЕНТАРИЙ ОТ АНАЛИТИКА БД:
            1. select * from background_feat_option - не заполнена, хотя feats заполнена 
            2. select * from background_language_proficiency - не заполнена, хотя languages заполнена. видимо, исходных json миграции не было прописано какие происхождения дают какие черты и языки. Если это так, нужно сообщить об этом пользователю дполнительно!
  (`background_ability_option` и т.д., которые уже засеяны), не из расы.
      `PlayerCharacter.race` (FK `race_id`) → species; `raceSnapshotJson` → снапшот по
      species+background.
- [x] **S3. Homebrew-авторинг species на новой модели** (зеркало R4), если авторинг рас есть.
- [ ] **S4. Миграция рантайм-данных** `characters.race_id` legacy→species + перенаведение
      FK (зеркало R6/R7). Snapshot’ы пересобрать.
- [x] **S5. Отвязка legacy (без дропа):** `RaceService`, `RaceContentValidator`,
      `CharacterRace`, admin-эндпоинты `/api/admin/character-races|races` снесены;
      NPC/blueprints переведены на species; физические FK `character_races` сняты
      (changelog 068). Таблица `character_races` оставлена «в вакууме» — дроп после
      ручной проверки приложения пользователем.

## Журнал выполненного

### S1 — read-модель species (2026-06-18) — ГОТОВО (компиляция зелёная)
Новые @Entity: `Species`, `SpeciesSpeed`, `SpeciesTrait`, `SpeciesTraitEffect`,
`ContentCreatureType` (словарь `creature_type`; legacy `CreatureType` маппит другую
таблицу `creature_types`, переиспользовать нельзя). `SpeciesRepository`
(vanilla `findAllByHomebrewIsNull` + `findAllByHomebrewIdIn`). `SpeciesDetailResponse`
(+ вложенные SpeedDto/TraitDto/EffectDto). `SpeciesMapper`. В `ContentReferenceService`
— методы vanilla+campaign с тем же правилом видимости, что у классов
(`homebrew_id IS NULL` = core, всегда видим; homebrew — только если пакет активирован
в кампании). В `ContentReferenceController` — 4 маршрута:
`GET /api/reference/species`, `/{id}`, campaign-scoped пары (+ `/content/` алиасы).
Ability-бонусы/владения/языки сознательно НЕ в species — они на Background (см. маппинг).

### S2 — создание персонажа на 2024-сплите (2026-06-18) — ГОТОВО (compile + затронутые тесты зелёные)
`SpeciesService` (getSelectableSpecies / getSelectableVanillaSpecies / buildSpeciesSnapshotJson):
видимость как у классов (core всегда, homebrew — только если пакет активен в кампании),
снапшот строится по species (size = первый отсортированный size-option slug; speed из
`species_speed` → RaceSpeedDto walk/fly/swim/climb/burrow; darkvisionRange = max range_ft
эффектов трейтов с effect_type содержащим "darkvision"; traitNames по sort_order;
allowAbilityScoreBonuses=false). `PlayerCharacter.race` теперь `@ManyToOne Species`
(колонка `race_id` переиспользована, физический FK снят changelog 067 —
`NO_CONSTRAINT`, зеркало fk_ccl_class из 060; **без переноса legacy-строк**).
`ContentCharacterCreationService` и `CharacterService` (создание + смена расы) берут вид из
`Species` через `SpeciesService`, lineage-логика убрана (`selected_lineage_id` всегда null;
колонка остаётся до S5). Снапшот рендерится из species; в `CharacterResponse.race`
кладём id/имя/описание species. Background просто прикрепляется — feat/language НЕ
обрабатываются при создании, поэтому пустые `background_feat_option`/
`background_language_proficiency` (см. комментарий аналитика) не ломают создание; это
по-прежнему пробел **источника** (normalized JSON), чинится дополнением источника, не из legacy.
`RaceService.parseSnapshot` пока переиспользуется в `CharacterService.toResponse` (чистый
парс JSON, уйдёт в S5). NPC/blueprints ещё на legacy `CharacterRace` — переводятся в S5.

### S3 — homebrew-паритет для species (2026-06-18) — ГОТОВО (компиляция зелёная)
Уточнение по условию «если авторинг рас есть»: **отдельного homebrew-builder'а для рас
никогда не существовало** (в отличие от классов с `ClassAuthoringController` /
`ClassAuthoringService` + идемпотентность). Homebrew-расы подключались только через
reference-attach (`RaceContentValidator`, тип `"RACE"`) + legacy admin CRUD
(`/api/admin/character-races`, сносится в S5). Поэтому «зеркало R4»-builder для species
сознательно НЕ добавляю (это была бы новая фича, которой у рас не было). Корректный объём
S3 = довести species до паритета в reference-системе пакетов: добавлен `ContentType.SPECIES`
(varchar(30), CHECK-констрейнта нет → миграция не нужна) и `SpeciesContentValidator`
(тип `"SPECIES"`, по `SpeciesRepository`, зеркало `CharacterClassContentValidator`).
Теперь homebrew-species (строка `species` с `homebrew_id`) подключается в пакет так же,
как класс. Legacy `RaceContentValidator`/тип `"RACE"` остаются до сноса в S5.

### S5 — отвязка legacy-race кластера от БД и приложения (2026-06-18) — ГОТОВО (compile + затронутые тесты зелёные)
Директива пользователя: отвязать legacy-таблицы рас от всей БД и кода, оставить «в
вакууме» (НЕ дропать) — пользователь проверит работу приложения без них, и если ОК,
дропнем отдельно. S4 (рантайм-бэкфилл legacy→species) сознательно НЕ выполнялся:
запрет на просачивание мусорных legacy-строк в актуальную БД (см. constraint).

Снос кода (15 файлов удалено): `RaceService`, `homebrew/RaceContentValidator`,
`domain/CharacterRace`, `CharacterRaceRepository`, `enums/RaceSourceType` и 10 race-DTO
(Create/Update/Trait/Lineage request + Race/RaceTrait/RaceLineage/RaceListItem/
CharacterRaceDetail response). Оставлены (нужны новой модели): `RaceSpeedDto`,
`CharacterRaceSnapshotResponse`, `CharacterRaceResponse`.

NPC/blueprint переведены на `Species`: `CampaignNpc.race`/`BlueprintNpc.race` теперь
`@ManyToOne Species` с `@JoinColumn(... NO_CONSTRAINT)`; `NpcService`/
`CampaignBlueprintService` резолвят вид через `SpeciesRepository`.

Снятие admin/reference/homebrew/campaign race-эндпоинтов: вырезаны race-методы и поля
`RaceService`/`CharacterRaceRepository` из `AdminController`/`AdminService`,
`ReferenceController`/`VanillaReferenceController`/`ReferenceDataService`/
`ReferenceDataMapper` (+ `CacheConfig.VANILLA_RACES`), `HomebrewController`,
`CampaignController`. `CampaignContentService.getAvailableContent` «races»-бакет
перенаведён на species; `isRaceAvailableInCampaign` удалён.
`RaceService.parseSnapshot` перенесён в `SpeciesService.parseSnapshot`;
`CharacterService.toResponse` использует его.

Физические FK сняты changelog **068-detach-legacy-race-fks.xml**
(`campaign_npcs_race_id_fkey`, `fk_bnpc_race` — DROP IF EXISTS, rollback пере-добавляет
REFERENCES character_races(id)). Таблица `character_races` НЕ дропнута — ждёт ручной
проверки. `ContentType.RACE` оставлен в enum (varchar без CHECK, безвреден).

Тесты: `RaceServiceTest` удалён; убраны мёртвые `@Mock CharacterRaceRepository` из
`CharacterServiceTest`/`ItemTypeServiceTest`/`SkillServiceTest`; `NpcServiceTest`
переведён на `Species`/`SpeciesRepository`; убран `@MockitoBean RaceService` из
`AdminSecurityTest`. `compileTestJava` зелёный; затронутые тесты проходят.
