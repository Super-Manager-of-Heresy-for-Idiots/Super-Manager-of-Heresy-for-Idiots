# Roadmap: New Content Migration From 3/10 To 10/10

Дата: 2026-06-17.

Цель: довести миграцию от текущего состояния `3/10` до `10/10`, где:

- все non-bestiary PHB/content данные лежат в новой normalized schema;
- JSONB/raw archive подход не используется для новых механик;
- legacy PHB tables больше не являются runtime-источником;
- character creation, level-up, homebrew authoring, admin tools и frontend работают с новой моделью;
- legacy "книга игрока" удалена или полностью архивирована;
- бестиарий и все bestiary-related workflows не затронуты.

## Current Status: 3/10

Что уже есть:

- новая content schema из `054`;
- `raw_json` удаляется через `056`;
- class mechanics вынесены в `057`;
- class reward choices вынесены в `058`;
- compatibility fixes в `059`;
- JPA/repositories для core class/reward-choice модели;
- часть старых entities remapped на singular tables:
  - `StatType -> ability_score`
  - `Spell -> spell`
  - `Feat -> feat`
  - `Background -> background`
  - `CurrencyType -> currency`
  - `DamageType -> damage_type`
  - `Rarity -> magic_item_rarity`
- frontend уже частично подготовлен адаптерами/types, но реальные экраны всё ещё ждут старый API.

Что ещё не готово:

- services/controllers в основном читают старые `character_classes`, `class_level_rewards`, `proficiency_skills`;
- character-owned runtime state всё ещё ссылается на legacy IDs;
- level-up всё ещё работает через `rewardType + rewardEntryId`;
- homebrew rich class authoring всё ещё создаёт старую модель;
- admin rewards всё ещё управляют `class_level_rewards`;
- большинство таблиц из `054` не имеют полноценного JPA/service/API слоя;
- frontend не может полностью перейти на новую модель, пока backend не отдаёт стабильный final contract.

## Principles

1. Bestiary не трогаем.
2. Старые данные не удаляем до тех пор, пока runtime code и frontend их реально не перестали использовать.
3. Новые APIs заменяют старые in-place. Если для нормальной миграции нужно временно сломать экран или endpoint, это допустимо.
4. Для homebrew нельзя зашивать закрытые enum-only модели там, где пользователю нужна гибкость.
5. Каждый этап должен оставлять приложение запускаемым.
6. Миграция IDs персонажей, кошельков, наград и spell/skill selections должна быть отдельным контролируемым шагом, а не побочным эффектом entity remap.

---

# Phase 1. Stabilize Current Hybrid State

Target status after phase: `3.5/10`.

Цель: убрать startup noise, зафиксировать transitional model и не дать Hibernate/ручным правкам случайно портить legacy data.

## Backend

- [x] Исправить Liquibase checksum issue для уже применённого `054`.
- [x] Вынести compatibility changes в отдельные changesets:
  - drop old `raw_json`;
  - relax old rigid CHECK constraints;
  - fix `class_feature` uniqueness.
- [x] Исправить Spring Data repository method names для новых content repos.
- [x] Запретить Hibernate генерировать FK constraints для transitional old-runtime -> new-content связей:
  - `CharacterStat.statType`
  - `CharacterClass.primaryAbilityStat`
  - `CharacterClass.spellcastingStat`
  - `ProficiencySkill.governingStat`
  - `BuffDebuff.targetStat`
  - `CharacterWallet.currencyType`
  - `WalletTransaction.currencyType`
  - `QuestReward.currencyType`
  - `BlueprintReward.currencyType`
  - `PlayerCharacter.background`
  - `CharacterKnownSpell.spell`
  - `Skill.damageType`
  - `ItemType.damageType`
  - `ItemTemplate.damageType`
  - `ItemTemplate.rarity`
  - `EnchantmentType.damageType`
- [ ] Confirm deploy config does not override `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.
- [ ] Remove external explicit `hibernate.dialect=PostgreSQLDialect` if it exists in env/k8s/hosting config.
- [ ] Add a short `docs/deployment-content-migration-notes.md` explaining:
  - why `ddl-auto=update` is unsafe during this migration;
  - why transitional associations have `NO_CONSTRAINT`;
  - which warnings are expected to disappear after rebuild.

## Frontend

- [ ] Freeze non-essential work on old character creation and level-up screens.
- [ ] Do not add new compatibility layers for old contracts.
- [ ] Add a visible engineering note in frontend docs: backend is still hybrid; screens may be temporarily broken while replacement endpoints are implemented.

## Exit Criteria

- Backend starts without Liquibase checksum errors.
- Backend starts without Spring Data query creation errors.
- Startup logs no longer contain `GenerationTarget encountered exception accepting command` for transitional FK attempts.
- Healthcheck returns `200`.
- Frontend breakage is understood and isolated to migration work; no new feature should depend on old contracts.

---

# Phase 2. Define Stable Final Contracts Before Rewriting Screens

Target status after phase: `4/10`.

Цель: зафиксировать DTO/API shape для нового content model так, чтобы frontend мог начать миграцию без угадывания.

## Backend

- [ ] Create DTOs for new content labels:
  - `ContentLabelDto`
    - `id`
    - `slug`
    - `name`
    - `nameRu`
    - `nameEn`
- [ ] Create DTOs for class mechanics:
  - `ContentClassDetailResponse`
    - `id`
    - `slug`
    - `name`
    - `nameRu`
    - `nameEn`
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
- [ ] Create DTOs for class features:
  - `ClassFeatureSummaryDto`
    - `id`
    - `slug`
    - `classId`
    - `subclassId`
    - `level`
    - `sortOrder`
    - `title`
    - `description`
- [ ] Create DTOs for rewards:
  - `RewardGroupDto`
    - `id`
    - `classId`
    - `classFeatureId`
    - `classLevel`
    - `groupKind`
    - `prompt`
    - `description`
    - `chooseMin`
    - `chooseMax`
    - `repeatable`
    - `sortOrder`
    - `options[]`
    - `grants[]`
  - `RewardOptionDto`
    - `id`
    - `optionKey`
    - `label`
    - `labelRu`
    - `labelEn`
    - `description`
    - `recommended`
    - `sortOrder`
    - `grants[]`
  - `RewardGrantDto`
    - `id`
    - `grantType`
    - `label`
    - `labelRu`
    - `labelEn`
    - `description`
    - `sortOrder`
    - typed payload.
- [ ] Define typed grant payloads:
  - `FEATURE`
  - `SUBCLASS`
  - `FEAT`
  - `SPELL`
  - `SKILL_PROFICIENCY`
  - `ABILITY_SCORE`
  - `NUMERIC_MODIFIER`
  - `CUSTOM_TEXT`
- [ ] Define final `LevelUpOptionsResponse`.
- [ ] Define final `LevelUpRequest`.
- [ ] Define final `LevelUpResultResponse`.
- [ ] Decide final endpoint shape by replacing old routes in-place:
  - recommended:
    - `GET /api/campaigns/{campaignId}/reference/classes`
    - `GET /api/reference/classes`
    - `GET /api/characters/{id}/level-up-options`
    - `POST /api/characters/{id}/level-up`
    - `GET /api/characters/{id}/rewards`
  - remove the previous contract as soon as the replacement implementation starts.
  - temporary breakage is acceptable during this migration.
- [ ] Add OpenAPI annotations for final endpoints.
- [ ] Add examples for:
  - auto feature grant;
  - choose one subclass;
  - choose N skills;
  - ASI +2;
  - ASI +1/+1;
  - custom/manual homebrew grant.

## Frontend

- [ ] Update API types to match BE final DTOs exactly.
- [ ] Keep adapter layer only where it simplifies UI code; do not preserve old API compatibility for its own sake.
- [ ] Add contract fixtures/mocks for:
  - class with no spellcasting;
  - full caster;
  - half caster;
  - class with subclass choice;
  - class with several reward groups at one level;
  - homebrew custom grant.
- [ ] Add a frontend contract doc section:
  - old API fields;
  - final fields;
  - screen migration status.
- [ ] Rewrite screens against the final contract once backend implementation starts; temporary broken screens are acceptable.

## Exit Criteria

- Backend and frontend agree on DTOs.
- Frontend agent has exact payload examples.
- Old screens are allowed to break once replacement work starts.

---

# Phase 3. Implement Read-Only New Reference APIs

Target status after phase: `5/10`.

Цель: frontend can read new class/reference content as the replacement source for character creation.

## Backend

- [ ] Add `ContentReferenceService`.
- [ ] Add mapper from `ContentCharacterClass` to `ContentClassDetailResponse`.
- [ ] Load and return:
  - class mechanics from `character_class`;
  - primary abilities from `class_primary_ability`;
  - saving throws from `class_saving_throw`;
  - skill options from `class_skill_option`;
  - features from `class_feature`;
  - reward groups from `class_level_reward_group`;
  - options from `class_level_reward_option`;
  - grants from `class_level_reward_grant`;
  - typed grant details from grant tables.
- [ ] Implement campaign-aware filtering:
  - core content;
  - active campaign homebrew packages;
  - GM/admin access rules.
- [ ] Add read-only endpoints:
  - campaign classes;
  - vanilla/reference classes.
- [ ] Add tests:
  - class mechanics mapping;
  - reward group mapping;
  - choice group mapping;
  - custom text grant mapping;
  - campaign homebrew filtering.
- [ ] Replace old `ReferenceDataService` behavior in-place or extract `ContentReferenceService` and route existing endpoints through it.

## Frontend

- [ ] Replace class reference API calls with the final class reference endpoints.
- [ ] Build a hidden/dev-only class reference viewer:
  - list classes;
  - show mechanics;
  - show features by level;
  - show reward groups by level;
  - render typed grants.
- [ ] Validate rendering of:
  - multi-primary ability;
  - saving throws as badges/list;
  - separate armor/weapon/tool proficiency text;
  - subclass choice group;
  - ASI group;
  - custom grant.
- [ ] Start replacing current character creation wizard data source with the final endpoint.

## Exit Criteria

- Frontend can inspect real new classes from BE final endpoints.
- Screens may be temporarily incomplete while old reference flow is removed.
- Old reference endpoints still work.

---

# Phase 4. Seed/Backfill Class Features And Reward Groups

Target status after phase: `5.8/10`.

Цель: new level-up data must be complete enough to drive UI.

## Backend

- [ ] Audit imported class features:
  - every PHB class has expected level features;
  - subclass features have `subclass_id`;
  - base class features have `subclass_id IS NULL`;
  - `sort_order` stable.
- [ ] Add data loader for class reward groups if not complete:
  - level 1 class startup grants;
  - subclass choices;
  - ASI levels;
  - skill choices;
  - spell/cantrip choices where modeled;
  - automatic features.
- [ ] Establish canonical `grantType` values:
  - keep `grant_type` DB column flexible text;
  - app recognizes known values;
  - unknown values render as custom/manual.
- [ ] Add idempotent loader:
  - keyed by class slug + level + group key;
  - safe rerun;
  - no destructive overwrite of homebrew.
- [ ] Add data completeness report:
  - class count;
  - features per class;
  - reward groups per level;
  - missing rewards.
- [ ] Add tests for loader idempotency.

## Frontend

- [ ] Extend dev-only viewer to show data completeness:
  - per class level timeline;
  - missing reward markers;
  - unknown grant type markers.
- [ ] Add visual rendering components:
  - `RewardGroupView`
  - `RewardOptionCard`
  - `RewardGrantLine`
  - `FeatureTimeline`
- [ ] Ensure UI can render unknown/custom grants without crashing.
- [ ] Do not implement selection persistence yet.

## Exit Criteria

- Final reference endpoint returns complete enough data for class creation and level-up design.
- Data gaps are visible and trackable.

---

# Phase 5. Migrate Character Creation Read Flow

Target status after phase: `6.5/10`.

Цель: character creation UI can use new class mechanics and new reference content.

## Backend

- [ ] Replace character creation option endpoint if needed:
  - classes from new `character_class`;
  - skills from new `skill`;
  - backgrounds from new `background`;
  - spells from new `spell`;
  - currencies from new `currency`.
- [ ] Decide character-owned state strategy for new characters:
  - recommended: new characters should store new content IDs where the table has migrated;
  - legacy existing characters remain readable through transitional logic.
- [ ] Add final create character request:
  - classId from `character_class`;
  - skill choices from `skill`;
  - backgroundId from `background`;
  - spell choices from `spell`;
  - initial reward selections if class level 1 has choices.
- [ ] Replace create flow in service:
  - validate class exists in campaign content;
  - validate skill choices against `class_skill_option`;
  - validate spell choices against spell filters;
  - initialize class level;
  - initialize stats;
  - initialize wallet;
  - persist reward selections when relevant.
- [ ] Add integration tests:
  - create fighter;
  - create wizard;
  - create class with skill choices;
  - invalid skill rejected;
  - campaign homebrew class available only when package active.

## Frontend

- [ ] Rework character creation wizard directly:
  - class step reads final classes;
  - class detail panel uses new mechanics;
  - skill choice step uses new `skillOptions` and `skillChoiceAny`;
  - proficiency text displays as armor/weapon/tool blocks;
  - spellcasting displays from new `spellcasting`;
  - level-1 rewards render from reward groups.
- [ ] Add state model for reward selections:
  - selected `rewardGroupId`;
  - selected `rewardOptionIds`;
  - child selections for skill/ability/spell choices.
- [ ] Add validation:
  - chooseMin/chooseMax;
  - disabled options when already selected;
  - clear user-facing errors.
- [ ] Delete old wizard code path when replacement compiles.
- [ ] Add UI tests or at least fixture-based component tests for:
  - class detail rendering;
  - choose one reward;
  - choose two skills;
  - custom grant display.

## Exit Criteria

- New character creation works on final content data.
- Old creation still available.
- No bestiary screens changed.

---

# Phase 6. Implement Final Level-Up Read Model

Target status after phase: `7/10`.

Цель: level-up screen can display new reward groups/options/grants before commit logic is switched.

## Backend

- [ ] Replace old level-up query logic with `LevelUpQueryService` or an equivalent final service.
- [ ] Determine target class level:
  - current total level;
  - current level in selected class;
  - multiclass constraints;
  - class availability in campaign.
- [ ] Read new reward groups for next class level:
  - `class_level_reward_group.class_id`;
  - `class_level_reward_group.class_level`;
  - group options;
  - grants.
- [ ] Include already selected/acquired state:
  - from old `character_acquired_rewards` as transitional read;
  - from new `character_reward_selection` when available.
- [ ] Include derived data:
  - HP gain;
  - proficiency bonus;
  - total level;
  - class level.
- [ ] Add final `LevelUpOptionsResponse`.
- [ ] Add tests for:
  - single-class level-up;
  - multiclass level-up;
  - subclass choice level;
  - ASI level;
  - no rewards level;
  - already selected option.

## Frontend

- [ ] Replace level-up API call with the final endpoint contract.
- [ ] Build new level-up screen sections:
  - class selection;
  - HP gain;
  - automatic rewards;
  - choice rewards;
  - review/confirm.
- [ ] Render reward groups exactly like creation reward groups.
- [ ] Add local validation:
  - all required groups selected;
  - chooseMax not exceeded;
  - ability score choices respect max score;
  - spell/skill choices selected where required.
- [ ] Remove old level-up flow once replacement compiles.

## Exit Criteria

- Level-up screen can display real new data.
- Commit can stay disabled until Phase 7 if the final commit path is not ready yet.

---

# Phase 7. Implement Final Level-Up Commit And Selection Persistence

Target status after phase: `8/10`.

Цель: level-up becomes functionally driven by new reward-choice tables.

## Backend

- [ ] Replace old level-up command logic with `LevelUpCommandService` or an equivalent final service.
- [ ] Add request validation:
  - selected class exists;
  - target level is correct;
  - reward group belongs to class and level;
  - option belongs to group;
  - chooseMin/chooseMax enforced;
  - repeatable rules enforced;
  - child selections match grant filters.
- [ ] Persist:
  - `character_reward_selection`;
  - `character_reward_ability_score_selection`;
  - `character_reward_skill_selection`;
  - `character_reward_spell_selection`.
- [ ] Apply deterministic grants:
  - class feature acquisition/visibility;
  - subclass selection;
  - feat acquisition;
  - known spell;
  - skill proficiency;
  - ability score increase;
  - numeric modifier where deterministic.
- [ ] For non-deterministic/manual grants:
  - persist selection;
  - return manual action item;
  - do not fake automatic application.
- [ ] Write level-up selections only to the final reward-selection model.
- [ ] Delete old `character_acquired_rewards` write path from active level-up flow.
- [ ] Add transaction boundaries.
- [ ] Add tests:
  - commit subclass choice;
  - commit ASI +2;
  - commit ASI +1/+1;
  - commit skill choices;
  - commit spell choices;
  - invalid over-selection rejected;
  - duplicate selection rejected;
  - rollback on partial failure.

## Frontend

- [ ] Enable confirm button for the final level-up flow.
- [ ] Submit final `LevelUpRequest`.
- [ ] Render success result:
  - level summary;
  - applied rewards;
  - manual follow-up items;
  - updated stats/spells/skills.
- [ ] Add error handling:
  - validation errors per reward group;
  - stale options after data refresh;
  - already selected duplicate.
- [ ] Remove old submit path after final request is wired.

## Exit Criteria

- New level-up can be completed end to end.
- New selections persist in new tables.
- Old flow is removed from the active code path.

---

# Phase 8. Migrate Homebrew Authoring To New Content Model

Target status after phase: `8.7/10`.

Цель: homebrew class creation/editing can create the same new model that reference and level-up consume.

## Backend

- [ ] Replace old homebrew class request with final `CreateHomebrewClassRequest`:
  - class identity;
  - class mechanics;
  - primary abilities;
  - saving throws;
  - skill options;
  - proficiency texts;
  - spellcasting profile;
  - features;
  - reward groups;
  - options;
  - typed grants;
  - custom grants.
- [ ] Support homebrew flexibility:
  - arbitrary `grantType` as text;
  - known typed payloads validated;
  - unknown/custom payload rendered as manual/custom text;
  - user-editable grants supported.
- [ ] Implement create/update:
  - `character_class`;
  - `class_primary_ability`;
  - `class_saving_throw`;
  - `class_skill_option`;
  - `class_feature`;
  - `class_level_reward_group`;
  - `class_level_reward_option`;
  - `class_level_reward_grant`;
  - typed grant tables.
- [ ] Add ownership rules:
  - every homebrew row must be reachable from `homebrew_id`;
  - child rows cascade through parent class/package;
  - no user can mutate another user's package.
- [ ] Add validation:
  - duplicate slug within package;
  - invalid ability/skill IDs;
  - invalid class level;
  - invalid chooseMin/chooseMax;
  - option without grants warning or rejection;
  - AUTO group with options warning or rejection.
- [ ] Add tests:
  - create homebrew class with custom grant;
  - create class with subclass choices;
  - update reward group;
  - delete option;
  - package activation exposes content.
- [ ] Remove old rich class authoring endpoints when final endpoints are ready.

## Frontend

- [ ] Rework class creation/edit modal:
  - identity tab;
  - mechanics tab;
  - proficiency text tab;
  - features timeline;
  - rewards builder;
  - review tab.
- [ ] Rewards builder must support:
  - automatic grant;
  - choose one of N;
  - choose N of M;
  - ASI;
  - skill choice;
  - spell choice;
  - subclass choice;
  - feat grant;
  - numeric modifier;
  - custom text/manual grant.
- [ ] Add user-friendly controls:
  - dropdowns for known grant types;
  - free text for custom grant type;
  - add/remove/reorder options;
  - validation badges;
  - preview exactly as players will see it.
- [ ] Avoid making user enter raw JSON.
- [ ] Add save draft/update flow.
- [ ] Delete old authoring UI path once the final builder reaches parity.

## Exit Criteria

- GM can create a homebrew class entirely in new model.
- Created class appears in final reference and final level-up.
- No JSONB-style authoring needed.

---

# Phase 9. Admin Tools And Data Management

Target status after phase: `9/10`.

Цель: admin sees and edits the same model that runtime uses.

## Backend

- [ ] Add admin endpoints for:
  - content classes;
  - class mechanics;
  - class features;
  - reward groups;
  - reward options;
  - reward grants;
  - content skills;
  - subclasses.
- [ ] Replace old admin class level reward endpoints.
- [ ] Add admin data quality endpoints:
  - classes missing mechanics;
  - features without rewards;
  - choice groups without options;
  - grants without typed payload or custom/manual representation;
  - orphan content rows.
- [ ] Add import warning viewer for `import_warning`.
- [ ] Add non-bestiary content audit command/service.

## Frontend

- [ ] Add admin screens or internal tools for:
  - class mechanics;
  - class features;
  - reward groups;
  - reward options;
  - typed grants;
  - import warnings;
  - data quality report.
- [ ] Add table/list views with filters:
  - class;
  - level;
  - grant type;
  - source/homebrew package;
  - validation status.
- [ ] Make destructive admin actions explicit:
  - confirmation modal;
  - affected children preview;
  - no accidental cascade deletes.

## Exit Criteria

- Admin no longer needs old `class_level_rewards`.
- Data quality issues are visible without DB access.

---

# Phase 10. Migrate Existing Runtime Data

Target status after phase: `9.5/10`.

Цель: existing characters/campaign data can survive the final legacy removal.

## Backend

- [ ] Inventory existing runtime FK columns:
  - `characters.background_id`;
  - `character_stats.stat_type_id`;
  - `character_wallets.currency_type_id`;
  - `wallet_transactions.currency_type_id`;
  - `character_known_spells.spell_id`;
  - `character_skill_proficiencies.skill_id`;
  - `character_class_levels.class_id`;
  - `character_acquired_rewards.class_level_reward_id`;
  - old reward target IDs.
- [ ] Build mapping strategy:
  - prefer slug/code mapping;
  - use localized name mapping only as an explicit reviewed mapping strategy;
  - manual review table for ambiguous rows;
  - never guess silently for user-owned data.
- [ ] Create migration report tables or CSV exports:
  - mapped rows;
  - unmapped rows;
  - ambiguous rows;
  - skipped rows.
- [ ] Add one-time migration scripts:
  - legacy stat IDs -> `ability_score`;
  - legacy currency IDs -> `currency`;
  - legacy background IDs -> `background`;
  - legacy spell IDs -> `spell`;
  - legacy proficiency skill IDs -> `skill`;
  - legacy class IDs -> `character_class`;
  - legacy rewards -> `character_reward_selection`.
- [ ] Add dry-run mode.
- [ ] Add backup requirement before destructive step.
- [ ] Add post-migration validation:
  - no runtime row points to missing new content;
  - old and new character summaries match;
  - wallets total preserved;
  - known spells preserved;
  - skill proficiencies preserved.
- [ ] After successful migration, remove transitional `NO_CONSTRAINT` only where new constraints are safe.

## Frontend

- [ ] Add compatibility banner/handling for old characters during migration window:
  - "legacy character data is being upgraded";
  - temporary read-only blocked state for characters that cannot be mapped automatically.
- [ ] Ensure character sheet can render both:
  - old transitional data;
  - new migrated data.
- [ ] After migration, remove temporary compatibility display paths.
- [ ] Add smoke tests for migrated characters:
  - stats;
  - class levels;
  - skills;
  - spells;
  - rewards;
  - wallet.

## Exit Criteria

- Existing characters and campaign data are mapped to new content IDs.
- No user-facing data is lost.
- Old runtime FK columns are either migrated or intentionally archived.

---

# Phase 11. Frontend Replacement And Cleanup

Target status after phase: `9.7/10`.

Цель: frontend screens stop using old content APIs.

## Backend

- [ ] Remove old endpoints from active backend routes.
- [ ] Add temporary logging only where needed to catch accidental old route usage during local testing.
- [ ] Confirm final endpoint performance:
  - avoid N+1;
  - batch fetch reward details;
  - cache reference data where safe.

## Frontend

- [ ] Use final reference API everywhere.
- [ ] Use final character creation everywhere.
- [ ] Use final level-up everywhere.
- [ ] Use final homebrew class authoring everywhere.
- [ ] Remove old adapters from runtime path.
- [ ] Remove old rewardType-only assumptions.
- [ ] Remove old class detail assumptions:
  - one primary ability only;
  - combined armor/weapon proficiency;
  - flat reward entries.
- [ ] Update user-facing flows:
  - creation;
  - level-up;
  - class detail;
  - campaign available content;
  - homebrew package preview.
- [ ] Run full frontend QA:
  - desktop;
  - mobile;
  - long class names;
  - long custom grants;
  - empty optional data;
  - invalid backend response handling.

## Exit Criteria

- Frontend does not call old non-bestiary content endpoints in normal workflows.
- Users can create, level up, and author classes using the final model.

---

# Phase 12. Remove Legacy Non-Bestiary PHB Model

Target status after phase: `10/10`.

Цель: legacy PHB полностью перестаёт быть частью runtime.

## Backend

- [ ] Confirm old endpoint usage is zero.
- [ ] Confirm migration reports have no unresolved rows.
- [ ] Remove old service dependencies:
  - `CharacterClassRepository` where used as class catalog;
  - `SubclassRepository` for class content;
  - `ProficiencySkillRepository`;
  - `ClassLevelRewardRepository`;
  - `CharacterAcquiredRewardRepository` for class rewards.
- [ ] Remove old entities or mark as archive-only if still needed for audit:
  - `CharacterClass`;
  - `Subclass`;
  - `ProficiencySkill`;
  - `ClassLevelReward`;
  - `CharacterAcquiredReward`.
- [ ] Remove old DTOs:
  - old `CharacterClassDetailResponse` if replaced;
  - old `LevelUpOptionsResponse`;
  - old `LevelUpRequest`;
  - old `ClassLevelRewardResponse`.
- [ ] Remove old reward resolver registry or replace with grant resolver.
- [ ] Add final cleanup migration:
  - drop old class/proficiency/reward tables only after backup and validation;
  - keep bestiary tables untouched;
  - keep unrelated operational tables.
- [ ] Remove transitional `NO_CONSTRAINT` where no longer needed and add real FK constraints to new model.
- [ ] Add final integration tests:
  - fresh DB;
  - migrated DB;
  - character creation;
  - level-up;
  - homebrew class;
  - admin edit;
  - no bestiary regressions.

## Frontend

- [ ] Delete old API clients for non-bestiary content.
- [ ] Delete old adapters that normalize legacy reward types.
- [ ] Delete old class creation/level-up code paths.
- [ ] Delete temporary migration toggles and compatibility switches.
- [ ] Update docs/screenshots/stories.
- [ ] Run final regression:
  - character creation;
  - level-up;
  - class authoring;
  - campaign content availability;
  - character sheet;
  - admin tools.

## Exit Criteria

- Legacy PHB tables are not used by runtime.
- Legacy PHB endpoints are removed or archive-only.
- Frontend does not depend on old contracts.
- New normalized content model is the only non-bestiary PHB source.
- Homebrew can add flexible custom content without JSONB.
- Bestiary remains intact.

---

# Final 10/10 Definition

Migration is complete only when all of this is true:

- [ ] Fresh database boots and seeds new content without old PHB tables.
- [ ] Existing database migrates without data loss.
- [ ] Character creation uses new content IDs.
- [ ] Level-up uses reward groups/options/grants.
- [ ] Homebrew authoring writes new content tables.
- [ ] Admin edits new content tables.
- [ ] Frontend runtime path does not call old non-bestiary PHB endpoints.
- [ ] Old non-bestiary PHB tables/entities/repos/services are removed or archive-only.
- [ ] No `raw_json`/JSONB is used for structured mechanics.
- [ ] No startup Hibernate DDL warnings from transitional FK attempts.
- [ ] Bestiary behavior is unchanged.
