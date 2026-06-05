# Frontend Prompt: Rich Class Creation and JSON Import

Нужно доработать UI создания и редактирования классов персонажа. Один и тот же rich-editor должен работать для двух областей:

- стандартные классы: только админ, global/vanilla scope;
- homebrew-классы: GM только внутри своих homebrew-пакетов.

Старые формы `name/description` оставить как быстрый минимальный путь, но основной редактор класса должен позволять описывать прогрессию уровней 1-20, награды уровней и связанный контент.

## Backend Endpoints

Admin, стандартные классы:

- `POST /api/admin/character-classes/rich`
- `POST /api/admin/character-classes/import-json`
- `PUT /api/admin/character-classes/{classId}/rich`

GM, homebrew-классы:

- `POST /api/homebrew/my/{packageId}/content/classes/rich`
- `POST /api/homebrew/my/{packageId}/content/classes/import-json`
- `PUT /api/homebrew/my/{packageId}/content/classes/{classId}/rich`

Standalone buff/debuff внутри homebrew-пакета:

- `POST /api/homebrew/my/{packageId}/content/buffs-debuffs`

JSON-import использует тот же request body, что rich-create. На фронте загрузка файла должна быть обычным file picker: прочитать файл, `JSON.parse`, провалидировать базовую форму и отправить parsed object на нужный endpoint.

В корне backend-проекта есть пример: `CLASS_IMPORT_EXAMPLE.json`.

## Access Rules

Админ:

- может создавать и редактировать только стандартные классы через `/api/admin/character-classes/*`;
- inline-created `SKILL`, `FEAT`, `SUBCLASS`, `BUFF_DEBUFF` создаются как стандартный/global контент;
- `rewardId` может ссылаться только на стандартный/global контент, не на homebrew.

GM:

- может создавать и редактировать классы только в своем homebrew-пакете через `/api/homebrew/my/{packageId}/...`;
- inline-created `SKILL`, `FEAT`, `SUBCLASS`, `BUFF_DEBUFF` автоматически попадают в тот же `packageId`;
- `rewardId` может ссылаться только на контент этого же homebrew-пакета.

Если UI открыт в homebrew-пакете, не показывать global content как доступный `rewardId`. Если UI открыт для админа в стандартном справочнике, не показывать homebrew content.

## Request Shape

```json
{
  "name": "Chronomancer",
  "description": "A class focused on time magic.",
  "levels": [
    {
      "level": 1,
      "rewards": [
        {
          "rewardType": "SKILL",
          "isChoice": false,
          "skill": {
            "name": "Temporal Bolt",
            "description": "Deal force damage and slow the target.",
            "skillType": "SPELL",
            "damageDice": "1d8",
            "damageBonus": 0,
            "damageType": "FORCE",
            "effects": [
              {
                "effectRole": "DEBUFF",
                "chancePercent": 100,
                "buffDebuff": {
                  "name": "Slowed by Time",
                  "description": "Target is slowed.",
                  "effectType": "SLOW",
                  "modifierValue": -2,
                  "durationRounds": 2,
                  "isBuff": false
                }
              }
            ]
          }
        },
        {
          "rewardType": "FEAT",
          "isChoice": true,
          "feat": {
            "name": "Time Sense",
            "description": "You sense temporal anomalies.",
            "prerequisites": "Chronomancer level 1"
          }
        }
      ]
    },
    {
      "level": 3,
      "rewards": [
        {
          "rewardType": "SUBCLASS",
          "isChoice": true,
          "subclass": {
            "name": "Keeper of Seconds",
            "description": "Subclass focused on precision."
          }
        }
      ]
    }
  ]
}
```

For every reward exactly one content source is allowed:

- `rewardId` for existing content in the current scope;
- `skill` for inline skill/spell;
- `feat` for inline feat;
- `subclass` for inline subclass;
- `buffDebuff` for inline buff/debuff.

Supported `rewardType`: `SKILL`, `FEAT`, `SUBCLASS`, `BUFF_DEBUFF`.

`isChoice=true` means the reward is a player choice option. The UI should visually group choice rewards on the same level so a user understands that these are alternatives, not all automatic grants.

## Response Shape

```json
{
  "success": true,
  "data": {
    "characterClass": {
      "id": "uuid",
      "name": "Chronomancer",
      "description": "Time mage"
    },
    "rewards": [
      {
        "id": "uuid",
        "classId": "uuid",
        "requiredLevel": 1,
        "rewardType": "SKILL",
        "rewardId": "uuid",
        "rewardName": "Temporal Bolt",
        "isChoice": false
      }
    ],
    "createdContent": {
      "SKILL": [],
      "FEAT": [],
      "SUBCLASS": [],
      "BUFF_DEBUFF": []
    },
    "packageDetail": {}
  }
}
```

For admin/global endpoints `packageDetail` can be `null`. For homebrew endpoints use `packageDetail` to refresh package content after save.

## UI Requirements

Build one reusable editor-style modal or full-screen drawer. Do not make this a small form.

Layout:

- header with class name, scope label, save, import JSON, cancel;
- left rail with levels 1-20, reward count and validation state per level;
- main pane with rewards for selected level;
- right pane or nested modal for creating/editing a reward source.

Controls:

- fixed level selector 1-20;
- reward type segmented control: Skill/Spell, Feat, Subclass, Buff/Debuff;
- Existing vs New tabs;
- `isChoice` toggle;
- inline skill form: name, description, skillType, damageDice, damageBonus, damageType, effects;
- skill effects repeatable rows: `BUFF`/`DEBUFF`, chance percent, existing buff/debuff select or inline buff/debuff form;
- buff/debuff form: name, description, effectType, targetStat, modifier, duration, isBuff;
- subclass inline form: name, description. Parent class is implicit.

Validation:

- class name required, max 50;
- skill/feat/subclass/buff names required;
- levels must be 1-20;
- reward must have exactly one source;
- `STAT_MODIFIER` buff/debuff requires `targetStatId`;
- `effectRole=BUFF` requires `isBuff=true`;
- `effectRole=DEBUFF` requires `isBuff=false`;
- prevent duplicate same reward source in the same level;
- JSON import must show parse/validation errors before network request.

After successful save:

- standard admin flow: refresh global class list and open class details/progression;
- homebrew flow: refresh package detail from `data.packageDetail`, or invalidate and reload package cache if null;
- show created content grouped by type from `data.createdContent`;
- keep the user in the editor summary state so they can review levels and rewards.
