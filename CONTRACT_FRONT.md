# CONTRACT_FRONT.md — Frontend API Contract & Development Guide

## Overview

This document describes the complete backend API for the DnD Campaign Management application.
Use it as the single source of truth when building the React/TypeScript/Tailwind frontend.

**Base URL:** `/api`
**Auth:** JWT Bearer token in `Authorization` header for all endpoints except register and login.
**Response envelope:** Every response uses `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { ... },
  "message": "optional message",
  "error": null,
  "fields": null
}
```

On error:

```json
{
  "success": false,
  "data": null,
  "error": "ERROR_CODE",
  "message": "Human-readable description",
  "fields": { "fieldName": "error message" }
}
```

`fields` is populated only for validation errors (HTTP 400). `data`, `message`, `error`, `fields` are omitted from JSON when null (uses `@JsonInclude(NON_NULL)`).

**Pagination:** Spring `Pageable` — use query params `?page=0&size=20&sort=createdAt,desc`. Paginated responses wrap `Page<T>` with: `content`, `totalElements`, `totalPages`, `number`, `size`.

---

## Roles

| Role | Code |
|------|------|
| Admin | `ADMIN` |
| Game Master | `GAME_MASTER` |
| Player | `PLAYER` |

Registration only allows `PLAYER` or `GAME_MASTER`. Admin is pre-seeded.

---

## Enums

Use these string values in requests and expect them in responses.

| Enum | Values |
|------|--------|
| EquipmentSlot | `HEAD`, `CHEST`, `LEGS`, `FEET`, `MAIN_HAND`, `OFF_HAND`, `RING_LEFT`, `RING_RIGHT`, `NECK`, `CLOAK` |
| Rarity | `COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE`, `LEGENDARY` |
| CampaignStatus | `ACTIVE`, `PAUSED`, `COMPLETED` |
| CampaignRole | `GM`, `PLAYER` |
| CharacterStatus | `ACTIVE`, `DEAD`, `RESERVE` |
| QuestStatus | `ACTIVE`, `COMPLETED`, `FAILED`, `HIDDEN`, `ARCHIVED` |
| HomebrewStatus | `DRAFT`, `PUBLISHED`, `UNPUBLISHED`, `ARCHIVED` |
| RewardType | `SKILL`, `SUBCLASS`, `FEAT` |
| DamageType | `SLASHING`, `PIERCING`, `BLUDGEONING`, `FIRE`, `COLD`, `LIGHTNING`, `POISON`, `NECROTIC`, `RADIANT`, `PSYCHIC`, `FORCE`, `THUNDER`, `ACID` |
| SkillActivation | `PASSIVE`, `ACTIVE` |
| ContentType | `ITEM_TYPE`, `CHARACTER_CLASS`, `SKILL`, `FEAT`, `SUBCLASS`, `RACE`, `STAT_TYPE`, `BUFF_DEBUFF`, `ENCHANTMENT_TYPE`, `CURRENCY`, `CUSTOM_RESOURCE`, `ITEM_TEMPLATE` |
| WebSocketEventType | `ITEM_GRANTED`, `ITEM_REMOVED`, `BUFF_APPLIED`, `BUFF_REMOVED`, `XP_GRANTED`, `HP_CHANGED`, `CHARACTER_UPDATED`, `NPC_REVEALED`, `NPC_HIDDEN`, `QUEST_UPDATED`, `CAMPAIGN_STATUS_CHANGED`, `MEMBER_KICKED` |

---

## 1. Authentication — `/api/auth`

### POST `/api/auth/register` — Register (no auth required)

Request:
```json
{
  "username": "string (3-30 chars, ^[a-zA-Z0-9_]+$)",
  "email": "string (valid email)",
  "password": "string (min 8 chars)",
  "role": "PLAYER | GAME_MASTER"
}
```

Response: `AuthResponse`
```json
{
  "token": "jwt-string",
  "expiresIn": 3600000,
  "user": {
    "id": "uuid",
    "username": "string",
    "email": "string",
    "role": "PLAYER | GAME_MASTER",
    "createdAt": "ISO-8601"
  }
}
```

### POST `/api/auth/login` — Login (no auth required)

Request:
```json
{
  "username": "string",
  "password": "string"
}
```

Response: same `AuthResponse` as register.

---

## 2. Campaigns — `/api/campaigns`

### POST `/api/campaigns` — Create campaign

Request:
```json
{
  "name": "string (max 120)",
  "description": "string (optional)"
}
```

Response `201`: `CampaignResponse`

### GET `/api/campaigns` — List my campaigns (paginated)

Query: `?page=0&size=20&sort=...`

Response: `Page<CampaignResponse>`

### GET `/api/campaigns/{id}` — Get campaign details

Response: `CampaignDetailResponse`
```json
{
  "id": "uuid",
  "name": "string",
  "description": "string",
  "status": "ACTIVE | PAUSED | COMPLETED",
  "inviteCode": "string (visible to GMs; to players only if no GMs in campaign)",
  "memberCount": 5,
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601",
  "members": [
    {
      "userId": "uuid",
      "username": "string",
      "roleInCampaign": "GM | PLAYER",
      "isCreator": true,
      "joinedAt": "ISO-8601",
      "kicked": false
    }
  ]
}
```

### PUT `/api/campaigns/{id}` — Update campaign

Request:
```json
{
  "name": "string (max 120)",
  "description": "string (optional)"
}
```

Response: `CampaignResponse`

### DELETE `/api/campaigns/{id}` — Delete campaign (creator only)

Response: `Void` with message "Campaign deleted"

### POST `/api/campaigns/join` — Join campaign by invite code

Request:
```json
{
  "inviteCode": "string"
}
```

Response: `CampaignResponse`

### POST `/api/campaigns/{id}/leave` — Leave campaign

Response: `Void` with message "Left campaign". When a player leaves, all their characters become RESERVE.

### POST `/api/campaigns/{id}/kick` — Kick member (creator only)

Request:
```json
{
  "userId": "uuid"
}
```

Response: `Void`. Regenerates invite code on kick.

### PUT `/api/campaigns/{id}/status` — Change campaign status (creator only)

Request:
```json
{
  "status": "ACTIVE | PAUSED | COMPLETED"
}
```

Response: `CampaignResponse`. When PAUSED/COMPLETED, everything becomes read-only for players.

### GET `/api/campaigns/{id}/invite-code` — Get invite code

Response: `InviteCodeResponse` — `{ "inviteCode": "string" }`

### POST `/api/campaigns/{id}/invite-code/regenerate` — Regenerate invite code (creator only)

Response: `InviteCodeResponse`

### POST `/api/campaigns/{id}/characters/{characterId}/reassign` — Reassign RESERVE character (GM only)

Creates a deep copy of the character for a new owner (copies stats, class levels, items, wallet, resources).

Request:
```json
{
  "newOwnerUserId": "uuid"
}
```

Response `201`: `CharacterResponse`

---

## 3. Campaign Homebrew — `/api/campaigns/{id}/homebrew`

### POST `/api/campaigns/{id}/homebrew` — Attach homebrew package (GM only)

Request:
```json
{
  "homebrewPackageId": "uuid"
}
```

Response `201`: `CampaignHomebrewResponse`
```json
{
  "packageId": "uuid",
  "title": "string",
  "pinnedVersion": null,
  "contentSummary": { "ITEM_TYPE": 3, "SKILL": 5, ... }
}
```

### DELETE `/api/campaigns/{id}/homebrew/{packageId}` — Detach homebrew (GM only)

Response: `Void`

### PUT `/api/campaigns/{id}/homebrew/{packageId}/version` — Update pinned version (GM only)

Request:
```json
{
  "pinnedVersion": 3
}
```

Set `pinnedVersion` to `null` to always use latest.

Response: `CampaignHomebrewResponse`

### GET `/api/campaigns/{id}/homebrew` — List active homebrew in campaign

Response: `List<CampaignHomebrewResponse>`

### GET `/api/campaigns/{id}/available-content` — Get available content (global + homebrew)

Response: `TeamAvailableContentResponse`
```json
{
  "classes": [{ "id": "uuid", "name": "string", "source": "vanilla | homebrew-title", "homebrewTitle": "string | null" }],
  "races": [...],
  "itemTypes": [...],
  "skills": [...],
  "feats": [...]
}
```

---

## 4. Characters — `/api/campaigns/{cid}/characters`

### POST `/api/campaigns/{cid}/characters` — Create character

Request:
```json
{
  "name": "string (max 100)",
  "classId": "uuid",
  "raceId": "uuid",
  "campaignId": "uuid"
}
```

Response `201`: `CharacterResponse`

### GET `/api/campaigns/{cid}/characters` — List campaign characters

All campaign members can see all characters in the campaign. Response: `List<CharacterResponse>`

### GET `/api/campaigns/{cid}/characters/{charId}` — Get character details

Response: `CharacterResponse`
```json
{
  "id": "uuid",
  "name": "string",
  "totalLevel": 5,
  "experience": 6500,
  "classLevels": [
    { "classId": "uuid", "className": "Fighter", "classLevel": 3 },
    { "classId": "uuid", "className": "Wizard", "classLevel": 2 }
  ],
  "race": { "id": "uuid", "name": "Elf", "description": "..." },
  "teamId": "uuid",
  "teamName": "string",
  "ownerId": "uuid",
  "ownerUsername": "string",
  "stats": [
    {
      "id": "uuid",
      "statTypeId": "uuid",
      "statTypeName": "STR",
      "value": 16,
      "effectiveValue": 18,
      "activeModifiers": [
        { "source": "Blessed", "modifierValue": 2 }
      ]
    }
  ],
  "inventorySlots": [...],
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

### PUT `/api/campaigns/{cid}/characters/{charId}` — Update character

Request:
```json
{
  "name": "string (max 100, optional)",
  "raceId": "uuid (optional)"
}
```

Response: `CharacterResponse`

### DELETE `/api/campaigns/{cid}/characters/{charId}` — Delete character

Response: `Void`

---

## 5. Character Stats — `/api/campaigns/{cid}/characters/{charId}/stats`

### GET `.../stats` — Get character stats

Response: `List<CharacterStatResponse>`

### PUT `.../stats/{statId}` — Update stat value

Request:
```json
{
  "value": 16
}
```

Value range: 1-30. Response: `CharacterStatResponse`

### GET `.../ability-check/{statId}` — Calculate ability check modifier

Response: `AbilityCheckResponse`
```json
{
  "statName": "STR",
  "baseValue": 16,
  "modifier": 3,
  "buffBonus": 2,
  "equipmentBonus": 1,
  "totalModifier": 6
}
```

Calculation: `modifier = floor((baseValue - 10) / 2)`, then add buff/equipment bonuses.

---

## 6. Character Inventory — `/api/campaigns/{cid}/characters/{charId}/inventory`

### GET `.../inventory` — Get all items (equipped + backpack)

Response: `List<ItemInstanceResponse>`
```json
{
  "id": "uuid",
  "templateId": "uuid",
  "templateName": "Longsword",
  "displayName": "Excalibur",
  "customName": "Excalibur",
  "quantity": 1,
  "isUnique": true,
  "slot": "MAIN_HAND",
  "notes": "string",
  "rarity": "RARE",
  "enchantments": [
    {
      "id": "uuid",
      "enchantmentType": { "id": "uuid", "name": "Fire", ... },
      "appliedAt": "ISO-8601",
      "notes": "string"
    }
  ]
}
```

`slot` = null means item is in the backpack. `displayName` = `customName` if set, otherwise `templateName`.

### GET `.../inventory/equipped` — Get equipped items only

Response: `List<ItemInstanceResponse>` (only items with `slot != null`)

### GET `.../inventory/backpack` — Get backpack items only

Response: `List<ItemInstanceResponse>` (only items with `slot == null`)

### POST `.../inventory/grant` — Grant item (GM/Admin only)

Request:
```json
{
  "templateId": "uuid",
  "quantity": 1,
  "customName": "string (optional)",
  "isUnique": false
}
```

For stackable templates: if the character already has an unequipped, non-unique instance of this template, quantity is incremented. Otherwise a new instance is created.

Response: `ItemInstanceResponse`

### PUT `.../inventory/{instanceId}/equip` — Equip item

Request:
```json
{
  "slot": "MAIN_HAND"
}
```

Automatically applies buff/debuff effects from `item_template_buffs`. Response: `ItemInstanceResponse`

### PUT `.../inventory/{instanceId}/unequip` — Unequip item

No body required. Automatically removes buff/debuff effects. Response: `ItemInstanceResponse`

### DELETE `.../inventory/{instanceId}` — Remove item (GM/Admin only)

Response: `Void`

### POST `.../inventory/{instanceId}/transfer` — Transfer item

Request:
```json
{
  "toCharacterId": "uuid"
}
```

Validation: both characters must be in the same campaign. Item must be unequipped. The caller must be the item owner, a campaign GM, or an admin.

Response: `ItemInstanceResponse`

### PUT `.../inventory/{instanceId}/rename` — Rename item

Request:
```json
{
  "customName": "Excalibur",
  "renameEntireStack": true
}
```

If `renameEntireStack = true` and quantity > 1: the entire stack is renamed, becomes unique.
If `renameEntireStack = false` and quantity > 1: the stack is split — original loses 1 quantity, a new unique instance is created with quantity=1 and the custom name.
If quantity = 1: just sets the name, becomes unique.

Response: `ItemInstanceResponse` (the renamed item)

---

## 7. Enchantments on Items — `/api/campaigns/{cid}/characters/{charId}/inventory/{instanceId}/enchantments`

### GET `.../enchantments` — List enchantments on item

Response: `List<EnchantmentResponse>`
```json
{
  "id": "uuid",
  "enchantmentType": {
    "id": "uuid",
    "name": "Fire Enchantment",
    "description": "string",
    "damageDice": "1d6",
    "damageBonus": 2,
    "damageType": "FIRE",
    "buffDebuff": { ... }
  },
  "appliedAt": "ISO-8601",
  "notes": "string"
}
```

### POST `.../enchantments` — Add enchantment

Request:
```json
{
  "enchantmentTypeId": "uuid",
  "notes": "string (max 255, optional)"
}
```

Response `201`: `EnchantmentResponse`

### DELETE `.../enchantments/{enchantmentId}` — Remove enchantment

Response: `Void`

---

## 8. Character Active Effects — `/api/campaigns/{cid}/characters/{charId}/effects`

### GET `.../effects` — List active effects

Response: `List<CharacterActiveEffectResponse>`
```json
{
  "id": "uuid",
  "buffDebuffId": "uuid",
  "buffDebuffName": "Blessed",
  "isBuff": true,
  "effectType": "STAT_MODIFIER",
  "modifierValue": 2,
  "targetStatName": "STR",
  "remainingRounds": 5,
  "appliedAt": "ISO-8601",
  "appliedByUsername": "gm_user"
}
```

`remainingRounds` = null means permanent effect.

### POST `.../effects` — Apply effect (GM/Admin only)

Request:
```json
{
  "buffDebuffId": "uuid",
  "remainingRounds": 5
}
```

`remainingRounds` is optional (null = permanent).

Response `201`: `CharacterActiveEffectResponse`

### DELETE `.../effects/{effectId}` — Remove effect (GM/Admin only)

Response: `Void`

---

## 9. Character Wallet — `/api/campaigns/{cid}/characters/{charId}/wallet`

### GET `.../wallet` — Get wallet

Response: `List<WalletEntryResponse>`
```json
{
  "currencyTypeId": "uuid",
  "currencyName": "Gold",
  "amount": 150.00,
  "goldEquivalent": 150.00
}
```

### POST `.../wallet` — Modify currency

Request:
```json
{
  "currencyTypeId": "uuid",
  "amount": 50.00
}
```

Positive amount adds, negative subtracts. Response: `WalletEntryResponse`

---

## 10. Character Resources — `/api/campaigns/{cid}/characters/{charId}/resources`

### GET `.../resources` — Get character resources

Response: `List<ResourceResponse>`
```json
{
  "resourceTypeId": "uuid",
  "resourceName": "Blood Points",
  "currentValue": 3,
  "maxValue": 10
}
```

### POST `.../resources` — Modify resource

Request:
```json
{
  "resourceTypeId": "uuid",
  "currentValue": 5
}
```

Response: `ResourceResponse`

---

## 11. Character HP — `/api/campaigns/{cid}/characters/{charId}/hp`

### POST `.../hp` — Modify HP

Request:
```json
{
  "amount": -5
}
```

Positive = heal, negative = damage. Response: `CharacterResponse`

---

## 12. Quests — `/api/campaigns/{cid}/quests`

### POST `.../quests` — Create quest (GM only)

Request:
```json
{
  "title": "string (max 200)",
  "description": "string (optional)",
  "status": "ACTIVE (default)",
  "isVisibleToPlayers": false
}
```

Response `201`: `QuestResponse`

### GET `.../quests` — List quests

GM sees all quests; players see only visible quests.

Response: `List<QuestResponse>`
```json
{
  "id": "uuid",
  "title": "string",
  "description": "string",
  "status": "ACTIVE | COMPLETED | FAILED | HIDDEN | ARCHIVED",
  "isVisibleToPlayers": true,
  "notes": [...],
  "rewards": [...],
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

### GET `.../quests/{questId}` — Get quest details

Response: `QuestResponse`

### PUT `.../quests/{questId}` — Update quest (GM only)

Request:
```json
{
  "title": "string (max 200, optional)",
  "description": "string (optional)",
  "status": "string (optional)",
  "isVisibleToPlayers": true
}
```

Response: `QuestResponse`

### DELETE `.../quests/{questId}` — Delete quest (GM only)

Response: `Void`

### POST `.../quests/{questId}/notes` — Add note to quest

Any campaign member can add notes to visible quests.

Request:
```json
{
  "content": "string"
}
```

Response `201`: `NoteResponse`
```json
{
  "id": "uuid",
  "authorId": "uuid",
  "authorUsername": "string",
  "content": "string",
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

### PUT `.../quests/{questId}/notes/{noteId}` — Update note

Request: `{ "content": "string" }`. Response: `NoteResponse`

### DELETE `.../quests/{questId}/notes/{noteId}` — Delete note

Response: `Void`

### POST `.../quests/{questId}/npcs/{npcId}` — Link NPC to quest (GM only)

No body. Response: `Void`. NPC must be in the same campaign.

### DELETE `.../quests/{questId}/npcs/{npcId}` — Unlink NPC (GM only)

Response: `Void`

### POST `.../quests/{questId}/locations/{locationId}` — Link location (GM only)

No body. Response: `Void`. Location must be in the same campaign.

### DELETE `.../quests/{questId}/locations/{locationId}` — Unlink location (GM only)

Response: `Void`

### GET `.../quests/{questId}/rewards` — List quest rewards

Response: `List<QuestRewardResponse>`
```json
{
  "id": "uuid",
  "itemTemplateId": "uuid",
  "itemTemplateName": "Longsword",
  "quantity": 2,
  "currencyTypeId": "uuid",
  "currencyTypeName": "Gold",
  "currencyAmount": 100.00
}
```

### POST `.../quests/{questId}/rewards` — Add quest reward (GM only)

Request:
```json
{
  "itemTemplateId": "uuid (optional)",
  "quantity": 1,
  "currencyTypeId": "uuid (optional)",
  "currencyAmount": 100.00
}
```

Response `201`: `QuestRewardResponse`

### DELETE `.../quests/{questId}/rewards/{rewardId}` — Delete quest reward (GM only)

Response: `Void`

---

## 13. NPCs — `/api/campaigns/{cid}/npcs`

### POST `.../npcs` — Create NPC (GM only)

Request:
```json
{
  "name": "string (max 100)",
  "publicDescription": "string (optional)",
  "privateDescription": "string (optional, GM-only)",
  "isVisibleToPlayers": false
}
```

Response `201`: `NpcResponse`
```json
{
  "id": "uuid",
  "name": "string",
  "publicDescription": "string",
  "privateDescription": "string (null for players)",
  "isVisibleToPlayers": false,
  "notes": [...],
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

**Important:** `privateDescription` is NEVER visible to players. The backend strips it for non-GM responses.

### GET `.../npcs` — List NPCs

GM sees all; players see only visible NPCs (without `privateDescription`).

Response: `List<NpcResponse>`

### GET `.../npcs/{npcId}` — Get NPC details

Response: `NpcResponse`

### PUT `.../npcs/{npcId}` — Update NPC (GM only)

Request:
```json
{
  "name": "string (max 100, optional)",
  "publicDescription": "string (optional)",
  "privateDescription": "string (optional)",
  "isVisibleToPlayers": true
}
```

Response: `NpcResponse`

### DELETE `.../npcs/{npcId}` — Delete NPC (GM only)

Cascades to all NPC notes. Response: `Void`

### PUT `.../npcs/{npcId}/visibility` — Toggle NPC visibility (GM only)

No body. Toggles `isVisibleToPlayers`. Response: `NpcResponse`

### POST `.../npcs/{npcId}/notes` — Add note to NPC

Request: `{ "content": "string" }`. Response `201`: `NoteResponse`

### PUT `.../npcs/{npcId}/notes/{noteId}` — Update note

Request: `{ "content": "string" }`. Response: `NoteResponse`

### DELETE `.../npcs/{npcId}/notes/{noteId}` — Delete note

Response: `Void`

---

## 14. Locations — `/api/campaigns/{cid}/locations`

### POST `.../locations` — Create location (GM only)

Request:
```json
{
  "name": "string (max 100)",
  "description": "string (optional)",
  "isVisibleToPlayers": false
}
```

Response `201`: `LocationResponse`
```json
{
  "id": "uuid",
  "name": "string",
  "description": "string",
  "isVisibleToPlayers": false,
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

### GET `.../locations` — List locations

GM sees all; players see only visible locations.

Response: `List<LocationResponse>`

### GET `.../locations/{locId}` — Get location

Response: `LocationResponse`

### PUT `.../locations/{locId}` — Update location (GM only)

Request:
```json
{
  "name": "string (max 100, optional)",
  "description": "string (optional)",
  "isVisibleToPlayers": true
}
```

Response: `LocationResponse`

### DELETE `.../locations/{locId}` — Delete location (GM only)

Response: `Void`

### PUT `.../locations/{locId}/visibility` — Toggle visibility (GM only)

No body. Toggles `isVisibleToPlayers`. Response: `LocationResponse`

---

## 15. Shared Storage — `/api/campaigns/{cid}/shared-storage`

### POST `.../shared-storage` — Create storage (GM only)

Request:
```json
{
  "name": "string (max 100)"
}
```

Response `201`: `SharedStorageResponse`
```json
{
  "id": "uuid",
  "name": "Forest Cache",
  "campaignId": "uuid",
  "items": [],
  "createdAt": "ISO-8601"
}
```

### GET `.../shared-storage` — List all storages

Response: `List<SharedStorageResponse>`

### GET `.../shared-storage/{storageId}` — Get storage with items

Response: `SharedStorageResponse`

### DELETE `.../shared-storage/{storageId}` — Delete storage (GM only)

Response: `Void`

### POST `.../shared-storage/{storageId}/deposit/{instanceId}` — Deposit item into storage

Moves an item from a character's inventory to shared storage. The item's owner character must be in the same campaign as the storage.

Response: `ItemInstanceResponse`

### POST `.../shared-storage/{storageId}/take/{instanceId}?characterId={charId}` — Take item from storage

Moves an item from shared storage to a character. Query param `characterId` specifies the target. Target character must be in the same campaign.

Response: `ItemInstanceResponse`

---

## 16. XP Distribution — `/api/campaigns/{cid}/xp`

### POST `.../xp` — Distribute XP (GM only)

Request:
```json
{
  "amount": 500,
  "target": "ALL | SELECTED | SINGLE",
  "characterIds": ["uuid", "uuid"]
}
```

- `ALL`: XP to all characters in campaign (ignore `characterIds`)
- `SELECTED`: XP to specified characters
- `SINGLE`: XP to one character (`characterIds` has one entry)

Response: `Void` with message. Triggers `XP_GRANTED` WebSocket event. When a character crosses a level threshold, the frontend should show the level-up modal.

**D&D 5e XP thresholds:**
Lv2: 300, Lv3: 900, Lv4: 2,700, Lv5: 6,500, Lv6: 14,000, Lv7: 23,000, Lv8: 34,000, Lv9: 48,000, Lv10: 64,000, Lv11: 85,000, Lv12: 100,000, Lv13: 120,000, Lv14: 140,000, Lv15: 165,000, Lv16: 240,000, Lv17: 265,000, Lv18: 355,000, Lv19: 405,000, Lv20: 475,000.

---

## 17. Level Up — `/api/characters`

### GET `/api/characters/{charId}/level-up-options` — Get level-up options

Response: `LevelUpOptionsResponse`
```json
{
  "currentTotalLevel": 4,
  "xpToNextLevel": 6500,
  "availableClasses": [
    {
      "classId": "uuid",
      "className": "Fighter",
      "currentLevelInClass": 3,
      "newLevelInClass": 4,
      "rewardGroups": [
        {
          "rewardType": "FEAT",
          "isChoice": true,
          "rewards": [
            {
              "rewardEntryId": "uuid",
              "rewardId": "uuid",
              "name": "Great Weapon Master",
              "description": "...",
              "alreadyAcquired": false
            }
          ]
        }
      ]
    }
  ]
}
```

### POST `/api/characters/{charId}/level-up` — Commit level-up

Request:
```json
{
  "classId": "uuid",
  "selections": [
    { "rewardType": "FEAT", "rewardEntryId": "uuid" }
  ]
}
```

Response: `LevelUpResultResponse`
```json
{
  "newTotalLevel": 5,
  "classLeveled": "Fighter",
  "newClassLevel": 4,
  "rewardsAcquired": [
    { "rewardType": "FEAT", "name": "Great Weapon Master" }
  ]
}
```

### GET `/api/characters/{charId}/rewards` — Get all acquired rewards

Response: `CharacterRewardsResponse`
```json
{
  "characterId": "uuid",
  "totalLevel": 5,
  "classBreakdown": [
    {
      "classId": "uuid",
      "className": "Fighter",
      "classLevel": 4,
      "subclass": { "name": "Champion", "description": "..." },
      "rewardsByType": {
        "SKILL": [{ "name": "Second Wind", "acquiredAt": "ISO-8601" }],
        "FEAT": [{ "name": "Great Weapon Master", "acquiredAt": "ISO-8601" }]
      }
    }
  ]
}
```

---

## 18. GM Session Notes — `/api/campaigns/{cid}/gm-notes`

**NEVER visible to players.** Only GMs in the campaign can access.

### POST `.../gm-notes` — Create note

Request:
```json
{
  "title": "string (max 200)",
  "content": "string"
}
```

Response `201`: `GmSessionNoteResponse`
```json
{
  "id": "uuid",
  "campaignId": "uuid",
  "authorUsername": "string",
  "title": "Session 30.05.2026 Plan",
  "content": "Introduce the dragon...",
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

### GET `.../gm-notes` — List all GM notes in campaign

Response: `List<GmSessionNoteResponse>`

### GET `.../gm-notes/{noteId}` — Get note

Response: `GmSessionNoteResponse`

### PUT `.../gm-notes/{noteId}` — Update note

Request:
```json
{
  "title": "string (max 200, optional)",
  "content": "string (optional)"
}
```

Response: `GmSessionNoteResponse`

### DELETE `.../gm-notes/{noteId}` — Delete note

Response: `Void`

---

## 19. Homebrew Authoring — `/api/homebrew`

### POST `/api/homebrew` — Create homebrew package (GM only)

Request:
```json
{
  "title": "string (max 120)",
  "description": "string (max 2000, optional)",
  "tagNames": ["combat", "magic"]
}
```

Response `201`: `HomebrewPackageResponse`

### GET `/api/homebrew/mine` — List my homebrew packages

Response: `List<HomebrewPackageResponse>`
```json
{
  "id": "uuid",
  "title": "string",
  "description": "string",
  "status": "DRAFT | PUBLISHED | UNPUBLISHED | ARCHIVED",
  "version": 3,
  "downloadCount": 42,
  "authorUsername": "string",
  "tags": ["combat"],
  "contentSummary": { "itemTypeCount": 5, "classCount": 2, "skillCount": 3, "featCount": 1 },
  "publishedAt": "ISO-8601",
  "createdAt": "ISO-8601",
  "isDeleted": false
}
```

### GET `/api/homebrew/{id}` — Get homebrew detail

Response: `HomebrewDetailResponse` (includes `contentByType` map with full content listings)

### PUT `/api/homebrew/{id}` — Update homebrew metadata

Request:
```json
{
  "title": "string (max 120, optional)",
  "description": "string (max 2000, optional)",
  "tagNames": ["combat", "magic"]
}
```

Response: `HomebrewPackageResponse`

### POST `/api/homebrew/{id}/content` — Add content to homebrew

Request:
```json
{
  "contentType": "ITEM_TYPE | CHARACTER_CLASS | SKILL | FEAT | SUBCLASS | RACE | ...",
  "contentId": "uuid"
}
```

Response: `HomebrewDetailResponse`

### POST `/api/homebrew/{id}/publish` — Publish homebrew

Increments version on each publish. Validates that no published content was deleted.

Response: `HomebrewPackageResponse`

### POST `/api/homebrew/{id}/unpublish` — Unpublish homebrew

Response: `HomebrewPackageResponse`

### DELETE `/api/homebrew/{id}` — Soft-delete homebrew (author only)

Sets `deleted_at`, status becomes ARCHIVED. Existing campaign attachments unaffected.

Response: `Void`

---

## 20. Homebrew Marketplace — `/api/homebrew/marketplace`

### GET `/api/homebrew/marketplace` — Browse marketplace

Query params: `search`, `tags`, `sort` (downloads|newest|rating), `page`, `size`

Only shows PUBLISHED packages where `deleted_at IS NULL`.

Response: `Page<HomebrewPackageResponse>`

### GET `/api/homebrew/marketplace/{id}` — Get marketplace package detail

Response: `HomebrewDetailResponse`

### POST `/api/homebrew/marketplace/{id}/install` — Install package to library

Response: `InstalledHomebrewResponse`
```json
{
  "installationId": "uuid",
  "packageId": "uuid",
  "title": "string",
  "authorUsername": "string",
  "isDeleted": false,
  "installedAt": "ISO-8601",
  "sourceVersion": 3,
  "contentSummary": { "itemTypeCount": 5, ... }
}
```

---

## 21. Homebrew Library (My Installed) — `/api/homebrew`

### GET `/api/homebrew/installed` — List installed homebrew

Response: `List<InstalledHomebrewResponse>`

### DELETE `/api/homebrew/installed/{installId}` — Uninstall homebrew

Response: `Void`

### GET `/api/homebrew/ratings/{packageId}` — Get ratings for package

Response: `HomebrewRatingResponse`
```json
{
  "likes": 42,
  "dislikes": 3,
  "netRating": 39,
  "userRating": 1
}
```

`userRating`: 1 = liked, -1 = disliked, null = not rated.

### POST `/api/homebrew/ratings/{packageId}` — Rate package

Request:
```json
{
  "rating": 1
}
```

Values: 1 (like), -1 (dislike). Response: `HomebrewRatingResponse`

### GET `/api/homebrew/library` — GM's homebrew library

Response: `List<HomebrewPackageResponse>`

### POST `/api/homebrew/library/{packageId}` — Add to library

Response: `Void`

### DELETE `/api/homebrew/library/{packageId}` — Remove from library

Response: `Void`

---

## 22. Item Templates — `/api/item-templates`

### POST `/api/item-templates` — Create template (Admin/GM)

Request:
```json
{
  "name": "string (max 100)",
  "description": "string (optional)",
  "itemTypeId": "uuid (optional)",
  "rarity": "COMMON | UNCOMMON | RARE | VERY_RARE | LEGENDARY",
  "damageDice": "2d6 (optional, pattern: ^(\\d+)?d(\\d+)$)",
  "damageBonus": 0,
  "damageType": "SLASHING | ... (optional)",
  "isStackable": false,
  "skillId": "uuid (optional)",
  "skillActivation": "PASSIVE | ACTIVE (optional)"
}
```

Response `201`: `ItemTemplateResponse`
```json
{
  "id": "uuid",
  "name": "Longsword +5",
  "description": "string",
  "itemTypeName": "Weapon",
  "rarity": "RARE",
  "damageDice": "2d6",
  "damageBonus": 5,
  "damageType": "SLASHING",
  "isStackable": false,
  "skillName": "Cleave",
  "skillActivation": "ACTIVE",
  "sourceHomebrewTitle": "Custom Weapons Pack"
}
```

`sourceHomebrewTitle` is only present for homebrew items.

### GET `/api/item-templates/{id}` — Get template

Response: `ItemTemplateResponse`

### GET `/api/item-templates/campaign/{campaignId}` — List templates for campaign

Returns vanilla templates + templates from attached homebrew.

Response: `List<ItemTemplateResponse>`

### PUT `/api/item-templates/{id}` — Update template

Same request body as create. Response: `ItemTemplateResponse`

### DELETE `/api/item-templates/{id}` — Delete template (Admin only)

Response: `Void`

---

## 23. Admin Reference Data — `/api/admin`

All admin endpoints require ADMIN role.

### Stat Types — `/api/admin/stat-types`
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/admin/stat-types` | - | `List<StatTypeResponse>` |
| POST | `/api/admin/stat-types` | `{ name, description }` | `StatTypeResponse` (201) |
| GET | `/api/admin/stat-types/{id}` | - | `StatTypeResponse` |
| PUT | `/api/admin/stat-types/{id}` | `{ name, description }` | `StatTypeResponse` |
| DELETE | `/api/admin/stat-types/{id}` | - | `Void` |

### Item Types — `/api/admin/item-types`
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/admin/item-types` | - | `List<ItemTypeResponse>` |
| POST | `/api/admin/item-types` | `{ name, description, slot, damageDice, damageBonus, damageType, skillId, skillActivation }` | `ItemTypeResponse` (201) |
| GET | `/api/admin/item-types/{id}` | - | `ItemTypeResponse` |
| PUT | `/api/admin/item-types/{id}` | same as create | `ItemTypeResponse` |
| DELETE | `/api/admin/item-types/{id}` | - | `Void` |

### Character Classes — `/api/admin/classes`
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/admin/classes` | - | `List<CharacterClassResponse>` |
| POST | `/api/admin/classes` | `{ name (max 50), description }` | `CharacterClassResponse` (201) |
| GET | `/api/admin/classes/{id}` | - | `CharacterClassResponse` |
| PUT | `/api/admin/classes/{id}` | same as create | `CharacterClassResponse` |
| DELETE | `/api/admin/classes/{id}` | - | `Void` |

### Character Races — `/api/admin/races`
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/admin/races` | - | `List<CharacterRaceResponse>` |
| POST | `/api/admin/races` | `{ name (max 50), description }` | `CharacterRaceResponse` (201) |
| PUT | `/api/admin/races/{id}` | same as create | `CharacterRaceResponse` |
| DELETE | `/api/admin/races/{id}` | - | `Void` |

### Skills — `/api/admin/skills`
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/admin/skills` | - | `List<SkillResponse>` |
| POST | `/api/admin/skills` | `{ name (max 100), description, skillType (max 50), damageDice, damageBonus, damageType }` | `SkillResponse` (201) |
| GET | `/api/admin/skills/{id}` | - | `SkillResponse` |
| PUT | `/api/admin/skills/{id}` | same as create | `SkillResponse` |
| DELETE | `/api/admin/skills/{id}` | - | `Void` |

### Skill Effects — `/api/admin/skills/{skillId}/effects`
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `.../effects` | - | `List<SkillEffectResponse>` |
| PUT | `.../effects` | `{ effects: [{ buffDebuffId, effectRole: "BUFF|DEBUFF", chancePercent: 1-100 }] }` | `List<SkillEffectResponse>` |

### Subclasses — `/api/admin/subclasses`
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/admin/subclasses` | - | `List<SubclassResponse>` |
| POST | `/api/admin/subclasses` | `{ name (max 100), classId, description }` | `SubclassResponse` (201) |
| GET | `/api/admin/subclasses/{id}` | - | `SubclassResponse` |
| PUT | `/api/admin/subclasses/{id}` | same as create | `SubclassResponse` |
| DELETE | `/api/admin/subclasses/{id}` | - | `Void` |

### Feats — `/api/admin/feats`
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/admin/feats` | - | `List<FeatResponse>` |
| POST | `/api/admin/feats` | `{ name (max 100), description, prerequisites }` | `FeatResponse` (201) |
| GET | `/api/admin/feats/{id}` | - | `FeatResponse` |
| PUT | `/api/admin/feats/{id}` | same as create | `FeatResponse` |
| DELETE | `/api/admin/feats/{id}` | - | `Void` |

### Class Level Rewards — `/api/admin/classes/{classId}/level-rewards`
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `.../level-rewards` | - | `List<ClassLevelRewardResponse>` |
| POST | `.../level-rewards` | `{ requiredLevel (1-20), rewardType, rewardId, isChoice }` | `ClassLevelRewardResponse` (201) |
| DELETE | `.../level-rewards/{rewardId}` | - | `Void` |

### Buffs & Debuffs — `/api/admin/buffs-debuffs`
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/admin/buffs-debuffs` | Query: `?isBuff=true&effectType=STAT_MODIFIER` | `List<BuffDebuffResponse>` |
| POST | `/api/admin/buffs-debuffs` | `{ name (max 100), description, effectType (max 30), targetStatId, modifierValue, durationRounds, isBuff }` | `BuffDebuffResponse` (201) |
| GET | `/api/admin/buffs-debuffs/{id}` | - | `BuffDebuffResponse` |
| PUT | `/api/admin/buffs-debuffs/{id}` | same as create | `BuffDebuffResponse` |
| DELETE | `/api/admin/buffs-debuffs/{id}` | - | `Void` |

`BuffDebuffResponse`:
```json
{
  "id": "uuid",
  "name": "Blessed",
  "description": "...",
  "effectType": "STAT_MODIFIER",
  "targetStatId": "uuid",
  "targetStatName": "STR",
  "modifierValue": 2,
  "durationRounds": 5,
  "isBuff": true,
  "createdAt": "ISO-8601"
}
```

### Enchantment Types — `/api/admin/enchantment-types`
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/admin/enchantment-types` | - | `List<EnchantmentTypeResponse>` |
| POST | `/api/admin/enchantment-types` | `{ name (max 100), description, damageDice, damageBonus, damageType, buffDebuffId }` | `EnchantmentTypeResponse` (201) |
| GET | `/api/admin/enchantment-types/{id}` | - | `EnchantmentTypeResponse` |
| PUT | `/api/admin/enchantment-types/{id}` | same as create | `EnchantmentTypeResponse` |
| DELETE | `/api/admin/enchantment-types/{id}` | - | `Void` |

Public read-only: `GET /api/enchantment-types` — same response.

`EnchantmentTypeResponse`:
```json
{
  "id": "uuid",
  "name": "Fire Enchantment",
  "description": "...",
  "damageDice": "1d6",
  "damageBonus": 2,
  "damageType": "FIRE",
  "buffDebuff": { ... BuffDebuffResponse ... }
}
```

### Admin Users & Teams
| Method | Path | Response |
|--------|------|----------|
| GET | `/api/admin/users` | `List<UserResponse>` |
| GET | `/api/admin/teams` | `List<TeamResponse>` |

### Admin Homebrew Management
| Method | Path | Response |
|--------|------|----------|
| GET | `/api/admin/homebrew` | `Page<HomebrewPackageResponse>` |
| DELETE | `/api/admin/homebrew/{id}` | `Void` (hard delete) |
| GET | `/api/admin/homebrew/tags` | `List<HomebrewTagResponse>` |

---

## 24. WebSocket — Real-time Updates

### Connection

Protocol: STOMP over SockJS.

Connect URL: `/ws` with JWT token as query parameter.
```
ws://host/ws?token=<jwt>
```

### Topics

| Topic | Description |
|-------|-------------|
| `/topic/campaign/{campaignId}/updates` | Campaign-wide events |
| `/topic/character/{characterId}/updates` | Character-specific events |
| `/user/queue/notifications` | Private notifications per user |

### Event Payload

```json
{
  "type": "ITEM_GRANTED",
  "campaignId": "uuid",
  "characterId": "uuid",
  "data": { ... },
  "timestamp": "ISO-8601",
  "triggeredBy": "uuid"
}
```

### Event Types

| Event | When | Topic |
|-------|------|-------|
| `ITEM_GRANTED` | GM gives item to character | campaign + character |
| `ITEM_REMOVED` | GM takes item from character | campaign + character |
| `BUFF_APPLIED` | Effect applied to character | campaign + character |
| `BUFF_REMOVED` | Effect removed from character | campaign + character |
| `XP_GRANTED` | Character receives XP | campaign + character |
| `HP_CHANGED` | Damage or healing applied | campaign + character |
| `CHARACTER_UPDATED` | Any stat change by GM | campaign + character |
| `NPC_REVEALED` | NPC made visible to players | campaign |
| `NPC_HIDDEN` | NPC hidden from players | campaign |
| `QUEST_UPDATED` | Quest status/visibility changed | campaign |
| `CAMPAIGN_STATUS_CHANGED` | Campaign paused/completed/activated | campaign |
| `MEMBER_KICKED` | A member was removed | campaign + user |

### Auth

WebSocket connections are authenticated via JWT passed as a query param on the SockJS connect URL. Users can only subscribe to campaigns they belong to.

---

## Frontend Development Instructions

### Tech Stack
- React with TypeScript
- Tailwind CSS for styling
- STOMP.js + SockJS-client for WebSocket

### API Client Setup
1. Store JWT token from login/register in localStorage or a secure cookie.
2. Attach `Authorization: Bearer <token>` header to all API requests.
3. Parse every response through the `ApiResponse<T>` envelope — check `success` before accessing `data`.
4. On 401 responses, redirect to login.
5. On 400 with `fields`, display field-level validation errors.

### Key Business Rules to Enforce in UI

1. **Role-based rendering:** Hide admin panels from non-admin users. Hide GM-only actions (grant items, apply effects, create NPCs/quests, distribute XP, session notes) from players. Show invite code to players only when no GM is in the campaign.

2. **Campaign status:** When status is `PAUSED` or `COMPLETED`, disable all editing for players. GMs can still add NPCs, quests, and notes but cannot edit characters.

3. **Character ownership:** Players can only edit their own ACTIVE characters. DEAD/RESERVE characters are read-only. GMs can edit any character when campaign is ACTIVE.

4. **Item stacking:** When renaming a stacked item (quantity > 1), prompt the user: "Rename entire stack?" vs "Rename one (splits from stack)". Send `renameEntireStack: true/false` accordingly.

5. **Level-up flow:** After XP distribution, check if any character crossed a threshold. Show a level-up modal that calls GET `/level-up-options`, lets the user choose class + rewards, then POST `/level-up`.

6. **Ability checks:** The "Roll check" button under each stat should call the ability-check endpoint and display the total modifier. No dice roll — players use physical dice.

7. **NPC visibility:** `privateDescription` must NEVER be shown to players. The backend strips it, but the frontend should also not render it for non-GM users.

8. **GM session notes:** Only render the notes tab/section for GM users. Never show to players.

9. **Homebrew source tags:** When displaying items/classes/skills from homebrew, show the `sourceHomebrewTitle` as a tag/badge so users know which homebrew pack it comes from.

10. **WebSocket:** Subscribe to campaign and character topics on entering a campaign view. Refetch relevant data on event receipt. Unsubscribe on navigation away.

11. **Item transfer:** Only allow transfer of unequipped items. Both characters must be in the same campaign.

12. **Shared storage:** Items can be deposited from character inventory and taken by specifying a target character. Both operations require same-campaign validation.

### Suggested Page Structure

- `/login`, `/register` — Auth pages
- `/campaigns` — Campaign list (paginated)
- `/campaigns/:id` — Campaign dashboard (members, status, homebrew, invite code)
- `/campaigns/:id/characters` — Character list
- `/campaigns/:id/characters/:charId` — Character sheet (stats, inventory, effects, wallet, resources, HP)
- `/campaigns/:id/quests` — Quest list and detail
- `/campaigns/:id/npcs` — NPC list and detail
- `/campaigns/:id/locations` — Location list
- `/campaigns/:id/shared-storage` — Shared storage containers
- `/campaigns/:id/gm-notes` — GM session notes (GM only)
- `/campaigns/:id/xp` — XP distribution (GM only)
- `/homebrew` — Homebrew management (authoring)
- `/homebrew/marketplace` — Browse and install
- `/admin` — Admin panel (ADMIN only)
- `/admin/stat-types`, `/admin/item-types`, `/admin/classes`, etc. — Admin CRUD pages
