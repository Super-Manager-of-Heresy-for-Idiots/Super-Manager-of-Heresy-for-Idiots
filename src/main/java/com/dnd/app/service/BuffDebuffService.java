package com.dnd.app.service;

import com.dnd.app.dto.request.CreateBuffDebuffRequest;
import com.dnd.app.dto.response.BuffDebuffResponse;
import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.domain.StatType;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.BuffDebuffRepository;
import com.dnd.app.repository.EnchantmentTypeRepository;
import com.dnd.app.repository.SkillEffectRepository;
import com.dnd.app.repository.StatTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс BuffDebuffService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuffDebuffService {

    private final BuffDebuffRepository buffDebuffRepository;
    private final SkillEffectRepository skillEffectRepository;
    private final EnchantmentTypeRepository enchantmentTypeRepository;
    private final StatTypeRepository statTypeRepository;

    /**
     * Находит результат операции "find all" в рамках бизнес-логики домена.
     * @param isBuff входящее значение is buff, используемое бизнес-сценарием
     * @param effectType входящее значение effect type, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<BuffDebuffResponse> findAll(Boolean isBuff, String effectType) {
        List<BuffDebuff> results;

        if (isBuff != null && effectType != null) {
            results = buffDebuffRepository.findAllByIsBuffAndEffectType(isBuff, effectType);
        } else if (isBuff != null) {
            results = buffDebuffRepository.findAllByIsBuff(isBuff);
        } else if (effectType != null) {
            results = buffDebuffRepository.findAllByEffectType(effectType);
        } else {
            results = buffDebuffRepository.findAll();
        }

        return results.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Находит результат операции "find by id" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public BuffDebuffResponse findById(UUID id) {
        BuffDebuff buffDebuff = buffDebuffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден с id: " + id));
        return toResponse(buffDebuff);
    }

    /**
     * Создает результат операции "create" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BuffDebuffResponse create(CreateBuffDebuffRequest request) {
        validateRequest(request, null);

        BuffDebuff buffDebuff = new BuffDebuff();
        applyFields(buffDebuff, request);

        BuffDebuff saved = buffDebuffRepository.save(buffDebuff);
        log.info("Создан бафф/дебафф: {} (id={})", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    /**
     * Обновляет результат операции "update" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public BuffDebuffResponse update(UUID id, CreateBuffDebuffRequest request) {
        BuffDebuff buffDebuff = buffDebuffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден с id: " + id));

        validateRequest(request, id);

        applyFields(buffDebuff, request);

        BuffDebuff saved = buffDebuffRepository.save(buffDebuff);
        log.info("Обновлён бафф/дебафф: {} (id={})", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    /**
     * Удаляет результат операции "delete" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     */
    @Transactional
    public void delete(UUID id) {
        if (!buffDebuffRepository.existsById(id)) {
            throw new ResourceNotFoundException("Бафф/дебафф не найден с id: " + id);
        }

        long skillEffectCount = skillEffectRepository.countByBuffDebuffId(id);
        long enchantmentTypeCount = enchantmentTypeRepository.countByBuffDebuffId(id);
        long totalCount = skillEffectCount + enchantmentTypeCount;

        if (totalCount > 0) {
            throw new DuplicateResourceException(
                    "Невозможно удалить: бафф/дебафф используется в " + totalCount + " эффектах умений и/или типах зачарований");
        }

        buffDebuffRepository.deleteById(id);
        log.info("Удалён бафф/дебафф с id: {}", id);
    }

    private void validateRequest(CreateBuffDebuffRequest request, UUID excludeId) {
        if ("STAT_MODIFIER".equals(request.getEffectType())) {
            if (request.getTargetStatId() == null) {
                throw new BadRequestException("Для типа эффекта STAT_MODIFIER необходимо указать targetStatId");
            }
            if (!statTypeRepository.findById(request.getTargetStatId()).isPresent()) {
                throw new ResourceNotFoundException("Тип характеристики не найден с id: " + request.getTargetStatId());
            }
        }

        if (excludeId == null) {
            if (buffDebuffRepository.existsByName(request.getName())) {
                throw new DuplicateResourceException("Бафф/дебафф с именем '" + request.getName() + "' уже существует");
            }
        } else {
            buffDebuffRepository.findAll().stream()
                    .filter(bd -> bd.getName().equals(request.getName()) && !bd.getId().equals(excludeId))
                    .findFirst()
                    .ifPresent(bd -> {
                        throw new DuplicateResourceException("Бафф/дебафф с именем '" + request.getName() + "' уже существует");
                    });
        }
    }

    private void applyFields(BuffDebuff buffDebuff, CreateBuffDebuffRequest request) {
        buffDebuff.setName(request.getName());
        buffDebuff.setDescription(request.getDescription());
        buffDebuff.setEffectType(request.getEffectType());
        buffDebuff.setModifierValue(request.getModifierValue());
        buffDebuff.setDurationRounds(request.getDurationRounds());
        buffDebuff.setIsBuff(request.getIsBuff());

        if (request.getTargetStatId() != null) {
            StatType statType = statTypeRepository.findById(request.getTargetStatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Тип характеристики не найден с id: " + request.getTargetStatId()));
            buffDebuff.setTargetStat(statType);
        } else {
            buffDebuff.setTargetStat(null);
        }
    }

    private BuffDebuffResponse toResponse(BuffDebuff bd) {
        return BuffDebuffResponse.builder()
                .id(bd.getId()).name(bd.getName()).description(bd.getDescription())
                .effectType(bd.getEffectType())
                .targetStatId(bd.getTargetStat() != null ? bd.getTargetStat().getId() : null)
                .targetStatName(bd.getTargetStat() != null ? bd.getTargetStat().getNameRu() : null)
                .modifierValue(bd.getModifierValue())
                .durationRounds(bd.getDurationRounds())
                .isBuff(bd.getIsBuff())
                .createdAt(bd.getCreatedAt())
                .build();
    }
}
