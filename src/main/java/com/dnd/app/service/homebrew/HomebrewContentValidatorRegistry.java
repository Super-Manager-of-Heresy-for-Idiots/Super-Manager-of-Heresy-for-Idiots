package com.dnd.app.service.homebrew;

import com.dnd.app.dto.response.ContentSummaryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Класс HomebrewContentValidatorRegistry описывает сервис homebrew-логики, который проверяет и обслуживает пользовательский контент.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Component
public class HomebrewContentValidatorRegistry {

    private final Map<String, HomebrewContentValidator> validators;

    /**
     * Создает экземпляр компонента homebrew-контента и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param validatorList входящее значение validator list, используемое бизнес-сценарием
     */
    public HomebrewContentValidatorRegistry(List<HomebrewContentValidator> validatorList) {
        this.validators = validatorList.stream()
                .collect(Collectors.toMap(HomebrewContentValidator::getSupportedType, Function.identity()));
        log.info("HomebrewContentValidatorRegistry initialized with types: {}", validators.keySet());
    }

    /**
     * Проверяет корректность операции "validate" в рамках бизнес-логики homebrew-контента.
     * @param contentType входящее значение content type, используемое бизнес-сценарием
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     */
    public void validate(String contentType, UUID contentId) {
        getValidator(contentType).validateExists(contentId);
    }

    /**
     * Выполняет операции "summarize" в рамках бизнес-логики homebrew-контента.
     * @param contentType входящее значение content type, используемое бизнес-сценарием
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    public ContentSummaryDto summarize(String contentType, UUID contentId) {
        return getValidator(contentType).summarize(contentId);
    }

    /**
     * Возвращает результат операции "get owner id" в рамках бизнес-логики homebrew-контента.
     * @param contentType входящее значение content type, используемое бизнес-сценарием
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    public UUID getOwnerId(String contentType, UUID contentId) {
        return getValidator(contentType).getOwnerId(contentId);
    }

    /**
     * Проверяет условие операции "is known type" в рамках бизнес-логики homebrew-контента.
     * @param contentType входящее значение content type, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public boolean isKnownType(String contentType) {
        return validators.containsKey(contentType);
    }

    /**
     * Возвращает результат операции "get supported types" в рамках бизнес-логики homebrew-контента.
     * @return результат выполнения бизнес-операции
     */
    public Set<String> getSupportedTypes() {
        return validators.keySet();
    }

    private HomebrewContentValidator getValidator(String contentType) {
        HomebrewContentValidator validator = validators.get(contentType);
        if (validator == null) {
            log.error("Unknown content type: '{}', available types: {}", contentType, validators.keySet());
            throw new IllegalArgumentException("Неизвестный тип контента: " + contentType);
        }
        return validator;
    }
}
