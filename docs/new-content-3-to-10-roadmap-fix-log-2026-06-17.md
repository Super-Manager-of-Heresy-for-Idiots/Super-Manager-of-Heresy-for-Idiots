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
