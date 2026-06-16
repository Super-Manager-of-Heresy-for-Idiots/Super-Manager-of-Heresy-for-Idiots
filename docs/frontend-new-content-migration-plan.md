# Frontend Migration Plan for New Content Schema

## Scope

This repository currently contains backend code and frontend contract documentation, but no frontend source files.
The frontend migration must therefore be done as a contract-driven change.

Do not switch critical character creation or level-up screens to the new flow until backend endpoints expose the new DTOs behind stable contracts.

## Why the Frontend Must Change

The old frontend model assumes flat class data:

- class has `id`, `name`, `description`
- class mechanics are simple scalar fields or text
- level rewards are grouped by `rewardType`
- a reward selection is `{ rewardType, rewardEntryId }`
- ASI is a special extra object in `LevelUpRequest`

The new model is relational and more expressive:

- class has mechanics from dependent tables: saving throws, primary abilities, skill options
- class can have features by level
- level rewards are groups
- each group can have direct grants or selectable options
- each option/grant can have typed payloads: feature, subclass, feat, spell, skill, ability score, numeric modifier, custom text
- some rewards are fully automatic, some require user choice, some are manual/custom text

The frontend must stop treating rewards as a flat list and start rendering a decision tree.

## Safe Migration Rule

Keep current screens working as legacy mode until backend is ready.

Recommended frontend adapter shape:

- `legacyClassApi`: current endpoints and current DTOs
- `contentClassApi`: new endpoints and new DTOs
- `ClassCreationViewModel`: normalized UI model used by the screen
- `LevelUpViewModel`: normalized UI model used by the screen

The screen should depend on view models, not directly on backend DTOs.

This lets the frontend support old and new payloads during rollout without touching unrelated character sheet, campaign, inventory, bestiary, or combat flows.

## New Frontend Types

Add frontend-only types before changing UI behavior.

```ts
type ContentLabel = {
  id: string;
  slug?: string;
  name: string;
  nameEn?: string;
  nameRu?: string;
};

type ClassMechanics = {
  hitDie: number;
  primaryAbilities: ContentLabel[];
  savingThrows: ContentLabel[];
  skillChoiceCount: number;
  skillChoiceAny: boolean;
  skillOptions: ContentLabel[];
  armorProficiencyText?: string;
  weaponProficiencyText?: string;
  toolProficiencyText?: string;
  spellcasting?: {
    spellcaster: boolean;
    spellcastingAbility?: ContentLabel;
    hasCantrips: boolean;
    halfCaster: boolean;
  };
};

type ClassFeatureSummary = {
  id: string;
  slug?: string;
  level: number;
  title: string;
  description?: string;
};

type RewardGroupViewModel = {
  id: string;
  classLevel: number;
  groupKind: string;
  prompt: string;
  description?: string;
  chooseMin: number;
  chooseMax: number;
  repeatable: boolean;
  grants: RewardGrantViewModel[];
  options: RewardOptionViewModel[];
};

type RewardOptionViewModel = {
  id: string;
  optionKey: string;
  label: string;
  description?: string;
  recommended: boolean;
  grants: RewardGrantViewModel[];
};

type RewardGrantViewModel = {
  id: string;
  grantType: string;
  label?: string;
  description?: string;
  payload:
    | { kind: "feature"; feature: ClassFeatureSummary }
    | { kind: "subclass"; subclass: ContentLabel }
    | { kind: "feat"; feat?: ContentLabel; chooseCount?: number }
    | { kind: "spell"; spell?: ContentLabel; spellLevel?: number; chooseCount?: number; rawFilterText?: string }
    | { kind: "skill"; fixedSkill?: ContentLabel; chooseCount?: number; anySkill?: boolean; skillOptions?: ContentLabel[] }
    | { kind: "ability"; fixedAbility?: ContentLabel; chooseCount?: number; bonusPerChoice?: number; totalBonus?: number; maxPerAbility?: number; maxScore?: number; abilityOptions?: ContentLabel[] }
    | { kind: "numeric"; modifierKey: string; targetKind?: string; amount?: number; unitText?: string; durationText?: string }
    | { kind: "custom"; title?: string; body: string; userEditable: boolean }
    | { kind: "unknown"; rawLabel?: string };
};
```

Do not use these types to infer database structure in UI. They are a user-friendly render model.

## API Client Changes

### Reference Data

Current class endpoint:

- `GET /api/campaigns/{campaignId}/reference/classes`

Frontend should keep this endpoint but expect a richer response once backend migrates.

Required frontend behavior:

- tolerate missing new fields
- render old fields when new fields are absent
- prefer new arrays over old single fields:
  - `primaryAbilities[]` over `primaryAbilityStatId`
  - `savingThrows[]` over `savingThrowStatNames`
  - `skillOptions[]` over old `skillChoiceOptions`
- split proficiency text into three displayed rows:
  - armor
  - weapons
  - tools
- do not try to parse proficiency text into individual selectable IDs

### Level Up

Current level-up endpoints:

- `GET /api/characters/{id}/level-up-options`
- `POST /api/characters/{id}/level-up`
- `GET /api/characters/{id}/rewards`

Frontend must support both request formats during migration.

Legacy request:

```ts
type LegacyLevelUpRequest = {
  classId: string;
  selections?: { rewardType: string; rewardEntryId: string }[];
  abilityScoreImprovement?: {
    increases: { statTypeId: string; amount: number }[];
  };
};
```

New request target:

```ts
type ContentLevelUpRequest = {
  classId: string;
  rewardSelections: {
    groupId: string;
    optionId?: string;
    abilityScoreSelections?: { grantId: string; abilityScoreId: string; bonusAmount: number }[];
    skillSelections?: { grantId: string; skillId: string }[];
    spellSelections?: { grantId: string; spellId: string }[];
    noteText?: string;
  }[];
};
```

Until backend exposes the new request, the frontend adapter should convert only legacy-compatible groups to the legacy body and block unsupported groups with a clear disabled state.

## Character Creation Screen

The class selection step needs the largest rewrite.

### Current Assumption

User picks a class, maybe sees a short description, then the wizard proceeds.

### New UI Structure

Use a class detail panel with compact sections:

- name, subtitle/source badge, homebrew badge
- hit die
- primary abilities
- saving throws
- skill choices: `choose N`, options, or `any skill`
- armor proficiency text
- weapon proficiency text
- tool proficiency text
- spellcasting summary
- level 1 automatic grants
- level 1 required choices

### Required Interaction Changes

If a class has level 1 reward groups:

- render automatic grants as read-only rows
- render choice groups as required form controls
- validate `chooseMin` / `chooseMax`
- show selected grants preview before character submit
- store pending reward selections in wizard state

### User-Friendly Homebrew Handling

Homebrew classes may have unknown grant types.

For unknown/custom grant types:

- show label and description
- allow user note when `userEditable = true`
- do not block character creation if the grant is informational
- block only if backend marks the group as required and no valid option is selected

### What Not to Change Yet

Do not change:

- race/species step
- background step except ID compatibility
- stat rolling/allocation UX
- inventory setup
- character sheet display
- campaign membership
- bestiary

## Level-Up Screen

The level-up modal/page must move from "pick a reward from a flat group" to "resolve level-up decisions".

### New Layout

For each available class option:

- class name and current/new class level
- HP gain
- derived changes
- automatic grants for this level
- required reward groups
- optional/manual reward groups
- final preview

### Reward Group Rendering Rules

Render by group structure first, grant type second.

- `chooseMax = 0` or no options: automatic group
- `chooseMin = 1`, `chooseMax = 1`: radio group
- `chooseMin = chooseMax > 1`: checkbox group with exact count
- `chooseMin < chooseMax`: checkbox group with min/max validation
- `repeatable = true`: allow same logical option multiple times only if backend supports it

### Grant Type UI

- `feature`: read-only feature card
- `subclass`: radio/select card
- `feat`: feat picker or read-only fixed feat
- `spell`: spell picker filtered by backend-provided options or raw filter text
- `skill`: skill picker, fixed skill row, or any-skill selector
- `ability`: ASI allocation control
- `numeric`: read-only modifier preview
- `custom`: note/manual grant panel
- unknown grant type: render generic panel with label/description and optional note

### ASI Control

Replace the special hard-coded ASI block with a generic ability grant renderer.

It must support:

- fixed `+1` to one ability
- choose one ability and add `+1`
- choose two abilities and add `+1/+1`
- choose one ability and add `+2`
- homebrew total bonus values
- max score cap
- max per ability

Validation must run client-side before submit and backend-side after submit.

## Homebrew Class Authoring UI

The old "create class" form is too small for the new model.

Split it into tabs or steps:

1. Identity
   - slug
   - name RU/EN
   - subtitle
   - source/mod metadata if exposed
2. Core Mechanics
   - hit die
   - primary abilities
   - saving throws
   - skill choice count
   - any skill toggle
   - skill option multiselect
   - armor/weapon/tool proficiency textareas
   - spellcasting settings
3. Features
   - level
   - title
   - description
   - subclass-specific toggle
4. Reward Groups
   - class level
   - prompt
   - choose min/max
   - repeatable
   - direct grants
   - options
5. Grants
   - grant type selector
   - typed payload editor
   - custom text fallback
6. Review
   - show what character creation and level-up will display

Important: typed grant editors should always include a "custom text/manual" fallback so homebrew users can model rewards the system does not understand yet.

## Frontend Validation Rules

Class creation:

- class name required
- hit die positive
- skill choice count >= 0
- if `skillChoiceAny = false`, skill options count should be >= skill choice count
- spellcasting ability required only when spellcaster is true

Reward group:

- `chooseMin <= chooseMax`
- if options exist, `chooseMax` should not exceed options count unless repeatable
- direct grants and options may coexist, but UI must clearly separate automatic grants from selected option grants

Ability grant:

- total selected bonus must match total/required bonus if provided
- no ability can exceed max score
- no ability receives more than max per ability

Skill/spell grant:

- exact number of picks when choose count is fixed
- allow custom/manual note only for custom grants, unknown grants, or backend-permitted manual grants

## Rollout Order

1. Add new frontend types and DTO adapters.
2. Keep current screens on legacy mode.
3. Update class detail rendering to display new mechanics when present.
4. Add reward group renderer behind a feature flag or payload detection.
5. Use the renderer in level-up first because it is isolated.
6. Use the same renderer in character creation for level 1 choices.
7. Add homebrew class authoring last.
8. Remove legacy reward rendering only after backend no longer returns old reward DTOs.

## Critical Functionality Guardrails

Do not touch these flows during the first frontend migration slice:

- bestiary
- combat
- inventory
- quest rewards
- wallet/currency operations
- campaign membership
- existing character sheet read-only view

The first safe frontend change is adapter-only: add types, normalize payloads, and keep current UI output identical for old DTOs.
