# Homebrew content — class / race dependency binding

**Status:** PROPOSED (frontend implemented, awaiting backend)
**Date:** 2026-06-13
**Scope:** Homebrew package SKILL / FEAT / BUFF_DEBUFF creation endpoints

## Motivation

When a homebrew author creates a class-dependent or race-dependent piece of
content, they must be able to declare *which* classes or races it attaches to.
This is the first concrete case of a general rule: any content that depends on
another entity needs an explicit binding picker. The frontend now renders a
"Class & race binding" window in the create-content form for SKILL, FEAT and
BUFF_DEBUFF, sourcing the candidate classes/races from the package's own
`contentByType.CHARACTER_CLASS` and `contentByType.RACE`.

## Request shape changes

Two optional fields are added to each create request. Both are arrays of
content IDs that already belong to the same package (the IDs returned in
`ContentSummaryDto.id` for `CHARACTER_CLASS` / `RACE`). Omitted or empty means
"shared / not bound to anything".

### `POST /api/homebrew/packages/{id}/skills` — `CreateSkillRequest`

```jsonc
{
  "name": "string",
  "description": "string?",
  "skillType": "string?",
  "damageDice": "string?",
  "damageBonus": "number?",
  "damageType": "string?",
  "classIds": ["uuid", ...],   // NEW — optional, package class content IDs
  "raceIds":  ["uuid", ...]    // NEW — optional, package race content IDs
}
```

### `POST /api/homebrew/packages/{id}/feats` — `CreateFeatRequest`

```jsonc
{
  "name": "string",
  "description": "string?",
  "prerequisites": "string?",
  "classIds": ["uuid", ...],   // NEW — optional
  "raceIds":  ["uuid", ...]    // NEW — optional
}
```

### `POST /api/homebrew/packages/{id}/buffs-debuffs` — `CreateBuffDebuffRequest`

```jsonc
{
  "name": "string",
  "description": "string?",
  "effectType": "string",
  "targetStatId": "uuid?",
  "modifierValue": "number?",
  "durationRounds": "number?",
  "isBuff": "boolean",
  "classIds": ["uuid", ...],   // NEW — optional
  "raceIds":  ["uuid", ...]    // NEW — optional
}
```

## Validation

- Each ID in `classIds` MUST reference a `CHARACTER_CLASS` content item in the
  **same** package; each ID in `raceIds` MUST reference a `RACE` content item
  in the same package. Reject with `400` otherwise.
- Both arrays are optional and independent; either, both, or neither may be
  supplied.
- Duplicate IDs within an array should be de-duplicated server-side.

## Response

The existing endpoints return `HomebrewDetailResponse`. Bindings should be
persisted and surfaced back so the editor can show existing attachments on
reload. Recommended: extend `ContentSummaryDto` with the resolved bindings:

```jsonc
{
  "id": "uuid",
  "name": "string",
  // ...existing fields...
  "classIds": ["uuid", ...],   // NEW — echo of bound class content IDs
  "raceIds":  ["uuid", ...]    // NEW — echo of bound race content IDs
}
```

(If `ContentSummaryDto` is extended, update the frontend type in
`src/types/index.ts` and prefill the picker when editing.)

## Frontend touch points (already implemented)

- `src/types/index.ts` — `classIds?`/`raceIds?` added to `CreateSkillRequest`,
  `CreateFeatRequest`, `CreateBuffDebuffRequest`.
- `src/pages/gm/homebrew/EditDoctrinePage.tsx` — binding window + `newDepClassIds`
  / `newDepRaceIds` state, sent in `handleCreateAndAddContent`.
- i18n keys `hb.edit.dep*` in `src/i18n/dict/homebrew.ts`.
