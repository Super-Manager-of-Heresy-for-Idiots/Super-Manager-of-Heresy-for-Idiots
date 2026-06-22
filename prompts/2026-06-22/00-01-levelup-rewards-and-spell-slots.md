# Frontend Prompt: награды level-up (все классы) + трекер ячеек заклинаний

Дата генерации: 2026-06-22 00:01.

## Зачем это

Был критический баг: повышение уровня на UI давало персонажу **только +HP**. Например, волшебник на 2 уровне должен получать:
- +1 подготовленное заклинание (нужно выбрать какое),
- +1 ячейку заклинаний,
- особенность «Академические знания» (Экспертность в одном из навыков, которым уже владеешь).

А получал только здоровье. Это касалось **всех классов**.

Причина — на бэкенде не были засеяны per-level reward-группы. Теперь бэкенд их генерирует для всех классов на уровнях 2–20: автоматические классовые особенности, выбор увеличения характеристик (ASI), выбор Экспертности, выбор новых заклинаний/заговоров. Плюс добавлен **трекер потраченных ячеек заклинаний** (с восстановлением).

Задача фронта: корректно отрисовать эти reward-группы в визарде level-up и реализовать UI ячеек заклинаний.

## ВАЖНО: актуальный контракт — это «content model»

Предыдущий промпт `prompts/frontend/frontend-levelup_2026-06-06.md` описывает **устаревшую** форму ответа (`rewardType` / `isChoice` / `rewards[]` / `rewardEntryId`, и `selections` с `rewardType`+`rewardEntryId`). Эта форма больше не отражает реальный ответ бэкенда.

Актуальные in-place маршруты `GET/POST /api/characters/{id}/level-up-options|level-up` теперь возвращают и принимают **content-модель** (DTO из `com.dnd.app.dto.content`): reward-группы с `groupKind`, `options[]`, `grants[]` и типизированным `payload`, а коммит — `selections[]` с `rewardGroupId` + `optionIds` + `childSelections`. Ниже — её точная форма. Если визард всё ещё на старой форме — мигрировать на эту.

## GET /api/characters/{id}/level-up-options

`?lang=ru|en` (по умолчанию `en`). Ответ: `ApiResponse<LevelUpOptions>`.

```jsonc
{
  "success": true,
  "data": {
    "currentTotalLevel": 1,
    "xpToNextLevel": 0,                  // 0 => можно повышать
    "availableClasses": [
      {
        "classId": "uuid-wizard",
        "className": "Волшебник",
        "currentLevelInClass": 1,
        "newLevelInClass": 2,
        "hpGain": { "hitDie": 6, "averageGain": 4, "conModifier": 1 },
        "derived": {
          "newTotalLevel": 2, "newClassLevel": 2,
          "proficiencyBonusBefore": 2, "proficiencyBonusAfter": 2
        },
        "rewardGroups": [ /* см. ниже */ ],
        "alreadySelected": [             // уже зафиксированные выборы (для повторного входа)
          { "rewardGroupId": "uuid", "rewardOptionId": "uuid" }
        ]
      }
    ]
  }
}
```

### RewardGroup

```jsonc
{
  "id": "uuid-group",
  "classId": "uuid-wizard",
  "classFeatureId": "uuid-or-null",
  "classLevel": 2,
  "groupKind": "AUTO" | "CHOICE",
  "prompt": "Экспертность: выберите навык, которым вы уже владеете",
  "description": "текст особенности",
  "chooseMin": 1,                        // AUTO => 0
  "chooseMax": 1,                        // AUTO => 0; у этих групп всегда ровно 1
  "repeatable": false,
  "sortOrder": 0,
  "options": [ /* RewardOption[], только для CHOICE */ ],
  "grants":  [ /* RewardGrant[],  только для AUTO  */ ]
}
```

- **AUTO** — награды в `grants[]` применяются автоматически. Показать как «получено автоматически», выбор не нужен.
- **CHOICE** — игрок обязан выбрать ровно `chooseMin..chooseMax` опций (для этих групп = ровно 1). Гранты лежат в `options[].grants[]`.

### RewardOption

```jsonc
{
  "id": "uuid-option", "optionKey": "expertise",
  "label": "Экспертность", "labelRu": "...", "labelEn": "...",
  "description": null, "recommended": false, "sortOrder": 0,
  "grants": [ /* RewardGrant[] */ ]
}
```

### RewardGrant (типизированный `payload`, дискриминатор — `grantType`)

```jsonc
{
  "id": "uuid-grant",
  "grantType": "FEATURE|SUBCLASS|FEAT|SPELL|SKILL_PROFICIENCY|ABILITY_SCORE|NUMERIC_MODIFIER|CUSTOM_TEXT",
  "label": "...", "labelRu": "...", "labelEn": "...",
  "description": "...", "sortOrder": 0,
  "payload": { /* форма зависит от grantType, см. ниже */ }
}
```

Неизвестные `grantType` рендерить как `CUSTOM_TEXT` (просто текст label/description).

#### payload по типам, важным для этой задачи

**ABILITY_SCORE** (увеличение характеристик):
```jsonc
{ "chooseCount": 2, "bonusPerChoice": 1, "totalBonus": 2, "maxPerAbility": null, "maxScore": 20, "abilityOptionIds": ["uuid-str","uuid-dex", ...] }
```
- Игрок выбирает ровно `chooseCount` (=2) **разных** характеристик, каждой +`bonusPerChoice` (=1).
- В коммит уходит `childSelections.abilityScores` — ровно `chooseCount` записей `{ abilityScoreId, amount: 1 }`.
- Сервер требует именно `chooseCount` записей (иначе 422 «ASI: нужно выбрать ровно 2 характеристик»). Вариант «+2 в одну характеристику» здесь не поддерживается — всегда две разные по +1.

**SKILL_PROFICIENCY** (в т.ч. Экспертность):
```jsonc
{ "mode": "FIXED|CHOICE|ANY", "skillIds": [...], "skillOptionIds": [...], "chooseCount": 1, "grantsExpertise": true|false }
```
- `mode=ANY` — выбрать любой навык; `mode=CHOICE` — только из `skillOptionIds`; `mode=FIXED` — навыки в `skillIds` выдаются без выбора.
- Выбрать ровно `chooseCount` навыков → `childSelections.skillIds`.
- **`grantsExpertise=true`**: выбранный навык должен быть тем, которым персонаж **уже владеет**. В пикере показывать только освоенные навыки. Сервер отклонит:
  - 422 «Нельзя получить Экспертность в навыке, которым вы не владеете»,
  - 422 «Нельзя получить Экспертность в одном навыке дважды».
  - Экспертность = удвоение бонуса владения для этого навыка.

**SPELL** (новые заклинания/заговоры):
```jsonc
{ "mode": "CHOICE|FIXED", "fixedSpellIds": [...], "spellLevel": null|0|N, "minLevel": null, "maxLevel": null, "schoolIds": null, "chooseCount": 1, "allowReplaceOnLevelUp": null }
```
- `mode=FIXED` — заклинания в `fixedSpellIds` выдаются без выбора.
- `mode=CHOICE` — выбрать ровно `chooseCount`:
  - `spellLevel = 0` → заговоры (cantrips),
  - `spellLevel = N` (1..9) → заклинания этого круга,
  - `spellLevel = null` → подготовленные заклинания доступного персонажу круга (фильтровать по списку заклинаний класса).
- В коммит уходит `childSelections.spellIds` — ровно `chooseCount` id.
- Сервер требует именно `chooseCount` (иначе 422 «Заклинания: нужно выбрать ровно N»), дубликаты запрещены.

Источник списка заклинаний/навыков класса — каталог контента (та же база, что при создании персонажа). Используйте существующие справочные эндпоинты контента для наполнения пикеров.

## POST /api/characters/{id}/level-up

`?lang=ru|en`. Тело:

```jsonc
{
  "classId": "uuid-wizard",
  "selections": [
    {
      "rewardGroupId": "uuid-group-asi",
      "optionIds": ["uuid-option"],          // ровно chooseMin..chooseMax (для этих групп = 1)
      "childSelections": {
        "abilityScores": [
          { "abilityScoreId": "uuid-str", "amount": 1 },
          { "abilityScoreId": "uuid-con", "amount": 1 }
        ],
        "skillIds": ["uuid-skill"],          // для SKILL_PROFICIENCY-группы
        "spellIds": ["uuid-spell"],          // для SPELL-группы
        "featId": null                        // для FEAT-группы
      }
    }
    // ...по одной записи на каждую CHOICE-группу; AUTO-группы НЕ включать
  ]
}
```

Правила:
- Для **AUTO**-групп ничего слать не нужно — применяются сами.
- Для **CHOICE**-группы: `optionIds` = выбранные опции (тут всегда одна), плюс `childSelections`, релевантные типу гранта выбранной опции.
- `childSelections` — один объект на группу; заполняются только нужные поля (abilityScores / skillIds / spellIds / featId).

Ответ: `ApiResponse<LevelUpResult>` (`newTotalLevel`, `classLeveled`, `newClassLevel`, `hpIncrease`, `newMaxHp`, `rewardsAcquired[]`). После успеха — перезагрузить данные персонажа (HP, уровень, ячейки, навыки, заклинания).

### Ошибки коммита (показывать message как есть)

| Код | Когда |
|-----|-------|
| 422 | «ASI: нужно выбрать ровно N характеристик» |
| 422 | «Навыки: нужно выбрать ровно N» / «Навыки: дублирующиеся значения» / «Навык недоступен для этого гранта» |
| 422 | «Нельзя получить Экспертность в навыке, которым вы не владеете» / «...в одном навыке дважды» |
| 422 | «Заклинания: нужно выбрать ровно N» / «Заклинания: дублирующиеся значения» |
| 403 | нет прав на персонажа |

## Трекер ячеек заклинаний (новое)

Максимум ячеек выводится из класса+уровня (его НЕ хранят и НЕ редактируют). Хранится только расход. `available = max − expended`.

### Эндпоинты

- `GET  /api/characters/{id}/spell-slots` — текущее состояние.
- `POST /api/characters/{id}/spell-slots/{spellLevel}/expend` — потратить одну ячейку круга `spellLevel` (1..9).
- `POST /api/characters/{id}/spell-slots/restore-all` — восстановить все потраченные (долгий отдых).
- `POST /api/characters/{id}/spell-slots/restore-half` — восстановить половину (округление вниз) от потраченных.

Все четыре возвращают `ApiResponse<SpellSlots>`:

```jsonc
{
  "success": true,
  "data": {
    "levels": [
      { "spellLevel": 1, "max": 3, "expended": 1, "available": 2 },
      { "spellLevel": 2, "max": 2, "expended": 0, "available": 2 }
    ]
  }
}
```

- Возвращаются только круги, где `max>0` или есть расход. Показывать как ряд «pip»-ячеек: закрашенные = available, пустые = expended.
- Колдун (pact magic): единственный пул ячеек выводится на соответствующий круг по уровню колдуна — отдельной логики на фронте не требуется, просто отрисуйте то, что пришло в `levels`.

### Ошибки ячеек

| Код | Когда | message |
|-----|-------|---------|
| 400 | круг вне 1..9 | «Уровень ячейки должен быть от 1 до 9» |
| 422 | у персонажа нет ячеек этого круга | «У персонажа нет ячеек заклинаний N-го уровня» |
| 422 | все ячейки круга уже потрачены | «Все ячейки N-го уровня уже потрачены» |
| 403 | нет прав | «Нет прав на изменение этого персонажа» / «Нет доступа к этому персонажу» |

### UI

- На листе персонажа-заклинателя — секция «Ячейки заклинаний»: по кругам ряды pip’ов с кнопкой «потратить» (expend) и кнопками «Долгий отдых» (restore-all) / «Короткое восстановление» (restore-half).
- После любого действия перерисовать из ответа (он всегда содержит полное актуальное состояние).

## Доступ

Владелец персонажа, GM кампании или админ — для всех операций (level-up и ячеек). Иначе 403; на фронте прятать соответствующие контролы для чужих персонажей (кроме GM в своей кампании).

## UI-шаги визарда (обновление к существующему)

1. Выбор класса (как раньше; `availableClasses`).
2. Награды нового уровня — пройтись по `rewardGroups`:
   - AUTO → «получено автоматически» (read-only список грантов).
   - CHOICE ASI → выбор двух разных характеристик (+1/+1).
   - CHOICE SKILL_PROFICIENCY с `grantsExpertise` → пикер **только из освоенных** навыков.
   - CHOICE SPELL → пикер заклинаний/заговоров из списка класса по фильтру круга, ровно `chooseCount`.
3. Подтверждение → `POST .../level-up` с `selections`.
4. Результат + перезагрузка персонажа (вкл. ячейки заклинаний).

Обязательность выбора (заклинания/навык/характеристики) форсится сервером — визард не должен давать «Подтвердить», пока все CHOICE-группы не заполнены ровно по `chooseCount`.
