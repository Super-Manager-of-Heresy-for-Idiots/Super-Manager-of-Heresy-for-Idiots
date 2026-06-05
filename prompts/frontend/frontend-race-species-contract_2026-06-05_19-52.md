# Frontend Contract: Race/Species Backend

Нужно добавить UI для race/species. В коде backend используется термин `Race`, но в интерфейсе можно показывать `Species`, если это подходит под D&D 2024.

## Roles And Scope

- `ADMIN` управляет стандартными системными race/species.
- `GAME_MASTER` управляет кастомными race только внутри своего DRAFT homebrew-пакета.
- `PLAYER` только просматривает race, доступные в выбранной campaign, и выбирает race при создании/редактировании персонажа.

System races доступны всем. Homebrew races доступны только в campaign, где подключен соответствующий homebrew package.

## Admin API

Base: `/api/admin/races`

- `GET /api/admin/races` - список всех races, включая disabled.
- `POST /api/admin/races` - создать SYSTEM race.
- `GET /api/admin/races/{raceId}` - подробности.
- `PUT /api/admin/races/{raceId}` - обновить SYSTEM race.
- `POST /api/admin/races/{raceId}/enable` - включить.
- `POST /api/admin/races/{raceId}/disable` - выключить.

Legacy endpoints `/api/admin/character-races` остаются, но для полноценного редактора использовать `/api/admin/races`.

## GM Homebrew API

Base: `/api/homebrew/my/{packageId}/content/races`

- `POST /api/homebrew/my/{packageId}/content/races` - создать HOMEBREW race.
- `PUT /api/homebrew/my/{packageId}/content/races/{raceId}` - обновить свою HOMEBREW race.
- `POST /api/homebrew/my/{packageId}/content/races/{raceId}/enable` - включить.
- `POST /api/homebrew/my/{packageId}/content/races/{raceId}/disable` - выключить.
- `POST /api/homebrew/my/{packageId}/content/races/{systemRaceId}/duplicate` - скопировать SYSTEM race в homebrew для кастомизации.

Из-за текущего unique constraint на `character_races.name` duplicate создает имя вида `{SystemName} - {PackageTitle}`.

## Player/Campaign API

- `GET /api/campaigns/{campaignId}/races` - список races, доступных для создания персонажа.
- `GET /api/campaigns/{campaignId}/races/{raceId}` - подробности race, если она доступна в campaign.
- `GET /api/campaigns/{campaignId}/available-content` теперь также возвращает только active system races и active homebrew races подключенных пакетов.

## Character API Changes

Create character:

`POST /api/campaigns/{campaignId}/characters`

```json
{
  "name": "Lia",
  "classId": "uuid",
  "raceId": "uuid",
  "selectedLineageId": "uuid-or-null",
  "campaignId": "uuid"
}
```

Update character:

`PUT /api/campaigns/{campaignId}/characters/{characterId}`

```json
{
  "name": "Lia",
  "raceId": "uuid",
  "selectedLineageId": "uuid-or-null"
}
```

If a race has `lineageRequired=true` and non-empty `lineageOptions`, `selectedLineageId` is required. Disabled race cannot be selected. Race from unattached homebrew cannot be selected.

Character response includes:

```json
{
  "race": {
    "id": "uuid",
    "name": "Elf",
    "description": "..."
  },
  "selectedLineageId": "uuid-or-null",
  "raceSnapshot": {
    "raceId": "uuid",
    "raceName": "Elf",
    "lineageId": "uuid",
    "lineageName": "High Elf",
    "size": "MEDIUM",
    "speed": { "walk": 30 },
    "darkvisionRange": 60,
    "traitNames": ["Darkvision", "Fey Ancestry"],
    "allowAbilityScoreBonuses": false
  }
}
```

Snapshot is read-only. It protects existing characters from later homebrew edits.

## Race Request Shape

```json
{
  "name": "Elf",
  "slug": "elf",
  "description": "Short public description.",
  "loreDescription": "Optional lore text.",
  "sourceType": "SYSTEM",
  "sourceName": "Player's Handbook 2024",
  "active": true,
  "creatureType": "HUMANOID",
  "sizeOptions": ["MEDIUM"],
  "defaultSize": "MEDIUM",
  "speed": {
    "walk": 30,
    "fly": null,
    "swim": null,
    "climb": null,
    "burrow": null
  },
  "darkvisionRange": 60,
  "traits": [
    {
      "id": "uuid-optional",
      "name": "Darkvision",
      "description": "Short paraphrase.",
      "levelRequirement": 1,
      "uses": {
        "type": "PASSIVE",
        "recharge": "NONE",
        "amountExpression": null
      },
      "actionType": "PASSIVE",
      "damage": null,
      "savingThrow": null,
      "grantedSpells": null,
      "innateSpells": null,
      "metadata": null
    }
  ],
  "lineageOptions": [
    {
      "id": "uuid-optional",
      "name": "High Elf",
      "description": "Short paraphrase.",
      "traits": [],
      "innateSpells": null,
      "resistances": [],
      "speedModifiers": null,
      "metadata": null
    }
  ],
  "lineageRequired": true,
  "languages": [],
  "languageOptions": null,
  "proficiencies": [],
  "resistances": [],
  "vulnerabilities": [],
  "immunities": [],
  "conditionResistances": [],
  "conditionAdvantages": [],
  "innateSpells": null,
  "allowAbilityScoreBonuses": false,
  "abilityScoreBonuses": [],
  "metadata": null
}
```

For homebrew create/update, frontend can still send `sourceType`, but backend forces `HOMEBREW`. For admin create/update, backend forces `SYSTEM`.

## Enums

`sourceType`: `SYSTEM`, `HOMEBREW`

`sizeOptions/defaultSize`: `TINY`, `SMALL`, `MEDIUM`, `LARGE`, `HUGE`, `GARGANTUAN`

`ability`: `STRENGTH`, `DEXTERITY`, `CONSTITUTION`, `INTELLIGENCE`, `WISDOM`, `CHARISMA`

`abilityScoreBonuses.mode`: `FIXED`, `CHOICE`

Trait `uses.type`: `PASSIVE`, `LIMITED`, `ACTION`, `BONUS_ACTION`, `REACTION`

Trait `uses.recharge`: `NONE`, `SHORT_REST`, `LONG_REST`, `PROFICIENCY_BONUS_PER_LONG_REST`, `CUSTOM`

Trait `actionType`: `PASSIVE`, `ACTION`, `BONUS_ACTION`, `REACTION`, `PART_OF_ATTACK_ACTION`

`damage.damageType`: existing backend damage enum, e.g. `FIRE`, `COLD`, `LIGHTNING`, `POISON`, `NECROTIC`, `RADIANT`, `PSYCHIC`, `FORCE`, `THUNDER`, `ACID`, etc.

## Validation UI

- `name` required, max 50.
- `creatureType` required.
- At least one `sizeOptions` value required.
- `defaultSize` must be one of `sizeOptions`.
- `speed.walk` required and `>= 0`.
- `darkvisionRange` nullable, but if set must be `>= 0`.
- `SYSTEM` PHB 2024 races cannot have ability score bonuses.
- `abilityScoreBonuses` are allowed only when `allowAbilityScoreBonuses=true`.
- If `lineageRequired=true`, require one selected lineage during character creation.
- Do not show disabled races in player character creation.
- Do not show homebrew races from packages not attached to the current campaign.

## UX Notes

Use one rich editor for admin/system and GM/homebrew. Show a scope badge: `System` or `Homebrew: {packageTitle}`.

For D&D 2024, do not present ability score bonuses as a default race field. Put them under a collapsed `Legacy/Homebrew mechanics` section, with a clear toggle `allowAbilityScoreBonuses`.
