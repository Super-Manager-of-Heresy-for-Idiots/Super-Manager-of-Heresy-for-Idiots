package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Класс CharacterFeatureResolver описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class CharacterFeatureResolver {

    private static final String OWNER = FeatureRuleOwnerType.CLASS_FEATURE.getCode();
    private static final String APPROVED = FeatureReviewStatus.APPROVED.getCode();

    private final CharacterClassLevelRepository classLevelRepository;
    private final ClassFeatureRepository classFeatureRepository;
    private final FeatureRuleRepository ruleRepository;

    /**
     * Выполняет операции "known base class features" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ClassFeature> knownBaseClassFeatures(UUID characterId) {
        List<ClassFeature> out = new ArrayList<>();
        for (CharacterClassLevel ccl : classLevelRepository.findAllByCharacterId(characterId)) {
            int classLevel = ccl.getClassLevel() != null ? ccl.getClassLevel() : 0;
            classFeatureRepository.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(ccl.getClassId()).stream()
                    .filter(f -> f.getSubclass() == null && f.getLevel() != null && f.getLevel() <= classLevel)
                    .forEach(out::add);
        }
        return out;
    }

    /**
     * Выполняет операции "approved enabled rules" в рамках бизнес-логики домена.
     * @param featureIds входящее значение feature ids, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<FeatureRule> approvedEnabledRules(Collection<UUID> featureIds) {
        return approvedEnabledRules(FeatureRuleOwnerType.CLASS_FEATURE, featureIds);
    }

    /**
     * Выполняет операции "approved enabled rules" в рамках бизнес-логики домена.
     * @param ownerType входящее значение owner type, используемое бизнес-сценарием
     * @param ownerIds входящее значение owner ids, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<FeatureRule> approvedEnabledRules(FeatureRuleOwnerType ownerType, Collection<UUID> ownerIds) {
        if (ownerIds.isEmpty()) {
            return List.of();
        }
        return ruleRepository.findByOwnerTypeAndOwnerIdIn(ownerType.getCode(), ownerIds).stream()
                .filter(FeatureRule::isEnabled)
                .filter(r -> APPROVED.equals(r.getReviewStatus()) && r.getApprovedRevisionId() != null)
                .toList();
    }
}
