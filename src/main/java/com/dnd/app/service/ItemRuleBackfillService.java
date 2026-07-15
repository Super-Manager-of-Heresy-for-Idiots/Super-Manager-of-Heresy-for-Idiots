package com.dnd.app.service;

import com.dnd.app.domain.Spell;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.domain.content.MagicItemType;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureItemBinding;
import com.dnd.app.domain.featurerule.FeatureIssueSeverity;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureResourceScope;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleIssue;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
import com.dnd.app.domain.featurerule.FeatureRuleSource;
import com.dnd.app.domain.featurerule.FeatureSpellGrant;
import com.dnd.app.domain.featurerule.RestType;
import com.dnd.app.dto.featurerule.ItemRuleBackfillResult;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureItemBindingRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.FeatureRuleIssueRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.FeatureSpellGrantRepository;
import com.dnd.app.repository.MagicItemRepository;
import com.dnd.app.repository.RestTypeRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.service.ItemRuleTextExtractor.Charges;
import com.dnd.app.service.ItemRuleTextExtractor.ItemRuleExtraction;
import com.dnd.app.service.ItemRuleTextExtractor.StaticBonus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Класс ItemRuleBackfillService — сервис бэкфилла исполняемых правил для ванильных магических
 * предметов (Фаза 6 плана ITEM_ABIL_PLAN §7.1). Построен по образцу {@code SpellRuleBackfillService}:
 * <ul>
 *   <li>итерирует корпус ({@code magicItemRepository.findAllByHomebrewIsNull()});</li>
 *   <li>создаёт {@link FeatureRule} со статусом {@code needs_review} и источником {@code MIGRATION};</li>
 *   <li>идемпотентен — пропускает предмет+тип-правила, если MIGRATION-правило уже есть;</li>
 *   <li>поддерживает dry-run через флаг {@code apply=false} (счётчики растут, БД не пишется);</li>
 *   <li>заводит {@link FeatureRuleIssue} для неоднозначного/непарсибельного разбора;</li>
 *   <li>возвращает DTO со счётчиками.</li>
 * </ul>
 * Owner-тип — {@link FeatureRuleOwnerType#ITEM_MAGIC}, ownerId — id магического предмета.
 * <p>
 * Текстовый разбор делегирован чистому {@link ItemRuleTextExtractor}; сопоставление имён
 * заклинаний со справочником {@link SpellRepository} делается здесь.
 * <p>
 * Сервис требует БД (Docker/Postgres), поэтому предназначен только для компиляции и запуска на
 * стенде, не в этой среде.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemRuleBackfillService {

    private static final String OWNER = FeatureRuleOwnerType.ITEM_MAGIC.getCode();
    private static final String ACTOR = "item-backfill";

    /** Код issue для пересечения статического бонуса с легаси-баффами (§7.1 п.3, D7). */
    private static final String ISSUE_LEGACY_BUFF_OVERLAP = "LEGACY_BUFF_OVERLAP";
    /** Код issue неоднозначного/непарсибельного разбора. */
    private static final String ISSUE_AMBIGUOUS = "ambiguous_parse";
    /** Код issue ручной адъюдикации нераспознанного фрагмента. */
    private static final String ISSUE_MANUAL = "manual_adjudication";

    /** Слаги потребляемых типов предметов → consume_on_use. */
    private static final Set<String> CONSUMABLE_SLUGS = Set.of("potion", "scroll");

    private final MagicItemRepository magicItemRepository;
    private final SpellRepository spellRepository;
    private final FeatureRuleRepository ruleRepository;
    private final FeatureRuleIssueRepository issueRepository;
    private final FeatureRuleRevisionService revisionService;
    private final FeatureFormulaService formulaService;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureItemBindingRepository itemBindingRepository;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;
    private final FeatureSpellGrantRepository spellGrantRepository;
    private final RestTypeRepository restTypeRepository;
    private final ItemRuleTextExtractor extractor;

    /** Изменяемые счётчики одного прогона; DTO собирается в конце. */
    private static final class Run {
        final boolean apply;
        int created;
        int skippedExisting;
        int manual;
        int overlaps;
        int resourcesCreated;
        int spellGrantsCreated;
        int issuesCreated;
        final List<String> notes = new ArrayList<>();

        Run(boolean apply) {
            this.apply = apply;
        }
    }

    /**
     * Выполняет бэкфилл правил магических предметов.
     * @param apply признак применения изменений (true — писать в БД; false — сухой прогон)
     * @return результат прогона со счётчиками и построчными заметками
     */
    @Transactional
    public ItemRuleBackfillResult backfill(boolean apply) {
        Run run = new Run(apply);

        // Кэш имён заклинаний (ru/en, lower-case) → id для сопоставления грантов.
        Map<String, UUID> spellIdByName = indexSpellNames();

        List<MagicItem> items = magicItemRepository.findAllByHomebrewIsNull();
        for (MagicItem item : items) {
            processItem(run, item, spellIdByName);
        }

        if (apply) {
            log.info("Item rule backfill: {} magic items; created={}, skippedExisting={}, "
                            + "manual={}, overlaps={}, resources={}, spellGrants={}, issues={}",
                    items.size(), run.created, run.skippedExisting, run.manual, run.overlaps,
                    run.resourcesCreated, run.spellGrantsCreated, run.issuesCreated);
        }
        return ItemRuleBackfillResult.builder()
                .applied(apply)
                .itemsTotal(items.size())
                .created(run.created)
                .skippedExisting(run.skippedExisting)
                .manual(run.manual)
                .overlaps(run.overlaps)
                .resourcesCreated(run.resourcesCreated)
                .spellGrantsCreated(run.spellGrantsCreated)
                .issuesCreated(run.issuesCreated)
                .notes(run.notes)
                .build();
    }

    /**
     * Обрабатывает один магический предмет: парсит описание и создаёт правила по §7.1.
     * @param run счётчики прогона
     * @param item магический предмет
     * @param spellIdByName индекс имён заклинаний → id
     */
    private void processItem(Run run, MagicItem item, Map<String, UUID> spellIdByName) {
        // Отпечаток идемпотентности: какие типы MIGRATION-правил у предмета уже есть.
        Map<String, FeatureRule> existingByType = existingMigrationRules(item.getId());

        ItemRuleExtraction extraction = extractor.extract(item.getDescription());
        boolean attunement = Boolean.TRUE.equals(item.getAttunementRequired());
        boolean consumable = isConsumable(item);

        // 1) Заряды → ресурсное определение (scope=ITEM_INSTANCE).
        UUID chargesResourceId = backfillCharges(run, item, extraction.charges(), existingByType, attunement);

        // 2) Статические бонусы → static_grant (requires_equipped=true).
        backfillStaticBonuses(run, item, extraction.staticBonuses(), existingByType, attunement);

        // 3) Заклинания → spell_grant (cast_without_slot, uses_resource_definition_id → заряды).
        backfillSpellGrants(run, item, extraction.spellNames(), spellIdByName, existingByType,
                chargesResourceId, attunement, consumable);

        // 4) Нераспознанное → manual_adjudication + issue с фрагментом.
        backfillManual(run, item, extraction.manualFragments(), existingByType, attunement);
    }

    // ── 1. Заряды ─────────────────────────────────────────────────────────────

    /**
     * Создаёт ресурсное определение зарядов (scope=ITEM_INSTANCE) из экстракции.
     * Формула max сохраняется через {@link FeatureFormulaService}. Если распознана формула
     * восстановления и тип отдыха «длинный отдых» — проставляет reset_rest_type_id и
     * reset_amount_formula_id. Если reset-тип отдыха отсутствует в справочнике — создаёт
     * ресурс без сброса и заводит issue для ручного завершения (не выдумывая схему).
     * @param run счётчики прогона
     * @param item предмет-владелец
     * @param charges распознанные заряды (или null)
     * @param existingByType уже существующие правила по типу (идемпотентность)
     * @param attunement требуется ли настройка (для binding)
     * @return id созданного ресурсного определения или null, если зарядов нет / пропущено
     */
    private UUID backfillCharges(Run run, MagicItem item, Charges charges,
                                 Map<String, FeatureRule> existingByType, boolean attunement) {
        if (charges == null) {
            return null;
        }
        String ruleType = FeatureRuleProfile.RESOURCE.getCode();
        if (existingByType.containsKey(ruleType)) {
            run.skippedExisting++;
            return null;
        }
        if (!run.apply) {
            run.created++;
            run.resourcesCreated++;
            note(run, item, "заряды: max=" + charges.max()
                    + (charges.resetFormula() != null ? ", reset=" + charges.resetFormula() : ""));
            return null;
        }

        FeatureRule rule = createRule(item, FeatureRuleProfile.RESOURCE, 0,
                "Заряды предмета из описания (backfill Фаза 6)");
        run.created++;
        ensureBinding(rule.getId(), attunement, false, false);

        FeatureFormula maxFormula = intFormula(String.valueOf(charges.max()));

        UUID resetRestTypeId = null;
        UUID resetFormulaId = null;
        boolean resetIncomplete = false;
        if (charges.resetFormula() != null) {
            Optional<RestType> restType = charges.resetRest() != null
                    ? restTypeRepository.findByCode(charges.resetRest())
                    : Optional.empty();
            if (restType.isPresent()) {
                resetRestTypeId = restType.get().getId();
                resetFormulaId = diceFormula(charges.resetFormula()).getId();
            } else {
                // Тип отдыха для сброса не найден в справочнике — не выдумываем схему,
                // создаём ресурс без сброса и помечаем issue для ручного завершения.
                resetIncomplete = true;
            }
        }

        FeatureResourceDefinition def = resourceDefinitionRepository.save(FeatureResourceDefinition.builder()
                .featureRuleId(rule.getId())
                .resourceKey(resourceKey(item))
                .displayName(truncate("Заряды: " + displayName(item), 120))
                .maxFormulaId(maxFormula.getId())
                .resetRestTypeId(resetRestTypeId)
                .resetAmountFormulaId(resetFormulaId)
                .scope(FeatureResourceScope.ITEM_INSTANCE)
                .build());
        run.resourcesCreated++;

        if (resetIncomplete) {
            createIssue(run, item, rule.getId(), ISSUE_AMBIGUOUS, FeatureIssueSeverity.WARN,
                    "Не удалось привязать сброс зарядов: формула «" + charges.resetFormula()
                            + "», тип отдыха «" + charges.resetRest() + "» — завершите вручную",
                    null);
        }
        approve(rule);
        return def.getId();
    }

    // ── 2. Статические бонусы ─────────────────────────────────────────────────

    /**
     * Создаёт правило {@code static_grant} для статических бонусов (атака+урон / КД / спасброски)
     * с binding {@code requires_equipped=true}.
     * <p>
     * ЗАМЕЧАНИЕ (§7.1 п.3, D7): проверка пересечения {@code LEGACY_BUFF_OVERLAP} применима только к
     * {@code item_templates} — легаси-сущность {@code ItemTemplateBuff} ссылается на
     * {@code ItemTemplate}, но НЕ на {@code magic_item}. Прямой связи magic_item↔buff нет, поэтому
     * для магических предметов пересечение невозможно и проверка пропускается (счётчик overlaps
     * остаётся при 0). Собственно числовые значения бонусов (+N) в проекте не имеют отдельной
     * detail-таблицы static-grant (есть только proficiency/language grants), поэтому величина и
     * цель бонуса фиксируются в notes правила для ручной доводки в Workbench.
     * @param run счётчики прогона
     * @param item предмет-владелец
     * @param bonuses распознанные статические бонусы
     * @param existingByType уже существующие правила по типу (идемпотентность)
     * @param attunement требуется ли настройка (для binding)
     */
    private void backfillStaticBonuses(Run run, MagicItem item, List<StaticBonus> bonuses,
                                       Map<String, FeatureRule> existingByType, boolean attunement) {
        if (bonuses.isEmpty()) {
            return;
        }
        String ruleType = FeatureRuleProfile.STATIC_GRANT.getCode();
        if (existingByType.containsKey(ruleType)) {
            run.skippedExisting++;
            return;
        }
        String bonusSummary = summarizeBonuses(bonuses);
        if (!run.apply) {
            run.created++;
            note(run, item, "static_grant: " + bonusSummary);
            return;
        }

        FeatureRule rule = createRule(item, FeatureRuleProfile.STATIC_GRANT, 1,
                "Статические бонусы из описания (backfill Фаза 6): " + bonusSummary
                        + ". Величины требуют ручной привязки — detail-таблицы static-grant нет.");
        run.created++;
        ensureBinding(rule.getId(), attunement, true, false);
        // Числовые значения фиксируем issue-ом, чтобы ревьюер довёл правило вручную.
        createIssue(run, item, rule.getId(), ISSUE_MANUAL, FeatureIssueSeverity.INFO,
                "Статический бонус требует ручной привязки величины/цели: " + bonusSummary, null);
        approve(rule);
    }

    // ── 3. Заклинания ─────────────────────────────────────────────────────────

    /**
     * Создаёт правила {@code spell_grant} для однозначно сопоставленных имён заклинаний.
     * Неоднозначный/ненайденный матч → issue, без гранта. При наличии ресурса зарядов
     * грант ссылается на него ({@code uses_resource_definition_id}).
     * @param run счётчики прогона
     * @param item предмет-владелец
     * @param spellNames имена-кандидаты из экстракции
     * @param spellIdByName индекс имён → id
     * @param existingByType уже существующие правила по типу (идемпотентность)
     * @param chargesResourceId id ресурса зарядов (или null)
     * @param attunement требуется ли настройка (для binding)
     * @param consumable потребляемый ли предмет (potion/scroll) — consume_on_use
     */
    private void backfillSpellGrants(Run run, MagicItem item, List<String> spellNames,
                                     Map<String, UUID> spellIdByName,
                                     Map<String, FeatureRule> existingByType, UUID chargesResourceId,
                                     boolean attunement, boolean consumable) {
        if (spellNames.isEmpty()) {
            return;
        }
        String ruleType = FeatureRuleProfile.SPELL_GRANT.getCode();
        if (existingByType.containsKey(ruleType)) {
            run.skippedExisting++;
            return;
        }

        // Разрешаем имена; неоднозначные/ненайденные пойдут в issue.
        List<UUID> matched = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (String name : spellNames) {
            UUID id = spellIdByName.get(name.toLowerCase(Locale.ROOT));
            if (id != null) {
                matched.add(id);
            } else {
                unresolved.add(name);
            }
        }

        if (matched.isEmpty()) {
            // Нечего грантить — только issue по неоднозначным именам.
            if (run.apply) {
                for (String name : unresolved) {
                    createIssue(run, item, null, ISSUE_AMBIGUOUS, FeatureIssueSeverity.WARN,
                            "Имя заклинания не сопоставлено со справочником: «" + name + "»", name);
                }
            } else {
                run.issuesCreated += unresolved.size();
            }
            return;
        }

        if (!run.apply) {
            run.created++;
            run.spellGrantsCreated += matched.size();
            run.issuesCreated += unresolved.size();
            note(run, item, "spell_grant: " + matched.size() + " матч(ей), "
                    + unresolved.size() + " неоднозначн.");
            return;
        }

        FeatureRule rule = createRule(item, FeatureRuleProfile.SPELL_GRANT, 2,
                "Заклинания из описания (backfill Фаза 6)");
        run.created++;
        ensureBinding(rule.getId(), attunement, false, consumable);
        for (UUID spellId : matched) {
            spellGrantRepository.save(FeatureSpellGrant.builder()
                    .featureRuleId(rule.getId())
                    .spellId(spellId)
                    .castWithoutSlot(true)
                    .usesResourceDefinitionId(chargesResourceId)
                    .build());
            run.spellGrantsCreated++;
        }
        for (String name : unresolved) {
            createIssue(run, item, rule.getId(), ISSUE_AMBIGUOUS, FeatureIssueSeverity.WARN,
                    "Имя заклинания не сопоставлено со справочником: «" + name + "»", name);
        }
        approve(rule);
    }

    // ── 4. Ручная адъюдикация ─────────────────────────────────────────────────

    /**
     * Для каждого нераспознанного фрагмента создаёт правило-профиль {@code manual_adjudication}
     * (по одному на предмет) и issue с исходным фрагментом текста.
     * @param run счётчики прогона
     * @param item предмет-владелец
     * @param fragments нераспознанные фрагменты описания
     * @param existingByType уже существующие правила по типу (идемпотентность)
     * @param attunement требуется ли настройка (для binding)
     */
    private void backfillManual(Run run, MagicItem item, List<String> fragments,
                                Map<String, FeatureRule> existingByType, boolean attunement) {
        if (fragments.isEmpty()) {
            return;
        }
        String ruleType = FeatureRuleProfile.MANUAL_ADJUDICATION.getCode();
        if (existingByType.containsKey(ruleType)) {
            run.skippedExisting++;
            return;
        }
        if (!run.apply) {
            run.created++;
            run.manual += fragments.size();
            run.issuesCreated += fragments.size();
            note(run, item, "manual: " + fragments.size() + " фрагмент(ов)");
            return;
        }

        FeatureRule rule = createRule(item, FeatureRuleProfile.MANUAL_ADJUDICATION, 3,
                "Нераспознанные эффекты предмета — ручная адъюдикация (backfill Фаза 6)");
        run.created++;
        ensureBinding(rule.getId(), attunement, false, false);
        for (String fragment : fragments) {
            createIssue(run, item, rule.getId(), ISSUE_MANUAL, FeatureIssueSeverity.INFO,
                    "Требует ручного разбора: " + truncate(fragment, 300), fragment);
            run.manual++;
        }
        // Ручные правила НЕ approve-им: остаются needs_review для ревьюера Workbench.
    }

    // ── shared helpers ────────────────────────────────────────────────────────

    /**
     * Собирает существующие MIGRATION-правила предмета по типу (для идемпотентности).
     * Зеркалит дедуп {@code SpellRuleBackfillService}: берём первое правило каждого типа.
     * @param itemId id магического предмета
     * @return карта rule_type → правило
     */
    private Map<String, FeatureRule> existingMigrationRules(UUID itemId) {
        Map<String, FeatureRule> byType = new HashMap<>();
        ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(OWNER, itemId).stream()
                .filter(r -> FeatureRuleSource.MIGRATION.getCode().equals(r.getSource()))
                .forEach(r -> byType.putIfAbsent(r.getRuleType(), r));
        return byType;
    }

    /**
     * Создаёт правило предмета в статусе needs_review + источник MIGRATION и инициализирует
     * первый черновик ревизии.
     * @param item предмет-владелец
     * @param profile профиль правила (тип)
     * @param sortOrder порядок сортировки
     * @param notes заметка правила
     * @return сохранённое правило
     */
    private FeatureRule createRule(MagicItem item, FeatureRuleProfile profile, int sortOrder, String notes) {
        FeatureRule rule = ruleRepository.save(FeatureRule.builder()
                .ownerType(OWNER)
                .ownerId(item.getId())
                .ruleType(profile.getCode())
                .enabled(true)
                .reviewStatus(FeatureReviewStatus.NEEDS_REVIEW.getCode())
                .source(FeatureRuleSource.MIGRATION.getCode())
                .confidence(0.5)
                .sortOrder(sortOrder)
                .notes(notes)
                .homebrewPackId(item.getHomebrew() != null ? item.getHomebrew().getId() : null)
                .build());
        revisionService.createInitialDraft(rule, ACTOR);
        return rule;
    }

    /**
     * Одобряет текущую ревизию правила (low-risk детерминированные правила).
     * @param rule правило
     */
    private void approve(FeatureRule rule) {
        revisionService.approveCurrent(rule.getId(), "Детерминированный бэкфилл правил предмета (Фаза 6)", ACTOR);
    }

    /**
     * Создаёт/обновляет binding правила предмета (attunement / equipped / consume).
     * @param featureRuleId id правила
     * @param requiresAttunement требуется ли настройка (данные предмета)
     * @param requiresEquipped требуется ли надетость (для static_grant)
     * @param consumeOnUse списывать ли предмет при использовании (potion/scroll)
     */
    private void ensureBinding(UUID featureRuleId, boolean requiresAttunement,
                               boolean requiresEquipped, boolean consumeOnUse) {
        itemBindingRepository.save(FeatureItemBinding.builder()
                .featureRuleId(featureRuleId)
                .requiresAttunement(requiresAttunement)
                .requiresEquipped(requiresEquipped)
                .consumeOnUse(consumeOnUse)
                .build());
    }

    /**
     * Создаёт issue прогона и инкрементирует счётчики.
     * @param run счётчики прогона
     * @param item предмет-владелец
     * @param featureRuleId id связанного правила (или null)
     * @param issueType код типа issue
     * @param severity серьёзность
     * @param message сообщение
     * @param sourceFragment исходный фрагмент текста (или null)
     */
    private void createIssue(Run run, MagicItem item, UUID featureRuleId, String issueType,
                             FeatureIssueSeverity severity, String message, String sourceFragment) {
        issueRepository.save(FeatureRuleIssue.builder()
                .ownerType(OWNER)
                .ownerId(item.getId())
                .featureRuleId(featureRuleId)
                .issueType(issueType)
                .severity(severity.getCode())
                .message(message)
                .sourceTextFragment(sourceFragment)
                .resolved(false)
                .build());
        run.issuesCreated++;
        if (ISSUE_LEGACY_BUFF_OVERLAP.equals(issueType)) {
            run.overlaps++;
        }
    }

    /**
     * Сохраняет скалярную int-формулу через {@link FeatureFormulaService} (как в spell-бэкфилле).
     * @param expression выражение (например «7»)
     * @return сохранённая формула
     */
    private FeatureFormula intFormula(String expression) {
        return saveFormula(expression, "scalar", "integer");
    }

    /**
     * Сохраняет кубиковую формулу (например «1d6+1») через {@link FeatureFormulaService}.
     * @param dice нормализованная кубиковая формула
     * @return сохранённая формула
     */
    private FeatureFormula diceFormula(String dice) {
        return saveFormula("dice(\"" + dice + "\")", "dice", "dice");
    }

    /**
     * Валидирует и сохраняет формулу (зеркалит {@code SpellRuleBackfillService.saveFormula}).
     * @param expression выражение
     * @param expressionType тип выражения
     * @param resultType тип результата
     * @return сохранённая формула
     */
    private FeatureFormula saveFormula(String expression, String expressionType, String resultType) {
        FeatureFormula formula = FeatureFormula.builder()
                .expression(expression)
                .expressionType(expressionType)
                .resultType(resultType)
                .validationStatus("unknown")
                .build();
        formulaService.validateAndStamp(formula);
        return formulaRepository.save(formula);
    }

    /**
     * Индексирует имена ванильных заклинаний (ru/en, lower-case) → id для сопоставления грантов.
     * @return карта нормализованное имя → id заклинания
     */
    private Map<String, UUID> indexSpellNames() {
        Map<String, UUID> byName = new HashMap<>();
        for (Spell spell : spellRepository.findAllByHomebrewIsNull()) {
            putName(byName, spell.getNameRu(), spell.getId());
            putName(byName, spell.getNameEn(), spell.getId());
        }
        return byName;
    }

    /**
     * Кладёт имя заклинания в индекс (без перезаписи, чтобы неоднозначность не терялась молча).
     * @param byName индекс
     * @param name имя (может быть null/blank)
     * @param id id заклинания
     */
    private void putName(Map<String, UUID> byName, String name, UUID id) {
        if (name != null && !name.isBlank()) {
            byName.putIfAbsent(name.trim().toLowerCase(Locale.ROOT), id);
        }
    }

    /**
     * Проверяет, является ли предмет потребляемым (potion/scroll) по слагу типа.
     * @param item магический предмет
     * @return true, если тип — potion или scroll
     */
    private boolean isConsumable(MagicItem item) {
        MagicItemType type = item.getType();
        if (type == null || type.getSlug() == null) {
            return false;
        }
        return CONSUMABLE_SLUGS.contains(type.getSlug().toLowerCase(Locale.ROOT));
    }

    /**
     * Формирует ключ ресурса зарядов для предмета (уникален в рамках правила).
     * @param item предмет
     * @return ключ ресурса длиной ≤ 64
     */
    private String resourceKey(MagicItem item) {
        String base = "charges:" + (item.getSlug() != null ? item.getSlug() : item.getId());
        return truncate(base, 64);
    }

    /**
     * Отображаемое имя предмета (ru → en → slug).
     * @param item предмет
     * @return лучшее доступное имя
     */
    private String displayName(MagicItem item) {
        if (item.getNameRu() != null && !item.getNameRu().isBlank()) {
            return item.getNameRu();
        }
        if (item.getNameEn() != null && !item.getNameEn().isBlank()) {
            return item.getNameEn();
        }
        return String.valueOf(item.getSlug());
    }

    /**
     * Кратко описывает набор бонусов для заметок/issue.
     * @param bonuses бонусы
     * @return строка вида «+2 ATTACK_AND_DAMAGE; +1 AC»
     */
    private String summarizeBonuses(List<StaticBonus> bonuses) {
        StringBuilder sb = new StringBuilder();
        for (StaticBonus b : bonuses) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append('+').append(b.amount()).append(' ').append(b.target());
        }
        return sb.toString();
    }

    /**
     * Добавляет построчную заметку прогона по предмету.
     * @param run счётчики прогона
     * @param item предмет
     * @param message заметка
     */
    private void note(Run run, MagicItem item, String message) {
        run.notes.add(displayName(item) + " (" + item.getId() + "): " + message);
    }

    /**
     * Обрезает строку до максимальной длины.
     * @param s строка (может быть null)
     * @param max максимальная длина
     * @return обрезанная строка
     */
    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
