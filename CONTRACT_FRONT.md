# CONTRACT_FRONT.md - Frontend API Contract

This contract is checked against the Spring controllers and DTOs in `src/main/java/com/dnd/app` on 2026-05-31.

## Global Rules

Base API path: `/api`.

Auth:
- `POST /api/auth/register`, `POST /api/auth/login`, Swagger, Actuator, and `/ws/**` are permit-all.
- Every other REST endpoint requires `Authorization: Bearer <jwt>`.
- `/api/enchantment-types` is read-only, but it is still authenticated by `SecurityConfig`.

Response envelope:

```json
{
  "success": true,
  "data": {},
  "message": "optional",
  "error": null,
  "fields": null
}
```

Null fields are omitted by `@JsonInclude(NON_NULL)`.

Known error codes:

| HTTP | error |
|---|---|
| 400 | `BAD_REQUEST`, `VALIDATION_ERROR` |
| 401 | `BAD_CREDENTIALS` |
| 403 | `ACCESS_DENIED` |
| 404 | `NOT_FOUND` |
| 409 | `DUPLICATE` |
| 422 | `UNPROCESSABLE_ENTITY` |
| 500 | `INTERNAL_ERROR` |

Pagination uses Spring `Page<T>`: `content`, `totalElements`, `totalPages`, `number`, `size`, etc. Pageable endpoints accept `page`, `size`, `sort`.

## Enums

| Enum | Values |
|---|---|
| `Role` | `PLAYER`, `GAME_MASTER`, `ADMIN` |
| `CampaignRole` | `GM`, `PLAYER` |
| `CampaignStatus` | `ACTIVE`, `PAUSED`, `COMPLETED` |
| `CharacterStatus` | `ACTIVE`, `DEAD`, `RESERVE` |
| `ContentType` | `ITEM_TYPE`, `CHARACTER_CLASS`, `SKILL`, `FEAT`, `SUBCLASS`, `RACE`, `STAT_TYPE`, `BUFF_DEBUFF`, `ENCHANTMENT_TYPE`, `CURRENCY`, `CUSTOM_RESOURCE`, `ITEM_TEMPLATE` |
| `DamageType` | `SLASHING`, `PIERCING`, `BLUDGEONING`, `FIRE`, `COLD`, `LIGHTNING`, `POISON`, `NECROTIC`, `RADIANT`, `PSYCHIC`, `FORCE`, `THUNDER`, `ACID` |
| `EffectRole` | `BUFF`, `DEBUFF` |
| `EquipmentSlot` | `HEAD`, `CHEST`, `LEGS`, `FEET`, `MAIN_HAND`, `OFF_HAND`, `RING_LEFT`, `RING_RIGHT`, `NECK`, `CLOAK` |
| `HomebrewStatus` | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| `QuestStatus` | `ACTIVE`, `COMPLETED`, `FAILED`, `HIDDEN`, `ARCHIVED` |
| `Rarity` | `COMMON`, `UNCOMMON`, `RARE`, `VERY_RARE`, `LEGENDARY` |
| `RewardType` | `SKILL`, `SUBCLASS`, `FEAT` |
| `SkillActivation` | `PASSIVE`, `ACTIVE` |
| `WebSocketEventType` | `ITEM_GRANTED`, `ITEM_REMOVED`, `BUFF_APPLIED`, `BUFF_REMOVED`, `XP_GRANTED`, `HP_CHANGED`, `CHARACTER_UPDATED`, `NPC_REVEALED`, `NPC_HIDDEN`, `QUEST_UPDATED`, `CAMPAIGN_STATUS_CHANGED`, `MEMBER_KICKED`, `WALLET_CHANGED` |

## REST Endpoints

All response types below are wrapped as `ApiResponse<T>`.

### Auth

| Method | Path | Body | Response | Notes |
|---|---|---|---|---|
| POST | `/api/auth/register` | `RegisterRequest` | `UserResponse` | 201. Does not return JWT. `role` must be `PLAYER` or `GAME_MASTER`. |
| POST | `/api/auth/login` | `LoginRequest` | `AuthResponse` | Returns JWT and user. |

### Campaigns

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/campaigns` | `CreateCampaignRequest` | `CampaignResponse` |
| GET | `/api/campaigns` | - | `Page<CampaignResponse>` |
| GET | `/api/campaigns/{id}` | - | `CampaignDetailResponse` |
| PUT | `/api/campaigns/{id}` | `UpdateCampaignRequest` | `CampaignResponse` |
| DELETE | `/api/campaigns/{id}` | - | `Void` |
| POST | `/api/campaigns/join` | `JoinCampaignRequest` | `CampaignResponse` |
| POST | `/api/campaigns/{id}/leave` | - | `Void` |
| POST | `/api/campaigns/{id}/kick` | `KickMemberRequest` | `Void` |
| PUT | `/api/campaigns/{id}/status` | `ChangeCampaignStatusRequest` | `CampaignResponse` |
| GET | `/api/campaigns/{id}/invite-code` | - | `InviteCodeResponse` |
| POST | `/api/campaigns/{id}/invite-code/regenerate` | - | `InviteCodeResponse` |
| POST | `/api/campaigns/{id}/characters/{characterId}/reassign` | `ReassignCharacterRequest` | `CharacterResponse` |
| POST | `/api/campaigns/{id}/homebrew` | `ActivateHomebrewRequest` | `CampaignHomebrewResponse` |
| DELETE | `/api/campaigns/{id}/homebrew/{packageId}` | - | `Void` |
| PUT | `/api/campaigns/{id}/homebrew/{packageId}/version` | `UpdatePinnedVersionRequest` | `CampaignHomebrewResponse` |
| GET | `/api/campaigns/{id}/homebrew` | - | `List<CampaignHomebrewResponse>` |
| GET | `/api/campaigns/{id}/available-content` | - | `CampaignAvailableContentResponse` |

### Campaign Characters

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/campaigns/{campaignId}/characters` | `CreateCharacterRequest` | `CharacterResponse` |
| GET | `/api/campaigns/{campaignId}/characters` | - | `List<CharacterResponse>` |
| GET | `/api/campaigns/{campaignId}/characters/{characterId}` | - | `CharacterResponse` |
| PUT | `/api/campaigns/{campaignId}/characters/{characterId}` | `UpdateCharacterRequest` | `CharacterResponse` |
| DELETE | `/api/campaigns/{campaignId}/characters/{characterId}` | - | `Void` |
| GET | `/api/campaigns/{campaignId}/characters/{characterId}/stats` | - | `List<CharacterStatResponse>` |
| PUT | `/api/campaigns/{campaignId}/characters/{characterId}/stats/{statId}` | `UpdateStatRequest` | `CharacterStatResponse` |
| GET | `/api/campaigns/{campaignId}/characters/{characterId}/ability-check/{statTypeId}` | - | `AbilityCheckResponse` |
| GET | `/api/campaigns/{campaignId}/characters/{characterId}/wallet` | - | `List<WalletEntryResponse>` |
| POST | `/api/campaigns/{campaignId}/characters/{characterId}/wallet` | `ModifyCurrencyRequest` | `WalletEntryResponse` |
| GET | `/api/campaigns/{campaignId}/characters/{characterId}/resources` | - | `List<ResourceResponse>` |
| POST | `/api/campaigns/{campaignId}/characters/{characterId}/resources` | `ModifyResourceRequest` | `ResourceResponse` |
| POST | `/api/campaigns/{campaignId}/characters/{characterId}/hp` | `ModifyHpRequest` | `CharacterResponse` |

> **Wallet POST (`/wallet`)** is a single endpoint for **both** credit and debit. `ModifyCurrencyRequest.amount > 0`
> adds; `amount < 0` deducts. The wallet entry for a currency is created on its **first credit** — you do not need to
> initialize it first (only `Gold` is auto-created with the character). Debiting below zero, or debiting a currency the
> character has no entry for, returns **400** `Insufficient funds for this operation`. On success the server broadcasts a
> `WALLET_CHANGED` event (see WebSocket section). Available currency types come from
> `GET /api/campaigns/{campaignId}/reference/currencies` (`List<CurrencyTypeResponse>`).

### Character Inventory and Effects

| Method | Path | Body | Response | Notes |
|---|---|---|---|---|
| GET | `/api/campaigns/{campaignId}/characters/{characterId}/inventory` | - | `List<ItemInstanceResponse>` | All items. |
| GET | `/api/campaigns/{campaignId}/characters/{characterId}/inventory/equipped` | - | `List<ItemInstanceResponse>` | Equipped only. |
| GET | `/api/campaigns/{campaignId}/characters/{characterId}/inventory/backpack` | - | `List<ItemInstanceResponse>` | Backpack only. |
| POST | `/api/campaigns/{campaignId}/characters/{characterId}/inventory` | `GrantItemRequest` | `ItemInstanceResponse` | 201. This is the grant endpoint; there is no `/grant` suffix. |
| POST | `/api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/equip` | `EquipItemRequest` | `ItemInstanceResponse` | Uses POST, not PUT. |
| POST | `/api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/unequip` | - | `ItemInstanceResponse` | Uses POST, not PUT. |
| DELETE | `/api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}` | - | `Void` | Remove item. |
| POST | `/api/campaigns/{campaignId}/characters/{fromCharId}/inventory/{instanceId}/transfer` | `TransferItemRequest` | `ItemInstanceResponse` | Source path var is `fromCharId`. |
| PUT | `/api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/rename` | `RenameItemRequest` | `ItemInstanceResponse` | Rename or split stack. |
| GET | `/api/campaigns/{campaignId}/characters/{characterId}/effects` | - | `List<CharacterActiveEffectResponse>` | Active effects. |
| POST | `/api/campaigns/{campaignId}/characters/{characterId}/effects` | `ApplyEffectRequest` | `CharacterActiveEffectResponse` | 201. |
| DELETE | `/api/campaigns/{campaignId}/characters/{characterId}/effects/{effectId}` | - | `Void` | Remove effect. |
| GET | `/api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/enchantments` | - | `List<EnchantmentResponse>` | Item enchantments. |
| POST | `/api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/enchantments` | `AddEnchantmentRequest` | `EnchantmentResponse` | 201. |
| DELETE | `/api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/enchantments/{enchantmentId}` | - | `Void` | Remove enchantment. |

### Level Up and XP

| Method | Path | Body | Response |
|---|---|---|---|
| GET | `/api/characters/{id}/level-up-options` | - | `LevelUpOptionsResponse` |
| POST | `/api/characters/{id}/level-up` | `LevelUpRequest` | `LevelUpResultResponse` |
| GET | `/api/characters/{id}/rewards` | - | `CharacterRewardsResponse` |
| POST | `/api/campaigns/{campaignId}/xp` | `DistributeXpRequest` | `Map` with `charactersUpdated`, `xpGranted` |

### Quests

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/campaigns/{campaignId}/quests` | `CreateQuestRequest` | `QuestResponse` |
| GET | `/api/campaigns/{campaignId}/quests` | - | `List<QuestResponse>` |
| GET | `/api/campaigns/{campaignId}/quests/{questId}` | - | `QuestResponse` |
| PUT | `/api/campaigns/{campaignId}/quests/{questId}` | `UpdateQuestRequest` | `QuestResponse` |
| DELETE | `/api/campaigns/{campaignId}/quests/{questId}` | - | `Void` |
| GET | `/api/campaigns/{campaignId}/quests/{questId}/rewards` | - | `List<QuestRewardResponse>` |
| POST | `/api/campaigns/{campaignId}/quests/{questId}/rewards` | `CreateQuestRewardRequest` | `QuestRewardResponse` |
| DELETE | `/api/campaigns/{campaignId}/quests/{questId}/rewards/{rewardId}` | - | `Void` |
| POST | `/api/campaigns/{campaignId}/quests/{questId}/notes` | `CreateNoteRequest` | `NoteResponse` |
| PUT | `/api/campaigns/{campaignId}/quests/{questId}/notes/{noteId}` | `UpdateNoteRequest` | `NoteResponse` |
| DELETE | `/api/campaigns/{campaignId}/quests/{questId}/notes/{noteId}` | - | `Void` |
| POST | `/api/campaigns/{campaignId}/quests/{questId}/npcs/{npcId}` | - | `Void` |
| DELETE | `/api/campaigns/{campaignId}/quests/{questId}/npcs/{npcId}` | - | `Void` |
| POST | `/api/campaigns/{campaignId}/quests/{questId}/locations/{locationId}` | - | `Void` |
| DELETE | `/api/campaigns/{campaignId}/quests/{questId}/locations/{locationId}` | - | `Void` |

### NPCs, Locations, GM Notes, Shared Storage

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/campaigns/{campaignId}/npcs` | `CreateNpcRequest` | `NpcResponse` |
| GET | `/api/campaigns/{campaignId}/npcs` | - | `List<NpcResponse>` |
| GET | `/api/campaigns/{campaignId}/npcs/{npcId}` | - | `NpcResponse` |
| PUT | `/api/campaigns/{campaignId}/npcs/{npcId}` | `UpdateNpcRequest` | `NpcResponse` |
| DELETE | `/api/campaigns/{campaignId}/npcs/{npcId}` | - | `Void` |
| POST | `/api/campaigns/{campaignId}/npcs/{npcId}/toggle-visibility` | - | `NpcResponse` |
| POST | `/api/campaigns/{campaignId}/npcs/{npcId}/notes` | `CreateNoteRequest` | `NoteResponse` |
| PUT | `/api/campaigns/{campaignId}/npcs/{npcId}/notes/{noteId}` | `UpdateNoteRequest` | `NoteResponse` |
| DELETE | `/api/campaigns/{campaignId}/npcs/{npcId}/notes/{noteId}` | - | `Void` |
| POST | `/api/campaigns/{campaignId}/locations` | `CreateLocationRequest` | `LocationResponse` |
| GET | `/api/campaigns/{campaignId}/locations` | - | `List<LocationResponse>` |
| GET | `/api/campaigns/{campaignId}/locations/{locationId}` | - | `LocationResponse` |
| PUT | `/api/campaigns/{campaignId}/locations/{locationId}` | `UpdateLocationRequest` | `LocationResponse` |
| DELETE | `/api/campaigns/{campaignId}/locations/{locationId}` | - | `Void` |
| POST | `/api/campaigns/{campaignId}/locations/{locationId}/toggle-visibility` | - | `LocationResponse` |
| POST | `/api/campaigns/{campaignId}/gm-notes` | `CreateGmNoteRequest` | `GmSessionNoteResponse` |
| GET | `/api/campaigns/{campaignId}/gm-notes` | - | `List<GmSessionNoteResponse>` |
| GET | `/api/campaigns/{campaignId}/gm-notes/{noteId}` | - | `GmSessionNoteResponse` |
| PUT | `/api/campaigns/{campaignId}/gm-notes/{noteId}` | `UpdateGmNoteRequest` | `GmSessionNoteResponse` |
| DELETE | `/api/campaigns/{campaignId}/gm-notes/{noteId}` | - | `Void` |
| POST | `/api/campaigns/{campaignId}/shared-storage` | `CreateSharedStorageRequest` | `SharedStorageResponse` |
| GET | `/api/campaigns/{campaignId}/shared-storage` | - | `List<SharedStorageResponse>` |
| GET | `/api/campaigns/{campaignId}/shared-storage/{storageId}` | - | `SharedStorageResponse` |
| DELETE | `/api/campaigns/{campaignId}/shared-storage/{storageId}` | - | `Void` |
| POST | `/api/campaigns/{campaignId}/shared-storage/{storageId}/items/{instanceId}/deposit` | - | `Void` |
| POST | `/api/campaigns/{campaignId}/shared-storage/{storageId}/items/{instanceId}/take/{characterId}` | - | `Void` |

### Homebrew

| Method | Path | Body | Response | Notes |
|---|---|---|---|---|
| POST | `/api/homebrew` | `CreateHomebrewRequest` | `HomebrewDetailResponse` | 201. |
| GET | `/api/homebrew/my` | query `status?`, pageable | `Page<HomebrewPackageResponse>` | Own packages. |
| GET | `/api/homebrew/my/{id}` | - | `HomebrewDetailResponse` | Own package. |
| PUT | `/api/homebrew/my/{id}` | `UpdateHomebrewRequest` | `HomebrewDetailResponse` | Draft only. |
| POST | `/api/homebrew/my/{id}/content` | `AddContentRequest` | `HomebrewDetailResponse` | 201. |
| DELETE | `/api/homebrew/my/{id}/content/{contentItemId}` | - | `Void` | Draft only. |
| POST | `/api/homebrew/my/{id}/publish` | - | `HomebrewDetailResponse` | DRAFT -> PUBLISHED, increments version. |
| POST | `/api/homebrew/my/{id}/unpublish` | - | `HomebrewDetailResponse` | PUBLISHED -> DRAFT. |
| DELETE | `/api/homebrew/my/{id}` | - | `Map` with `message`, `installationCount` | Soft delete. |
| GET | `/api/homebrew/marketplace` | query `search?`, `tags?`, `sort?`, `page`, `size` | `Page<HomebrewPackageResponse>` | `sort`: `downloads`, `oldest`, default `newest`. |
| GET | `/api/homebrew/marketplace/{id}` | - | `HomebrewDetailResponse` | Published packages only. |
| POST | `/api/homebrew/marketplace/{id}/install` | - | `Map` with `addedAt`, `packageVersion` | Adds to GM library. |
| GET | `/api/homebrew/installed` | pageable | `Page<InstalledHomebrewResponse>` | GM installed packages. |
| DELETE | `/api/homebrew/installed/{installationId}` | - | `Void` | Controller name says `installationId`; service treats the value as `packageId`. |
| POST | `/api/homebrew/marketplace/{id}/rate` | `RateHomebrewRequest` | `HomebrewRatingResponse` | Validation accepts `rating` from `-1` to `1`; rating totals count only `1` and `-1`. |
| GET | `/api/homebrew/marketplace/{id}/rating` | - | `HomebrewRatingResponse` | Current user's rating included. |
| GET | `/api/homebrew/library` | - | `List<HomebrewPackageResponse>` | GM library. |
| POST | `/api/homebrew/library/{packageId}` | - | `Void` | 201. |
| DELETE | `/api/homebrew/library/{packageId}` | - | `Void` | Remove from library. |

### Item Templates and Enchantment Types

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/item-templates` | `CreateItemTemplateRequest` | `ItemTemplateResponse` |
| GET | `/api/item-templates/{id}` | - | `ItemTemplateResponse` |
| GET | `/api/item-templates/campaign/{campaignId}` | - | `List<ItemTemplateResponse>` |
| PUT | `/api/item-templates/{id}` | `CreateItemTemplateRequest` | `ItemTemplateResponse` |
| DELETE | `/api/item-templates/{id}` | - | `Void` |
| GET | `/api/enchantment-types` | - | `List<EnchantmentTypeResponse>` |

### Admin

All `/api/admin/**` endpoints require `ADMIN`.

| Method | Path | Body | Response |
|---|---|---|---|
| GET | `/api/admin/stat-types` | - | `List<StatTypeResponse>` |
| POST | `/api/admin/stat-types` | `CreateStatTypeRequest` | `StatTypeResponse` |
| GET | `/api/admin/stat-types/{id}` | - | `StatTypeResponse` |
| PUT | `/api/admin/stat-types/{id}` | `CreateStatTypeRequest` | `StatTypeResponse` |
| DELETE | `/api/admin/stat-types/{id}` | - | `Void` |
| GET | `/api/admin/item-types` | - | `List<ItemTypeResponse>` |
| POST | `/api/admin/item-types` | `CreateItemTypeRequest` | `ItemTypeResponse` |
| GET | `/api/admin/item-types/{id}` | - | `ItemTypeResponse` |
| PUT | `/api/admin/item-types/{id}` | `CreateItemTypeRequest` | `ItemTypeResponse` |
| DELETE | `/api/admin/item-types/{id}` | - | `Void` |
| GET | `/api/admin/character-classes` | - | `List<CharacterClassResponse>` |
| POST | `/api/admin/character-classes` | `CreateCharacterClassRequest` | `CharacterClassResponse` |
| GET | `/api/admin/character-classes/{id}` | - | `CharacterClassResponse` |
| PUT | `/api/admin/character-classes/{id}` | `CreateCharacterClassRequest` | `CharacterClassResponse` |
| DELETE | `/api/admin/character-classes/{id}` | - | `Void` |
| GET | `/api/admin/character-races` | - | `List<CharacterRaceResponse>` |
| POST | `/api/admin/character-races` | `CreateCharacterRaceRequest` | `CharacterRaceResponse` |
| GET | `/api/admin/character-races/{id}` | - | `CharacterRaceResponse` |
| PUT | `/api/admin/character-races/{id}` | `CreateCharacterRaceRequest` | `CharacterRaceResponse` |
| DELETE | `/api/admin/character-races/{id}` | - | `Void` |
| GET | `/api/admin/skills` | - | `List<SkillResponse>` |
| POST | `/api/admin/skills` | `CreateSkillRequest` | `SkillResponse` |
| GET | `/api/admin/skills/{id}` | - | `SkillResponse` |
| PUT | `/api/admin/skills/{id}` | `CreateSkillRequest` | `SkillResponse` |
| DELETE | `/api/admin/skills/{id}` | - | `Void` |
| GET | `/api/admin/skills/{id}/effects` | - | `List<SkillEffectResponse>` |
| PUT | `/api/admin/skills/{id}/effects` | `SetSkillEffectsRequest` | `List<SkillEffectResponse>` |
| GET | `/api/admin/subclasses` | - | `List<SubclassResponse>` |
| POST | `/api/admin/subclasses` | `CreateSubclassRequest` | `SubclassResponse` |
| GET | `/api/admin/subclasses/{id}` | - | `SubclassResponse` |
| PUT | `/api/admin/subclasses/{id}` | `CreateSubclassRequest` | `SubclassResponse` |
| DELETE | `/api/admin/subclasses/{id}` | - | `Void` |
| GET | `/api/admin/feats` | - | `List<FeatResponse>` |
| POST | `/api/admin/feats` | `CreateFeatRequest` | `FeatResponse` |
| GET | `/api/admin/feats/{id}` | - | `FeatResponse` |
| PUT | `/api/admin/feats/{id}` | `CreateFeatRequest` | `FeatResponse` |
| DELETE | `/api/admin/feats/{id}` | - | `Void` |
| GET | `/api/admin/classes/{classId}/level-rewards` | - | `List<ClassLevelRewardResponse>` |
| POST | `/api/admin/classes/{classId}/level-rewards` | `CreateClassLevelRewardRequest` | `ClassLevelRewardResponse` |
| DELETE | `/api/admin/classes/{classId}/level-rewards/{rewardEntryId}` | - | `Void` |
| GET | `/api/admin/users` | - | `List<UserResponse>` |
| GET | `/api/admin/homebrew` | query `status?`, `authorId?`, pageable | `Page<HomebrewPackageResponse>` |
| DELETE | `/api/admin/homebrew/{id}` | - | `Map` with `deletedPackageId`, `affectedLibraryEntries` |
| GET | `/api/admin/homebrew/tags` | - | `List<HomebrewTagResponse>` |
| DELETE | `/api/admin/homebrew/tags/{id}` | - | `Void` |
| GET | `/api/admin/buffs-debuffs` | query `isBuff?`, `effectType?` | `List<BuffDebuffResponse>` |
| POST | `/api/admin/buffs-debuffs` | `CreateBuffDebuffRequest` | `BuffDebuffResponse` |
| GET | `/api/admin/buffs-debuffs/{id}` | - | `BuffDebuffResponse` |
| PUT | `/api/admin/buffs-debuffs/{id}` | `CreateBuffDebuffRequest` | `BuffDebuffResponse` |
| DELETE | `/api/admin/buffs-debuffs/{id}` | - | `Void` |
| GET | `/api/admin/enchantment-types` | - | `List<EnchantmentTypeResponse>` |
| POST | `/api/admin/enchantment-types` | `CreateEnchantmentTypeRequest` | `EnchantmentTypeResponse` |
| GET | `/api/admin/enchantment-types/{id}` | - | `EnchantmentTypeResponse` |
| PUT | `/api/admin/enchantment-types/{id}` | `CreateEnchantmentTypeRequest` | `EnchantmentTypeResponse` |
| DELETE | `/api/admin/enchantment-types/{id}` | - | `Void` |

There is no `GET /api/admin/teams` controller in the current codebase.

## Request Schemas

`?` means the field is optional or nullable in the DTO.

| DTO | JSON fields |
|---|---|
| `ActivateHomebrewRequest` | `homebrewPackageId` |
| `AddContentRequest` | `contentType`, `contentId` |
| `AddEnchantmentRequest` | `enchantmentTypeId`, `notes?` |
| `ApplyEffectRequest` | `buffDebuffId`, `remainingRounds?` |
| `ChangeCampaignStatusRequest` | `status` |
| `CreateBuffDebuffRequest` | `name`, `description?`, `effectType`, `targetStatId?`, `modifierValue?`, `durationRounds?`, `isBuff` |
| `CreateCampaignRequest` | `name`, `description?` |
| `CreateCharacterRequest` | `name`, `classId`, `raceId`, `campaignId` |
| `CreateCharacterClassRequest` | `name`, `description?` |
| `CreateCharacterRaceRequest` | `name`, `description?` |
| `CreateClassLevelRewardRequest` | `requiredLevel`, `rewardType`, `rewardId`, `isChoice?` |
| `CreateEnchantmentTypeRequest` | `name`, `description?`, `damageDice?`, `damageBonus?`, `damageType?`, `buffDebuffId?` |
| `CreateFeatRequest` | `name`, `description?`, `prerequisites?` |
| `CreateGmNoteRequest` | `title`, `content` |
| `CreateHomebrewRequest` | `title`, `description?`, `tagNames?` |
| `CreateItemTemplateRequest` | `name`, `description?`, `itemTypeId?`, `rarity?`, `damageDice?`, `damageBonus?`, `damageType?`, `isStackable?`, `skillId?`, `skillActivation?` |
| `CreateItemTypeRequest` | `name`, `description?`, `slot`, `damageDice?`, `damageBonus?`, `damageType?`, `skillId?`, `skillActivation?` |
| `CreateLocationRequest` | `name`, `description?`, `isVisibleToPlayers?` |
| `CreateNoteRequest` | `content` |
| `CreateNpcRequest` | `name`, `publicDescription?`, `privateDescription?`, `isVisibleToPlayers?` |
| `CreateQuestRequest` | `title`, `description?`, `status?`, `isVisibleToPlayers?` |
| `CreateQuestRewardRequest` | `itemTemplateId?`, `quantity?`, `currencyTypeId?`, `currencyAmount?` |
| `CreateSharedStorageRequest` | `name` |
| `CreateSkillRequest` | `name`, `description?`, `skillType?`, `damageDice?`, `damageBonus?`, `damageType?` |
| `CreateStatTypeRequest` | `name`, `description?` |
| `CreateSubclassRequest` | `name`, `classId`, `description?` |
| `DistributeXpRequest` | `target`, `characterIds?`, `amount` |
| `EquipItemRequest` | `slot` |
| `GrantItemRequest` | `templateId`, `quantity?`, `customName?`, `isUnique?` |
| `JoinCampaignRequest` | `inviteCode` |
| `KickMemberRequest` | `userId` |
| `LevelUpRequest` | `classId`, `selections?` where each selection has `rewardType`, `rewardEntryId` |
| `LoginRequest` | `username`, `password` |
| `ModifyCurrencyRequest` | `currencyTypeId`, `amount` |
| `ModifyHpRequest` | `amount` (delta to currentHp; negative = damage absorbed by tempHp first), `setTempHp?` (optional: set new tempHp pool, applied BEFORE amount) |
| `ModifyResourceRequest` | `resourceTypeId`, `currentValue` |
| `RateHomebrewRequest` | `rating` (`-1` to `1`) |
| `ReassignCharacterRequest` | `newOwnerUserId` |
| `RegisterRequest` | `username`, `email`, `password`, `role` |
| `RenameItemRequest` | `customName`, `renameEntireStack?` |
| `SetSkillEffectsRequest` | `effects` where each effect has `buffDebuffId`, `effectRole`, `chancePercent` |
| `TransferItemRequest` | `toCharacterId` |
| `UpdateCampaignRequest` | `name?`, `description?` |
| `UpdateCharacterRequest` | `name?`, `raceId?` |
| `UpdateGmNoteRequest` | `title?`, `content?` |
| `UpdateHomebrewRequest` | `title?`, `description?`, `tagNames?` |
| `UpdateLocationRequest` | `name?`, `description?`, `isVisibleToPlayers?` |
| `UpdateNoteRequest` | `content` |
| `UpdateNpcRequest` | `name?`, `publicDescription?`, `privateDescription?`, `isVisibleToPlayers?` |
| `UpdatePinnedVersionRequest` | `pinnedVersion?` |
| `UpdateQuestRequest` | `title?`, `description?`, `status?`, `isVisibleToPlayers?` |
| `UpdateStatRequest` | `value` |

## Response Schemas

| DTO | JSON fields |
|---|---|
| `AbilityCheckResponse` | `statName`, `baseValue`, `modifier`, `buffBonus`, `equipmentBonus`, `totalModifier` |
| `AuthResponse` | `token`, `expiresIn`, `user` |
| `BuffDebuffResponse` | `id`, `name`, `description`, `effectType`, `targetStatId`, `targetStatName`, `modifierValue`, `durationRounds`, `isBuff`, `createdAt` |
| `CampaignAvailableContentResponse` | `classes`, `races`, `itemTypes`, `skills`, `feats`; items have `id`, `name`, `source`, `homebrewTitle` |
| `CampaignDetailResponse` | `id`, `name`, `description`, `status`, `inviteCode`, `memberCount`, `createdAt`, `updatedAt`, `members` |
| `CampaignHomebrewResponse` | `packageId`, `title`, `pinnedVersion`, `contentSummary` |
| `CampaignMemberResponse` | `userId`, `username`, `roleInCampaign`, `isCreator`, `joinedAt`, `kicked` |
| `CampaignResponse` | `id`, `name`, `description`, `status`, `inviteCode`, `memberCount`, `createdAt`, `updatedAt` |
| `CharacterActiveEffectResponse` | `id`, `buffDebuffId`, `buffDebuffName`, `isBuff`, `effectType`, `modifierValue`, `targetStatName`, `remainingRounds`, `appliedAt`, `appliedByUsername` |
| `CharacterClassResponse` | `id`, `name`, `description` |
| `CharacterRaceResponse` | `id`, `name`, `description` |
| `CharacterResponse` | `id`, `name`, `totalLevel`, `experience`, `classLevels`, `race`, `selectedLineageId`, `raceSnapshot`, `ownerId`, `ownerUsername`, `campaignId`, `status`, `stats`, `currentHp`, `maxHp`, `tempHp`, `alignment`, `background`, `avatarUrl`, `armorClass`, `speed`, `inspiration`, `hitDiceType`, `hitDiceTotal`, `deathSaveSuccesses`, `deathSaveFailures`, `savingThrowProficiencyStatNames`, `skillProficiencies`, `knownSpells`, `biography`, `features`, `attacks`, `createdAt`, `updatedAt` |
| `CharacterRewardsResponse` | `characterId`, `totalLevel`, `classBreakdown` |
| `CharacterStatResponse` | `id`, `statTypeId`, `statTypeName`, `value`, `effectiveValue`, `activeModifiers` |
| `ClassLevelResponse` | `classId`, `className`, `classLevel` |
| `ClassLevelRewardResponse` | `id`, `classId`, `requiredLevel`, `rewardType`, `rewardId`, `rewardName`, `isChoice` |
| `ContentSummaryDto` | `id`, `name`, `description`, `slot`, `skillType`, `prerequisites` |
| `EnchantmentResponse` | `id`, `enchantmentType`, `appliedAt`, `notes` |
| `EnchantmentTypeResponse` | `id`, `name`, `description`, `damageDice`, `damageBonus`, `damageType`, `buffDebuff` |
| `FeatResponse` | `id`, `name`, `description`, `prerequisites`, `createdAt`, `updatedAt` |
| `GmSessionNoteResponse` | `id`, `campaignId`, `authorUsername`, `title`, `content`, `createdAt`, `updatedAt` |
| `HomebrewContentSummary` | `itemTypeCount`, `classCount`, `skillCount`, `featCount` |
| `HomebrewDetailResponse` | `id`, `title`, `description`, `status`, `version`, `downloadCount`, `authorUsername`, `tags`, `contentSummary`, `contentByType`, `publishedAt`, `createdAt`, `isDeleted` |
| `HomebrewPackageResponse` | `id`, `title`, `description`, `status`, `version`, `downloadCount`, `authorUsername`, `tags`, `contentSummary`, `publishedAt`, `createdAt`, `isDeleted` |
| `HomebrewRatingResponse` | `likes`, `dislikes`, `netRating`, `userRating` |
| `HomebrewTagResponse` | `id`, `name`, `usageCount` |
| `InstalledHomebrewResponse` | `packageId`, `title`, `authorUsername`, `isDeleted`, `installedAt`, `sourceVersion`, `contentSummary` |
| `InviteCodeResponse` | `inviteCode` |
| `ItemInstanceResponse` | `id`, `templateId`, `templateName`, `displayName`, `customName`, `quantity`, `isUnique`, `slot`, `notes`, `rarity`, `enchantments` |
| `ItemTemplateResponse` | `id`, `name`, `description`, `itemTypeName`, `rarity`, `damageDice`, `damageBonus`, `damageType`, `isStackable`, `skillName`, `skillActivation`, `sourceHomebrewTitle` |
| `ItemTypeResponse` | `id`, `name`, `description`, `slot`, `damageDice`, `damageBonus`, `damageType`, `skillId`, `skillName`, `skillActivation` |
| `LevelUpOptionsResponse` | `currentTotalLevel`, `xpToNextLevel`, `availableClasses` |
| `LevelUpResultResponse` | `newTotalLevel`, `classLeveled`, `newClassLevel`, `rewardsAcquired` |
| `LocationResponse` | `id`, `name`, `description`, `isVisibleToPlayers`, `createdAt`, `updatedAt` |
| `NoteResponse` | `id`, `authorId`, `authorUsername`, `content`, `createdAt`, `updatedAt` |
| `NpcResponse` | `id`, `name`, `publicDescription`, `privateDescription`, `isVisibleToPlayers`, `notes`, `createdAt`, `updatedAt` |
| `QuestResponse` | `id`, `title`, `description`, `status`, `isVisibleToPlayers`, `notes`, `rewards`, `createdAt`, `updatedAt` |
| `QuestRewardResponse` | `id`, `itemTemplateId`, `itemTemplateName`, `quantity`, `currencyTypeId`, `currencyTypeName`, `currencyAmount` |
| `ResourceResponse` | `resourceTypeId`, `resourceName`, `currentValue`, `maxValue` |
| `SharedStorageResponse` | `id`, `name`, `campaignId`, `items`, `createdAt` |
| `SkillEffectResponse` | `id`, `buffDebuff`, `effectRole`, `chancePercent` |
| `SkillResponse` | `id`, `name`, `description`, `skillType`, `damageDice`, `damageBonus`, `damageType`, `effects`, `createdAt`, `updatedAt` |
| `StatModifierDetail` | `source`, `modifierValue` |
| `StatTypeResponse` | `id`, `name`, `description`, `isDefault` |
| `SubclassResponse` | `id`, `name`, `classId`, `className`, `description`, `createdAt`, `updatedAt` |
| `UserResponse` | `id`, `username`, `email`, `role`, `createdAt` |
| `WalletEntryResponse` | `currencyTypeId`, `currencyName`, `amount`, `goldEquivalent` |
| `WebSocketEventPayload` | `type`, `campaignId`, `characterId`, `data`, `timestamp`, `triggeredBy` |

## WebSocket

STOMP over SockJS endpoint: `/ws`.

Client inbound auth:
- Send native STOMP header `Authorization: Bearer <jwt>`, or native header `token: <jwt>`.
- `/ws/**` is permit-all at HTTP level; the interceptor authenticates CONNECT and checks campaign subscription membership.

Broker config:
- Application destination prefix: `/app`
- Simple broker prefixes: `/topic`, `/queue`
- User destination prefix: `/user`

Server sends:

| Destination | Payload |
|---|---|
| `/topic/campaign.{campaignId}` | `WebSocketEventPayload` |
| `/user/queue/notifications` | `WebSocketEventPayload` |

Only `/topic/campaign.{campaignId}` subscriptions are membership-checked in `WebSocketAuthInterceptor`.

> Note the **dot** before `{campaignId}`, not a slash. Under the RabbitMQ STOMP relay a `/topic/` destination
> must be a single segment with no inner slash; `/topic/campaign/{id}` is rejected with `Invalid destination`.
> The dot keeps the destination broker-valid while still encoding the campaign id.

Event payloads (the `data` field of `WebSocketEventPayload`):

| `type` | `characterId` | `data` |
|---|---|---|
| `WALLET_CHANGED` | the affected character | `WalletEntryResponse` (the **new** balance of the changed currency: `currencyTypeId`, `currencyName`, `amount`, `goldEquivalent`) |

`WALLET_CHANGED` is broadcast on `/topic/campaign.{campaignId}` to the whole campaign (GM + players) after the
currency change commits. Treat the payload as a notification: the FE should refetch authoritative wallet state
(`GET …/wallet`) and history (`GET …/wallet/history`) rather than trusting `data` as the only source of truth.

## Removed or Not Present

These paths are not present in current controllers and must not be used by the frontend:

- `/api/teams/**`
- `/api/artifacts/**`
- `/api/conditions/**`
- `/api/admin/classes` CRUD aliases
- `/api/admin/races` CRUD aliases
- `/api/homebrew/mine`
- `/api/homebrew/{id}` for authoring
- `/api/homebrew/ratings/{packageId}`
- `/api/campaigns/{id}/characters/{characterId}/inventory/grant`
- `PUT .../inventory/{instanceId}/equip`
- `PUT .../inventory/{instanceId}/unequip`
- `/topic/character/{characterId}/updates`
