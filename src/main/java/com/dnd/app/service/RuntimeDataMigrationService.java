package com.dnd.app.service;

import com.dnd.app.domain.CharacterClass;
import com.dnd.app.domain.ProficiencySkill;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.dto.content.RuntimeMigrationReport;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.CharacterClassRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.ProficiencySkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * One-time migration of existing runtime FK columns from legacy plural-table IDs to the
 * new content-model IDs (Phase 10). Only {@code character_class_levels.class_id} and
 * {@code character_skill_proficiencies.skill_id} require remapping — stat/currency/spell/
 * background already reference the new tables.
 *
 * <p>Mapping strategy: legacy rows carry no slug, so matching is by name (name / nameEngloc
 * / nameRusloc ↔ new nameEn / nameRu). A unique name match is applied; multiple candidates
 * are reported as <b>ambiguous</b> and never auto-applied; no candidate is <b>unmapped</b>.
 * Dry-run by default; applying requires an explicit backup confirmation. User data is never
 * silently guessed.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimeDataMigrationService {

    private final JdbcTemplate jdbc;
    private final ContentCharacterClassRepository contentClassRepository;
    private final CharacterClassRepository legacyClassRepository;
    private final ContentSkillRepository contentSkillRepository;
    private final ProficiencySkillRepository legacySkillRepository;

    @Transactional
    public RuntimeMigrationReport migrate(boolean dryRun, boolean confirmBackup) {
        if (!dryRun && !confirmBackup) {
            throw new BadRequestException(
                    "Применение миграции требует подтверждения бэкапа (confirmBackup=true)");
        }
        List<String> notes = new ArrayList<>();
        if (dryRun) {
            notes.add("DRY-RUN: изменения не записаны. Проверьте mapped/ambiguous/unmapped перед применением.");
        } else {
            notes.add("ВНИМАНИЕ: применение выполнено. Убедитесь, что бэкап БД был сделан до запуска.");
        }

        RuntimeMigrationReport.EntityMigration classes = migrateClasses(dryRun);
        RuntimeMigrationReport.EntityMigration skills = migrateSkills(dryRun);

        notes.add("Незамапленных/неоднозначных class_id: "
                + (classes.getAmbiguous().size() + classes.getUnmapped().size())
                + "; skill_id: " + (skills.getAmbiguous().size() + skills.getUnmapped().size())
                + ". Эти строки требуют ручного разбора и не тронуты.");
        notes.add(postValidation());

        return RuntimeMigrationReport.builder()
                .dryRun(dryRun).classes(classes).skills(skills).notes(notes).build();
    }

    private RuntimeMigrationReport.EntityMigration migrateClasses(boolean dryRun) {
        Map<String, List<ContentCharacterClass>> byName = indexByName(
                contentClassRepository.findAllByHomebrewIsNull(),
                ContentCharacterClass::getNameEn, ContentCharacterClass::getNameRu);

        List<RuntimeMigrationReport.Mapping> mapped = new ArrayList<>();
        List<RuntimeMigrationReport.Mapping> ambiguous = new ArrayList<>();
        List<RuntimeMigrationReport.Mapping> unmapped = new ArrayList<>();
        int alreadyNew = 0;
        int rowsUpdated = 0;

        for (UUID legacyId : distinctIds("character_class_levels", "class_id")) {
            if (contentClassRepository.existsById(legacyId)) {
                alreadyNew++;
                continue;
            }
            Optional<CharacterClass> legacy = legacyClassRepository.findById(legacyId);
            String legacyName = legacy.map(CharacterClass::getName).orElse(null);
            List<ContentCharacterClass> candidates = legacy
                    .map(c -> matchByNames(byName, c.getName(), c.getNameEngloc(), c.getNameRusloc()))
                    .orElse(List.of());

            if (candidates.size() == 1) {
                ContentCharacterClass target = candidates.get(0);
                RuntimeMigrationReport.Mapping m = mapping(legacyId, legacyName, target.getId(),
                        target.getNameEn(), 1);
                if (!dryRun) {
                    rowsUpdated += jdbc.update(
                            "UPDATE character_class_levels SET class_id = ? WHERE class_id = ?",
                            target.getId(), legacyId);
                }
                mapped.add(m);
            } else if (candidates.size() > 1) {
                ambiguous.add(mapping(legacyId, legacyName, null, null, candidates.size()));
            } else {
                unmapped.add(mapping(legacyId, legacyName, null, null, 0));
            }
        }

        return RuntimeMigrationReport.EntityMigration.builder()
                .target("character_class_levels.class_id -> character_class")
                .alreadyNew(alreadyNew).mapped(mapped).ambiguous(ambiguous).unmapped(unmapped)
                .rowsUpdated(rowsUpdated).build();
    }

    private RuntimeMigrationReport.EntityMigration migrateSkills(boolean dryRun) {
        Map<String, List<ContentSkill>> byName = indexByName(
                contentSkillRepository.findAllByHomebrewIsNull(),
                ContentSkill::getNameEn, ContentSkill::getNameRu);

        List<RuntimeMigrationReport.Mapping> mapped = new ArrayList<>();
        List<RuntimeMigrationReport.Mapping> ambiguous = new ArrayList<>();
        List<RuntimeMigrationReport.Mapping> unmapped = new ArrayList<>();
        int alreadyNew = 0;
        int rowsUpdated = 0;

        for (UUID legacyId : distinctIds("character_skill_proficiencies", "skill_id")) {
            if (contentSkillRepository.existsById(legacyId)) {
                alreadyNew++;
                continue;
            }
            Optional<ProficiencySkill> legacy = legacySkillRepository.findById(legacyId);
            String legacyName = legacy.map(ProficiencySkill::getName).orElse(null);
            List<ContentSkill> candidates = legacy
                    .map(s -> matchByNames(byName, s.getName(), s.getNameEngloc(), s.getNameRusloc()))
                    .orElse(List.of());

            if (candidates.size() == 1) {
                ContentSkill target = candidates.get(0);
                RuntimeMigrationReport.Mapping m = mapping(legacyId, legacyName, target.getId(),
                        target.getNameEn(), 1);
                if (!dryRun) {
                    rowsUpdated += jdbc.update(
                            "UPDATE character_skill_proficiencies SET skill_id = ? WHERE skill_id = ?",
                            target.getId(), legacyId);
                }
                mapped.add(m);
            } else if (candidates.size() > 1) {
                ambiguous.add(mapping(legacyId, legacyName, null, null, candidates.size()));
            } else {
                unmapped.add(mapping(legacyId, legacyName, null, null, 0));
            }
        }

        return RuntimeMigrationReport.EntityMigration.builder()
                .target("character_skill_proficiencies.skill_id -> skill")
                .alreadyNew(alreadyNew).mapped(mapped).ambiguous(ambiguous).unmapped(unmapped)
                .rowsUpdated(rowsUpdated).build();
    }

    /** Counts runtime rows still pointing at content rows that do not exist. */
    private String postValidation() {
        Integer danglingClasses = jdbc.queryForObject(
                "SELECT count(*) FROM character_class_levels ccl "
                        + "WHERE NOT EXISTS (SELECT 1 FROM character_class cc WHERE cc.class_id = ccl.class_id) "
                        + "AND NOT EXISTS (SELECT 1 FROM character_classes oc WHERE oc.id = ccl.class_id)",
                Integer.class);
        Integer danglingSkills = jdbc.queryForObject(
                "SELECT count(*) FROM character_skill_proficiencies csp "
                        + "WHERE NOT EXISTS (SELECT 1 FROM skill s WHERE s.skill_id = csp.skill_id) "
                        + "AND NOT EXISTS (SELECT 1 FROM proficiency_skills ps WHERE ps.id = csp.skill_id)",
                Integer.class);
        return "Post-validation: dangling class_id rows=" + danglingClasses
                + ", dangling skill_id rows=" + danglingSkills
                + " (должны быть 0 после полной миграции).";
    }

    // --- helpers ---

    private List<UUID> distinctIds(String table, String column) {
        return jdbc.queryForList("SELECT DISTINCT " + column + " FROM " + table
                + " WHERE " + column + " IS NOT NULL", UUID.class);
    }

    @SafeVarargs
    private <T> Map<String, List<T>> indexByName(List<T> items, Function<T, String>... nameAccessors) {
        Map<String, List<T>> index = new LinkedHashMap<>();
        for (T item : items) {
            for (Function<T, String> accessor : nameAccessors) {
                String name = normalize(accessor.apply(item));
                if (name != null) {
                    index.computeIfAbsent(name, k -> new ArrayList<>()).add(item);
                }
            }
        }
        return index;
    }

    private <T> List<T> matchByNames(Map<String, List<T>> byName, String... names) {
        List<T> result = new ArrayList<>();
        for (String name : names) {
            String norm = normalize(name);
            if (norm != null && byName.containsKey(norm)) {
                for (T candidate : byName.get(norm)) {
                    if (!result.contains(candidate)) {
                        result.add(candidate);
                    }
                }
            }
        }
        return result;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private RuntimeMigrationReport.Mapping mapping(UUID legacyId, String legacyName, UUID newId,
                                                   String newName, int candidateCount) {
        return RuntimeMigrationReport.Mapping.builder()
                .legacyId(legacyId).legacyName(legacyName).newId(newId).newName(newName)
                .candidateCount(candidateCount).build();
    }
}
