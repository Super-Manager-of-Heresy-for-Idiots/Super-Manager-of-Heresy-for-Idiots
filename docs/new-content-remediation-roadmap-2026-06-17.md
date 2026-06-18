# Remediation Roadmap: From ~6.5/10 To 10/10

Date: 2026-06-17.

Контекст: контрактные блокеры закрыты (см. `new-content-3-to-10-roadmap-fix-log-2026-06-17.md`,
Steps 1-3 и Resolution Addendum в audit-доке). Маршруты FE/BE согласованы, class-builder и
admin-инструменты работают по новой модели. Осталась глубокая часть миграции — переключение
runtime, перенос данных и удаление legacy.

## Current Readiness

- Backend: ~7/10.
- Frontend: ~7/10.
- Integrated product: ~6.5/10.

Почему не 10/10: несмотря на согласованные контракты, нормальные runtime-пути всё ещё
гибридные. Level-up и создание персонажа пишут в legacy-модель/смешанные ID, награды
уровня 1 не персистятся, нет полноценной миграции существующих данных, нет финальной
cleanup-миграции и нет интеграционных тестов на реальной БД.

## Principles (без изменений)

1. Бестиарий не трогаем.
2. JSONB не вводим — только реляционная модель (JSONB только с явного разрешения).
3. Новые API заменяют старые in-place; временная поломка экрана/endpoint допустима.
4. Старые данные не удаляем, пока runtime и FE реально их не перестали использовать.
5. Каждый этап оставляет приложение запускаемым.
6. Миграция пользовательских ID — отдельный контролируемый шаг с dry-run и бэкапом.

---

# R1. Unify Skill Identity (foundational)

Status target: разблокирует R2/R3, убирает самый хрупкий мост.

Проблема (audit BE gap #5): `CharacterSkillProficiency.skill` типизирован как
`ProficiencySkill`, но создание/level-up пишут туда ID из `ContentSkill` через
`entityManager.getReference(...)`. Навигация может указывать на несуществующие строки.

- [x] Решить целевую модель: `CharacterSkillProficiency.skill` ссылается на `skill`
      (content), не на `proficiency_skills`.
- [x] Перевести запись (creation + level-up) на `ContentSkill` без `getReference` обмана.
      (также переведён legacy `CharacterWizardService` на `ContentSkillRepository`.)
- [ ] Liquibase: при необходимости отдельный changeset для FK на `skill` (после R6) → R7.
- [~] Тесты: чтение листа персонажа с content-skill ID не падает на навигации →
      unit/compile покрыто; read-through тест на реальной БД вынесен в R9.

# R2. Final Level-Up Runtime Switch-Over

Проблема (audit BE gap #2, FE gap #2): legacy `LevelUpController`/`LevelUpService` всё ещё
активный write-путь (`character_acquired_rewards`, `rewardType`, `RewardResolverRegistry`),
а FE-запрос новой формы уходит на старый контроллер.

- [x] FE `levelup.api.ts` → стабильные content-маршруты (`/characters/{id}/level-up[-options]`),
      которые уже обслуживает `ContentLevelUpController` (сделано в Step 1, проверено).
- [x] Убедиться, что commit идёт в `LevelUpCommandService` и пишет только
      `character_reward_selection*` таблицы.
- [~] Удалить legacy write-путь из активного flow: legacy перенесён на `/legacy/*` и
      `@Deprecated(forRemoval=true)`; физическое удаление → R8.
- [x] `GET /characters/{id}/rewards`: переведён на новую reward-selection модель
      (`CharacterRewardQueryService` поверх `CharacterRewardSelectionRepository`).
- [~] Тесты: level-up unit-тесты зелёные; полный набор (subclass/ASI/skill/over-select) на
      реальной БД → R9.

# R3. Character Creation Persistence

Проблема (audit BE gap #4, FE gap #4): уровень-1 reward selections собираются в UI, но не
доходят до backend; `ContentCharacterCreationService` не пишет `character_reward_selection`.

- [x] Расширить `CreateContentCharacterRequest` полем reward-selections (`initialRewardSelections`).
- [x] `ContentCharacterCreationService`: валидировать и персистить level-1 выбор
      (`LevelUpCommandService.applyInitialRewardSelections`).
- [x] FE wizard: включить reward-selections в submit (`initialRewardSelections`, уже строится).
- [x] Маппинг локальных spell-имён → content spell ID: `nameEn` добавлен в spell-reference,
      wizard резолвит выбранные английские имена через карту nameEn→id и шлёт `cantripIds/spellIds`;
      нерезолвленные имена блокируют submit с тостом. (У `Spell` нет связи с классами,
      поэтому отображение остаётся на локальном каталоге; content нужен только для id.)
- [~] Тесты: BE-unit зелёные; e2e создание с skill/subclass/ASI/spell на реальной БД → R9.

# R4. Homebrew Authoring On New Model Only

Проблема (audit BE gap #7): `HomebrewAuthoringService` всё ещё создаёт `CharacterClass` +
`ClassLevelReward`; старый rich/import путь пишет старую модель.

- [x] Перевести все homebrew class create/update на `ClassAuthoringService` (новый граф):
      аггрегатный `ClassAuthoringController` обслуживает homebrew-пакеты; FE class-builder
      уже ходит туда.
- [x] Удалить из активных маршрутов старые rich/import пути, пишущие legacy:
      `AdminController` (`/character-classes/rich`, `/import-json`, `/{id}/rich`) и
      `HomebrewController` (`/my/{packageId}/content/classes`) удалены; снят неиспользуемый
      `HomebrewAuthoringService`-field из `AdminController` и dead FE `homebrewApi.createPackageCharacterClass`.
- [~] Мёртвые service-методы `HomebrewAuthoringService` (createPackage*Rich/updateStandard*Rich
      и class-only helpers) и legacy-entities → удаление вынесено в R8.
- [~] Тесты: created homebrew-класс виден в content-reference/content-level-up → e2e в R9.

# R5. Idempotency-Key For Class Create

Проблема: заголовок принимается, но dedup нет.

- [x] Реляционная таблица idempotency-ключей (`class_authoring_idempotency`: scope + idem_key
      UNIQUE + request_hash + result_class_id + created_at), opportunistic TTL-очистка (24h).
- [x] `ClassAuthoringService.create`: при повторе (scope,key) возвращает прежний результат;
      тот же ключ с другим телом → 412. Контроллер читает заголовок `Idempotency-Key`
      на обоих create-маршрутах (core + package).
- [x] Тест: двойной POST с тем же ключом создаёт одну сущность
      (`ClassAuthoringServiceTest.idempotentCreate_dedup`, зелёный).

# R6. Runtime Data Migration (roadmap Phase 10, full)

Проблема (audit BE gap #8): `RuntimeDataMigrationService` мигрирует только class-levels и
skill-proficiencies. Нужны background/stat/currency/spell/rewards.

- [x] Инвентаризация всех runtime FK-колонок: class_levels.class_id, skill_proficiencies.skill_id,
      character_stats.stat_type_id, character_wallets/wallet_transactions.currency_type_id,
      character_known_spells.spell_id, characters.background_id, acquired_rewards (report-only).
- [x] Стратегия маппинга: legacy без slug → матч по name (+ для class/skill локализованные
      варианты) против nameEn/nameRu; уникальный матч применяется; >1 кандидат = ambiguous и
      никогда не применяется; 0 = unmapped; молча не угадываем.
- [x] Отчёты: per-column EntityMigration (alreadyNew/mapped/ambiguous/unmapped/rowsUpdated)
      в `RuntimeMigrationReport.entities`.
- [x] Dry-run режим (по умолчанию) + требование `confirmBackup=true` перед записью.
- [x] Пост-валидация: per-column danglingNote — runtime-строки, указывающие на отсутствующий
      content (должно быть 0 после полной миграции).
- [x] legacy rewards → report-only (подсчёт distinct id для ручного разбора; авто-конвертация
      не выполняется — несовместимые модели group/option, риск порчи пользовательских данных).
- [x] Liquibase 062: relax физических FK на stat_type_id/currency_type_id/spell_id/background_id
      (чтобы name-based UPDATE на content id не нарушал FK на legacy-таблицы).
- [x] Тест: `RuntimeDataMigrationServiceTest` (dry-run классификация, отказ без бэкапа,
      применение только однозначных) зелёный на новом entities-API.

# R7. Final Cleanup Liquibase + Real FKs

Проблема (audit BE gap #9): `master.xml` заканчивался на `060`; нет финальной очистки.

- [x] Убрать transitional `NO_CONSTRAINT` и добавить реальные FK на новую модель: changeset
      **063** добавляет FK от runtime-колонок к content-таблицам
      (character_class_levels.class_id→character_class, character_skill_proficiencies.skill_id→skill,
      character_stats.stat_type_id→ability_score, character_wallets/wallet_transactions.currency_type_id→currency,
      character_known_spells.spell_id→spell, characters.background_id→background). Безопасно: на
      fresh DB user-таблицы пусты (сиды в них не пишут — проверено), на существующей — fail-fast,
      если R6 не отработал (dangling > 0).
- [~] Drop/archive старых non-bestiary PHB таблиц **переехал в R8**. Причина: `ddl-auto: validate`
      всё ещё мапит legacy @Entity (`CharacterClass`/`ProficiencySkill`/`StatType`/`CurrencyType`/
      `Spell`/`Background`/`Subclass`/`ClassLevelReward`/`CharacterAcquiredReward`), поэтому дроп
      таблицы до удаления entity уронит boot-валидацию. Удаление entity (R8) и drop таблиц должны
      идти одним шагом. **Бестиарий не трогать.**
- [~] Проверка fresh DB boot/seed — выполняется в R9 (Testcontainers); интеграционных тестов сейчас нет.

# R8. Legacy Removal (roadmap Phase 11/12)

После R2-R7 и подтверждения нулевого использования legacy-маршрутов.

Разбито на два под-этапа: **R8a** — самодостаточный legacy level-up/reward кластер
(удаляется чисто, ни на что живое не завязан); **R8b** — миграция живых фич
(`NpcService`, `CampaignContentService`, `CampaignBlueprintService`, `CharacterService`,
`ContentReferenceService`, `AdminService`, `CharacterWizardService` и др.) с legacy
`CharacterClass`/`Subclass` на `Content*`-граф, затем удаление этих entity и drop таблиц.

R8a (выполнено):
- [x] Удалить legacy level-up/reward кластер: `LevelUpService`, `service/reward/*`
      (`RewardResolverRegistry` + резолверы), `ClassLevelRewardRepository`,
      `ClassLevelReward`, `CharacterAcquiredReward` + `CharacterAcquiredRewardRepository`.
- [x] `LevelUpController`: оставлен только `GET /{id}/rewards` (новая reward-selection
      модель); `/legacy/level-up[-options]` и поле `LevelUpService` удалены.
- [x] `HomebrewAuthoringService`: вырезаны все class/reward-authoring методы и поля
      (`classRepository`, `subclassRepository`, `skillEffectRepository`,
      `classLevelRewardRepository`, `rewardResolverRegistry`); оставлены живые
      package/item/skill/feat/buff методы.
- [x] Удалены мёртвые DTO/enum: `ClassLevelRewardResponse`, `CreateClassLevelRewardRequest`,
      `HomebrewClassCreationResponse`, `CreateHomebrewClassRequest`, `RewardDetailDto`,
      `enums.RewardType`. FE-аналогов уже нет.
- [x] Changeset **064**: drop `character_acquired_rewards` + `class_level_rewards`
      (CASCADE, idempotent). Безопасно: ни один @Entity больше не мапит эти таблицы.
- [x] Удалены мёртвые тесты `LevelUpServiceTest`, `RewardResolverRegistryTest`; из
      `ItemTypeServiceTest`/`SkillServiceTest` убраны @Mock на удалённые типы. BUILD SUCCESSFUL.

R8b (выполнено):
- [x] Живые фичи переведены с legacy `CharacterClass`/`Subclass` на `ContentCharacterClass`/
      `ContentSubclass`: ассоциации `CampaignNpc`/`BlueprintNpc`/`CharacterClassLevel`/
      `CustomResourceType`; сервисы `NpcService`, `CampaignContentService`,
      `CampaignBlueprintService`, `CharacterService`; валидаторы
      `CharacterClassContentValidator`/`SubclassContentValidator`; мапперы
      `CharacterMapper`/`ReferenceDataMapper` (accessors `getName`→`getNameRu`/`getNameEn`).
- [x] Удалён `CharacterWizardService` + его `/legacy/full` маршруты в
      `CampaignCharacterController`/`CharacterTemplateController`; создание персонажей идёт
      только через `ContentCharacterCreationService`.
- [x] Из `AdminService`/`AdminController` вырезан legacy class/subclass CRUD; class/subclass
      authoring живёт только в `ClassAuthoringController` на content-модели.
- [x] `RuntimeDataMigrationService.migrateClasses` переведён на чтение legacy-имён
      (`name`/`name_engloc`/`name_rusloc`) из `character_classes` через JDBC (helper
      `legacyThreeNamesFrom`, толерантен к отсутствию таблицы); `ProficiencySkill` оставлен.
- [x] Удалены legacy entities `CharacterClass`, `Subclass` + репозитории, мёртвые DTO
      (`CharacterClassResponse`, `CreateCharacterClassRequest`, `SubclassResponse`,
      `CreateSubclassRequest`, `CreateFullCharacterRequest`). `ProficiencySkill` сохранён.
- [x] Changeset **065**: drop `subclasses` + `character_classes` (CASCADE, idempotent).
      Ни один @Entity больше не мапит эти таблицы. **Бестиарий не тронут.**
- [x] Тесты приведены к content-модели; `compileJava`+`compileTestJava` зелёные,
      затронутые unit-тесты проходят. BUILD SUCCESSFUL.

R8c — закрытие миграционного окна (выполнено):
- [x] Из `RuntimeDataMigrationService` удалены JDBC-чтения legacy plural-таблиц
      (`migrateStats`/`migrateCurrency`/`migrateSpells`/`migrateBackgrounds` + helper
      `legacyNamesFrom` + неиспользуемые поля/импорты). Остались class+skill remap.
- [x] Changeset **066**: drop `stat_types`/`currency_types`/`spells`/`backgrounds` (CASCADE).
      Эти таблицы — не чистые orphan'ы: на них висели устаревшие FK от живых фич
      (`buffs_debuffs`/`combat_modifiers`/`campaign_npc_spells`/`blueprint_npc_spells`/
      blueprint-награды), не входивших в миграцию `character_*`. CASCADE снимает эти FK;
      колонки фич остаются, их сущности уже смотрят на content-таблицы (новые строки валидны),
      старые строки с legacy id становятся висячими и считаются одноразовыми (решение:
      новый контент работает, старый — выбрасывается). **Бестиарий не тронут.**
- [x] `ddl-auto=validate` зелёный: ни один @Entity не маплен на дропаемые plural-таблицы
      (StatType/CurrencyType/Spell/Background → ability_score/currency/spell/background).
      После этого старой PHB-схемы в БД не остаётся.

# R9. Integration / E2E Tests (roadmap Phase 12)

Проблема (audit BE gap #10): тесты в основном Mockito; нет Testcontainers/E2E.

- [ ] Testcontainers Postgres: fresh DB boot + seed.
- [ ] Migrated DB: прогон R6-миграции на снимке legacy-данных.
- [ ] E2E: создание персонажа, level-up, homebrew-класс, admin-edit.
- [ ] Регрессия: бестиарий без изменений.

---

# Dependency Order

```
R1 (skill identity)
  └─> R2 (level-up switch) ─┐
  └─> R3 (creation persist)─┤
R4 (homebrew new model) ────┤
R5 (idempotency) ───────────┤
                            ├─> R6 (data migration)
                            │      └─> R7 (cleanup + real FKs)
                            │             └─> R8 (legacy removal)
R9 (integration tests) ── параллельно, обязателен как gate для R7/R8
```

# 10/10 Definition (из исходного roadmap, ещё не выполнено)

- [ ] Fresh DB поднимается и сидится без старых PHB таблиц.
- [ ] Существующая БД мигрирует без потери данных.
- [ ] Создание персонажа использует content ID и персистит level-1 выборы.
- [ ] Level-up пишет только reward groups/options/grants.
- [ ] Homebrew authoring пишет только новые таблицы.
- [ ] FE runtime не вызывает старые non-bestiary PHB endpoints.
- [ ] Старые entities/repos/services удалены или archive-only.
- [ ] Нет `raw_json`/JSONB для структурных механик.
- [ ] Нет startup Hibernate DDL warnings от transitional FK.
- [ ] Бестиарий не изменён.
