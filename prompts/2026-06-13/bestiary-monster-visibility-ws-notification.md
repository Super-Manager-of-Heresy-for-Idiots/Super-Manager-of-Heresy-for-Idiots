# Bestiary — player notification on monster reveal (WebSocket)

**Status:** PROPOSED (frontend implemented, awaiting backend)
**Date:** 2026-06-13
**Scope:** Campaign bestiary — push a real-time event to players when the GM
makes a campaign monster visible, or creates a new monster that is immediately
visible.

## Motivation

On `/campaigns/{id}/bestiary` a player only ever sees monsters whose
`isVisibleToPlayers == true`. Today, when the GM reveals a monster (or creates
one already visible), nothing is pushed to connected players: their bestiary
list is served by the query `['bestiary','campaign',{cid},'monsters']`, which
only refetches on mount/manual invalidation. So a player sitting on the page
never learns a new monster appeared and gets no notification.

The app already has a STOMP/WebSocket fan-out (`/topic/campaign.{campaignId}`
broadcast + `/user/queue/notifications` private queue) that drives both a toast
and the notifications feed. NPC visibility already does this via
`NPC_REVEALED` / `NPC_HIDDEN`. Monsters need the exact same treatment.

## What the frontend now does (already implemented)

The FE understands two **new** `WsEventType` values and reacts to them:

- `MONSTER_REVEALED` — invalidates the campaign monster list (and the single
  monster query) **and** raises a player toast / feed entry.
- `MONSTER_HIDDEN` — invalidates the same caches **silently** (no toast — we do
  not want to reveal to a player that a monster they never saw existed). This
  exists so a player's list drops the monster live when it is hidden again.

FE touch points (for reference, no BE action needed here):
- `src/types/index.ts` — `MONSTER_REVEALED` / `MONSTER_HIDDEN` added to `WsEventType`.
- `src/hooks/useWebSocket.tsx` — cache invalidation + toast handling.
- `src/components/realtime/EventToast.tsx`, `NotificationsFeed.tsx` — visuals.
- `src/i18n/dict/components2.ts` — `cmp2.event.MONSTER_REVEALED` / `..._HIDDEN`.

## What the backend must do

### 1. Emit `MONSTER_REVEALED` when a monster becomes visible

Trigger points:

- `POST /api/campaigns/{campaignId}/monsters/{id}/toggle-visibility`
  — when the toggle flips `isVisibleToPlayers` from `false` → `true`.
- `POST /api/campaigns/{campaignId}/monsters`
  — when the created monster has `isVisibleToPlayers == true`.
- `PUT  /api/campaigns/{campaignId}/monsters/{id}`
  — if an update flips `isVisibleToPlayers` from `false` → `true`.
- `POST /api/campaigns/{campaignId}/monsters/clone/{sourceId}`
  — only if the resulting clone is created visible (likely it is not — clones
    usually start hidden; emit only if visible).

### 2. Emit `MONSTER_HIDDEN` when a monster becomes hidden

Trigger points: the toggle / update flipping `isVisibleToPlayers` from
`true` → `false`. (Optional but recommended so player lists stay consistent
live. No toast is shown for this on the FE.)

### 3. Event envelope

Publish to the campaign broadcast topic `/topic/campaign.{campaignId}` using
the existing `WsEvent` envelope (same shape as `NPC_REVEALED`):

```jsonc
{
  "type": "MONSTER_REVEALED",        // or "MONSTER_HIDDEN"
  "campaignId": "uuid",
  "data": {
    "monsterId": "uuid",             // REQUIRED — FE invalidates this monster
    "monsterName": "string",         // recommended — for a richer toast
    "message": "string?"             // optional — FE shows this as the toast body
  },
  "timestamp": "ISO-8601",
  "triggeredBy": "uuid"              // GM user id — FE suppresses the toast for the actor
}
```

Notes for the BE implementer:
- `triggeredBy` MUST be the acting GM's user id. The FE suppresses the
  toast/feed entry for the user who triggered the event (`isOwn` check), so the
  GM won't be notified about their own action — only the players will.
- The FE keys cache invalidation off `data.monsterId`; it must be present.
- No new enum value is needed beyond adding `MONSTER_REVEALED` / `MONSTER_HIDDEN`
  to whatever server-side event-type enum mirrors the FE `WsEventType`.

### 4. Authorization / delivery

- Broadcasting to `/topic/campaign.{campaignId}` is fine: only members of that
  campaign are subscribed, and players already filter to visible monsters. A
  `MONSTER_REVEALED` only ever references a now-visible monster, so no hidden
  data leaks. `MONSTER_HIDDEN` carries only the id/name of a monster that *was*
  visible, so it also leaks nothing new.
- If you prefer the private queue, `/user/queue/notifications` per player also
  works; the FE handles both identically.

## Acceptance

1. Player A is on `/campaigns/{id}/bestiary`. GM toggles a hidden monster to
   visible → Player A sees a "Monster Revealed" toast and the monster appears
   in the list without a manual refresh.
2. GM creates a new monster with "visible to players" on → same result.
3. GM toggles a visible monster back to hidden → it disappears from Player A's
   list with no toast.
4. The GM performing the action receives no toast for their own change.
