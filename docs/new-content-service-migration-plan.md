# New Content Schema Service Migration Plan

## Goal

Move application reads/writes from the old plural content model to the new normalized singular content model:

- old: `character_classes`, `subclasses`, `skills`, `proficiency_skills`, `class_level_rewards`, `backgrounds`, `spells`, `feats`, `stat_types`, `currency_types`
- new: `character_class`, `subclass`, `class_feature`, `skill`, `class_level_reward_*`, `background`, `spell`, `feat`, `ability_score`, `currency`

Bestiary and bestiary-adjacent tables are out of scope and must not be touched.

## Current State Snapshot

- Already mostly new-schema mapped:
  - `StatType` -> `ability_score`
  - `Spell` -> `spell`
  - `Feat` -> `feat`
  - `Background` -> `background`
  - `CurrencyType` -> `currency`
- Still legacy/plural mapped:
  - `CharacterClass` -> `character_classes`
  - `Subclass` -> `subclasses`
  - `Skill` -> `skills` (old active class abilities)
  - `ProficiencySkill` -> `proficiency_skills`
  - `CharacterClassLevel` -> `character_class_levels.class_id` pointing at old `character_classes`
  - `ClassLevelReward` -> `class_level_rewards`
  - `CharacterAcquiredReward` -> `character_acquired_rewards`
  - `CharacterSkillProficiency` -> old `proficiency_skills`
- New content import currently writes directly via `DndContentLoader` into singular tables.
- New class mechanics and reward choice tables exist in migrations and now have JPA/repository coverage.
- Service/controller migration is still pending and must be done in controlled slices.

## Step 1 - Retire Old Plural Data First

Status: in progress

Purpose: stop expanding the old model and define the bridge from old character-owned state to new content IDs.

Tasks:

- [x] Mark old plural content tables as legacy in code comments/docs and stop adding features to them.
- [x] Inventory every runtime FK from character/player data to legacy content tables:
  - `characters.race_id`
  - `characters.background_id`
  - `character_class_levels.class_id`
  - `character_skill_proficiencies.skill_id`
  - `character_known_spells.spell_id`
  - `character_stats.stat_type_id`
  - `class_level_rewards.*`
  - `character_acquired_rewards.*`
- [x] Decide per FK whether it migrates now or later:
  - migrate now: class, subclass, class level rewards, class feature/reward choices
  - preserve for now if risky: races/species until the race/species workflow is handled
- [ ] Add transitional mapping strategy where needed:
  - prefer direct FK migration when table shape is already compatible
  - use one-time SQL mapping by slug/name only as a temporary migration aid
- [ ] Add a final cleanup migration only after services no longer use old tables.

Exit criteria:

- No service/controller writes new class, subclass, feature, skill-proficiency, or class reward data to old plural tables.
- No new API contract is built around old plural IDs unless explicitly marked transitional.

## Step 2 - Add JPA Coverage for New Content Tables

Status: JPA/repository foundation complete; service integration pending

Entities/repositories to add or remap:

- [x] `ContentCharacterClass` -> `character_class`
- [x] `ContentSubclass` -> `subclass`
- [x] `ClassFeature` -> `class_feature`
- [x] `ContentSkill` -> `skill`
- [x] `ContentCharacterClass.savingThrows` -> `class_saving_throw`
- [x] `ContentCharacterClass.primaryAbilities` -> `class_primary_ability`
- [x] `ContentCharacterClass.skillOptions` -> `class_skill_option`
- [x] `ClassLevelRewardGroup` -> `class_level_reward_group`
- [x] `ClassLevelRewardOption` -> `class_level_reward_option`
- [x] `ClassLevelRewardGrant` -> `class_level_reward_grant`
- [x] typed grant entities:
  - feature
  - subclass
  - feat
  - spell
  - skill proficiency
  - ability score bonus
  - numeric modifier
  - custom text
- [x] character selection entities:
  - reward option selection
  - ability score selection
  - skill selection
  - spell selection
- [x] repositories for class catalog, features, reward groups/options/grants, typed grants, and character reward selections

Exit criteria:

- Every class/reward-choice table introduced in 057 and 058 that participates in app workflows has JPA/repository coverage.
- Broader 054 tables outside class/reward workflows must be audited as each service migrates.

## Step 3 - Replace Reference APIs

Status: pending

Tasks:

- [ ] Update `/api/reference/classes` and campaign reference variants to read from `character_class`.
- [ ] Return normalized class mechanics:
  - hit die
  - primary abilities
  - saving throws
  - skill choice count
  - skill choice `any`
  - skill options
  - armor/weapon/tool proficiency text
  - spellcasting profile
  - reward groups/options/grants by level
- [ ] Update spells/backgrounds/feats/stat/currency endpoints to consistently expose new singular IDs and naming fields.
- [ ] Keep response shape frontend-friendly; avoid leaking table mechanics unless useful.

Exit criteria:

- Character creation UI can be driven entirely from new content reference endpoints.

## Step 4 - Replace Character Creation and Level-Up

Status: pending

Tasks:

- [ ] Update `CharacterWizardService` to resolve classes, skills, abilities, spells, backgrounds from new content tables.
- [ ] Replace JSON/text class mechanics reads with dependent tables.
- [ ] Replace `ClassLevelRewardRepository` usage with reward group/option/grant services.
- [ ] Persist user choices to `character_reward_selection` and child selection tables.
- [ ] Apply known structured grants automatically:
  - feature
  - subclass
  - feat
  - spell
  - skill proficiency
  - ability score bonus
  - numeric modifier where deterministic
- [ ] Surface custom text grants as manual/user-facing entries.

Exit criteria:

- Creating a character and levelling up no longer requires old class metadata or old class rewards.

## Step 5 - Replace Homebrew Authoring

Status: pending

Tasks:

- [ ] Update class authoring requests to create new `character_class`, mechanics, features, and reward groups.
- [ ] Support homebrew choice groups:
  - choose one of N
  - choose N of M
  - choose any skill/spell/feat matching a rule
  - fixed +1/+N numeric or ability reward
  - custom text/manual grant
- [ ] Update validators to validate new content ownership via `homebrew_id`.
- [ ] Ensure every homebrew-owned child row is reachable through its parent package.

Exit criteria:

- A user can create a class with mechanics, level features, selectable rewards, and custom modifiers without touching old plural tables or JSON.

## Step 6 - Replace Admin and Reward Resolver Layer

Status: pending

Tasks:

- [ ] Replace old `RewardResolver` usage over `class_level_rewards`.
- [ ] Add resolver for `ClassLevelRewardGrant`.
- [ ] Update admin CRUD endpoints for:
  - class features
  - reward groups
  - reward options
  - reward grants
- [ ] Keep old admin endpoints either removed or marked transitional.

Exit criteria:

- Admin tools manage the same data model that the character wizard consumes.

## Step 7 - Remove Old Plural Schema

Status: pending

Prerequisite: Steps 2-6 complete and tests passing.

Tasks:

- [ ] Add final migration to drop or archive old non-bestiary content tables.
- [ ] Remove old entities/repositories/services:
  - old class level reward model
  - old active `skills` reward table if replaced by `class_feature`
  - old proficiency skills table after `skill` fully replaces it
  - old plural spell/background/feat/stat/currency tables if no longer referenced
- [ ] Remove old JSON text mechanics from character/class workflows where replaced by relational state.
- [ ] Keep bestiary tables intact.

Exit criteria:

- No runtime code references old plural content tables.
- Fresh DB and migrated DB both use the same singular content model.

## Verification Strategy

- Unit tests for reward group validation.
- Integration tests for reference endpoints.
- Character creation tests for:
  - fixed class feature grant
  - choose one subclass
  - choose N skills
  - ASI +2 one ability
  - ASI +1/+1 two abilities
  - custom homebrew text grant
- Level-up tests for applying selected grants.
- Regression tests that bestiary services still use existing bestiary tables.
