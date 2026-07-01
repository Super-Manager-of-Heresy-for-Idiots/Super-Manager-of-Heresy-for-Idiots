package com.dnd.app.service;

import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGrantAbilityScore;
import com.dnd.app.domain.content.ClassLevelRewardGrantFeature;
import com.dnd.app.domain.content.ClassLevelRewardGrantSkillProficiency;
import com.dnd.app.domain.content.ClassLevelRewardGrantSpell;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.content.grant.GrantType;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.ClassLevelRewardGrantAbilityScoreRepository;
import com.dnd.app.repository.ClassLevelRewardGrantFeatureRepository;
import com.dnd.app.repository.ClassLevelRewardGrantRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSkillProficiencyRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSpellRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ClassLevelRewardOptionRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Idempotent backfill of the per-level reward groups that drive the content level-up
 * flow, derived from {@code dnd_import/classes.normalized.json}. Without this data a
 * level-up only bumps HP, because there are no reward groups to surface or apply.
 *
 * <p>For every core class and every gained level 2..20 it seeds, from the authoritative
 * {@code klassovye-umeniya} progression column and the spell progression columns:</p>
 * <ul>
 *   <li>base class features &rarr; AUTO group + FEATURE grant (also ensures the
 *       {@code class_feature} row exists);</li>
 *   <li>"Увеличение характеристик" &rarr; mandatory CHOICE group + ABILITY_SCORE grant
 *       (+1/+1 to two distinct abilities);</li>
 *   <li>features whose text confers Экспертность &rarr; mandatory CHOICE group +
 *       SKILL_PROFICIENCY grant ({@code grantsExpertise=true});</li>
 *   <li>increases in prepared spells / cantrips &rarr; mandatory CHOICE group + SPELL
 *       grant forcing the player to pick the new spells.</li>
 * </ul>
 *
 * <p>The subclass-choice group (level 3) is owned by {@link ClassRewardSeedService};
 * "Подкласс" features are skipped here. Idempotency key: a (class, level) is skipped
 * entirely once any FEATURE/ABILITY_SCORE/SKILL_PROFICIENCY/SPELL group exists for it.
 * Never touches homebrew. Safe to run on every startup.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassLevelRewardSeedService {

    private static final String SOURCE_FILE = "dnd_import/classes.normalized.json";
    private static final int MAX_LEVEL = 20;

    private static final String COL_FEATURES = "klassovye-umeniya";
    private static final String COL_PREPARED = "podgotovlennye-zaklinaniya";
    private static final String COL_CANTRIPS = "zagovory";

    private static final Set<String> MANAGED_GRANTS = Set.of(
            GrantType.FEATURE.name(), GrantType.ABILITY_SCORE.name(),
            GrantType.SKILL_PROFICIENCY.name(), GrantType.SPELL.name());

    private final ObjectMapper mapper;
    private final ContentCharacterClassRepository classRepo;
    private final ClassFeatureRepository featureRepo;
    private final StatTypeRepository statTypeRepo;
    private final ClassLevelRewardGroupRepository groupRepo;
    private final ClassLevelRewardOptionRepository optionRepo;
    private final ClassLevelRewardGrantRepository grantRepo;
    private final ClassLevelRewardGrantFeatureRepository featureGrantRepo;
    private final ClassLevelRewardGrantAbilityScoreRepository abilityGrantRepo;
    private final ClassLevelRewardGrantSkillProficiencyRepository skillGrantRepo;
    private final ClassLevelRewardGrantSpellRepository spellGrantRepo;

    @Transactional
    public void seedCoreLevelRewards() {
        JsonNode root = read();
        if (root == null || !root.isArray()) {
            log.warn("Level-reward backfill skipped: {} missing or not an array", SOURCE_FILE);
            return;
        }
        List<StatType> abilities = statTypeRepo.findByHomebrewIsNull();

        int classesSeeded = 0;
        for (JsonNode classNode : root) {
            String slug = text(classNode, "slug");
            if (slug == null) {
                continue;
            }
            ContentCharacterClass clazz = classRepo.findBySlugAndHomebrewIsNull(slug).orElse(null);
            if (clazz == null) {
                continue;
            }
            if (seedClass(clazz, classNode, abilities)) {
                classesSeeded++;
            }
        }
        log.info("Per-level reward backfill complete: {} core classes touched", classesSeeded);
    }

    private boolean seedClass(ContentCharacterClass clazz, JsonNode classNode, List<StatType> abilities) {
        Map<Integer, List<String>> featureNamesByLevel = new HashMap<>();
        Map<Integer, Integer> preparedByLevel = new HashMap<>();
        Map<Integer, Integer> cantripsByLevel = new HashMap<>();
        parseProgression(classNode, featureNamesByLevel, preparedByLevel, cantripsByLevel);

        Map<String, JsonNode> featureInfo = indexFeatures(classNode);

        boolean touched = false;
        for (int level = 2; level <= MAX_LEVEL; level++) {
            if (alreadySeeded(clazz.getId(), level)) {
                continue;
            }
            int sortOrder = 10; // leave room for the subclass-choice group (sort 0) at level 3

            List<String> names = featureNamesByLevel.getOrDefault(level, List.of());
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                if (normalize(name).contains("подкласс")) {
                    continue; // owned by ClassRewardSeedService
                }
                JsonNode info = featureInfo.get(featureKey(level, name));
                String description = info != null ? text(info, "text") : null;
                ClassFeature feature = ensureFeature(clazz, level, i, name, info, description);

                if (normalize(name).equals("увеличение характеристик")) {
                    seedAsiGroup(clazz, level, feature, sortOrder++, abilities);
                } else if (description != null && description.contains("Экспертность")) {
                    seedExpertiseGroup(clazz, level, feature, sortOrder++, expertiseCount(description));
                } else {
                    seedFeatureAutoGroup(clazz, level, feature, sortOrder++);
                }
                touched = true;
            }

            int preparedDelta = preparedByLevel.getOrDefault(level, 0)
                    - preparedByLevel.getOrDefault(level - 1, 0);
            if (preparedDelta > 0) {
                seedSpellChoiceGroup(clazz, level, preparedDelta, false, sortOrder++);
                touched = true;
            }
            int cantripDelta = cantripsByLevel.getOrDefault(level, 0)
                    - cantripsByLevel.getOrDefault(level - 1, 0);
            if (cantripDelta > 0) {
                seedSpellChoiceGroup(clazz, level, cantripDelta, true, sortOrder++);
                touched = true;
            }
        }
        return touched;
    }

    // ------------------------------------------------------------------ parsing

    private void parseProgression(JsonNode classNode,
                                  Map<Integer, List<String>> featureNamesByLevel,
                                  Map<Integer, Integer> preparedByLevel,
                                  Map<Integer, Integer> cantripsByLevel) {
        JsonNode progression = classNode.get("progression");
        if (progression == null || !progression.isArray()) {
            return;
        }
        for (JsonNode row : progression) {
            Integer level = row.hasNonNull("level") ? row.get("level").asInt() : null;
            if (level == null) {
                continue;
            }
            JsonNode values = row.get("values");
            if (values == null || !values.isArray()) {
                continue;
            }
            for (JsonNode v : values) {
                String col = text(v, "column_slug");
                if (col == null) {
                    continue;
                }
                switch (col) {
                    case COL_FEATURES -> featureNamesByLevel.put(level, splitFeatureNames(text(v, "value_raw")));
                    case COL_PREPARED -> preparedByLevel.put(level, numeric(v));
                    case COL_CANTRIPS -> cantripsByLevel.put(level, numeric(v));
                    default -> { }
                }
            }
        }
    }

    private List<String> splitFeatureNames(String raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String part : raw.split(",")) {
            String name = part.trim();
            if (name.isEmpty() || "—".equals(name) || "-".equals(name)) {
                continue;
            }
            names.add(name);
        }
        return names;
    }

    /** Index features[] by (level + normalized title) so a progression name resolves slug + text. */
    private Map<String, JsonNode> indexFeatures(JsonNode classNode) {
        Map<String, JsonNode> byKey = new HashMap<>();
        JsonNode features = classNode.get("features");
        if (features == null || !features.isArray()) {
            return byKey;
        }
        for (JsonNode f : features) {
            Integer level = f.hasNonNull("level") ? f.get("level").asInt() : null;
            String title = text(f, "title");
            if (level == null || title == null) {
                continue;
            }
            byKey.putIfAbsent(featureKey(level, title), f);
        }
        return byKey;
    }

    // ------------------------------------------------------------------ class_feature

    private ClassFeature ensureFeature(ContentCharacterClass clazz, int level, int idx,
                                       String name, JsonNode info, String description) {
        String slug = info != null && text(info, "slug") != null
                ? text(info, "slug")
                : clazz.getSlug() + "-l" + level + "-f" + idx;
        return featureRepo.findByCharacterClassIdAndSubclassIsNullAndSlug(clazz.getId(), slug)
                .orElseGet(() -> featureRepo.save(ClassFeature.builder()
                        .characterClass(clazz)
                        .subclass(null)
                        .slug(slug)
                        .level(level)
                        .sortOrder(idx)
                        .title(name)
                        .description(description)
                        .build()));
    }

    // ------------------------------------------------------------------ group seeders

    private void seedFeatureAutoGroup(ContentCharacterClass clazz, int level, ClassFeature feature, int sortOrder) {
        ClassLevelRewardGroup group = groupRepo.save(ClassLevelRewardGroup.builder()
                .characterClass(clazz)
                .classFeature(feature)
                .classLevel(level)
                .groupKind("AUTO")
                .promptRu(feature.getTitle())
                .description(feature.getDescription())
                .chooseMin(0)
                .chooseMax(0)
                .repeatable(false)
                .sortOrder(sortOrder)
                .build());
        ClassLevelRewardGrant grant = grantRepo.save(ClassLevelRewardGrant.builder()
                .rewardGroup(group)
                .grantType(GrantType.FEATURE.name())
                .labelRu(feature.getTitle())
                .description(feature.getDescription())
                .sortOrder(0)
                .build());
        featureGrantRepo.save(ClassLevelRewardGrantFeature.builder()
                .grant(grant)
                .classFeature(feature)
                .build());
    }

    private void seedAsiGroup(ContentCharacterClass clazz, int level, ClassFeature feature,
                              int sortOrder, List<StatType> abilities) {
        ClassLevelRewardGroup group = groupRepo.save(ClassLevelRewardGroup.builder()
                .characterClass(clazz)
                .classFeature(feature)
                .classLevel(level)
                .groupKind("CHOICE")
                .promptRu("Увеличение характеристик: выберите две разные характеристики (+1 к каждой)")
                .promptEn("Ability Score Improvement")
                .description(feature.getDescription())
                .chooseMin(1)
                .chooseMax(1)
                .repeatable(false)
                .sortOrder(sortOrder)
                .build());
        ClassLevelRewardOption option = optionRepo.save(ClassLevelRewardOption.builder()
                .rewardGroup(group)
                .optionKey("asi-plus1-plus1")
                .labelRu("+1 / +1 к двум характеристикам")
                .labelEn("+1 / +1 to two abilities")
                .recommended(false)
                .sortOrder(0)
                .build());
        ClassLevelRewardGrant grant = grantRepo.save(ClassLevelRewardGrant.builder()
                .rewardOption(option)
                .grantType(GrantType.ABILITY_SCORE.name())
                .labelRu(feature.getTitle())
                .sortOrder(0)
                .build());
        abilityGrantRepo.save(ClassLevelRewardGrantAbilityScore.builder()
                .grant(grant)
                .chooseCount(2)
                .bonusPerChoice(1)
                .totalBonus(2)
                .maxScore(20)
                .abilityOptions(new HashSet<>(abilities))
                .build());
    }

    private void seedExpertiseGroup(ContentCharacterClass clazz, int level, ClassFeature feature,
                                    int sortOrder, int count) {
        ClassLevelRewardGroup group = groupRepo.save(ClassLevelRewardGroup.builder()
                .characterClass(clazz)
                .classFeature(feature)
                .classLevel(level)
                .groupKind("CHOICE")
                .promptRu("Экспертность: выберите навык" + (count > 1 ? "и (" + count + ")" : "")
                        + ", которым вы уже владеете")
                .promptEn("Expertise")
                .description(feature.getDescription())
                .chooseMin(1)
                .chooseMax(1)
                .repeatable(false)
                .sortOrder(sortOrder)
                .build());
        ClassLevelRewardOption option = optionRepo.save(ClassLevelRewardOption.builder()
                .rewardGroup(group)
                .optionKey("expertise")
                .labelRu("Экспертность")
                .labelEn("Expertise")
                .recommended(false)
                .sortOrder(0)
                .build());
        ClassLevelRewardGrant grant = grantRepo.save(ClassLevelRewardGrant.builder()
                .rewardOption(option)
                .grantType(GrantType.SKILL_PROFICIENCY.name())
                .labelRu(feature.getTitle())
                .sortOrder(0)
                .build());
        skillGrantRepo.save(ClassLevelRewardGrantSkillProficiency.builder()
                .grant(grant)
                .chooseCount(count)
                .anySkill(true)
                .grantsExpertise(true)
                .build());
    }

    private void seedSpellChoiceGroup(ContentCharacterClass clazz, int level, int count,
                                      boolean cantrip, int sortOrder) {
        ClassLevelRewardGroup group = groupRepo.save(ClassLevelRewardGroup.builder()
                .characterClass(clazz)
                .classLevel(level)
                .groupKind("CHOICE")
                .promptRu((cantrip ? "Выберите новые заговоры" : "Выберите новые подготовленные заклинания")
                        + " (+" + count + ")")
                .promptEn(cantrip ? "Choose cantrips" : "Choose prepared spells")
                .chooseMin(1)
                .chooseMax(1)
                .repeatable(false)
                .sortOrder(sortOrder)
                .build());
        ClassLevelRewardOption option = optionRepo.save(ClassLevelRewardOption.builder()
                .rewardGroup(group)
                .optionKey(cantrip ? "cantrips" : "prepared-spells")
                .labelRu(cantrip ? "Заговоры (+" + count + ")" : "Подготовленные заклинания (+" + count + ")")
                .labelEn(cantrip ? "Cantrips (+" + count + ")" : "Prepared spells (+" + count + ")")
                .recommended(false)
                .sortOrder(0)
                .build());
        ClassLevelRewardGrant grant = grantRepo.save(ClassLevelRewardGrant.builder()
                .rewardOption(option)
                .grantType(GrantType.SPELL.name())
                .labelRu(cantrip ? "Заговоры класса" : "Заклинания класса")
                .sortOrder(0)
                .build());
        spellGrantRepo.save(ClassLevelRewardGrantSpell.builder()
                .grant(grant)
                .spellLevel(cantrip ? 0 : null)
                .chooseCount(count)
                .rawFilterText(cantrip
                        ? "Заговоры из списка заклинаний класса"
                        : "Заклинания из списка заклинаний класса доступного вам уровня")
                .build());
    }

    // ------------------------------------------------------------------ helpers

    private boolean alreadySeeded(UUID classId, int level) {
        List<ClassLevelRewardGroup> groups =
                groupRepo.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(classId, level);
        for (ClassLevelRewardGroup g : groups) {
            if (hasManagedGrant(g.getGrants())) {
                return true;
            }
            for (ClassLevelRewardOption o : g.getOptions()) {
                if (hasManagedGrant(o.getGrants())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasManagedGrant(List<ClassLevelRewardGrant> grants) {
        if (grants == null) {
            return false;
        }
        for (ClassLevelRewardGrant gr : grants) {
            if (gr.getGrantType() != null && MANAGED_GRANTS.contains(gr.getGrantType().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** Rogue/Bard-style Expertise grants two skills; everything else grants one. */
    private int expertiseCount(String description) {
        String n = normalize(description);
        if (n.contains("два навык") || n.contains("двух навык") || n.contains("двумя навык")) {
            return 2;
        }
        return 1;
    }

    private String featureKey(int level, String title) {
        return level + "::" + normalize(title);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private Integer numeric(JsonNode value) {
        JsonNode n = value.get("value_numeric");
        return n != null && n.isNumber() ? n.asInt() : 0;
    }

    private String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return n != null && !n.isNull() ? n.asText() : null;
    }

    private JsonNode read() {
        try {
            return mapper.readTree(new ClassPathResource(SOURCE_FILE).getInputStream());
        } catch (IOException e) {
            log.warn(
                    "ClassLevelRewardSeedService#read failed: operation=class-level-reward-source-read, sourceFile={}",
                    SOURCE_FILE,
                    e);
            return null;
        }
    }
}
