package com.dnd.app.service.formula;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.CharacterStatRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.util.AbilityScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds a {@link FormulaContext} from a character's live data, so feature formulas (resource max,
 * duration, DC, eligibility) can be evaluated on that character. Values are snapshotted into a plain
 * {@link MapFormulaContext}, avoiding lazy-loading concerns outside the loading transaction.
 *
 * <p>Registered keys:
 * {@code character_level}, {@code proficiency_bonus};
 * {@code class_level("<name>")} by English/Russian class name (any case);
 * {@code ability_mod("<key>")} by 3-letter code (STR/DEX/…) and English/Russian ability name;
 * {@code feature_resource_count("<resourceKey>")}.</p>
 */
@Service
@RequiredArgsConstructor
public class CharacterFormulaContextFactory {

    private final CharacterClassLevelRepository classLevelRepository;
    private final ContentCharacterClassRepository contentClassRepository;
    private final CharacterStatRepository characterStatRepository;
    private final CharacterFeatureResourceRepository resourceRepository;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;

    @Transactional(readOnly = true)
    public FormulaContext build(PlayerCharacter character) {
        MapFormulaContext ctx = new MapFormulaContext();
        UUID characterId = character.getId();

        int totalLevel = character.getTotalLevel() != null ? character.getTotalLevel() : 1;
        ctx.scalar("character_level", totalLevel);
        ctx.scalar("proficiency_bonus", ((Math.max(1, totalLevel) - 1) / 4) + 2);

        // class levels, keyed by class name (en/ru, any case)
        List<CharacterClassLevel> classLevels = classLevelRepository.findAllByCharacterId(characterId);
        if (!classLevels.isEmpty()) {
            List<UUID> classIds = classLevels.stream().map(CharacterClassLevel::getClassId).toList();
            Map<UUID, ContentCharacterClass> classes = contentClassRepository.findAllById(classIds).stream()
                    .collect(Collectors.toMap(ContentCharacterClass::getId, c -> c));
            for (CharacterClassLevel ccl : classLevels) {
                ContentCharacterClass clazz = classes.get(ccl.getClassId());
                int lvl = ccl.getClassLevel() != null ? ccl.getClassLevel() : 0;
                if (clazz != null) {
                    registerKeys(clazz.getNameEn(), clazz.getNameRu(), key -> ctx.classLevel(key, lvl));
                }
            }
        }

        // ability modifiers, keyed by 3-letter code + full name
        for (CharacterStat stat : characterStatRepository.findAllByCharacterId(characterId)) {
            StatType type = stat.getStatType();
            if (type == null || stat.getValue() == null) {
                continue;
            }
            int mod = AbilityScores.modifier(stat.getValue());
            String nameEn = type.getNameEn();
            if (nameEn != null && nameEn.length() >= 3) {
                ctx.abilityMod(nameEn.substring(0, 3).toUpperCase(Locale.ROOT), mod);
            }
            registerKeys(nameEn, type.getNameRu(), key -> ctx.abilityMod(key, mod));
        }

        // feature resource counts, keyed by resource_key
        List<CharacterFeatureResource> resources = resourceRepository.findByCharacterId(characterId);
        if (!resources.isEmpty()) {
            List<UUID> defIds = resources.stream()
                    .map(CharacterFeatureResource::getResourceDefinitionId).distinct().toList();
            Map<UUID, FeatureResourceDefinition> defs = resourceDefinitionRepository.findAllById(defIds).stream()
                    .collect(Collectors.toMap(FeatureResourceDefinition::getId, d -> d));
            for (CharacterFeatureResource res : resources) {
                FeatureResourceDefinition def = defs.get(res.getResourceDefinitionId());
                if (def != null && def.getResourceKey() != null && res.getCurrentValue() != null) {
                    ctx.resourceCount(def.getResourceKey(), res.getCurrentValue());
                }
            }
        }

        return ctx;
    }

    private interface KeySink {
        void put(String key);
    }

    private void registerKeys(String nameEn, String nameRu, KeySink sink) {
        registerName(nameEn, sink);
        registerName(nameRu, sink);
    }

    private void registerName(String name, KeySink sink) {
        if (name == null || name.isBlank()) {
            return;
        }
        sink.put(name);
        sink.put(name.toLowerCase(Locale.ROOT));
    }
}
