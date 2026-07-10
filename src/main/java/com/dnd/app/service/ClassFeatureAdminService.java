package com.dnd.app.service;

import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.dto.content.ClassFeatureWarningResponse;
import com.dnd.app.dto.request.ClassFeatureResolutionRequest;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Класс ClassFeatureAdminService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class ClassFeatureAdminService {

    private static final Set<String> ABILITIES = Set.of(
            "STRENGTH", "DEXTERITY", "CONSTITUTION", "INTELLIGENCE", "WISDOM", "CHARISMA");
    private static final Set<String> ACTIVATIONS = Set.of("PASSIVE", "ACTION", "BONUS_ACTION", "REACTION");
    private static final Set<String> DAMAGE_TYPES = Set.of(
            "SLASHING", "PIERCING", "BLUDGEONING", "FIRE", "COLD", "LIGHTNING",
            "POISON", "NECROTIC", "RADIANT", "PSYCHIC", "FORCE", "THUNDER", "ACID");

    private final ClassFeatureRepository classFeatureRepository;

    /**
     * Выполняет операции "resolve" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
    /**
     * Возвращает список для операции "list warnings" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ClassFeatureWarningResponse> listWarnings(String lang) {
        return classFeatureRepository.findWarnings().stream()
                .map(feature -> toResponse(feature, lang))
                .toList();
    }

    /**
     * Выполняет операции "resolve" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public ClassFeatureWarningResponse resolve(UUID id, ClassFeatureResolutionRequest request, String lang) {
        ClassFeature feature = classFeatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Умение класса не найдено: " + id));

        String activation = normalize(request.getActivationType(), ACTIVATIONS, "тип активации");
        if (activation != null) {
            feature.setActivationType(activation);
        }
        if (request.getAttackRoll() != null) {
            feature.setAttackRoll(request.getAttackRoll());
        }
        feature.setSaveAbility(normalize(request.getSaveAbility(), ABILITIES, "характеристика спасброска"));
        feature.setDamageDice(normDice(request.getDamageDice()));
        feature.setDamageType(normalize(request.getDamageType(), DAMAGE_TYPES, "тип урона"));
        feature.setHealingDice(normDice(request.getHealingDice()));
        feature.setHealingFlat(request.getHealingFlat());

        boolean stillFlagged = Boolean.TRUE.equals(request.getWarning());
        feature.setWarning(stillFlagged);
        if (!stillFlagged) {
            feature.setWarningReason(null);
        }

        classFeatureRepository.save(feature);
        return toResponse(feature, lang);
    }

    private static String normalize(String value, Set<String> allowed, String contextRu) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("Недопустимое значение поля " + contextRu + ": " + value);
        }
        return normalized;
    }

    private static String normDice(String dice) {
        if (dice == null || dice.isBlank()) {
            return null;
        }
        return dice.trim().replace('к', 'd').replace('К', 'd');
    }

    private ClassFeatureWarningResponse toResponse(ClassFeature feature, String lang) {
        String className = feature.getCharacterClass() == null ? null
                : Localization.pick(lang,
                feature.getCharacterClass().getNameRu(),
                feature.getCharacterClass().getNameEn(),
                feature.getCharacterClass().getNameRu());
        String subclassName = feature.getSubclass() == null ? null
                : Localization.pick(lang,
                feature.getSubclass().getNameRu(),
                feature.getSubclass().getNameEn(),
                feature.getSubclass().getNameRu());

        return ClassFeatureWarningResponse.builder()
                .id(feature.getId())
                .slug(feature.getSlug())
                .title(feature.getTitle())
                .className(className)
                .subclassName(subclassName)
                .level(feature.getLevel())
                .activationType(feature.getActivationType())
                .attackRoll(feature.getAttackRoll())
                .saveAbility(feature.getSaveAbility())
                .damageDice(feature.getDamageDice())
                .damageType(feature.getDamageType())
                .healingDice(feature.getHealingDice())
                .healingFlat(feature.getHealingFlat())
                .warning(feature.getWarning())
                .warningReason(feature.getWarningReason())
                .description(feature.getDescription())
                .build();
    }
}
