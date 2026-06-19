# BE-чеклист по фидбеку тестировщика + wizard target-level

Документ для BE-разработчика. Сводный список того, **что нужно проверить/доделать на бэкенде**.
Фронт по своим частям закрыт (см. отметки «FE: готово»). Каждый пункт — с критерием готовности.

Контекст репозиториев:
- FE: `C:\SuperHerecy\Super-Manager-of-Heresy-for-Idiots-frontend`
- BE: `C:\SuperHerecy\SuperManagerofHeresyforIdiots\SuperManagerofHeresyforIdiots`

---

## 1. P2 — нарратив и удары пропадают после создания персонажа

**Симптом:** в визарде задаются описание/предыстория/черты/идеалы/минусы и доступные удары,
но после создания этой информации в листе нет.

**Диагноз (FE-проводка корректна, упирается в BE):**
- Сущность `PlayerCharacter` уже имеет колонки: `biography_json` (поле `biographyJson`),
  `features`, `attacks_json` (поле `attacksJson`), `proficiencies`, `equipment`. Миграция не нужна.
- `CharacterResponse` уже **возвращает** biography / features / attacks / proficiencies / equipment.
- Визард уже собирает эти поля; FE кладёт `biography`, `features`, `proficiencies` в
  `CreateFullCharacterRequest`. **Но** `toContentCharacterRequest` (FE) их отбрасывает,
  потому что серверный `CreateContentCharacterRequest` их не принимает.

**Что сделать на BE (любой из путей, лучше оба):**
- [ ] Расширить `CreateContentCharacterRequest` полями `biography` / `features` / `attacks`
      (+ при желании `proficiencies` / `equipment`) и сохранять их в
      `ContentCharacterCreationService` при создании.
- [ ] И/или расширить `UpdateCharacterRequest` (сейчас принимает только
      `name`, `playerName`, `proficiencies`, `equipment`, `raceId`, `selectedLineageId`) полями
      `biography` / `features` / `attacks`, и дописать их запись в `CharacterService.updateCharacter`.

**Критерий готовности:** создать персонажа с заполненными нарративом и ударами →
`GET` персонажа возвращает эти поля непустыми; в листе (FolioPage) они отображаются.

**После готовности BE — мелкая доработка FE:** снять отбрасывание полей в
`toContentCharacterRequest` (`src/api/characters-full.api.ts`) и довести проброс `attacks`/`equipment`.

---

## 2. P3 — арсенал/реликварий пусты: нет типов и списка предметов

**Симптом:** функционал «Арсенал и Реликварий» не работает, нет типов предметов и списка
предметов; при создании за мастера доступных предметов тоже нет.

**Диагноз (FE-проводка корректна):**
- Список шаблонов: `GET /item-templates/campaign/{id}` (FE-хук `useCampaignItemTemplates`).
- Типы/предметы в визарде: `getAvailableContent` → поле `itemTypes`.
- `CampaignContentService.getAvailableContent` берёт `itemTypes` из
  `itemTypeRepository.findAllByHomebrewIsNull()` — это **легаси-таблица** `item_type`.
  Для сравнения: расы/виды (`species`) уже читаются из новой модели (`speciesRepository`).

**Что проверить/сделать на BE:**
- [ ] Применена ли миграция `038-seed-phb-full.xml` на живой БД (есть ли строки в `item_type`
      и `item_template` с `homebrew_id IS NULL`). Если changelog не прогнан — прогнать Liquibase.
- [ ] Решить: предметы остаются на легаси-таблицах или их нужно перевести на новую content-модель
      (как сделали с расами в S5). Если читатели разъехались по старой/новой модели — будет та же
      тихая рассинхронизация, что ломала reference-чтения.
- [ ] Убедиться, что `ItemTemplateService.listTemplates` отдаёт vanilla (`findByHomebrewIsNull()`)
      + homebrew кампании, и что для свежей кампании список ненулевой.

**Критерий готовности:** на чистой кампании «Арсенал/Реликварий» показывает типы и список
предметов; визард создания за мастера показывает доступные типы предметов.

---

## 3. Wizard target-level — верификация (BE заявлен готовым)

Источник: `15-28-wizard-target-level-creates-level1.md`. BE должен создавать персонажа **всегда
1 уровня**, а целевой `level` превращать в порог XP. **FE: готово** — лимиты заклинаний и max-уровень
заклинания считаются по 1 уровню, поле подписано как «целевой уровень», добавлено пояснение про
«создан 1 ур. + XP», на `totalLevel` из ответа создания FE не опирается.

**Что проверить на BE:**
- [ ] `POST /campaigns/{id}/characters/full` и `POST /characters/full` с `level = N (1..20)`:
      результат `totalLevel = 1`, `classLevel = 1`, `experience = порог XP для N`,
      `hitDiceTotal = 1dX`, HP как у 1 уровня.
- [ ] Валидация заклинаний: `spellIds`, содержащий заклинание уровня > 1 → `400`
      («Spell (level X) exceeds max spell level …»). Заговоры (level 0) принимаются.
- [ ] `initialRewardSelections` принимаются только для reward-групп 1 уровня;
      выборы для уровней 2+ при создании не передаются (FE так и делает).
- [ ] С `experience` для N персонаж поднимается 1→N через level-up и **не выше**
      (порог N+1 не достигнут).
- [ ] `ContentCharacterCreationResponse.totalLevel` всегда `1` (FE на него не полагается — ок).

**Критерий готовности:** создание с `level = 5` (Воин) даёт `totalLevel = 1`, `experience = 6500`;
персонаж готов к подъёму до 5 ур. через level-up и не выше.
