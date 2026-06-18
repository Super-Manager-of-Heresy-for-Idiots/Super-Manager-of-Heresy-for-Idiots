package com.dnd.app.service;

import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.dto.content.RuntimeMigrationReport;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.ProficiencySkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
import java.util.function.Predicate;

/**
 * One-time migration of existing runtime FK columns from legacy plural-table IDs to the
 * new content-model IDs (Phase 10 / roadmap R6).
 *
 * <p>The runtime entities already map to the new content tables (ability_score, currency,
 * spell, background, character_class, skill). On a fresh database every runtime id is
 * already a content id and is reported as {@code alreadyNew}. On a legacy database some
 * rows still carry old plural-table ids; those are remapped here.</p>
 *
 * <p>Mapping strategy: legacy rows carry no slug, so matching is by name — the legacy
 * {@code name} (and, for class/skill, the localized variants) is matched against the new
 * {@code nameEn}/{@code nameRu}. A unique match is applied; multiple candidates are
 * <b>ambiguous</b> and never auto-applied; no candidate is <b>unmapped</b>. Dry-run by
 * default; applying requires explicit backup confirmation. User data is never guessed.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuntimeDataMigrationService {

    private final JdbcTemplate jdbc;
    private final ContentCharacterClassRepository contentClassRepository;
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

        List<RuntimeMigrationReport.EntityMigration> entities = new ArrayList<>();
        entities.add(migrateClasses(dryRun));
        entities.add(migrateSkills(dryRun));

        int unresolved = entities.stream()
                .mapToInt(e -> e.getAmbiguous().size() + e.getUnmapped().size())
                .sum();
        notes.add("Незамапленных/неоднозначных строк (требуют ручного разбора, не тронуты): " + unresolved + ".");
        notes.addAll(postValidation());

        return RuntimeMigrationReport.builder()
                .dryRun(dryRun).entities(entities).notes(notes).build();
    }

    // --- class: legacy names read from the plural table via JDBC (3 name variants) ---

    private RuntimeMigrationReport.EntityMigration migrateClasses(boolean dryRun) {
        Map<String, List<ContentCharacterClass>> byName = indexByName(
                contentClassRepository.findAllByHomebrewIsNull(),
                ContentCharacterClass::getNameEn, ContentCharacterClass::getNameRu);
        return remapColumn(dryRun, "character_class_levels", "class_id",
                "character_class_levels.class_id -> character_class",
                contentClassRepository::existsById,
                legacyThreeNamesFrom("character_classes", "name", "name_engloc", "name_rusloc"),
                byName, ContentCharacterClass::getId, ContentCharacterClass::getNameEn);
    }

    // --- skill: legacy rows still backed by a JPA entity (3 name variants) ---

    private RuntimeMigrationReport.EntityMigration migrateSkills(boolean dryRun) {
        Map<String, List<ContentSkill>> byName = indexByName(
                contentSkillRepository.findAllByHomebrewIsNull(),
                ContentSkill::getNameEn, ContentSkill::getNameRu);
        return remapColumn(dryRun, "character_skill_proficiencies", "skill_id",
                "character_skill_proficiencies.skill_id -> skill",
                contentSkillRepository::existsById,
                legacyId -> legacySkillRepository.findById(legacyId)
                        .map(s -> new String[]{s.getName(), s.getNameEngloc(), s.getNameRusloc()})
                        .orElse(null),
                byName, ContentSkill::getId, ContentSkill::getNameEn);
    }

    // --- shared remap engine ---

    private <N> RuntimeMigrationReport.EntityMigration remapColumn(
            boolean dryRun, String runtimeTable, String runtimeColumn, String targetLabel,
            Predicate<UUID> contentExists, Function<UUID, String[]> legacyNames,
            Map<String, List<N>> contentByName, Function<N, UUID> idOf, Function<N, String> nameOf) {

        List<RuntimeMigrationReport.Mapping> mapped = new ArrayList<>();
        List<RuntimeMigrationReport.Mapping> ambiguous = new ArrayList<>();
        List<RuntimeMigrationReport.Mapping> unmapped = new ArrayList<>();
        int alreadyNew = 0;
        int rowsUpdated = 0;

        for (UUID legacyId : distinctIds(runtimeTable, runtimeColumn)) {
            if (contentExists.test(legacyId)) {
                alreadyNew++;
                continue;
            }
            String[] names = legacyNames.apply(legacyId);
            String legacyName = names != null && names.length > 0 ? names[0] : null;
            List<N> candidates = names == null ? List.of() : matchByNames(contentByName, names);

            if (candidates.size() == 1) {
                N target = candidates.get(0);
                mapped.add(mapping(legacyId, legacyName, idOf.apply(target), nameOf.apply(target), 1));
                if (!dryRun) {
                    rowsUpdated += jdbc.update(
                            "UPDATE " + runtimeTable + " SET " + runtimeColumn + " = ? WHERE " + runtimeColumn + " = ?",
                            idOf.apply(target), legacyId);
                }
            } else if (candidates.size() > 1) {
                ambiguous.add(mapping(legacyId, legacyName, null, null, candidates.size()));
            } else {
                unmapped.add(mapping(legacyId, legacyName, null, null, 0));
            }
        }

        return RuntimeMigrationReport.EntityMigration.builder()
                .target(targetLabel).alreadyNew(alreadyNew)
                .mapped(mapped).ambiguous(ambiguous).unmapped(unmapped)
                .rowsUpdated(rowsUpdated).build();
    }

    /** Reads several legacy name columns for an id, tolerating a legacy table absent on fresh DBs. */
    private Function<UUID, String[]> legacyThreeNamesFrom(String legacyTable, String... columns) {
        String select = String.join(", ", columns);
        return legacyId -> {
            try {
                return jdbc.query("SELECT " + select + " FROM " + legacyTable + " WHERE id = ?",
                        rs -> {
                            if (!rs.next()) {
                                return null;
                            }
                            String[] names = new String[columns.length];
                            for (int i = 0; i < columns.length; i++) {
                                names[i] = rs.getString(i + 1);
                            }
                            return names;
                        }, legacyId);
            } catch (DataAccessException e) {
                return null;
            }
        };
    }

    /** Counts, per column, runtime rows still pointing at content rows that do not exist. */
    private List<String> postValidation() {
        List<String> out = new ArrayList<>();
        out.add(danglingNote("character_class_levels", "class_id", "character_class", "class_id"));
        out.add(danglingNote("character_skill_proficiencies", "skill_id", "skill", "skill_id"));
        out.add(danglingNote("character_stats", "stat_type_id", "ability_score", "ability_score_id"));
        out.add(danglingNote("character_wallets", "currency_type_id", "currency", "currency_id"));
        out.add(danglingNote("wallet_transactions", "currency_type_id", "currency", "currency_id"));
        out.add(danglingNote("character_known_spells", "spell_id", "spell", "spell_id"));
        out.add(danglingNote("characters", "background_id", "background", "background_id"));
        return out;
    }

    private String danglingNote(String runtimeTable, String runtimeColumn, String contentTable, String contentPk) {
        Integer dangling = jdbc.queryForObject(
                "SELECT count(*) FROM " + runtimeTable + " t WHERE t." + runtimeColumn + " IS NOT NULL "
                        + "AND NOT EXISTS (SELECT 1 FROM " + contentTable + " c WHERE c." + contentPk
                        + " = t." + runtimeColumn + ")",
                Integer.class);
        return "Post-validation: " + runtimeTable + "." + runtimeColumn + " dangling=" + dangling
                + " (должно быть 0 после полной миграции).";
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
