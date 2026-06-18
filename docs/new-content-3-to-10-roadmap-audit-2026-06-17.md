# Audit: New Content Migration Roadmap Compliance

Date: 2026-06-17

Scope:

- Backend: `C:\SuperHerecy\SuperManagerofHeresyforIdiots\SuperManagerofHeresyforIdiots`
- Frontend: `C:\SuperHerecy\Super-Manager-of-Heresy-for-Idiots-frontend`
- Roadmap: `docs/new-content-3-to-10-roadmap.md`

Purpose: verify whether the latest backend and frontend commits/working tree actually satisfy the migration roadmap from 3/10 to 10/10.

## Audit Log

### 2026-06-17 Initial Snapshot

Backend repository:

- Branch: `migraciya))`
- HEAD: `04470e4 Phase 12 (safe subset): убрать замещённые legacy content-эндпоинты, archive-only`
- Working tree: only Gradle lock files were modified at audit start.
- Recent migration commits visible in log: Phase 1 through Phase 12.

Frontend repository:

- Branch: `migraciya))`
- HEAD: `f4b6961 docs: зафиксированы post-audit закрытия расхождений в migration-доке`
- Working tree: clean at audit start according to `git status --short --branch`.
- Recent migration commits visible in log: frontend phases 1 through 12 plus post-audit class builder fixes.

Comparison baseline:

- The user did not name an explicit fixed point.
- For this audit, the primary check is current-code compliance with the roadmap.
- Diff-oriented checks will use the visible pre-migration commits only as supporting evidence, not as the sole source of truth.

## Interim Findings

### Backend Pass 1: Runtime/API Compliance

Status: **not 10/10**. The backend contains substantial new-model work, but the current code does not satisfy the roadmap's Phase 11/12 or Final 10/10 definition.

Implemented or partially implemented:

- New read-only class reference exists:
  - `src/main/java/com/dnd/app/controller/ContentReferenceController.java`
  - `src/main/java/com/dnd/app/service/ContentReferenceService.java`
- New content DTO/model surface exists:
  - `ContentClassDetailResponse`, `RewardGroupDto`, `RewardOptionDto`, `RewardGrantDto`
  - typed grant payloads under `src/main/java/com/dnd/app/dto/content/grant/`
- New character creation service exists:
  - `src/main/java/com/dnd/app/controller/ContentCharacterController.java`
  - `src/main/java/com/dnd/app/service/ContentCharacterCreationService.java`
- New level-up read/commit services exist:
  - `src/main/java/com/dnd/app/controller/ContentLevelUpController.java`
  - `src/main/java/com/dnd/app/service/LevelUpQueryService.java`
  - `src/main/java/com/dnd/app/service/LevelUpCommandService.java`
- New aggregate class authoring exists:
  - `src/main/java/com/dnd/app/controller/ClassAuthoringController.java`
  - `src/main/java/com/dnd/app/service/ClassAuthoringService.java`
- New admin visibility/data-quality endpoints exist:
  - `src/main/java/com/dnd/app/controller/AdminContentController.java`
  - `src/main/java/com/dnd/app/service/ContentDataAuditService.java`
- Runtime migration service exists:
  - `src/main/java/com/dnd/app/service/RuntimeDataMigrationService.java`

Major roadmap gaps found:

1. **Final endpoints were not replaced in-place.**
   - Roadmap Phase 2/3/6/7 recommends final routes such as `GET /api/reference/classes`, `GET /api/campaigns/{campaignId}/reference/classes`, `GET /api/characters/{id}/level-up-options`, `POST /api/characters/{id}/level-up`.
   - Backend instead adds parallel `/content` routes:
     - `ContentReferenceController`: `/api/reference/content/classes`, `/api/campaigns/{campaignId}/reference/content/classes`.
     - `ContentLevelUpController`: `/api/characters/{characterId}/content/level-up-options`, `/api/characters/{characterId}/content/level-up`.
   - The old in-place level-up routes are still active in `LevelUpController`.

2. **Legacy level-up remains an active runtime path.**
   - `LevelUpController` still exposes:
     - `GET /api/characters/{id}/level-up-options`
     - `POST /api/characters/{id}/level-up`
     - `GET /api/characters/{id}/rewards`
   - These call `LevelUpService` and `CharacterRewardQueryService`.
   - `LevelUpService` still uses `CharacterClassRepository`, `ClassLevelRewardRepository`, `CharacterAcquiredRewardRepository`, `RewardResolverRegistry`, `rewardType`, `rewardEntryId`.
   - This directly violates Phase 7 ("Delete old `character_acquired_rewards` write path from active level-up flow") and Phase 12 ("Remove old service dependencies").

3. **Legacy full character creation remains active.**
   - `CharacterTemplateController` still exposes `POST /api/characters/full`.
   - `CampaignCharacterController` still exposes `POST /api/campaigns/{campaignId}/characters/full`.
   - Both use `CharacterWizardService`, which still depends on legacy `CharacterClassRepository` and `ProficiencySkillRepository`.
   - New creation exists only on parallel `/content` routes, not as the normal active route.

4. **New content character creation does not implement level-1 reward selection persistence.**
   - `CreateContentCharacterRequest` has `chosenSkillIds`, spells, coins, etc., but no reward-group/option selection model.
   - `ContentCharacterCreationService` creates class level, stats, skills, spells, wallet, but does not persist `character_reward_selection` for level-1 reward groups.
   - This leaves Phase 5 incomplete.

5. **New character creation and new level-up mix `ContentSkill` IDs into legacy `ProficiencySkill` entity slots.**
   - `ContentCharacterCreationService` validates `chosenSkillIds` against `ContentSkill`, then writes `CharacterSkillProficiency.skill` via `entityManager.getReference(ProficiencySkill.class, skillId)`.
   - `LevelUpCommandService` does the same for `SKILL_PROFICIENCY` grants.
   - `CharacterSkillProficiency.skill` is still typed as `ProficiencySkill`; comments say the FK is relaxed so raw IDs may point to either table.
   - This is a fragile transitional bridge, not a final normalized runtime model. It also means normal entity navigation can point at non-existent `proficiency_skills` rows when IDs are from `skill`.

6. **Legacy admin class/reward tools remain active.**
   - `AdminController` still exposes old class admin and level reward endpoints:
     - `/api/admin/character-classes`
     - `/api/admin/character-classes/rich`
     - `/api/admin/character-classes/import-json`
     - `/api/admin/classes/{classId}/level-rewards`
     - `/api/admin/proficiency-skills`
   - These still route to `AdminService` or old `HomebrewAuthoringService` paths using old `CharacterClass`, `ClassLevelReward`, `ProficiencySkill`.
   - This conflicts with Phase 9/12 ("Admin no longer needs old `class_level_rewards`", "Remove old endpoints from active backend routes").

7. **Legacy homebrew authoring service is still present and active through old endpoints.**
   - New `ClassAuthoringService` writes the new aggregate class graph.
   - But `HomebrewAuthoringService` still creates `CharacterClass` and `ClassLevelReward` in multiple paths.
   - `HomebrewController` still exposes `POST /api/homebrew/my/{packageId}/content/classes`, which uses the old `CreateCharacterClassRequest` path.
   - Admin rich/import routes still call old rich class authoring.

8. **Runtime migration is narrower than roadmap Phase 10.**
   - `RuntimeDataMigrationService` migrates only:
     - `character_class_levels.class_id`
     - `character_skill_proficiencies.skill_id`
   - The roadmap required inventory/mapping/reporting for background, stat, wallet/currency, known spells, class levels, acquired rewards, and old reward target IDs.
   - No legacy rewards -> `character_reward_selection` migration was found.

9. **No final cleanup Liquibase migration found.**
   - `master.xml` ends at `060-relax-runtime-content-fks.xml`.
   - `060` only drops runtime FKs for transitional mixed-ID storage.
   - No changeset found for dropping/archive-only DB cleanup of old non-bestiary PHB tables, migrating `character_acquired_rewards`, or re-adding real FKs to the new model.

10. **Tests are broad but mostly mock-based and do not prove 10/10 integration.**
    - There are unit tests for new services (`ContentReferenceServiceTest`, `ContentCharacterCreationServiceTest`, `LevelUpQueryServiceTest`, `LevelUpCommandServiceTest`, `ClassAuthoringServiceTest`, `RuntimeDataMigrationServiceTest`).
    - Most are Mockito unit tests; no Testcontainers/Postgres or end-to-end migration test evidence was found.
    - This does not satisfy Phase 12 final integration tests: fresh DB, migrated DB, creation, level-up, homebrew class, admin edit, and no bestiary regression.

Backend provisional verdict:

- **Roadmap phases 2-9: partially implemented.**
- **Phase 10: partial.**
- **Phase 11: partial/failed for normal routes.**
- **Phase 12 / Final 10/10: not satisfied.**

### Frontend Pass 1: Runtime/API Compliance

Status: **not 10/10**. The frontend contains real new-content UI work, especially around reward rendering, level-up request building, and aggregate class authoring. However, normal runtime flows still call old or mismatched endpoints, and several new UI pieces cannot work end-to-end against the current backend.

Implemented or partially implemented:

- New final-contract types exist in `src/types/index.ts`:
  - `ContentLevelUpRequest` and `ContentRewardSelection` at lines 215-227.
  - `RewardGroup`, `ContentRewardOption`, `ContentRewardGrant` at lines 280-320.
  - `ClassWriteRequest`, `RewardGroupInput`, `ClassSaveResult` at lines 849-900.
- Reward group adapter/renderer support exists:
  - `src/lib/contentAdapters.ts` normalizes `RewardGroup` and detects content-shaped groups at lines 24-60 and 92-115.
  - `src/components/content-rewards/*` is used from creation, level-up, dev viewer, and class builder.
- Level-up UI can assemble a content-shaped request:
  - `src/pages/gm/campaigns/contentLevelUp.ts` builds `ContentLevelUpRequest` at lines 134-158.
  - `src/pages/gm/campaigns/LevelUpWizardPage.tsx` renders content reward groups and uses `buildContentLevelUpRequest` at lines 133-164 and 510-521.
- Character creation UI reads class details and can render level-1 reward groups:
  - `src/features/character-wizard/steps.tsx` renders `RewardGroupView` for class reward groups at lines 311-315 and 484-500.
  - `src/features/character-wizard/rewardSelection.ts` validates chooseMin/chooseMax at lines 37-63.
- Aggregate class builder exists:
  - `src/features/class-builder/ClassBuilderModal.tsx` builds/saves `ClassWriteRequest` at lines 131-142.
  - `src/features/class-builder/classDraft.ts` has draft/request/validation logic at lines 32-150.
  - `src/features/class-builder/refData.ts` wires dropdown reference data at lines 27-57.

Major roadmap gaps found:

1. **Frontend and backend do not agree on final class reference endpoints.**
   - Roadmap Phase 2 recommends final in-place routes `GET /api/reference/classes` and `GET /api/campaigns/{campaignId}/reference/classes` (roadmap lines 200-207).
   - Roadmap Phase 3 also says to replace old reference behavior in-place or route existing endpoints through the new service (line 276).
   - Frontend calls:
     - `src/api/reference.api.ts:49-50` -> `/reference/classes`.
     - `src/api/homebrew-campaign.api.ts:50-51` -> `/campaigns/${campaignId}/reference/classes`.
   - Backend currently exposes new class details only at:
     - `ContentReferenceController.java:34` -> `/api/reference/content/classes`.
     - `ContentReferenceController.java:53` -> `/api/campaigns/{campaignId}/reference/content/classes`.
   - Backend `ReferenceController.java:26-27` and `VanillaReferenceController.java:25-26` explicitly say legacy class reference endpoints were removed and superseded by `/reference/content/classes`.
   - Result: frontend class reference calls hit endpoints that are absent in the current backend.

2. **Frontend level-up UI builds a final request but posts it to the legacy route.**
   - Roadmap Phase 6/7 requires the final level-up read/submit contract and removal of old submit path (lines 468-481 and 539-550).
   - Frontend `src/api/levelup.api.ts` calls:
     - `GET /characters/${characterId}/level-up-options` at line 12.
     - `POST /characters/${characterId}/level-up` at line 18.
     - `GET /characters/${characterId}/rewards` at line 23.
   - Backend `LevelUpController.java:26-39` still maps those old routes to legacy `LevelUpService`.
   - Backend new content level-up routes are different:
     - `ContentLevelUpController.java:40` -> `/api/characters/{characterId}/content/level-up-options`.
     - `ContentLevelUpController.java:52` -> `/api/characters/{characterId}/content/level-up`.
   - Result: the new `ContentLevelUpRequest` from `LevelUpWizardPage.tsx:155-162` is sent to the old controller DTO/service path, not to the new content command service.

3. **Character creation remains wired to legacy full-character endpoints.**
   - Roadmap Phase 5 requires final create request and service flow with new IDs and initial reward selections when level 1 has choices (lines 377-390), and frontend state for reward selections including child choices (lines 407-410).
   - Frontend `src/api/characters-full.api.ts` still documents fallback behavior for an endpoint that "may not exist yet" at lines 5-7.
   - Frontend posts:
     - `src/api/characters-full.api.ts:93-95` -> `/campaigns/${campaignId}/characters/full`.
     - `src/api/characters-full.api.ts:118-120` -> `/characters/full`.
   - `src/hooks/useCreateFullCharacter.ts:15-18` still falls back to the basic create endpoint if the aggregate endpoint is missing.
   - Backend `CampaignCharacterController.java:37-44` and `CharacterTemplateController.java:32-39` still map `/full` routes to legacy `CharacterWizardService`.
   - Backend new content creation is separate:
     - `ContentCharacterController.java:35` -> `/api/campaigns/{campaignId}/characters/content`.
     - `ContentCharacterController.java:48` -> `/api/characters/content`.
   - Result: normal FE creation does not use the new content creation controller.

4. **Level-1 reward selections in character creation are UI-only and not submitted.**
   - `src/features/character-wizard/wizardState.ts:42-43` stores `contentRewardSelections`.
   - `src/features/character-wizard/steps.tsx:484-500` renders and updates reward selections.
   - But `src/features/character-wizard/CharacterCreationWizard.tsx:378-413` builds `CreateFullCharacterRequest` without `contentRewardSelections`, reward group IDs, option IDs, or child selections.
   - The comment in `steps.tsx:311-313` says the selections are prepared but not submitted and wiring is deferred.
   - Result: Phase 5's "initial reward selections" requirement is not implemented end-to-end.

5. **Admin class reward legacy UI remains active.**
   - Roadmap Phase 9 says to replace old admin class level reward endpoints and that admin should no longer need old `class_level_rewards` (lines 670 and 701-704).
   - Roadmap Phase 11/12 says to remove old rewardType-only assumptions and old API clients (lines 796-805 and 871-873).
   - Frontend still has route `src/router.tsx:290` -> `/admin/character-classes/:classId/rewards` rendering `LevelRewardsPage`.
   - `src/pages/admin/LevelRewardsPage.tsx` is built around `ClassLevelRewardResponse`, `rewardType`, `rewardId`, and `isChoice`:
     - reward types at lines 44-46.
     - legacy reward display at lines 62-69.
     - legacy create payload at lines 131-140.
   - `src/api/admin.api.ts:191-200` calls `/admin/classes/${classId}/level-rewards`.
   - `src/hooks/useAdmin.ts:440-472` keeps the old `level-rewards` query/mutations.
   - Backend still exposes this legacy endpoint in `AdminController.java:400-417`.
   - Result: old class-level reward authoring remains a normal admin workflow.

6. **Class builder is substantial but not fully contracted with backend.**
   - Frontend `src/api/classAuthoring.api.ts:29-72` assumes:
     - GET detail for edit at `${basePath(scope)}/${id}`.
     - create with `Idempotency-Key`.
     - update with `If-Match`.
   - Backend `ClassAuthoringController.java:41-119` only provides POST/PUT/DELETE for new authoring. No GET detail endpoint is present there, no ETag header handling is visible, and no idempotency key handling is visible in the controller signature.
   - For admin GET, the same path is served by old `AdminController.java:127-130`, returning old `CharacterClassResponse`, not `ContentClassDetailResponse`.
   - For homebrew GET `/api/homebrew/packages/{packageId}/classes/{classId}`, no backend mapping was found.
   - Result: create may work for some cases, but edit/concurrency/idempotency flows are not proven and partly point at old or missing backend routes.

7. **Class builder reference dropdown endpoints are missing on backend.**
   - Frontend `src/api/reference.api.ts` calls:
     - `/reference/abilities` at lines 87-89.
     - `/reference/feats` at lines 93-97.
     - `/reference/modifier-keys` at lines 101-103.
   - `src/features/class-builder/refData.ts:27-57` uses those calls for builder dropdowns.
   - Backend controller search found reference endpoints for races, backgrounds, skills, stat-types, currencies, spells, and content classes, but no `/api/reference/abilities`, `/api/reference/feats`, or `/api/reference/modifier-keys`.
   - Result: parts of the new builder UI depend on reference APIs that appear not to exist.

8. **Old API clients and reward type assumptions remain in runtime source.**
   - `src/types/index.ts:652-667` still defines old `ClassLevelRewardResponse` and `CreateClassLevelRewardRequest`.
   - `src/types/index.ts:410` and `LevelUpWizardPage.tsx:813` still render acquired rewards via `rewardType`.
   - Some use of rewardType in result display may be acceptable if the new result intentionally summarizes applied grants that way, but the old admin class-level-reward path is definitely still active.

9. **Frontend tests exist, but they do not prove the roadmap final state.**
   - Found fixture/unit tests:
     - `src/__tests__/contentModel.test.ts`
     - `src/__tests__/contentLevelUp.test.ts`
     - `src/__tests__/classDraft.test.ts`
     - `src/__tests__/characterMigration.test.ts`
   - These prove helper logic around the new model, not FE<->BE endpoint compatibility or final user workflows.
   - Roadmap Phase 11/12 requires full frontend QA/regression for creation, level-up, authoring, campaign content availability, character sheet, and admin tools (lines 812-823 and 876-882).

Frontend provisional verdict:

- **Phases 3-4: partially implemented, but class reference endpoint mismatch breaks actual BE integration.**
- **Phase 5: partial UI only; normal creation still legacy and level-1 reward selections are not submitted.**
- **Phase 6-7: partial UI/helper logic; normal API client still uses old level-up routes.**
- **Phase 8: partial; builder exists, but edit/ref-data/concurrency contracts are incomplete or mismatched.**
- **Phase 9: partial; content quality exists, but old admin level reward page remains active.**
- **Phase 11: failed for normal workflows, because FE still calls old/missing non-bestiary content endpoints.**
- **Phase 12 / Final 10/10: not satisfied.**

### Cross-Project Contract Verdict

The latest BE and FE changes do **not** collectively satisfy the roadmap.

Most important blockers:

1. Final routes were not replaced in-place. BE added `/content` routes while FE partly uses the recommended in-place routes and partly still uses old legacy routes.
2. Character creation normal FE flow calls legacy `/characters/full`, not new `/characters/content`.
3. Level-up normal FE flow sends a new-shaped request to legacy `/characters/{id}/level-up`.
4. Class reference normal FE flow calls `/reference/classes`, but BE removed that class endpoint and exposes `/reference/content/classes`.
5. Old admin class-level reward workflow remains active in both BE and FE.
6. New class builder is real but not fully backed by matching GET/ref-data/ETag/idempotency endpoints.

Current practical score estimate:

- Backend: around **6/10 to 7/10**, not 10/10.
- Frontend: around **5.5/10 to 6.5/10**, not 10/10.
- Integrated product: around **5/10 to 6/10**, because several normal workflows are contract-broken despite substantial new-model code.

### Verification Commands

Commands run after the code audit:

- Frontend `npm test`
  - Result: passed.
  - Summary: 4 test files, 37 tests.
- Frontend `npm run build`
  - Result: passed.
  - Notes: Vite emitted the existing large chunk warning for `index-*.js`, but build completed.
- Backend `.\gradlew.bat test`
  - Result: passed with exit code 0.
  - Notes: Gradle produced no detailed console output in this run, but the process completed successfully.

Working tree after verification:

- Frontend: clean.
- Backend:
  - Existing Gradle lock file modifications remain:
    - `.gradle/8.12.1/executionHistory/executionHistory.lock`
    - `.gradle/8.12.1/fileHashes/fileHashes.lock`
    - `.gradle/buildOutputCleanup/buildOutputCleanup.lock`
  - New audit file:
    - `docs/new-content-3-to-10-roadmap-audit-2026-06-17.md`

Final audit verdict:

The developer's claim that almost the entire roadmap is complete is **not supported** by the current backend/frontend state. The implementation has many useful building blocks for the new model, but it is still a hybrid migration with active legacy runtime routes, active legacy admin tools, mismatched FE/BE contracts, incomplete character creation persistence, incomplete runtime data migration, and no final cleanup migration.

## Resolution Addendum (2026-06-17, post-audit)

Contract blockers closed since the audit (see fix-log Steps 1-3):

- **Cross-project blocker #4 (class reference endpoint mismatch): RESOLVED.**
  `ContentReferenceController` now serves the in-place routes the FE already calls
  (`/api/reference/classes`, `/api/campaigns/{campaignId}/reference/classes`) alongside
  the `/content` aliases.
- **Cross-project blockers #2/#3 (creation/level-up route mismatch): RESOLVED at the
  route level.** New content controllers now serve the stable `/full` and
  `/level-up[-options]` routes; legacy paths were moved under `/legacy/*`.
- **FE gap #5 (legacy admin class-level reward UI): RESOLVED.** `LevelRewardsPage` and its
  route deleted; admin reward hooks/api methods removed; legacy reward + class CRUD
  endpoints removed from `AdminController` / `AdminService` (this also fixed an ambiguous
  Spring mapping that blocked startup).
- **FE gap #6 (class-builder GET/ETag/If-Match): RESOLVED.** `ClassAuthoringController`
  now has detail GETs returning `ContentClassDetailResponse` with a deterministic
  SHA-256 content-hash ETag; updates honor `If-Match` and return `412` on conflict.
- **FE gap #7 (builder reference dropdown endpoints): RESOLVED.** `/api/reference/abilities`,
  `/api/reference/feats`, `/api/reference/modifier-keys` now exist.

Still open after this addendum (carried into the new roadmap):

- BE gap #2 (legacy level-up still an active runtime write path), gap #3/#11 (legacy full
  creation still active), gap #4 (level-1 reward selection not persisted), gap #5 (mixed
  `ContentSkill`/`ProficiencySkill` IDs), gap #7 (legacy homebrew authoring writes old
  model), gap #8 (runtime data migration narrower than Phase 10), gap #9 (no final
  cleanup Liquibase), gap #10 (no Testcontainers/E2E integration tests).
- `Idempotency-Key` on class create is accepted but ignored (no dedup store).

Revised score estimate after blockers closed:

- Backend: ~**7/10**.
- Frontend: ~**7/10** (normal builder/admin/reference workflows now contract-aligned).
- Integrated product: ~**6.5/10**. The remaining distance to 10/10 is the deep runtime
  switch-over (level-up commit, creation persistence, ID unification, data migration,
  legacy removal, integration testing), not contract plumbing.
