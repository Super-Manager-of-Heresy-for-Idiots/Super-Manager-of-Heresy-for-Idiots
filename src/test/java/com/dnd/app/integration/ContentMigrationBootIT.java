package com.dnd.app.integration;

import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.content.ContentCharacterCreationResponse;
import com.dnd.app.dto.request.CreateContentCharacterRequest;
import com.dnd.app.dto.response.ProficiencySkillResponse;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterSkillProficiencyRepository;
import com.dnd.app.repository.CharacterStatRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.dto.featurerule.FeatureRuleBackfillResult;
import com.dnd.app.dto.featurerule.FeatureRuleCoverageReport;
import com.dnd.app.dto.featurerule.SpellRuleBackfillResult;
import com.dnd.app.service.ContentCharacterCreationService;
import com.dnd.app.service.ContentReferenceService;
import com.dnd.app.service.FeatureRuleBackfillService;
import com.dnd.app.service.FeatureRuleCoverageService;
import com.dnd.app.service.ReferenceDataService;
import com.dnd.app.service.SpellRuleBackfillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

/**
 * Real integration test for the content migration. Boots the full Spring context against a
 * throwaway Postgres (Testcontainers, needs Docker), which exercises what the Mockito unit
 * suite never could and where the migration kept failing in production:
 *   - the entire Liquibase changelog (master.xml) applied to a fresh DB — catches
 *     changeset/FK failures like the 063 deploy crash before deploy, not after;
 *   - Hibernate ddl-auto=validate matching every @Entity to the migrated schema — catches a
 *     dropped/renamed table whose entity was left behind;
 *   - DndContentLoader importing the PHB bundle against the real schema;
 *   - the frontend-facing reference reads returning data instead of a 500 from a dangling
 *     lazy proxy (the /api/reference/skills regression);
 *   - a full character creation through the new content model, hitting the real 063 FKs
 *     (character_class_levels.class_id -> character_class, character_skill_proficiencies.skill_id
 *     -> skill, character_stats.stat_type_id -> ability_score).
 *
 * Passing this is not a 100% guarantee of correctness — it is the behavior-level verification
 * layer that was missing while "compiles / unit-green" was being treated as done. Named *IT so
 * it is excluded from the default `test`/`build`/`bootJar` and only runs via `gradlew integrationTest`.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ContentMigrationBootIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-test-secret-test-secret-1234");
    }

    @Autowired private ReferenceDataService referenceDataService;
    @Autowired private ContentReferenceService contentReferenceService;
    @Autowired private ContentCharacterCreationService creationService;
    @Autowired private UserRepository userRepository;
    @Autowired private StatTypeRepository statTypeRepository;
    @Autowired private ContentCharacterClassRepository contentClassRepository;
    @Autowired private ContentSkillRepository contentSkillRepository;
    @Autowired private ClassLevelRewardGroupRepository rewardGroupRepository;
    @Autowired private PlayerCharacterRepository characterRepository;
    @Autowired private CharacterStatRepository characterStatRepository;
    @Autowired private CharacterClassLevelRepository classLevelRepository;
    @Autowired private CharacterSkillProficiencyRepository skillProficiencyRepository;
    @Autowired private FeatureRuleBackfillService featureRuleBackfillService;
    @Autowired private FeatureRuleCoverageService featureRuleCoverageService;
    @Autowired private SpellRuleBackfillService spellRuleBackfillService;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void contextBootsLiquibaseValidatesAndContentLoaderRuns() {
        // Reaching here means the full changelog applied, ddl-auto=validate passed against
        // every entity, and the content loader ran — the deploy-time failure surface.
    }

    @Test
    @Transactional
    void featureRuleBackfillCreatesRulesForRuntimeFeaturesAndCoverageReports() {
        FeatureRuleCoverageReport before = featureRuleCoverageService.report("en");
        assertThat(before.getRuntimeFeatures()).isGreaterThan(0); // the 305 runtime features

        FeatureRuleBackfillResult first = featureRuleBackfillService.backfill(true);
        assertThat(first.isApplied()).isTrue();
        assertThat(first.getRuntimeFeatures()).isEqualTo(before.getRuntimeFeatures());
        assertThat(first.getRulesCreated()).isGreaterThan(0);

        FeatureRuleCoverageReport after = featureRuleCoverageService.report("en");
        assertThat(after.getFeaturesWithRules()).isGreaterThan(0);
        assertThat(after.getRulesByType()).isNotEmpty();
        assertThat(after.getTotalRules()).isGreaterThanOrEqualTo(first.getRulesCreated());

        // idempotent: a second run skips already-backfilled features and creates nothing new
        FeatureRuleBackfillResult second = featureRuleBackfillService.backfill(true);
        assertThat(second.getFeaturesSkipped()).isGreaterThan(0);
        assertThat(second.getRulesCreated()).isZero();
    }

    @Test
    @Transactional
    void spellRuleBackfillAbsorbsTheLegacySpellStackWithReconciledCounts() {
        SpellRuleBackfillResult first = spellRuleBackfillService.backfill(true);
        assertThat(first.isApplied()).isTrue();
        assertThat(first.getSpellsTotal()).isGreaterThan(0);

        // Верификация плана S2: COUNT источника = COUNT целевых правил, по каждой таблице 056-062.
        assertThat(first.getDamage().getRulesCreated()).isEqualTo(countInt(
                "SELECT COUNT(DISTINCT spell_id) FROM spell_damage WHERE dice IS NOT NULL AND dice <> ''"));
        assertThat(first.getHealing().getRulesCreated()).isEqualTo(countInt(
                "SELECT COUNT(DISTINCT spell_id) FROM spell_healing"));
        assertThat(first.getResolution().getRulesCreated()).isEqualTo(countInt(
                "SELECT COUNT(*) FROM spell WHERE save_ability IS NOT NULL "
                        + "OR check_ability IS NOT NULL OR is_attack_roll = TRUE"));
        assertThat(first.getActionCost().getRulesCreated()).isEqualTo(countInt(
                "SELECT COUNT(*) FROM spell WHERE casting_action_slug IS NOT NULL AND casting_action_slug <> ''"));
        assertThat(first.getEffects().getRulesCreated()).isEqualTo(countInt(
                "SELECT COUNT(DISTINCT spell_id) FROM spell_buffs"));

        // Целевые правила действительно созданы под владельцем SPELL и авто-утверждены.
        int spellRules = countInt("SELECT COUNT(*) FROM feature_rule WHERE owner_type = 'SPELL'");
        assertThat(spellRules).isEqualTo(first.getDamage().getRulesCreated()
                + first.getHealing().getRulesCreated()
                + first.getResolution().getRulesCreated()
                + first.getActionCost().getRulesCreated()
                + first.getEffects().getRulesCreated());
        assertThat(countInt("SELECT COUNT(*) FROM feature_rule WHERE owner_type = 'SPELL' "
                + "AND review_status = 'approved' AND approved_revision_id IS NOT NULL"))
                .isEqualTo(spellRules);

        // Идемпотентность: второй прогон ничего не создаёт, всё уже существует.
        SpellRuleBackfillResult second = spellRuleBackfillService.backfill(true);
        assertThat(second.getDamage().getRulesCreated()).isZero();
        assertThat(second.getHealing().getRulesCreated()).isZero();
        assertThat(second.getResolution().getRulesCreated()).isZero();
        assertThat(second.getActionCost().getRulesCreated()).isZero();
        assertThat(second.getEffects().getRulesCreated()).isZero();
        assertThat(second.getDamage().getSkippedExisting()).isEqualTo(first.getDamage().getRulesCreated());
        assertThat(countInt("SELECT COUNT(*) FROM feature_rule WHERE owner_type = 'SPELL'"))
                .isEqualTo(spellRules);
    }

    private int countInt(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value != null ? value : 0;
    }

    @Test
    void vanillaSkillsResolveGoverningAbilityWithoutDanglingProxy() {
        List<ProficiencySkillResponse> skills = referenceDataService.getVanillaSkills("ru");

        assertThat(skills).isNotEmpty();
        assertThat(skills).allSatisfy(s -> {
            assertThat(s.getName()).isNotBlank();
            assertThat(s.getGoverningStatId()).isNotNull();
            assertThat(s.getGoverningStatName()).isNotBlank();
        });
    }

    @Test
    void frontendReferenceReadsReturnDataNotErrors() {
        assertThat(referenceDataService.getVanillaStatTypes()).isNotEmpty();
        assertThat(referenceDataService.getVanillaCurrencies("ru")).isNotEmpty();
        assertThat(referenceDataService.getVanillaAbilities("ru")).isNotEmpty();
        assertThat(referenceDataService.getDamageTypes("ru")).isNotEmpty();
        assertThat(referenceDataService.getSpellSchools("ru")).isNotEmpty();
        assertThat(referenceDataService.getSizes("ru")).isNotEmpty();
        assertThat(referenceDataService.getRarities("ru")).isNotEmpty();

        assertThat(referenceDataService.getVanillaBackgrounds("ru")).isNotNull();
        assertThat(referenceDataService.getVanillaSpells(null, null, null, "ru")).isNotNull();
        assertThat(referenceDataService.getVanillaFeats(null, "ru")).isNotNull();

        assertThat(contentReferenceService.getVanillaClasses("ru")).isNotEmpty();
        assertThat(contentReferenceService.getVanillaSpecies("ru")).isNotEmpty();
    }

    @Test
    @Transactional
    void createsVanillaCharacterEndToEndOnContentModel() {
        User owner = userRepository.save(User.builder()
                .username("it-creator")
                .email("it-creator@example.test")
                .passwordHash("not-a-real-hash")
                .role(Role.PLAYER)
                .build());

        List<StatType> abilities = statTypeRepository.findByHomebrewIsNull();
        assertThat(abilities).hasSize(6);
        int[] standardArray = {15, 14, 13, 12, 10, 8};
        List<CreateContentCharacterRequest.AbilityScoreEntry> scores = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            scores.add(CreateContentCharacterRequest.AbilityScoreEntry.builder()
                    .statId(abilities.get(i).getId())
                    .baseValue(standardArray[i])
                    .build());
        }

        // Pick a vanilla class whose level 1 needs no mandatory reward choice, so creation
        // is valid with null initialRewardSelections — keeps the test robust to seed data.
        ContentCharacterClass chosen = pickVanillaClassWithoutRequiredLevel1Choice();
        assertThat(chosen).as("a vanilla class with no required level-1 choice").isNotNull();

        int skillCount = chosen.getSkillChoiceCount() != null ? chosen.getSkillChoiceCount() : 0;
        List<UUID> chosenSkillIds = (Boolean.TRUE.equals(chosen.getSkillChoiceAny())
                ? contentSkillRepository.findAllByHomebrewIsNull().stream()
                : chosen.getSkillOptions().stream())
                .limit(skillCount)
                .map(ContentSkill::getId)
                .toList();
        assertThat(chosenSkillIds).hasSize(skillCount);

        UUID raceId = contentReferenceService.getVanillaSpecies("ru").get(0).getId();
        UUID backgroundId = referenceDataService.getVanillaBackgrounds("ru").get(0).getId();

        CreateContentCharacterRequest req = CreateContentCharacterRequest.builder()
                .name("Integration Hero")
                .classId(chosen.getId())
                .raceId(raceId)
                .backgroundId(backgroundId)
                .level(1)
                .abilityScores(scores)
                .scoreMethod("STANDARD_ARRAY")
                .chosenSkillIds(chosenSkillIds)
                .build();

        ContentCharacterCreationResponse resp = creationService.createVanillaCharacter(req, owner.getUsername());

        assertThat(resp.getId()).isNotNull();
        assertThat(resp.getClassId()).isEqualTo(chosen.getId());
        assertThat(resp.getSkillProficiencyIds()).hasSize(skillCount);

        // The persisted runtime rows go through the real 063 FKs onto the content model.
        assertThat(characterRepository.findById(resp.getId())).isPresent();
        assertThat(classLevelRepository.findByCharacterIdAndClassId(resp.getId(), chosen.getId())).isPresent();
        assertThat(characterStatRepository.findAllByCharacterId(resp.getId())).hasSize(6);
        assertThat(skillProficiencyRepository.findByCharacterId(resp.getId())).hasSize(skillCount);
    }

    private ContentCharacterClass pickVanillaClassWithoutRequiredLevel1Choice() {
        for (ContentCharacterClass c : contentClassRepository.findAll()) {
            if (c.getHomebrew() != null) {
                continue;
            }
            List<ClassLevelRewardGroup> level1 = rewardGroupRepository
                    .findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(c.getId(), 1);
            boolean requiresChoice = level1.stream().anyMatch(g ->
                    "CHOICE".equalsIgnoreCase(g.getGroupKind())
                            && g.getChooseMin() != null && g.getChooseMin() > 0);
            if (!requiresChoice) {
                return c;
            }
        }
        return null;
    }
}
