# Fix Log: New Content Migration Roadmap

Date: 2026-06-17

Scope:

- Backend: `C:\SuperHerecy\SuperManagerofHeresyforIdiots\SuperManagerofHeresyforIdiots`
- Frontend: `C:\SuperHerecy\Super-Manager-of-Heresy-for-Idiots-frontend`
- Audit source: `docs/new-content-3-to-10-roadmap-audit-2026-06-17.md`

## Step 1: Final Reference And Level-Up Routes

Goal: remove the worst FE/BE contract mismatch for final class reference and level-up.

Backend changes:

- `ContentReferenceController`
  - Added final in-place class routes:
    - `GET /api/reference/classes`
    - `GET /api/reference/classes/{classId}`
    - `GET /api/campaigns/{campaignId}/reference/classes`
    - `GET /api/campaigns/{campaignId}/reference/classes/{classId}`
  - Kept `/reference/content/classes` aliases during rollout.
- `ContentLevelUpController`
  - Added final in-place level-up routes:
    - `GET /api/characters/{characterId}/level-up-options`
    - `POST /api/characters/{characterId}/level-up`
  - Kept `/content/level-up*` aliases during rollout.
- `LevelUpController`
  - Moved legacy active endpoints to:
    - `GET /api/characters/{id}/legacy/level-up-options`
    - `POST /api/characters/{id}/legacy/level-up`
  - Kept `GET /api/characters/{id}/rewards` unchanged for now.

Frontend changes:

- Updated `ContentLevelUpRequest` shape to match backend `dto.content.LevelUpRequest`:
  - `classId`
  - `selections[]`
  - `rewardGroupId`
  - `optionIds[]`
  - `childSelections.abilityScores/skillIds/spellIds`
- Updated `buildContentLevelUpRequest` to emit backend-compatible `selections[]`.
- Updated content level-up tests to assert the new request shape.
- Updated level-up result UI to render backend content result:
  - `appliedGrants`
  - `manualActions`
  - no longer `rewardsAcquired`.

Verification:

- Frontend `npm test`: passed, 4 test files / 37 tests.
- Frontend `npm run build`: passed, existing Vite large chunk warning only.
- Backend `.\gradlew.bat test`: passed, build successful.

Remaining known limitations after Step 1:

- Legacy `/characters/{id}/rewards` still uses old reward query service.
- Backend content level-up still has a transitional mixed-ID issue for skill proficiencies.
- Auto groups with child choices are not fully represented by backend `LevelUpRequest`; current UI/data likely does not rely on that yet.
- Character creation still uses legacy `/characters/full`; this is Step 2.

## Step 2: Character Creation On Content Model

Goal: make the normal wizard creation route use the new content-model controller/request instead of the legacy `CharacterWizardService` flow.

Backend changes:

- `ContentCharacterController`
  - Added stable wizard routes to the content controller:
    - `POST /api/campaigns/{campaignId}/characters/full`
    - `POST /api/characters/full`
  - Kept rollout aliases:
    - `POST /api/campaigns/{campaignId}/characters/content`
    - `POST /api/characters/content`
- `CampaignCharacterController`
  - Moved old wizard persistence route to:
    - `POST /api/campaigns/{campaignId}/characters/legacy/full`
- `CharacterTemplateController`
  - Moved old template wizard persistence route to:
    - `POST /api/characters/legacy/full`
- `LegacyContentRouteLoggingConfig`
  - Updated warnings to target only explicit `/legacy/*` routes, not the stable content-backed `/full` routes.

Frontend changes:

- `characters-full.api.ts`
  - Removed endpoint-missing fallback helpers and minimal `createBasic`.
  - Added `CreateContentCharacterRequest` mapper.
  - `createFull` and `createTemplate` now post only content DTO fields:
    - `name`, `playerName`, `classId`, `raceId`, `selectedLineageId`, `backgroundId`
    - `level`, `abilityScores`, `scoreMethod`
    - `chosenSkillIds`, `cantripIds`, `spellIds`, `startingCoins`
- `useCreateFullCharacter`
  - Removed fallback to basic character creation.
  - Mutation now returns `ContentCharacterCreationResponse`.
- `CharacterCreationWizard`
  - Renamed submitted class skill selection from legacy `chosenSkillProficiencyIds` to content `chosenSkillIds`.
- `useTemplates` / `TemplateWizardPage`
  - Template creation now uses an explicit request type without `campaignId`.

Verification:

- Frontend `npm test -- --run`: passed, 4 test files / 37 tests.
- Frontend `npm run build`: passed, existing Vite large chunk warning only.
- Backend `.\gradlew.bat test`: passed, build successful.

Remaining known limitations after Step 2:

- The creation wizard validates and submits selected level-1 reward groups in UI state, but `CreateContentCharacterRequest` still does not carry those selections to backend. This is Step 3.
- Spell local keys are still not mapped to spell IDs in the wizard, so content creation submits empty `cantripIds` / `spellIds` unless an ID-aware picker is added later.

## Step 3: Close Contract Blockers (audit gaps #5, #6, #7)

Goal: close the cross-project contract blockers that left the class builder and admin
class tooling unable to work end-to-end against the backend (audit findings: legacy
admin reward UI still active; class-builder GET/ETag/idempotency contract unmet;
class-builder reference dropdown endpoints missing).

### Blocker A: Class-builder reference dropdown endpoints (audit FE gap #7)

Backend changes:

- `VanillaReferenceController` now exposes the authoring dropdown endpoints the builder
  already called:
  - `GET /api/reference/abilities` -> `List<ContentLabelDto>`
  - `GET /api/reference/feats` (searchable `query`) -> `List<FeatOptionDto>`
  - `GET /api/reference/modifier-keys`
- Backed by `ReferenceDataService.getVanillaAbilities/getVanillaFeats/...` reading the
  new content tables.

### Blocker B: Class-authoring GET / ETag / If-Match contract (audit FE gap #6)

Backend changes:

- `ClassAuthoringController`
  - Added detail GET endpoints returning `ContentClassDetailResponse` with a real `ETag`:
    - `GET /api/admin/character-classes/{classId}` (`getCore`)
    - `GET /api/homebrew/packages/{packageId}/classes/{classId}` (`getPackageClass`)
  - `updateCore` / `updatePackageClass` now read `If-Match` and return the new `ETag`.
- `ClassAuthoringService`
  - Added `getCoreClass` / `getPackageClass` read methods (admin/ownership checks).
  - Added deterministic `etagFor(...)`: SHA-256 content hash of the Jackson JSON of the
    detail DTO (stable validator with no DB version column / no Liquibase migration).
  - Added `enforceIfMatch(...)`; `updateCoreClass` / `updatePackageClass` now take
    `ifMatch` and reject stale writes.
  - `saveResult` now returns the content detail + stable `etag` (was random UUID).
- `PreconditionFailedException` (new) + `GlobalExceptionHandler` -> HTTP `412`
  `PRECONDITION_FAILED` so optimistic-concurrency conflicts surface correctly.
- `ClassAuthoringServiceTest`: injects a real `ObjectMapper` via `@Spy` (so `etagFor`
  works under `@InjectMocks`) and passes `null` `ifMatch` in the update call.

### Blocker C: Legacy admin class-level reward workflow removed (audit gaps #5, #8)

This also fixed a boot blocker: `AdminController` and `ClassAuthoringController` both
mapped POST/PUT/DELETE `/api/admin/character-classes[/{id}]`, an ambiguous Spring
mapping that prevented startup. The content model is the migration target, so the
legacy CRUD was removed from `AdminController`.

Backend changes:

- `AdminController`
  - Removed legacy class CRUD (create/get/update/delete `/character-classes`).
    Kept `listClasses`, `createClassRich`, `importClassJson`, `updateClassRich`.
  - Removed legacy level-rewards endpoints
    (`GET/POST/DELETE /classes/{classId}/level-rewards`).
- `AdminService`
  - Removed `listClassLevelRewards`, `createClassLevelReward`, `deleteClassLevelReward`,
    helper `toClassLevelRewardResponse`, and the now-unused
    `ClassLevelRewardRepository` / `RewardResolverRegistry` dependencies.

Frontend changes:

- `useAdmin.ts`
  - `useCharacterClasses` now reads `referenceApi.getClasses()` (`GET /reference/classes`,
    content model) instead of the deprecated `character_classes` admin list, so
    builder-created classes appear in the admin list.
  - Removed dead reward hooks (`useLevelRewards`/`useCreateLevelReward`/`useDeleteLevelReward`)
    and dead class CRUD hooks.
- `admin.api.ts`: removed level-rewards methods, dead class CRUD methods, and the now-unused
  type imports.
- Deleted `src/pages/admin/LevelRewardsPage.tsx` and `LevelRewardsPage.module.css`.
- `router.tsx`: removed the `LevelRewardsPage` lazy import and the
  `/admin/character-classes/:classId/rewards` route.

Verification:

- Frontend `npx tsc --noEmit`: exit 0.
- Backend `./gradlew compileJava compileTestJava`: exit 0 (only the expected
  "uses deprecated API" note from the archive-only `CharacterClass` entity).

Remaining known limitations after Step 3:

- Backend ignores the `Idempotency-Key` header on class create; true dedup would need a
  key-store table/migration (out of scope, not yet done).
- Orphan FE types `ClassLevelRewardResponse` / `CreateClassLevelRewardRequest` remain in
  `types/index.ts` (dead, but compile-clean) and were left in place.
- All deeper migration gaps from the audit (legacy level-up/creation runtime paths,
  reward-selection persistence, mixed skill IDs, runtime data migration, final cleanup
  Liquibase) are untouched by Step 3 — see the new roadmap below.

## R1: Unify Skill Identity

Roadmap item: `new-content-remediation-roadmap-2026-06-17.md` → **R1**.

Goal: make `CharacterSkillProficiency.skill` reference content `skill` (ContentSkill) and
remove the `getReference(ProficiencySkill.class, id)` bridge in creation + level-up, so
runtime no longer mixes legacy `proficiency_skills` IDs into the relation.

Backend changes:

- `domain/CharacterSkillProficiency`
  - `skill` field retyped `ProficiencySkill` → `ContentSkill` (column `skill_id`,
    FK still `NO_CONSTRAINT` until R6 data migration runs, real FK added in R7).
  - Dropped the `@Deprecated` "transitional, points at legacy proficiency_skills" note;
    updated javadoc to state it now references content `skill`.
- `service/ContentCharacterCreationService`
  - Skill proficiency write now `getReference(ContentSkill.class, skillId)`; removed
    unused `ProficiencySkill` import.
- `service/LevelUpCommandService`
  - `SKILL_PROFICIENCY` grant write now `getReference(ContentSkill.class, skillId)`;
    removed unused `ProficiencySkill` import.
- `service/CharacterService` (template→character copy + sheet response)
  - Read site `sp.getSkill().getName()` → `getNameRu()` (copy path passes the entity
    through unchanged, now typed `ContentSkill`).
- `service/CharacterWizardService` (legacy `/legacy/full` wizard)
  - Swapped `ProficiencySkillRepository` field for `ContentSkillRepository`.
  - `saveSkillProficiencies` resolves `ContentSkill` (removed the always-empty background
    skill loop) and `validateSkillChoices` resolves `ContentSkill` (`getName()` →
    `getNameRu()`); read site `getName()` → `getNameRu()`.
  - Added `import com.dnd.app.domain.content.ContentSkill` (the `domain.*` wildcard does
    not cover the `content` subpackage).

Note: `RuntimeDataMigrationService` already remaps
`character_skill_proficiencies.skill_id -> skill` (R6), so existing legacy rows get
migrated rather than broken by this type switch; new writes were the remaining mixed-ID
source and are now content-only.

Verification:

- Backend `./gradlew compileJava compileTestJava`: exit 0.
- Backend `./gradlew test --tests *ContentCharacterCreationServiceTest
  --tests *LevelUpCommandServiceTest --tests *RuntimeDataMigrationServiceTest`: exit 0.

Remaining for R1's "read navigation" test bullet: a real read-through-navigation test for
a character holding content-skill IDs needs a DB (Testcontainers) and is folded into R9;
unit tests + compile-time type safety cover the change for now.

## R2: Final Level-Up Runtime Switch-Over

Roadmap item: `new-content-remediation-roadmap-2026-06-17.md` → **R2**.

Goal: make the normal level-up flow read/commit through the content model and stop
exposing the legacy reward write path on the active route.

State found (already done in Step 1, verified now):

- `ContentLevelUpController` owns the in-place routes `GET /api/characters/{id}/level-up-options`
  and `POST /api/characters/{id}/level-up` (plus `/content/*` aliases) and delegates to
  `LevelUpQueryService` / `LevelUpCommandService`, which write only the content
  `character_reward_selection*` tables.
- `LevelUpController` legacy read/commit are moved to `/{id}/legacy/level-up[-options]` and
  marked `@Deprecated(forRemoval = true)` — off the normal FE path.
- FE `levelup.api.ts` already calls the in-place routes and posts `ContentLevelUpRequest`.

Change made in R2:

- `service/CharacterRewardQueryService` (`GET /api/characters/{id}/rewards`) rewritten onto
  the new model:
  - reads `CharacterRewardSelectionRepository.findAllByCharacterId` instead of
    `character_acquired_rewards` + `RewardResolverRegistry`.
  - builds the per-class breakdown from each selected option's grants (keyed by
    `grant_type`; `SUBCLASS` grant fills `subclass`); class names resolved from the content
    class on the reward group / `ContentCharacterClassRepository`; class levels from
    `character_class_levels`.
  - dropped the `CharacterAcquiredRewardRepository` and `RewardResolverRegistry`
    dependencies (both still exist for legacy `LevelUpService` / homebrew, removed in R4/R8).
  - response DTO `CharacterRewardsResponse` is unchanged, so the FE needs no change.

Note: AUTO group grants are applied deterministically at level-up and are not recorded as
selections, so `/rewards` lists CHOICE selections only — consistent with the new model.

Verification:

- Backend `./gradlew compileJava`: exit 0.
- Backend `./gradlew test --tests *LevelUpQueryServiceTest --tests *LevelUpCommandServiceTest`:
  exit 0.

Remaining: full removal of the legacy `LevelUpController` / `LevelUpService` write path is
R8; a DB-backed `/rewards` read test is folded into R9.

## R3: Character Creation Persistence

Level-1 reward selections and known spells now persist through the content creation path.

- BE was already wired: `CreateContentCharacterRequest.initialRewardSelections` →
  `ContentCharacterCreationService.create(...)` → `LevelUpCommandService.applyInitialRewardSelections`,
  which validates choose-min/max and writes `character_reward_selection*`.
- FE wizard already builds `initialRewardSelections` from the level-1 content reward groups and
  includes it in submit. Confirmed; no change needed there.
- Spell persistence gap fixed. The wizard's spell step uses a local English 5e catalog and stored
  chosen spells by name, while submit hard-coded `cantripIds: []` / `spellIds: []` — so no spell
  ever reached the backend. `Spell` has no class association (`availableToClassIds` is always empty),
  so a fully content-driven spell step is not possible without a new relation; instead the display
  stays on the local catalog and we resolve the chosen English names to content spell ids:
    - BE: added `nameEn` to `SpellResponse` and populated it in `ReferenceDataService.mapSpell`.
    - FE: `SpellReferenceResponse.nameEn`; both reference hooks (`useGlobalReferenceContent`,
      `useCampaignReferenceContent`) now fetch spells; the wizard builds a `nameEn→id` map and
      maps the chosen cantrips/known spells to `cantripIds`/`spellIds`. Unmatched names abort the
      submit with a toast rather than silently dropping spells.

Verification: backend `compile` exit 0; frontend `tsc --noEmit` exit 0.

Remaining: e2e creation (skill/subclass/ASI/spell on level 1, real DB) is R9.

## R4: Homebrew Authoring On New Model Only

New-model authoring already exists end-to-end: aggregate `ClassAuthoringController`
(+ `ClassAuthoringService`) serves both admin-core and homebrew-package class
create/update with the normalized reward graph; the FE class-builder targets it.

Removed the remaining legacy class-write routes that wrote the old
`CharacterClass`/`ClassLevelReward` model:
- `AdminController`: `POST /character-classes/rich`, `POST /character-classes/import-json`,
  `PUT /character-classes/{id}/rich`. Also dropped the now-orphaned
  `HomebrewAuthoringService` field + import (used only by those three).
- `HomebrewController`: `POST /my/{packageId}/content/classes`.
- FE: dead `homebrewApi.createPackageCharacterClass` (+ its unused
  `CreateCharacterClassRequest` import) removed.

Deferred to R8 (legacy removal): the now-orphaned `HomebrewAuthoringService` class
methods (`createPackage*CharacterClassRich`, `updateStandard*Rich`, class-only private
helpers) and the legacy entities/DTOs — they no longer have active routes but stay
compilable until the R8 sweep.

Verification: backend `compileJava` exit 0; frontend `tsc --noEmit` exit 0.

Remaining: e2e (created homebrew class visible in content-reference/content-level-up) is R9.

## R5: Idempotency-Key For Class Create

Added a relational dedup store so a class-create request carrying an
`Idempotency-Key` header is not executed twice.

- New entity/table `class_authoring_idempotency` (changeset 061): `scope` +
  `idem_key` with a UNIQUE constraint, `request_hash` (SHA-256 of the serialized
  ClassWriteRequest), `result_class_id` (plain uuid — disposable TTL state, no FK),
  `package_id`, `created_at` (+ index for pruning).
- `ClassAuthoringService.createCoreClass`/`createPackageClass` now take an `idemKey`
  and route through `createIdempotent(...)`: on a repeated (scope, key) it replays the
  stored result (re-loading the class and rebuilding ClassSaveResult); the same key
  with a different body returns 412. The UNIQUE constraint is the real guard — a
  concurrent duplicate loses the insert race and its transaction rolls back, so no
  second class survives. TTL cleanup (24h) runs opportunistically on each keyed create
  (no scheduler infra introduced).
- `ClassAuthoringController` reads `Idempotency-Key` on both create endpoints
  (core + homebrew package) and passes it through.
- Test `ClassAuthoringServiceTest.idempotentCreate_dedup`: two POSTs with the same key
  return the same id and `classRepository.save` is invoked once. Existing create tests
  updated to the new 5-arg signature.

Verification: `compileJava`/`compileTestJava` exit 0; `ClassAuthoringServiceTest`
BUILD SUCCESSFUL.

## R6: Runtime Data Migration (full)

Extended `RuntimeDataMigrationService` from class/skill-only to every runtime FK column
that can still hold a legacy plural-table id, driven by a single generic remap engine.

- Inventory covered: `character_class_levels.class_id`, `character_skill_proficiencies.skill_id`,
  `character_stats.stat_type_id`, `character_wallets.currency_type_id`,
  `wallet_transactions.currency_type_id`, `character_known_spells.spell_id`,
  `characters.background_id`. Legacy `character_acquired_rewards.class_level_reward_id`
  is report-only.
- `RuntimeMigrationReport` refactored from two fixed fields (`classes`/`skills`) to a
  `List<EntityMigration> entities` — one block per column, each with
  alreadyNew/mapped/ambiguous/unmapped/rowsUpdated.
- Mapping strategy: legacy rows carry no slug, so matching is by name. class/skill read
  the legacy entity's 3 name variants (name + nameEngloc + nameRusloc) via the JPA legacy
  repos; stat/currency/spell/background read the legacy `name` straight from the plural
  table via JDBC (`legacyNamesFrom`, tolerates the table being absent on a fresh DB).
  Names normalized (trim+lowercase) and matched against content nameEn/nameRu. Unique
  match → applied; >1 candidate → ambiguous (never applied); 0 → unmapped. On a fresh DB
  every runtime id is already a content id → counted as alreadyNew, no writes.
- Dry-run by default; `confirmBackup=true` required before any write. Post-validation
  emits a per-column dangling count (runtime rows pointing at a non-existent content row).
- Legacy rewards: report-only per decision — the legacy flat reward and the new
  group/option selection models are incompatible, so auto-conversion would risk
  corrupting user data. Distinct legacy reward ids are counted for manual review.
- Liquibase 062 (`062-relax-runtime-content-fks-2.xml`): drops the physical FKs from
  stat_type_id/currency_type_id (×2)/spell_id/background_id to the legacy plural tables
  (mirrors 060 for class/skill), so a name-based UPDATE to a content id is not rejected.
  Idempotent (DROP IF EXISTS) with a rollback that re-adds the legacy FKs. Bestiary
  untouched.
- Test `RuntimeDataMigrationServiceTest` updated to the entities-list API (4 new repo
  mocks; lookups by target label): dry-run classification, reject-without-backup,
  apply-only-unique. BUILD SUCCESSFUL.

Verification: `compileJava`/`compileTestJava` exit 0; `RuntimeDataMigrationServiceTest`
BUILD SUCCESSFUL.

Remaining: real FKs to the new content model are re-added in R7; running the migration on
a real legacy-data snapshot is R9 (Testcontainers).

### R6 note: "currency ×2" clarification

Not a duplicate / not a v2. Two distinct runtime tables each carry their own
`currency_type_id` and both must be remapped to the new `currency` table:
`character_wallets` (FK fk_wallet_currency, changeset 022) and `wallet_transactions`
(FK fk_wallet_tx_currency, changeset 036). Hence `migrateCurrency(...)` runs twice (one
per table) and changeset 062 drops both legacy FKs. Confirmed correct, no change needed.

## R7: Final Cleanup Liquibase + Real FKs

Re-added real FKs from the runtime columns to the NEW content model and re-sequenced the
legacy-table drop.

- Changeset 063 (`063-runtime-content-real-fks.xml`): adds FK constraints
  character_class_levels.class_id→character_class, character_skill_proficiencies.skill_id→skill,
  character_stats.stat_type_id→ability_score, character_wallets.currency_type_id→currency,
  wallet_transactions.currency_type_id→currency, character_known_spells.spell_id→spell,
  characters.background_id→background. Each guarded with DROP CONSTRAINT IF EXISTS (idempotent)
  and a rollback. Verified target columns/PKs against changeset 054. Registered in master.xml.
- Safety: confirmed no seed (raw SQL or <insert>) writes the runtime/user tables, so they are
  empty on a fresh DB → FKs apply cleanly. On an existing DB the changeset is intentionally
  fail-fast: it errors if R6 left dangling rows, which is the desired gate.
- DECISION — legacy-table DROP moved from R7 to R8. `application.yml` has
  `spring.jpa.hibernate.ddl-auto: validate`, so Hibernate maps every legacy @Entity
  (CharacterClass/ProficiencySkill/StatType/CurrencyType/Spell/Background/Subclass/
  ClassLevelReward/CharacterAcquiredReward) at startup. Dropping a legacy table before its
  @Entity is removed would fail boot validation. Therefore entity removal (R8) and table drop
  must land together; R8 checklist updated accordingly. Bestiary untouched.
- No integration tests exist (suite is pure Mockito), so Liquibase changesets are not exercised
  by `gradlew test`; fresh-DB boot + changeset application is validated in R9 (Testcontainers).

Verification: XML mirrors the proven 060/062 pattern; column/PK names cross-checked against
054-create-dnd-content-schema.xml. Backend build unaffected (no Java change).

## R8: Legacy Removal — scope split (user granted full autonomy; old data disposable, app may break between steps)

Mapped the real blast radius. Two distinct sets:

LEGACY (entity → legacy plural table), but NOT all removable:
- CharacterClass→character_classes, Subclass→subclasses: still referenced by ~18 active files
  (NpcService, CampaignContentService, CampaignBlueprintService, CharacterService,
  ContentCharacterCreationService, LevelUpCommandService, ClassAuthoringService,
  ContentReferenceService, AdminService, ContentDataAuditService, ClassRewardSeedService,
  CharacterRewardQueryService, CharacterWizardService, LevelUpQueryService,
  CharacterClassContentValidator, HomebrewAuthoringService). Removing them = migrating those
  live features onto ContentCharacterClass — a real migration, deferred to R8b.
- ProficiencySkill→proficiency_skills: actively used by MonsterService (bestiary-adjacent!)
  and ReferenceDataService (skills reference API). NOT removed.

NOT legacy (entity already points at the NEW singular content table — leave alone):
- StatType→ability_score, CurrencyType→currency, Spell→spell, Background→background.

Orphan legacy tables (no entity; only read by R6 via raw JDBC): stat_types, currency_types,
spells, backgrounds — left in place for now (R6 still reads them); drop bundled with R8b.

### R8a (this step): remove the self-contained legacy level-up / reward cluster
Closed set verified by reference graph:
- LevelUpService, service/reward/* (RewardResolver, RewardResolverRegistry + 6 resolvers),
  ClassLevelReward (+repo), CharacterAcquiredReward (+repo), the two @Deprecated /legacy/*
  level-up endpoints on LevelUpController, and the dead class/reward methods of
  HomebrewAuthoringService (already off active routes since R4).
- Liquibase 064: DROP class_level_rewards + character_acquired_rewards (CASCADE).
- RuntimeDataMigrationService: drop reportLegacyRewards() (its source table is being removed;
  legacy rewards no longer exist).

### R8a — DONE (build verified)
Executed the closed-set removal above. Concrete changes:
- Deleted: LevelUpService.java; service/reward/ (RewardResolver, RewardResolverRegistry,
  AbilityScoreImprovement/BuffDebuff/Feat/Skill/Subclass RewardResolver);
  ClassLevelReward.java + ClassLevelRewardRepository.java;
  CharacterAcquiredReward.java + CharacterAcquiredRewardRepository.java.
- LevelUpController: trimmed to only GET /{id}/rewards (CharacterRewardQueryService);
  removed both /legacy/* endpoints and the LevelUpService field/import.
- HomebrewAuthoringService: removed all class/reward-authoring methods + fields
  (classRepository, subclassRepository, skillEffectRepository, classLevelRewardRepository,
  rewardResolverRegistry) and their imports (RewardResolverRegistry, RewardType, EffectRole);
  fixed a string-literal typo ("обязателen"→"обязателен").
- RuntimeDataMigrationService: removed reportLegacyRewards() and its call/notes.
- Deleted dead DTO/enum: ClassLevelRewardResponse, CreateClassLevelRewardRequest,
  HomebrewClassCreationResponse, CreateHomebrewClassRequest, RewardDetailDto,
  enums.RewardType. (No FE references exist — already removed in earlier steps.)
- Deleted dead tests LevelUpServiceTest, RewardResolverRegistryTest; removed @Mock refs to
  ClassLevelRewardRepository/RewardResolverRegistry from ItemTypeServiceTest & SkillServiceTest.
- Liquibase 064-drop-legacy-reward-tables.xml (registered in master.xml after 063):
  DROP TABLE IF EXISTS character_acquired_rewards, class_level_rewards (CASCADE). Safe because
  no @Entity maps these tables anymore, so ddl-auto:validate won't fault.
- Verification: ./gradlew compileJava compileTestJava → BUILD SUCCESSFUL.

### R8b — DONE (build verified)
Migrated all live consumers off the legacy CharacterClass/Subclass catalog onto the
content model, then removed the legacy entities + tables. Concrete changes:
- Entity associations retyped CharacterClass → ContentCharacterClass (import
  com.dnd.app.domain.content.ContentCharacterClass): CampaignNpc.characterClass,
  BlueprintNpc.characterClass, CharacterClassLevel.characterClass (kept NO_CONSTRAINT),
  CustomResourceType.classBound.
- Services migrated (repo field CharacterClassRepository → ContentCharacterClassRepository,
  accessor getName()/getNameRusloc() → getNameRu(), legacy getName() → getNameEn()):
  NpcService (resolveClass + display), CampaignContentService (getAvailableContent classes,
  isClassAvailableInCampaign), CampaignBlueprintService (resolveClass + npc display),
  CharacterService (createCharacter lookup + log).
- Homebrew validators: CharacterClassContentValidator (ContentCharacterClassRepository;
  getNameRu/getSubtitle), SubclassContentValidator (ContentSubclassRepository/ContentSubclass;
  getNameRu, description=null, getCharacterClass()).
- Mappers: CharacterMapper className expr getName()→getNameRu(); ReferenceDataMapper
  toCharacterClassResponse() removed.
- Admin: AdminService — removed class CRUD + subclass CRUD + toSubclassResponse helper +
  fields classRepository/subclassRepository. AdminController — removed GET /character-classes
  and all /subclasses endpoints.
- Character wizard removed: deleted CharacterWizardService.java and the /legacy/full routes
  in CampaignCharacterController & CharacterTemplateController (+ field/imports);
  ContentCharacterCreationService is now the sole creation flow (javadoc updated).
- RuntimeDataMigrationService: migrateClasses now reads legacy names from character_classes
  via JDBC (new helper legacyThreeNamesFrom: name, name_engloc, name_rusloc; tolerant of a
  missing table); removed CharacterClassRepository field/import. ProficiencySkill (skill
  migration) kept.
- Deleted legacy entities CharacterClass, Subclass + CharacterClassRepository,
  SubclassRepository; deleted dead DTOs CharacterClassResponse, CreateCharacterClassRequest,
  SubclassResponse, CreateSubclassRequest, CreateFullCharacterRequest.
- Liquibase 065-drop-legacy-class-subclass-tables.xml (registered in master.xml after 064):
  DROP TABLE IF EXISTS subclasses, character_classes (CASCADE). Safe: no @Entity maps them.
- Tests fixed to content model: CharacterServiceTest (ContentCharacterClass fixture),
  NpcServiceTest (ContentCharacterClassRepository + buildClass), RuntimeDataMigrationServiceTest
  (legacy names now stubbed via jdbc.query ResultSetExtractor instead of legacyClassRepository),
  removed stale @Mock from ItemTypeServiceTest/SkillServiceTest/HomebrewAuthoringServiceTest.
- Verification: ./gradlew compileJava compileTestJava → BUILD SUCCESSFUL; affected unit tests
  (RuntimeDataMigrationServiceTest, NpcServiceTest, CharacterServiceTest, SkillServiceTest,
  ItemTypeServiceTest, HomebrewAuthoringServiceTest) → BUILD SUCCESSFUL.
- Deferred (not R8b): orphan PHB tables stat_types/currency_types/spells/backgrounds remain —
  R6 still reads them via JDBC during the migration window.

### Hotfix (R7) — deploy-time FK failure at changeset 063 — DONE
- Symptom (log_error.txt, deploy 2026-06-18 16:21): Liquibase changeset
  063-runtime-content-real-fks failed — `character_class_levels.class_id =
  b0000000-0000-0000-0000-000000000005` not present in `character_class`; Spring context
  aborted (liquibase bean -> entityManagerFactory -> securityConfig cascade).
- Root cause: on an existing DB the R6 runtime-data migration (admin endpoint, not a changeset)
  had not repointed every legacy id onto the new content model, so the strict FKs in 063 fail
  on the first dangling row.
- Fix: new changeset 063a-cleanup-orphan-runtime-rows.xml, included in master.xml *before* 063.
  Purges dangling runtime rows for all 7 FKs added by 063 (character_class_levels,
  character_skill_proficiencies, character_stats, character_wallets, wallet_transactions,
  character_known_spells via DELETE; characters.background_id via SET NULL). Idempotent (no-op
  on a clean DB), empty rollback. Bestiary untouched; runtime/character data is disposable.
