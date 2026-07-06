package com.dnd.app.integration;

import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.content.ContentCharacterCreationResponse;
import com.dnd.app.dto.featurerule.SpellCastResult;
import com.dnd.app.dto.request.CreateContentCharacterRequest;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.CharacterSpellbookService;
import com.dnd.app.service.ContentCharacterCreationService;
import com.dnd.app.service.ContentReferenceService;
import com.dnd.app.service.ReferenceDataService;
import com.dnd.app.service.SpellCastService;
import com.dnd.app.service.SpellRuleBackfillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Golden end-to-end scenario for the S2 spell-stack absorption, with the feature-rules runtime ON
 * (runtime + spells + actions + effects flags). Against real PHB content on a throwaway Postgres:
 * the backfill turns the 056–062 spell columns into approved SPELL-owned rules, and casting a known
 * spell through {@code SpellCastService} spends the slot and returns the structured roll plan the
 * shared engine computed. This is the acceptance surface the unit mocks cannot provide.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SpellCastRuntimeIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-test-secret-test-secret-1234");
        registry.add("app.feature-rules.runtime-enabled", () -> "true");
        registry.add("app.feature-rules.spells-enabled", () -> "true");
        registry.add("app.feature-rules.actions-enabled", () -> "true");
        registry.add("app.feature-rules.effects-enabled", () -> "true");
    }

    @Autowired private SpellRuleBackfillService spellRuleBackfillService;
    @Autowired private SpellCastService spellCastService;
    @Autowired private CharacterSpellbookService spellbookService;
    @Autowired private ContentCharacterCreationService creationService;
    @Autowired private ContentReferenceService contentReferenceService;
    @Autowired private ReferenceDataService referenceDataService;
    @Autowired private UserRepository userRepository;
    @Autowired private StatTypeRepository statTypeRepository;
    @Autowired private ContentCharacterClassRepository contentClassRepository;
    @Autowired private ContentSkillRepository contentSkillRepository;
    @Autowired private ClassLevelRewardGroupRepository rewardGroupRepository;
    @Autowired private PlayerCharacterRepository characterRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    @Test
    @Transactional
    void cantripCastRunsThroughTheSharedRulesEngineWithoutASlot() {
        spellRuleBackfillService.backfill(true);

        ContentCharacterClass anyClass = pickCreatableClass(false);
        assertThat(anyClass).as("a vanilla class creatable without reward selections").isNotNull();
        UUID characterId = createCharacter(anyClass, "it-cantrip-caster");

        UUID cantripId = firstSpellId(
                "SELECT s.spell_id FROM spell s WHERE s.level = 0 AND s.homebrew_id IS NULL "
                        + "AND s.casting_action_slug = 'action' AND EXISTS (SELECT 1 FROM spell_damage d "
                        + "WHERE d.spell_id = s.spell_id AND d.dice IS NOT NULL AND d.dice <> '') "
                        + "ORDER BY (s.is_attack_roll = TRUE) DESC, s.slug LIMIT 1");
        assumeTrue(cantripId != null, "no damaging cantrip in the seeded content");
        spellbookService.learn(characterId, cantripId);

        PlayerCharacter caster = characterRepository.findById(characterId).orElseThrow();
        SpellCastResult result = spellCastService.cast(caster, cantripId, null, null);

        assertThat(result.getSlotLevelUsed()).isNull(); // cantrips never touch slots
        assertThat(result.getSlots()).isNull();
        assertThat(result.getPlan().getDamages())
                .as("the backfilled damage rule reaches the plan")
                .isNotEmpty();
        assertThat(result.getPlan().getDamages().get(0).getDiceExpression()).isNotBlank();
        entityManager.flush(); // the use log is written via JPA but counted via plain SQL below
        assertThat(useLogCount(characterId)).isEqualTo(1);
    }

    @Test
    @Transactional
    void leveledCastSpendsExactlyOneSlotOfTheSpellLevel() {
        spellRuleBackfillService.backfill(true);

        ContentCharacterClass caster = pickCreatableClass(true);
        assumeTrue(caster != null,
                "no vanilla caster class with level-1 slots is creatable without reward selections");
        UUID characterId = createCharacter(caster, "it-slot-caster");

        UUID spellId = firstSpellId(
                "SELECT s.spell_id FROM spell s WHERE s.level = 1 AND s.homebrew_id IS NULL "
                        + "AND s.casting_action_slug = 'action' AND (EXISTS (SELECT 1 FROM spell_damage d "
                        + "WHERE d.spell_id = s.spell_id AND d.dice IS NOT NULL AND d.dice <> '') "
                        + "OR EXISTS (SELECT 1 FROM spell_healing h WHERE h.spell_id = s.spell_id)) "
                        + "ORDER BY s.slug LIMIT 1");
        assertThat(spellId).as("a level-1 spell with structured damage/healing").isNotNull();
        spellbookService.learn(characterId, spellId);

        PlayerCharacter pc = characterRepository.findById(characterId).orElseThrow();
        SpellCastResult result = spellCastService.cast(pc, spellId, null, null);

        assertThat(result.getSlotLevelUsed()).isEqualTo(1);
        assertThat(result.getSlots()).isNotNull();
        assertThat(result.getSlots().getLevels())
                .filteredOn(l -> l.getSpellLevel() == 1)
                .singleElement()
                .satisfies(l -> assertThat(l.getExpended()).isEqualTo(1));
        assertThat(result.getPlan().getDamages().size() + result.getPlan().getHealings().size())
                .isGreaterThan(0);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UUID firstSpellId(String sql) {
        List<UUID> ids = jdbc.queryForList(sql, UUID.class);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private int useLogCount(UUID characterId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM feature_use_log WHERE character_id = ?", Integer.class, characterId);
        return n != null ? n : 0;
    }

    /** A vanilla class creatable without reward selections; optionally requiring level-1 spell slots. */
    private ContentCharacterClass pickCreatableClass(boolean needsLevel1Slots) {
        for (ContentCharacterClass c : contentClassRepository.findAll()) {
            if (c.getHomebrew() != null) {
                continue;
            }
            List<ClassLevelRewardGroup> level1 = rewardGroupRepository
                    .findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(c.getId(), 1);
            boolean requiresChoice = level1.stream().anyMatch(g ->
                    "CHOICE".equalsIgnoreCase(g.getGroupKind())
                            && g.getChooseMin() != null && g.getChooseMin() > 0);
            if (requiresChoice) {
                continue;
            }
            if (needsLevel1Slots && !hasLevel1Slots(c.getId())) {
                continue;
            }
            return c;
        }
        return null;
    }

    private boolean hasLevel1Slots(UUID classId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM class_progression_value cpv "
                        + "JOIN class_progression_column cpc "
                        + "  ON cpc.class_progression_column_id = cpv.class_progression_column_id "
                        + "WHERE cpv.class_id = ? AND cpv.class_level = 1 "
                        + "  AND cpc.slug LIKE 'yacheyki-zaklinaniy%' AND cpv.value_numeric > 0",
                Integer.class, classId);
        return n != null && n > 0;
    }

    private UUID createCharacter(ContentCharacterClass chosen, String username) {
        User owner = userRepository.save(User.builder()
                .username(username)
                .email(username + "@example.test")
                .passwordHash("not-a-real-hash")
                .role(Role.PLAYER)
                .build());

        List<StatType> abilities = statTypeRepository.findByHomebrewIsNull();
        int[] standardArray = {15, 14, 13, 12, 10, 8};
        List<CreateContentCharacterRequest.AbilityScoreEntry> scores = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            scores.add(CreateContentCharacterRequest.AbilityScoreEntry.builder()
                    .statId(abilities.get(i).getId())
                    .baseValue(standardArray[i])
                    .build());
        }
        int skillCount = chosen.getSkillChoiceCount() != null ? chosen.getSkillChoiceCount() : 0;
        List<UUID> chosenSkillIds = (Boolean.TRUE.equals(chosen.getSkillChoiceAny())
                ? contentSkillRepository.findAllByHomebrewIsNull().stream()
                : chosen.getSkillOptions().stream())
                .limit(skillCount)
                .map(ContentSkill::getId)
                .toList();

        UUID raceId = contentReferenceService.getVanillaSpecies("ru").get(0).getId();
        UUID backgroundId = referenceDataService.getVanillaBackgrounds("ru").get(0).getId();

        ContentCharacterCreationResponse resp = creationService.createVanillaCharacter(
                CreateContentCharacterRequest.builder()
                        .name("Spell Runtime Hero")
                        .classId(chosen.getId())
                        .raceId(raceId)
                        .backgroundId(backgroundId)
                        .level(1)
                        .abilityScores(scores)
                        .scoreMethod("STANDARD_ARRAY")
                        .chosenSkillIds(chosenSkillIds)
                        .build(),
                owner.getUsername());
        return resp.getId();
    }
}
