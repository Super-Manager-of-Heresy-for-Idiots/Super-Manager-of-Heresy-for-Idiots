package com.dnd.app.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One-time importer for the normalized D&D PHB content bundle in
 * {@code resources/dnd_import/}. It populates the normalized content tables
 * (created by migration 054 in the default {@code public} schema) and is guarded
 * by a row-count check so it runs only once. It never touches the bestiary /
 * homebrew / existing analog tables.
 *
 * Uses raw JDBC (no JPA entities for these content tables exist yet) with explicit
 * Java-generated UUIDs so child rows can be wired to parents in-memory.
 */
@Component
public class DndContentLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DndContentLoader.class);

    private static final String CORE_MOD_SLUG = "phb-2024-core";
    private static final Set<String> SPELL_COMPONENTS = Set.of("verbal", "somatic", "material");
    private static final Set<String> EQUIPMENT_ENTRY_TYPES =
            Set.of("equipment", "equipment_choice_ref", "currency", "raw");
    private static final Map<String, Integer> RARITY_SORT = Map.of(
            "common", 1, "uncommon", 2, "rare", 3, "very-rare", 4, "legendary", 5, "artifact", 6);

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final com.dnd.app.service.ClassRewardSeedService classRewardSeedService;
    private final com.dnd.app.service.ClassLevelRewardSeedService classLevelRewardSeedService;

    // slug -> generated UUID lookup tables, built as we go.
    private final Map<String, UUID> currencies = new HashMap<>();
    private final Map<String, UUID> abilities = new HashMap<>();
    private final Map<String, UUID> skills = new HashMap<>();
    private final Map<String, UUID> damageTypes = new HashMap<>();
    private final Map<String, UUID> equipmentCategories = new HashMap<>();
    private final Map<String, UUID> weaponProperties = new HashMap<>();
    private final Map<String, UUID> weaponMasteries = new HashMap<>();
    private final Map<String, UUID> spellSchools = new HashMap<>();
    private final Map<String, UUID> charSizes = new HashMap<>();
    private final Map<String, UUID> creatureTypes = new HashMap<>();
    private final Map<String, UUID> magicItemTypes = new HashMap<>();
    private final Map<String, UUID> magicItemRarities = new HashMap<>();
    private final Map<String, UUID> featCategories = new HashMap<>();
    private final Map<String, UUID> equipmentItems = new HashMap<>();
    private final Map<String, UUID> feats = new HashMap<>();
    private final Map<String, UUID> classes = new HashMap<>();
    private final Map<String, UUID> spells = new HashMap<>();
    // (classSlug + "::" + subclassNameRu) -> subclass UUID, for fuzzy spell links.
    private final Map<String, UUID> subclassesByName = new HashMap<>();

    private UUID sourceId;
    private UUID modId;

    public DndContentLoader(JdbcTemplate jdbc, ObjectMapper mapper,
                            com.dnd.app.service.ClassRewardSeedService classRewardSeedService,
                            com.dnd.app.service.ClassLevelRewardSeedService classLevelRewardSeedService) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.classRewardSeedService = classRewardSeedService;
        this.classLevelRewardSeedService = classLevelRewardSeedService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean coreAlreadyPresent;
        try {
            Integer existing = jdbc.queryForObject(
                    "SELECT count(*) FROM mod_package WHERE slug = ?", Integer.class, CORE_MOD_SLUG);
            coreAlreadyPresent = existing != null && existing > 0;
        } catch (DataAccessException e) {
            log.warn("content tables not available, skipping content import: {}", e.getMessage());
            return;
        }

        if (coreAlreadyPresent) {
            log.info("dnd content already present (mod '{}'), skipping base import", CORE_MOD_SLUG);
        } else {
            log.info("Importing normalized D&D content into public schema...");
            loadReferences();
            loadEquipment();
            loadInventoryWeapons();
            loadInventoryArmors();
            loadFeats();
            loadClasses();
            loadSpells();
            loadSpecies();
            loadMagicItems();
            loadBackgrounds();
            loadScrollCrafting();
            loadRandomTables();
            log.info("D&D content import complete: {} equipment, {} feats, {} classes, {} spells, "
                            + "{} magic items dictionaries primed",
                    equipmentItems.size(), feats.size(), classes.size(), spells.size(), magicItemTypes.size());
        }

        // Class build mechanics are seeded separately and idempotently (own
        // row-count guard) so they populate both a fresh import and an existing DB
        // whose base content was loaded before this step existed.
        loadClassMechanics();

        // Backfill reward groups derivable from the imported data (subclass-choice
        // groups). Idempotent and safe to run on every startup; never touches homebrew.
        try {
            classRewardSeedService.seedCoreSubclassChoiceGroups();
        } catch (DataAccessException e) {
            log.warn("class reward backfill skipped: {}", e.getMessage());
        }

        // Backfill per-level reward groups (base features, ASI, expertise, prepared/cantrip
        // spell choices) so level-up grants more than just HP. Idempotent; never touches homebrew.
        try {
            classLevelRewardSeedService.seedCoreLevelRewards();
        } catch (DataAccessException e) {
            log.warn("per-level reward backfill skipped: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ references / dictionaries

    private void loadReferences() {
        JsonNode root = read("references.json");

        JsonNode src = root.get("source");
        sourceId = UUID.randomUUID();
        jdbc.update("INSERT INTO source_book(source_id, slug, name, url, license_note) VALUES (?,?,?,?,?)",
                sourceId, txt(src, "slug"), txt(src, "name"), txt(src, "url"), txt(src, "license_note"));

        modId = UUID.randomUUID();
        jdbc.update("INSERT INTO mod_package(mod_id, slug, name, is_core) VALUES (?,?,?,?)",
                modId, CORE_MOD_SLUG, "Player's Handbook 2024 (core import)", true);

        for (JsonNode c : array(root, "currencies")) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO currency(currency_id, slug, name_ru, name_en, abbr_ru, abbr_en, copper_value) "
                            + "VALUES (?,?,?,?,?,?,?)",
                    id, txt(c, "slug"), txt(c, "name_ru"), txt(c, "name_en"),
                    txt(c, "abbr_ru"), txt(c, "abbr_en"), dec(c, "to_copper"));
            currencies.put(txt(c, "slug"), id);
        }

        for (JsonNode a : array(root, "ability_scores")) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO ability_score(ability_score_id, slug, name_ru, name_en) VALUES (?,?,?,?)",
                    id, txt(a, "slug"), txt(a, "name_ru"), txt(a, "name_en"));
            abilities.put(txt(a, "slug"), id);
        }

        for (JsonNode s : array(root, "skills")) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO skill(skill_id, slug, name_ru, name_en, ability_score_id) VALUES (?,?,?,?,?)",
                    id, txt(s, "slug"), txt(s, "name_ru"), txt(s, "name_en"), abilities.get(txt(s, "ability_slug")));
            skills.put(txt(s, "slug"), id);
        }

        for (JsonNode d : array(root, "damage_types")) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO damage_type(damage_type_id, slug, name_ru, name_en) VALUES (?,?,?,?)",
                    id, txt(d, "slug"), txt(d, "name_ru"), txt(d, "name_en"));
            damageTypes.put(txt(d, "slug"), id);
        }

        for (JsonNode e : array(root, "equipment_categories")) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO equipment_category(equipment_category_id, slug, name_ru, name_en) VALUES (?,?,?,?)",
                    id, txt(e, "slug"), txt(e, "name_ru"), txt(e, "name_en"));
            equipmentCategories.put(txt(e, "slug"), id);
        }

        for (JsonNode w : array(root, "weapon_properties")) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO weapon_property(weapon_property_id, slug, name_ru, name_en) VALUES (?,?,?,?)",
                    id, txt(w, "slug"), txt(w, "name_ru"), txt(w, "name_en"));
            weaponProperties.put(txt(w, "slug"), id);
        }

        for (JsonNode m : array(root, "weapon_masteries")) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO weapon_mastery(weapon_mastery_id, slug, name_ru, name_en) VALUES (?,?,?,?)",
                    id, txt(m, "slug"), txt(m, "name_ru"), txt(m, "name_en"));
            weaponMasteries.put(txt(m, "slug"), id);
        }

        // spell_schools has a known duplicate "conjuration" slug (Вызов + Призыв); keep the first.
        for (JsonNode s : array(root, "spell_schools")) {
            String slug = txt(s, "slug");
            if (spellSchools.containsKey(slug)) {
                log.info("Skipping duplicate spell_school slug '{}' ({})", slug, txt(s, "name_ru"));
                continue;
            }
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO spell_school(spell_school_id, slug, name_ru, name_en) VALUES (?,?,?,?)",
                    id, slug, txt(s, "name_ru"), txt(s, "name_en"));
            spellSchools.put(slug, id);
        }

        for (JsonNode s : array(root, "sizes")) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO character_size(character_size_id, slug, name_ru, name_en) VALUES (?,?,?,?)",
                    id, txt(s, "slug"), txt(s, "name_ru"), txt(s, "name_en"));
            charSizes.put(txt(s, "slug"), id);
        }
        // speed_types are stored as plain slugs on species_speed; no dictionary table needed.
    }

    // ------------------------------------------------------------------ equipment (canonical)

    private void loadEquipment() {
        for (JsonNode item : read("equipment.normalized.json")) {
            String slug = txt(item, "slug");
            String kind = orDefault(txt(item, "kind"), "equipment");
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO equipment_item(equipment_item_id, mod_id, source_id, slug, name_ru, name_en, "
                            + "category_id, kind, cost_money_value_id, weight_lb, properties_text, url) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, modId, sourceId, slug, txt(item, "name_ru"), txt(item, "name_en"),
                    equipmentCategories.get(txt(item, "category_slug")), kind,
                    insertMoneyValue(item.get("cost")), dec(weight(item), "pounds"),
                    txt(item, "properties_text"), txt(item, "url"));
            equipmentItems.put(slug, id);

            if ("weapon".equals(kind)) {
                insertWeaponStat(id, item);
            }
        }

        // Second pass: weapon ammunition / versatile FKs reference other equipment items,
        // but the schema models them via weapon_item_property which we already filled with
        // raw text.
        log.info("Loaded {} equipment items", equipmentItems.size());
    }

    private void insertWeaponStat(UUID equipmentItemId, JsonNode item) {
        JsonNode weapon = item.get("weapon");
        JsonNode damage = (weapon != null && weapon.hasNonNull("damage")) ? weapon.get("damage") : item.get("damage");
        UUID diceId = insertDiceFormula(damage);
        UUID dmgType = damage != null ? damageTypes.get(txt(damage, "damage_type_slug")) : null;
        Integer flat = damage != null ? intg(damage, "flat_damage") : null;
        UUID masteryId = weapon != null ? weaponMasteries.get(txt(weapon, "mastery_slug")) : null;

        jdbc.update("INSERT INTO weapon_stat(equipment_item_id, damage_dice_formula_id, damage_type_id, "
                        + "flat_damage, mastery_id) VALUES (?,?,?,?,?)",
                equipmentItemId, diceId, dmgType, flat, masteryId);

        if (weapon == null) {
            return;
        }
        Set<UUID> seen = new HashSet<>();
        for (JsonNode prop : array(weapon, "properties")) {
            UUID propId = weaponProperties.get(txt(prop, "property_slug"));
            if (propId == null || !seen.add(propId)) {
                continue;
            }
            UUID versatileDice = null;
            JsonNode params = prop.get("params");
            if (params != null && params.hasNonNull("versatile_damage_dice")) {
                versatileDice = insertDiceFromText(params.get("versatile_damage_dice").asText());
            }
            jdbc.update("INSERT INTO weapon_item_property(equipment_item_id, weapon_property_id, normal_range_ft, "
                            + "long_range_ft, versatile_dice_formula_id, ammunition_equipment_item_id, raw_text) "
                            + "VALUES (?,?,?,?,?,?,?)",
                    equipmentItemId, propId, intg(params, "normal_range_ft"), intg(params, "long_range_ft"),
                    versatileDice, null, txt(prop, "raw"));
        }
    }

    private void loadInventoryWeapons() {
        int added = 0;
        for (JsonNode item : read("weapons_from_inventory.normalized.json")) {
            String slug = txt(item, "slug");
            if (equipmentItems.containsKey(slug)) {
                continue; // already loaded from equipment.json (richer)
            }
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO equipment_item(equipment_item_id, mod_id, source_id, slug, name_ru, name_en, "
                            + "category_id, kind, cost_money_value_id, weight_lb, properties_text, url) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, modId, sourceId, slug, txt(item, "name_ru"), txt(item, "name_en"),
                    null, "weapon", insertMoneyValue(item.get("cost")), dec(weight(item), "pounds"),
                    txt(item, "short_properties_raw"), txt(item, "source_url"));
            equipmentItems.put(slug, id);

            JsonNode damage = item.get("damage");
            jdbc.update("INSERT INTO weapon_stat(equipment_item_id, damage_dice_formula_id, damage_type_id, "
                            + "flat_damage, mastery_id) VALUES (?,?,?,?,?)",
                    id, insertDiceFormula(damage), damage != null ? damageTypes.get(txt(damage, "damage_type_slug")) : null,
                    damage != null ? intg(damage, "flat_damage") : null, weaponMasteries.get(txt(item, "mastery_slug")));
            added++;
        }
        if (added > 0) {
            log.info("Added {} weapons from inventory not present in equipment.json", added);
        }
    }

    private void loadInventoryArmors() {
        Set<UUID> armorStatDone = new HashSet<>();
        for (JsonNode item : read("armors_from_inventory.normalized.json")) {
            String slug = txt(item, "slug");
            UUID id = equipmentItems.get(slug);
            if (id == null) {
                id = UUID.randomUUID();
                jdbc.update("INSERT INTO equipment_item(equipment_item_id, mod_id, source_id, slug, name_ru, name_en, "
                                + "category_id, kind, cost_money_value_id, weight_lb, properties_text, url) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                        id, modId, sourceId, slug, txt(item, "name_ru"), txt(item, "name_en"),
                        null, "armor", insertMoneyValue(item.get("cost")), dec(weight(item), "pounds"),
                        txt(item, "group_ru"), txt(item, "source_url"));
                equipmentItems.put(slug, id);
            }
            if (!armorStatDone.add(id)) {
                continue;
            }
            jdbc.update("INSERT INTO armor_stat(equipment_item_id, base_ac, dex_bonus_allowed, max_dex_bonus, "
                            + "strength_required, stealth_disadvantage, armor_class_raw) VALUES (?,?,?,?,?,?,?)",
                    id, null, false, null, intg(item, "strength_required"),
                    bool(item, "stealth_disadvantage", false), txt(item, "armor_class"));
        }
    }

    // ------------------------------------------------------------------ feats

    private void loadFeats() {
        for (JsonNode feat : read("feats.normalized.json")) {
            UUID categoryId = deriveFeatCategory(txt(feat, "category_slug"), txt(feat, "category_ru"));
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO feat(feat_id, mod_id, source_id, slug, name_ru, name_en, category_id, "
                            + "repeatable, description, url) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    id, modId, sourceId, txt(feat, "slug"), txt(feat, "name_ru"), txt(feat, "name_en"),
                    categoryId, bool(feat, "repeatable", false), txt(feat, "description"), txt(feat, "url"));
            feats.put(txt(feat, "slug"), id);

            int order = 0;
            for (JsonNode sec : array(feat, "sections")) {
                jdbc.update("INSERT INTO feat_section(feat_section_id, feat_id, sort_order, title, body) "
                                + "VALUES (?,?,?,?,?)",
                        UUID.randomUUID(), id, order++, txt(sec, "title"), txt(sec, "text"));
            }
            for (JsonNode pre : array(feat, "prerequisites")) {
                String type = pre.isObject() ? orDefault(txt(pre, "type"), "raw") : "raw";
                jdbc.update("INSERT INTO feat_prerequisite(feat_prerequisite_id, feat_id, prerequisite_type, "
                                + "level_required, ability_score_id, minimum_score, group_key, raw_text) "
                                + "VALUES (?,?,?,?,?,?,?,?)",
                        UUID.randomUUID(), id, type, intg(pre, "level"),
                        abilities.get(txt(pre, "ability_slug")), intg(pre, "minimum_score"),
                        txt(pre, "group_key"), pre.isObject() ? pre.toString() : pre.asText());
            }
        }
        log.info("Loaded {} feats", feats.size());
    }

    private UUID deriveFeatCategory(String slug, String nameRu) {
        if (slug == null) {
            return null;
        }
        return featCategories.computeIfAbsent(slug, s -> {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO feat_category(feat_category_id, slug, name_ru, name_en) VALUES (?,?,?,?)",
                    id, s, orDefault(nameRu, s), null);
            return id;
        });
    }

    // ------------------------------------------------------------------ classes

    private void loadClasses() {
        for (JsonNode cls : read("classes.normalized.json")) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO character_class(class_id, mod_id, source_id, slug, name_ru, name_en, subtitle, "
                            + "url) VALUES (?,?,?,?,?,?,?,?)",
                    id, modId, sourceId, txt(cls, "slug"), txt(cls, "name_ru"), txt(cls, "name_en"),
                    txt(cls, "subtitle"), txt(cls, "url"));
            classes.put(txt(cls, "slug"), id);

            for (JsonNode sub : array(cls, "subclasses")) {
                UUID subId = UUID.randomUUID();
                jdbc.update("INSERT INTO subclass(subclass_id, class_id, slug, name_ru, name_en, is_empty_placeholder) "
                                + "VALUES (?,?,?,?,?,?)",
                        subId, id, txt(sub, "slug"), txt(sub, "name_ru"), txt(sub, "name_en"),
                        bool(sub, "is_empty_placeholder", false));
                subclassesByName.put(txt(cls, "slug") + "::" + txt(sub, "name_ru"), subId);
            }

            Map<String, UUID> columns = new HashMap<>();
            for (JsonNode col : array(cls, "progression_columns")) {
                UUID colId = UUID.randomUUID();
                jdbc.update("INSERT INTO class_progression_column(class_progression_column_id, class_id, slug, "
                                + "name_ru, sort_order) VALUES (?,?,?,?,?)",
                        colId, id, txt(col, "slug"), txt(col, "name_ru"), orZero(intg(col, "sort_order")));
                columns.put(txt(col, "slug"), colId);
            }

            for (JsonNode row : array(cls, "progression")) {
                Integer level = intg(row, "level");
                if (level == null || level < 1 || level > 20) {
                    continue;
                }
                Set<UUID> seenCols = new HashSet<>();
                for (JsonNode val : array(row, "values")) {
                    UUID colId = columns.get(txt(val, "column_slug"));
                    if (colId == null || !seenCols.add(colId)) {
                        continue;
                    }
                    jdbc.update("INSERT INTO class_progression_value(class_id, class_level, "
                                    + "class_progression_column_id, value_raw, value_numeric) VALUES (?,?,?,?,?)",
                            id, level, colId, txt(val, "value_raw"), intg(val, "value_numeric"));
                }
            }
        }
        log.info("Loaded {} classes", classes.size());
    }

    // ------------------------------------------------------------------ spells

    private void loadSpells() {
        for (JsonNode spell : read("spells.normalized.json")) {
            JsonNode casting = spell.get("casting_time");
            JsonNode range = spell.get("range");
            JsonNode duration = spell.get("duration");
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO spell(spell_id, mod_id, source_id, slug, name_ru, name_en, level, school_id, "
                            + "casting_time_raw, casting_action_slug, is_ritual, range_type, range_distance, range_unit, "
                            + "duration_raw, duration_type, duration_amount, duration_unit, concentration, description, "
                            + "higher_levels, url) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, modId, sourceId, txt(spell, "slug"), txt(spell, "name_ru"), txt(spell, "name_en"),
                    orZero(intg(spell, "level")), spellSchools.get(txt(spell, "school_slug")),
                    txt(casting, "raw"), txt(casting, "action_slug"), bool(casting, "ritual", false),
                    txt(range, "range_type"), intg(range, "distance"), txt(range, "unit"),
                    txt(duration, "raw"), txt(duration, "duration_type"), intg(duration, "amount"),
                    txt(duration, "unit"), bool(duration, "concentration", false),
                    txt(spell, "description"), txt(spell, "higher_levels"), txt(spell, "url"));
            spells.put(txt(spell, "slug"), id);

            Set<String> comps = new HashSet<>();
            for (JsonNode comp : array(spell, "components")) {
                String cslug = txt(comp, "component_slug");
                if (cslug == null || !SPELL_COMPONENTS.contains(cslug) || !comps.add(cslug)) {
                    continue;
                }
                jdbc.update("INSERT INTO spell_component(spell_id, component_slug, material_text, consumed, "
                                + "cost_money_value_id) VALUES (?,?,?,?,?)",
                        id, cslug, txt(comp, "details"), null, null);
            }

            Set<UUID> linkedClasses = new HashSet<>();
            for (JsonNode link : array(spell, "class_links")) {
                UUID classId = classes.get(txt(link, "class_slug"));
                if (classId == null || !linkedClasses.add(classId)) {
                    continue;
                }
                jdbc.update("INSERT INTO spell_class(spell_id, class_id) VALUES (?,?)", id, classId);
            }

            Set<UUID> linkedSubs = new HashSet<>();
            for (JsonNode link : array(spell, "subclass_links")) {
                UUID subId = subclassesByName.get(txt(link, "class_slug") + "::" + txt(link, "name_ru"));
                if (subId == null || !linkedSubs.add(subId)) {
                    continue;
                }
                jdbc.update("INSERT INTO spell_subclass(spell_id, subclass_id, raw_text) VALUES (?,?,?)",
                        id, subId, txt(link, "name_ru"));
            }
        }
        log.info("Loaded {} spells", spells.size());
    }

    // ------------------------------------------------------------------ species

    private void loadSpecies() {
        int count = 0;
        for (JsonNode sp : read("species.normalized.json")) {
            UUID creatureTypeId = deriveCreatureType(txt(sp, "creature_type_slug"), txt(sp, "creature_type_ru"));
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO species(species_id, mod_id, source_id, slug, name_ru, name_en, "
                            + "creature_type_id, description, url) VALUES (?,?,?,?,?,?,?,?,?)",
                    id, modId, sourceId, txt(sp, "slug"), txt(sp, "name_ru"), txt(sp, "name_en"),
                    creatureTypeId, txt(sp, "description"), txt(sp, "url"));
            count++;

            Set<UUID> sizes = new HashSet<>();
            for (JsonNode group : array(sp, "size_options")) {
                for (JsonNode opt : array(group, "options")) {
                    UUID sizeId = charSizes.get(txt(opt, "size_slug"));
                    if (sizeId == null || !sizes.add(sizeId)) {
                        continue;
                    }
                    jdbc.update("INSERT INTO species_size_option(species_id, character_size_id) VALUES (?,?)",
                            id, sizeId);
                }
            }

            for (JsonNode speed : array(sp, "speeds")) {
                jdbc.update("INSERT INTO species_speed(species_speed_id, species_id, speed_type_slug, amount_ft, "
                                + "raw_text) VALUES (?,?,?,?,?)",
                        UUID.randomUUID(), id, orDefault(txt(speed, "speed_type_slug"), "walk"),
                        intg(speed, "amount_ft"), txt(speed, "raw"));
            }

            Set<String> traitSlugs = new HashSet<>();
            for (JsonNode trait : array(sp, "traits")) {
                String tslug = txt(trait, "slug");
                if (tslug == null || !traitSlugs.add(tslug)) {
                    continue;
                }
                UUID traitId = UUID.randomUUID();
                jdbc.update("INSERT INTO species_trait(species_trait_id, species_id, slug, sort_order, name, "
                                + "description) VALUES (?,?,?,?,?,?)",
                        traitId, id, tslug, orZero(intg(trait, "sort_order")), txt(trait, "name"), txt(trait, "text"));

                for (JsonNode eff : array(trait, "effects")) {
                    jdbc.update("INSERT INTO species_trait_effect(species_trait_effect_id, species_trait_id, "
                                    + "effect_type, damage_type_id, spell_id, range_ft) "
                                    + "VALUES (?,?,?,?,?,?)",
                            UUID.randomUUID(), traitId, orDefault(txt(eff, "effect_type"), "unknown"),
                            damageTypes.get(txt(eff, "damage_type_slug")), spells.get(txt(eff, "spell_slug")),
                            intg(eff, "range_ft"));
                }
            }
        }
        log.info("Loaded {} species", count);
    }

    private UUID deriveCreatureType(String slug, String nameRu) {
        if (slug == null) {
            return null;
        }
        return creatureTypes.computeIfAbsent(slug, s -> {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO creature_type(creature_type_id, slug, name_ru, name_en) VALUES (?,?,?,?)",
                    id, s, orDefault(nameRu, s), null);
            return id;
        });
    }

    // ------------------------------------------------------------------ magic items

    private void loadMagicItems() {
        int count = 0;
        for (JsonNode mi : read("magic_items.normalized.json")) {
            JsonNode type = mi.get("item_type");
            JsonNode rarity = mi.get("rarity");
            UUID typeId = type == null ? null
                    : deriveMagicItemType(txt(type, "base_type_slug"), txt(type, "base_type_ru"));
            UUID rarityId = rarity == null ? null
                    : deriveMagicItemRarity(txt(rarity, "rarity_slug"), txt(rarity, "raw"));
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO magic_item(magic_item_id, mod_id, source_id, slug, name_ru, name_en, "
                            + "magic_item_type_id, type_restriction_raw, rarity_id, variable_rarity, attunement_required, "
                            + "attunement_requirement, cost_money_value_id, description, embedded_tables_detected, url) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, modId, sourceId, txt(mi, "slug"), txt(mi, "name_ru"), txt(mi, "name_en"),
                    typeId, txt(type, "restriction_raw"), rarityId, bool(rarity, "variable", false),
                    bool(mi, "attunement_required", false), txt(mi, "attunement_requirement"),
                    insertMoneyValue(mi.get("cost")), txt(mi, "description"),
                    bool(mi, "embedded_tables_detected", false), txt(mi, "url"));
            count++;

            if (type != null) {
                Set<UUID> seen = new HashSet<>();
                for (JsonNode allowed : array(type, "allowed_equipment")) {
                    String eqSlug = allowed.isTextual() ? allowed.asText() : txt(allowed, "equipment_slug");
                    UUID eqId = equipmentItems.get(eqSlug);
                    if (eqId == null || !seen.add(eqId)) {
                        continue;
                    }
                    jdbc.update("INSERT INTO magic_item_allowed_equipment(magic_item_id, equipment_item_id, raw_text) "
                                    + "VALUES (?,?,?)",
                            id, eqId, allowed.isTextual() ? allowed.asText() : txt(allowed, "raw"));
                }
            }
        }
        log.info("Loaded {} magic items", count);
    }

    private UUID deriveMagicItemType(String slug, String nameRu) {
        if (slug == null) {
            return null;
        }
        return magicItemTypes.computeIfAbsent(slug, s -> {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO magic_item_type(magic_item_type_id, slug, name_ru, name_en) VALUES (?,?,?,?)",
                    id, s, orDefault(nameRu, s), null);
            return id;
        });
    }

    private UUID deriveMagicItemRarity(String slug, String nameRu) {
        if (slug == null) {
            return null;
        }
        return magicItemRarities.computeIfAbsent(slug, s -> {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO magic_item_rarity(magic_item_rarity_id, slug, name_ru, name_en, sort_order) "
                            + "VALUES (?,?,?,?,?)",
                    id, s, orDefault(nameRu, s), null, RARITY_SORT.get(s));
            return id;
        });
    }

    // ------------------------------------------------------------------ backgrounds

    private void loadBackgrounds() {
        int count = 0;
        for (JsonNode bg : read("backgrounds.normalized.json")) {
            JsonNode featNode = bg.get("feat");
            UUID featId = featNode != null ? feats.get(txt(featNode, "feat_slug")) : null;
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO background(background_id, mod_id, source_id, slug, name_ru, name_en, feat_id, "
                            + "description, url) VALUES (?,?,?,?,?,?,?,?,?)",
                    id, modId, sourceId, txt(bg, "slug"), txt(bg, "name_ru"), txt(bg, "name_en"),
                    featId, txt(bg, "description"), txt(bg, "url"));
            count++;

            Set<UUID> abilitySet = new HashSet<>();
            for (JsonNode ab : array(bg, "ability_score_options")) {
                UUID abId = abilities.get(txt(ab, "ability_slug"));
                if (abId == null || !abilitySet.add(abId)) {
                    continue;
                }
                jdbc.update("INSERT INTO background_ability_option(background_id, ability_score_id) VALUES (?,?)",
                        id, abId);
            }

            Set<UUID> skillSet = new HashSet<>();
            for (JsonNode sk : array(bg, "skill_proficiencies")) {
                UUID skId = skills.get(txt(sk, "skill_slug"));
                if (skId == null || !skillSet.add(skId)) {
                    continue;
                }
                jdbc.update("INSERT INTO background_skill_proficiency(background_id, skill_id) VALUES (?,?)",
                        id, skId);
            }

            for (JsonNode tool : array(bg, "tool_proficiencies")) {
                jdbc.update("INSERT INTO background_tool_proficiency(background_tool_proficiency_id, background_id, "
                                + "equipment_item_id, choose_count, choice_group_slug, raw_text) VALUES (?,?,?,?,?,?)",
                        UUID.randomUUID(), id, equipmentItems.get(txt(tool, "equipment_slug")), null,
                        txt(tool, "kind"), orDefault(txt(tool, "name_ru"), txt(tool, "raw")));
            }

            for (JsonNode lang : array(bg, "language_proficiencies")) {
                jdbc.update("INSERT INTO background_language_proficiency(background_language_proficiency_id, "
                                + "background_id, language_slug, choose_count, raw_text) VALUES (?,?,?,?,?)",
                        UUID.randomUUID(), id, txt(lang, "language_slug"), intg(lang, "choose_count"),
                        orDefault(txt(lang, "name_ru"), txt(lang, "raw")));
            }

            for (JsonNode group : array(bg, "equipment_choices")) {
                UUID groupId = UUID.randomUUID();
                jdbc.update("INSERT INTO background_equipment_choice_group(background_equipment_choice_group_id, "
                                + "background_id, group_slug, choose_count, raw_text) VALUES (?,?,?,?,?)",
                        groupId, id, orDefault(txt(group, "choice_group"), "starting_equipment"),
                        intg(group, "choose_count"), txt(group, "raw"));

                for (JsonNode opt : array(group, "options")) {
                    UUID optId = UUID.randomUUID();
                    jdbc.update("INSERT INTO background_equipment_option(background_equipment_option_id, "
                                    + "background_equipment_choice_group_id, option_code, sort_order, raw_text) "
                                    + "VALUES (?,?,?,?,?)",
                            optId, groupId, orDefault(txt(opt, "option_code"), "A"),
                            orZero(intg(opt, "sort_order")), txt(opt, "raw"));

                    for (JsonNode entry : array(opt, "entries")) {
                        insertEquipmentEntry(optId, entry);
                    }
                }
            }
        }
        log.info("Loaded {} backgrounds", count);
    }

    private void insertEquipmentEntry(UUID optionId, JsonNode entry) {
        String rawType = txt(entry, "entry_type");
        String entryType = EQUIPMENT_ENTRY_TYPES.contains(rawType) ? rawType : "raw";
        UUID equipmentItemId = null;
        UUID moneyValueId = null;
        if ("equipment".equals(entryType)) {
            equipmentItemId = equipmentItems.get(txt(entry, "equipment_slug"));
        } else if ("currency".equals(entryType)) {
            moneyValueId = insertMoneyValue(entry.get("currency"));
        }
        BigDecimal qty = dec(entry, "quantity");
        jdbc.update("INSERT INTO background_equipment_entry(background_equipment_entry_id, "
                        + "background_equipment_option_id, entry_type, equipment_item_id, money_value_id, quantity, "
                        + "quantity_unit_raw, variant_note, choice_ref, raw_text) VALUES (?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), optionId, entryType, equipmentItemId, moneyValueId,
                qty != null ? qty : BigDecimal.ONE, txt(entry, "quantity_unit_raw"), txt(entry, "variant_note"),
                txt(entry, "choice_ref"), txt(entry, "raw"));
    }

    // ------------------------------------------------------------------ scroll crafting + random tables

    private void loadScrollCrafting() {
        for (JsonNode rule : read("spell_scroll_crafting.normalized.json")) {
            Integer level = intg(rule, "spell_level");
            if (level == null || level < 0 || level > 9) {
                continue;
            }
            jdbc.update("INSERT INTO spell_scroll_crafting_rule(spell_scroll_crafting_rule_id, spell_level, "
                            + "time_days, cost_money_value_id) VALUES (?,?,?,?)",
                    UUID.randomUUID(), level, orZero(intg(rule, "time_days")),
                    insertMoneyValue(rule.get("cost")));
        }
    }

    private void loadRandomTables() {
        for (JsonNode table : read("random_tables.normalized.json")) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO random_table(random_table_id, mod_id, source_id, slug, name_ru, dice, url) "
                            + "VALUES (?,?,?,?,?,?,?)",
                    id, modId, sourceId, txt(table, "slug"), txt(table, "name_ru"), txt(table, "dice"),
                    txt(table, "url"));

            for (JsonNode entry : array(table, "entries")) {
                jdbc.update("INSERT INTO random_table_entry(random_table_entry_id, random_table_id, range_start, "
                                + "range_end, display_range, result_text, linked_equipment_item_id, linked_magic_item_id) "
                                + "VALUES (?,?,?,?,?,?,?,?)",
                        UUID.randomUUID(), id, intg(entry, "range_start"), intg(entry, "range_end"),
                        orDefault(txt(entry, "display_range"), ""), orDefault(txt(entry, "result_text"), ""),
                        null, null);
            }
        }
    }

    // ------------------------------------------------------------------ class mechanics

    /**
     * Seeds class build mechanics (hit die, saving throws, primary ability, skill
     * choices, the spellcasting profile, and proficiency texts) from
     * {@code class_mechanics.json} into the character_class columns and the
     * class_saving_throw / class_primary_ability / class_skill_option tables added
     * by migration 057.
     *
     * Idempotent and self-guarded by a row-count check. It resolves its slug to id
     * lookups straight from the DB rather than the in-memory maps, so it works
     * whether or not the base import ran in this same execution.
     */
    private void loadClassMechanics() {
        try {
            Integer seeded = jdbc.queryForObject("SELECT count(*) FROM class_saving_throw", Integer.class);
            if (seeded != null && seeded > 0) {
                log.info("class mechanics already seeded, skipping");
                return;
            }
        } catch (DataAccessException e) {
            log.warn("class mechanics tables not available, skipping: {}", e.getMessage());
            return;
        }

        String coreModId = jdbc.queryForObject(
                "SELECT mod_id FROM mod_package WHERE slug = ?", String.class, CORE_MOD_SLUG);
        Map<String, UUID> abilityBySlug = loadSlugIds("SELECT slug, ability_score_id FROM ability_score");
        Map<String, UUID> skillBySlug = loadSlugIds("SELECT slug, skill_id FROM skill");
        Map<String, UUID> classBySlug = new HashMap<>();
        jdbc.query("SELECT slug, class_id FROM character_class WHERE mod_id = ?",
                (RowCallbackHandler) rs -> classBySlug.put(rs.getString(1), UUID.fromString(rs.getString(2))),
                UUID.fromString(coreModId));

        int updated = 0;
        for (JsonNode cls : array(read("class_mechanics.json"), "classes")) {
            String slug = txt(cls, "slug");
            UUID classId = classBySlug.get(slug);
            if (classId == null) {
                log.warn("class mechanics: no character_class row for slug '{}', skipping", slug);
                continue;
            }

            UUID castAbilityId = abilityBySlug.get(txt(cls, "spellcasting_ability_slug"));
            jdbc.update("UPDATE character_class SET hit_die = ?, is_spellcaster = ?, has_cantrips = ?, "
                            + "is_half_caster = ?, spellcasting_ability_id = ?, skill_choice_count = ?, "
                            + "skill_choice_any = ?, armor_proficiency_text = ?, weapon_proficiency_text = ?, "
                            + "tool_proficiency_text = ? WHERE class_id = ?",
                    intg(cls, "hit_die"), bool(cls, "is_spellcaster", false), bool(cls, "has_cantrips", false),
                    bool(cls, "is_half_caster", false), castAbilityId, orZero(intg(cls, "skill_choice_count")),
                    bool(cls, "skill_choice_any", false), txt(cls, "armor_proficiency_text"),
                    txt(cls, "weapon_proficiency_text"), txt(cls, "tool_proficiency_text"), classId);

            insertClassAbilities(classId, abilityBySlug, array(cls, "saving_throw_slugs"),
                    "INSERT INTO class_saving_throw(class_id, ability_score_id) VALUES (?,?) ON CONFLICT DO NOTHING");
            insertClassAbilities(classId, abilityBySlug, array(cls, "primary_ability_slugs"),
                    "INSERT INTO class_primary_ability(class_id, ability_score_id) VALUES (?,?) ON CONFLICT DO NOTHING");
            for (JsonNode sk : array(cls, "skill_option_slugs")) {
                UUID skillId = skillBySlug.get(sk.asText());
                if (skillId != null) {
                    jdbc.update("INSERT INTO class_skill_option(class_id, skill_id) VALUES (?,?) "
                            + "ON CONFLICT DO NOTHING", classId, skillId);
                }
            }
            updated++;
        }
        log.info("Seeded mechanics for {} classes", updated);
    }

    private void insertClassAbilities(UUID classId, Map<String, UUID> abilityBySlug,
                                      Iterable<JsonNode> abilitySlugs, String sql) {
        for (JsonNode node : abilitySlugs) {
            UUID abilityId = abilityBySlug.get(node.asText());
            if (abilityId != null) {
                jdbc.update(sql, classId, abilityId);
            }
        }
    }

    private Map<String, UUID> loadSlugIds(String sql) {
        Map<String, UUID> map = new HashMap<>();
        jdbc.query(sql, (RowCallbackHandler) rs -> map.put(rs.getString(1), UUID.fromString(rs.getString(2))));
        return map;
    }

    // ------------------------------------------------------------------ shared insert helpers

    private UUID insertMoneyValue(JsonNode cost) {
        if (cost == null || cost.isNull()) {
            return null;
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO money_value(money_value_id, amount, currency_id, copper_value, raw_text) "
                        + "VALUES (?,?,?,?,?)",
                id, dec(cost, "amount"), currencies.get(txt(cost, "currency_slug")),
                dec(cost, "copper_value"), txt(cost, "raw"));
        return id;
    }

    private UUID insertDiceFormula(JsonNode damage) {
        if (damage == null || damage.isNull()) {
            return null;
        }
        Integer count = intg(damage, "dice_count");
        Integer size = intg(damage, "die_size");
        Integer bonus = intg(damage, "bonus");
        String raw = txt(damage, "raw");
        if (count == null && size == null && bonus == null && raw == null) {
            return null;
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO dice_formula(dice_formula_id, dice_count, die_size, bonus, raw_text) "
                        + "VALUES (?,?,?,?,?)",
                id, count, size, bonus, raw);
        return id;
    }

    /** Parses Russian dice notation like "1к8" into a dice_formula row. */
    private UUID insertDiceFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Integer count = null;
        Integer size = null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*[кkдd]\\s*(\\d+)").matcher(text);
        if (m.find()) {
            count = Integer.valueOf(m.group(1));
            size = Integer.valueOf(m.group(2));
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO dice_formula(dice_formula_id, dice_count, die_size, bonus, raw_text) "
                        + "VALUES (?,?,?,?,?)",
                id, count, size, null, text);
        return id;
    }

    // ------------------------------------------------------------------ JSON / value helpers

    private JsonNode read(String fileName) {
        try {
            return mapper.readTree(new ClassPathResource("dnd_import/" + fileName).getInputStream());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read dnd_import/" + fileName, e);
        }
    }

    private static Iterable<JsonNode> array(JsonNode node, String field) {
        if (node == null) {
            return java.util.Collections.emptyList();
        }
        JsonNode arr = node.get(field);
        if (arr == null || !arr.isArray()) {
            return java.util.Collections.emptyList();
        }
        return arr;
    }

    private static JsonNode weight(JsonNode item) {
        return item == null ? null : item.get("weight");
    }

    private static String txt(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static Integer intg(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        return (v == null || v.isNull() || !v.isNumber()) ? null : v.asInt();
    }

    private static BigDecimal dec(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        return (v == null || v.isNull() || !v.isNumber()) ? null : v.decimalValue();
    }

    private static Boolean bool(JsonNode node, String field, boolean def) {
        if (node == null) {
            return def;
        }
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? def : v.asBoolean(def);
    }

    private static String orDefault(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private static int orZero(Integer value) {
        return value != null ? value : 0;
    }
}
