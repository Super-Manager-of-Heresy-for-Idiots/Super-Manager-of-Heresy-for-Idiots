# Backend New Content Coverage Audit

Дата аудита: 2026-06-17.

Scope: новая content-схема из миграций `054`, `057`, `058`; бестиарий и связанные с ним таблицы не считаются целью миграции.

## Короткий вывод

В BE уже есть фундамент новой модели: миграции, часть JPA-entity и репозитории для классов, механик классов, фич и новой reward-choice модели. Но пользовательские сценарии все еще в основном работают по старым таблицам и старым DTO.

Главный риск для фронта: новые таблицы `character_class`, `class_level_reward_group`, `class_level_reward_option`, `class_level_reward_grant` и `character_reward_selection` пока не являются рабочим API-контрактом. Они есть в БД/JPA, но `/reference/classes`, `/level-up-options`, `/level-up`, `/characters/{id}/rewards`, admin level-rewards и rich homebrew class creation все еще построены вокруг старых `character_classes`, `class_level_rewards`, `character_acquired_rewards`, `proficiency_skills`.

## Что уже есть

### Миграции

Есть и подключены к master:

- `054-create-dnd-content-schema.xml`: новая нормализованная content-схема без JSONB.
- `056-drop-content-raw-json.xml`: удаление `raw_json`.
- `057-class-mechanics.xml`: механики класса без JSONB:
  - `character_class.hit_die`
  - `is_spellcaster`
  - `has_cantrips`
  - `is_half_caster`
  - `spellcasting_ability_id`
  - `skill_choice_count`
  - `skill_choice_any`
  - `armor_proficiency_text`
  - `weapon_proficiency_text`
  - `tool_proficiency_text`
  - `class_saving_throw`
  - `class_primary_ability`
  - `class_skill_option`
- `058-class-reward-choices.xml`: реляционная модель наград:
  - `class_level_reward_group`
  - `class_level_reward_option`
  - `class_level_reward_grant`
  - typed grant tables
  - `character_reward_selection`
  - child selection tables for ability/skill/spell choices

### JPA/repository foundation for class and reward workflows

Есть entity/repository слой для основной новой модели классов:

- `ContentCharacterClass` -> `character_class`
- `ContentSubclass` -> `subclass`
- `ContentSkill` -> `skill`
- `ClassFeature` -> `class_feature`
- `ClassLevelRewardGroup` -> `class_level_reward_group`
- `ClassLevelRewardOption` -> `class_level_reward_option`
- `ClassLevelRewardGrant` -> `class_level_reward_grant`
- typed grants:
  - feature
  - subclass
  - feat
  - spell
  - skill proficiency
  - ability score
  - numeric modifier
  - custom text
- character selections:
  - reward option selection
  - ability score selection
  - skill selection
  - spell selection

Join-таблицы корректно описаны через `@JoinTable`, поэтому отдельные entity для них не нужны:

- `class_saving_throw`
- `class_primary_ability`
- `class_skill_option`
- `class_level_reward_grant_skill_option`
- `class_level_reward_grant_ability_option`

### Частично remapped старые доменные entity

Часть старых классов уже смотрит в новые singular-таблицы:

- `StatType` -> `ability_score`
- `Spell` -> `spell`
- `SpellSchool` -> `spell_school`
- `Feat` -> `feat`
- `Background` -> `background`
- `CurrencyType` -> `currency`
- `DamageType` -> `damage_type`
- `Rarity` -> `magic_item_rarity`
- `CreatureSize` -> `character_size`

Это не значит, что вся новая структура вокруг них используется. Например, `Background` замаплен на `background`, но child-таблицы background choices/proficiencies/equipment пока не имеют JPA/API покрытия.

## Что есть в JPA, но фактически не подключено к пользовательским сценариям

По поиску новые content-репозитории находятся только как объявления интерфейсов, но не используются сервисами:

- `ContentCharacterClassRepository`
- `ContentSubclassRepository`
- `ContentSkillRepository`
- `ClassFeatureRepository`
- `ClassLevelRewardGroupRepository`
- `ClassLevelRewardOptionRepository`
- `ClassLevelRewardGrantRepository`
- typed grant repositories
- `CharacterRewardSelectionRepository`
- child selection repositories

Практический эффект: новая модель компилируется, но не кормит фронт и не принимает пользовательские выборы при левелапе.

## Что все еще работает по старым таблицам

### Reference APIs

`ReferenceDataService` все еще использует:

- `CharacterClassRepository` -> old `character_classes`
- `ProficiencySkillRepository` -> old `proficiency_skills`
- old `CharacterClass` JSON/text mechanics:
  - `savingThrowStatIdsJson`
  - `skillChoiceOptionIdsJson`
  - `primaryAbilityStat`
  - combined `armorWeaponProficiencies`

Текущий `CharacterClassDetailResponse` старый по форме:

- одна `primaryAbilityStatId`, а не список `primaryAbilities`
- `savingThrowStatNames` строками, а не полноценными ability objects/ids
- `skillChoiceOptions` из old `proficiency_skills`
- один общий `armorWeaponProficiencies`, а не `armor/weapon/tool` отдельно
- нет `features`
- нет `rewardGroups/options/grants`

### Level-up

`LevelUpService` все еще использует:

- `CharacterClassRepository`
- `ClassLevelRewardRepository`
- `CharacterAcquiredRewardRepository`
- группировку по строковому `rewardType`
- `LevelUpRequest.RewardSelection.rewardEntryId`
- старый `ClassLevelReward`

Текущий `LevelUpOptionsResponse` старый по форме:

- `RewardGroup.rewardType`
- `RewardGroup.isChoice`
- `RewardEntry.rewardEntryId`
- нет `rewardGroupId`
- нет `rewardOptionId`
- нет `chooseMin/chooseMax/repeatable`
- нет typed grants
- нет child selections для ASI/skill/spell choices

### Character rewards view

`CharacterRewardQueryService` читает `CharacterAcquiredReward`, а не `character_reward_selection`.

### Homebrew authoring

`HomebrewAuthoringService` все еще создает rich classes через старые:

- `CharacterClass`
- `Subclass`
- `ClassLevelReward`
- `RewardType`
- `rewardId`

Это ограничивает homebrew: нельзя нормально описать "выбери одну из трех наград", "выбери N из M", custom text/manual grant, numeric modifier и typed grants в новой модели.

### Admin level rewards

`AdminService` и `AdminController` все еще имеют endpoints:

- `GET /admin/classes/{classId}/level-rewards`
- `POST /admin/classes/{classId}/level-rewards`
- `DELETE /admin/classes/{classId}/level-rewards/{rewardEntryId}`

Они управляют old `class_level_rewards`, не новой group/option/grant схемой.

## Таблицы из миграции, которые имеют JPA mapping

Mapped напрямую через `@Table`:

- `ability_score`
- `background`
- `character_class`
- `character_reward_ability_score_selection`
- `character_reward_selection`
- `character_reward_skill_selection`
- `character_reward_spell_selection`
- `character_size`
- `class_feature`
- `class_level_reward_grant`
- `class_level_reward_grant_ability_score`
- `class_level_reward_grant_custom_text`
- `class_level_reward_grant_feat`
- `class_level_reward_grant_feature`
- `class_level_reward_grant_numeric_modifier`
- `class_level_reward_grant_skill_proficiency`
- `class_level_reward_grant_spell`
- `class_level_reward_grant_subclass`
- `class_level_reward_group`
- `class_level_reward_option`
- `currency`
- `damage_type`
- `feat`
- `magic_item_rarity`
- `skill`
- `spell`
- `spell_school`
- `subclass`

Mapped через `@JoinTable`, отдельные entity не требуются:

- `class_saving_throw`
- `class_primary_ability`
- `class_skill_option`
- `class_level_reward_grant_skill_option`
- `class_level_reward_grant_ability_option`

## Таблицы из миграции без JPA/API покрытия

Это не обязательно bug прямо сейчас, но фронт и сервисы не могут на них опираться, пока не появятся entity/repository/service/DTO.

Source/package/import:

- `source_book`
- `mod_package`
- `import_warning`

Money/equipment:

- `money_value`
- `dice_formula`
- `equipment_category`
- `equipment_item`
- `weapon_property`
- `weapon_mastery`
- `weapon_stat`
- `weapon_item_property`
- `armor_stat`

Feat structured data:

- `feat_category`
- `feat_prerequisite`
- `feat_section`

Background structured data:

- `background_feat_option`
- `background_ability_option`
- `background_skill_proficiency`
- `background_tool_proficiency`
- `background_language_proficiency`
- `background_equipment_choice_group`
- `background_equipment_option`
- `background_equipment_entry`

Spell structured relations:

- `spell_component`
- `spell_class`
- `spell_subclass`

Species/race replacement:

- `creature_type`
- `species`
- `species_size_option`
- `species_speed`
- `species_trait`
- `species_trait_effect`

Magic items:

- `magic_item_type`
- `magic_item`
- `magic_item_allowed_equipment`

Class progression:

- `class_progression_column`
- `class_progression_value`

Random/crafting:

- `random_table`
- `random_table_entry`
- `spell_scroll_crafting_rule`

## Backend gaps that block frontend migration

### 1. No new class reference DTO

Frontend needs class detail shaped around the new schema:

- `id`
- `slug`
- `nameRu`
- `nameEn`
- localized `name`
- `subtitle`
- `hitDie`
- `primaryAbilities[]`
- `savingThrows[]`
- `skillChoiceCount`
- `skillChoiceAny`
- `skillOptions[]`
- `armorProficiencyText`
- `weaponProficiencyText`
- `toolProficiencyText`
- `spellcasting`
- `features[]`
- `rewardGroups[]`

Current DTO does not expose this.

### 2. No new reward DTO

Frontend needs:

- `rewardGroupId`
- `classLevel`
- `groupKind`: `AUTO | CHOICE`
- `prompt`
- `description`
- `chooseMin`
- `chooseMax`
- `repeatable`
- `sortOrder`
- `options[]`
- direct `grants[]` for AUTO groups
- option `grants[]` for CHOICE groups
- grant payload per `grantType`

Current API only returns old `rewardType` buckets and flat reward entries.

### 3. No new level-up request contract

Frontend will need to submit:

- target `classId` from `character_class`
- selected reward groups/options:
  - `rewardGroupId`
  - `rewardOptionId`
  - optional `noteText`
- child selections:
  - ability score choices
  - skill choices
  - spell choices

Current request only supports `rewardType + rewardEntryId` and a separate ASI structure.

### 4. No backend application of new grants

Needed service logic:

- validate group belongs to selected class/new level
- validate option belongs to group
- enforce `chooseMin/chooseMax`
- persist `character_reward_selection`
- persist child selections
- apply deterministic grants:
  - subclass
  - feat
  - spell
  - skill proficiency
  - ability score
  - feature acquisition or feature availability
- surface custom/manual grants without pretending they were automatically applied

### 5. Homebrew authoring still cannot create the new shape

The authoring API must move from old `rewardType/rewardId` plans to group/option/grant authoring:

- fixed automatic grant
- choose one of N
- choose N of M
- any skill/spell matching filter
- constrained skill/ability option list
- numeric modifier
- custom text/manual grant
- user-editable custom grant

## Recommended backend implementation order

### Step 1: Add read-only new content reference service

Create a new service instead of rewriting all old service code at once:

- `ContentReferenceService`
- new mapper for `ContentCharacterClass`
- new DTOs for class mechanics/features/reward groups/grants
- endpoint can be either:
  - versioned: `/api/campaigns/{campaignId}/reference/classes/content`
  - or backwards-compatible extension of `/classes`

This gives frontend something real to integrate without breaking current character creation.

### Step 2: Add new level-up options read model

Build `LevelUpOptionsResponseV2` from:

- `ContentCharacterClassRepository`
- `ClassLevelRewardGroupRepository`
- group options/grants
- old character level state only as transitional character-owned state

Do not persist new selections yet in this step. Just make the UI able to render the real new model.

### Step 3: Add new level-up commit path

Add `LevelUpRequestV2` and persist:

- `character_reward_selection`
- `character_reward_ability_score_selection`
- `character_reward_skill_selection`
- `character_reward_spell_selection`

Keep old endpoint until frontend is switched.

### Step 4: Bridge character-owned class/skill state

Current character state still references old content IDs:

- `character_class_levels.class_id`
- `character_skill_proficiencies.skill_id`
- old acquired rewards

Either migrate these FKs to new IDs or add a transitional mapping by slug/name. Direct FK migration is cleaner, but it touches persisted character data and must be done carefully.

### Step 5: Replace homebrew class authoring

Move rich class authoring to:

- `character_class`
- `class_primary_ability`
- `class_saving_throw`
- `class_skill_option`
- `class_feature`
- `class_level_reward_group`
- `class_level_reward_option`
- `class_level_reward_grant`
- typed grant tables

This is where homebrew friendliness is won or lost.

### Step 6: Retire old class reward APIs

After frontend and services use V2:

- stop writing `class_level_rewards`
- stop writing `character_acquired_rewards`
- replace admin level-rewards endpoints
- remove old reward resolver registry or adapt it around grants

### Step 7: Audit and implement remaining 054 domains by product priority

Not all 054 tables need immediate UI. Recommended priority:

1. classes/rewards/features/skills: required for creation and level-up
2. backgrounds structured choices: required for full character creation
3. spells class/subclass/component relations: required for spell picker filters
4. equipment/money: required for inventory and starting equipment
5. species: race replacement, separate migration slice
6. magic items/random tables/crafting: later admin/reference slices
7. source/mod/import_warning: admin/import observability

## Frontend contract warning

Frontend adapters can be prepared for the new shape, but production screens should not assume the BE returns it yet.

Currently reliable BE behavior:

- old class reference DTO
- old level-up options DTO
- old level-up request DTO
- old acquired rewards DTO

Currently not reliable as API:

- class `features[]`
- class `rewardGroups[]`
- reward group ids/options/grants
- separate armor/weapon/tool proficiency texts
- multi-primary ability objects
- `character_reward_selection` persistence

## Verification performed

Commands used for this audit:

- `rg` over repositories/services/controllers/DTOs for old and new repository usage.
- `rg` over migrations for `CREATE TABLE`.
- PowerShell comparison of migration table names against Java `@Table`.

No backend source code was changed during this audit.
