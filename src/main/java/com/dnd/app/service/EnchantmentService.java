package com.dnd.app.service;

import com.dnd.app.dto.request.AddEnchantmentRequest;
import com.dnd.app.dto.request.CreateEnchantmentTypeRequest;
import com.dnd.app.dto.response.BuffDebuffResponse;
import com.dnd.app.dto.response.EnchantmentResponse;
import com.dnd.app.dto.response.EnchantmentTypeResponse;
import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.DamageType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnchantmentService {

    private final EnchantmentTypeRepository enchantmentTypeRepository;
    private final ItemEnchantmentRepository itemEnchantmentRepository;
    private final ItemInstanceRepository itemInstanceRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final BuffDebuffRepository buffDebuffRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;

    // ==================== Enchantment Type CRUD (admin) ====================

    @Transactional(readOnly = true)
    public List<EnchantmentTypeResponse> listEnchantmentTypes() {
        return enchantmentTypeRepository.findAll().stream()
                .map(this::toEnchantmentTypeResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EnchantmentTypeResponse getEnchantmentType(UUID id) {
        EnchantmentType enchantmentType = enchantmentTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Тип зачарования не найден с id: " + id));
        return toEnchantmentTypeResponse(enchantmentType);
    }

    @Transactional
    public EnchantmentTypeResponse createEnchantmentType(CreateEnchantmentTypeRequest request) {
        validateEnchantmentTypeRequest(request, null);

        EnchantmentType enchantmentType = new EnchantmentType();
        applyEnchantmentTypeFields(enchantmentType, request);

        EnchantmentType saved = enchantmentTypeRepository.save(enchantmentType);
        log.info("Создан тип зачарования: {} (id={})", saved.getName(), saved.getId());
        return toEnchantmentTypeResponse(saved);
    }

    @Transactional
    public EnchantmentTypeResponse updateEnchantmentType(UUID id, CreateEnchantmentTypeRequest request) {
        EnchantmentType enchantmentType = enchantmentTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Тип зачарования не найден с id: " + id));

        validateEnchantmentTypeRequest(request, id);

        applyEnchantmentTypeFields(enchantmentType, request);

        EnchantmentType saved = enchantmentTypeRepository.save(enchantmentType);
        log.info("Обновлён тип зачарования: {} (id={})", saved.getName(), saved.getId());
        return toEnchantmentTypeResponse(saved);
    }

    @Transactional
    public void deleteEnchantmentType(UUID id) {
        if (!enchantmentTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Тип зачарования не найден с id: " + id);
        }

        long count = itemEnchantmentRepository.countByEnchantmentTypeId(id);
        if (count > 0) {
            throw new DuplicateResourceException(
                    "Невозможно удалить: тип зачарования используется в " + count + " зачарованиях");
        }

        enchantmentTypeRepository.deleteById(id);
        log.info("Удалён тип зачарования с id: {}", id);
    }

    // ==================== Item instance enchantment operations ====================

    @Transactional
    public EnchantmentResponse addItemEnchantment(UUID characterId, UUID instanceId, AddEnchantmentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));

        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден с id: " + characterId));

        checkOwnerOrGmOrAdmin(user, character);

        ItemInstance item = itemInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет не найден с id: " + instanceId));

        if (item.getOwnerCharacter() == null || !item.getOwnerCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Предмет не принадлежит указанному персонажу");
        }

        EnchantmentType enchantmentType = enchantmentTypeRepository.findById(request.getEnchantmentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Тип зачарования не найден с id: " + request.getEnchantmentTypeId()));

        if (itemEnchantmentRepository.existsByItemInstanceIdAndEnchantmentTypeId(instanceId, request.getEnchantmentTypeId())) {
            throw new DuplicateResourceException("Данное зачарование уже применено к этому предмету");
        }

        ItemEnchantment enchantment = new ItemEnchantment();
        enchantment.setItemInstance(item);
        enchantment.setEnchantmentType(enchantmentType);
        enchantment.setNotes(request.getNotes());

        ItemEnchantment saved = itemEnchantmentRepository.save(enchantment);
        log.info("Добавлено зачарование {} к предмету {} персонажа {}", enchantmentType.getName(), instanceId, characterId);
        return toItemEnchantmentResponse(saved);
    }

    @Transactional
    public void removeItemEnchantment(UUID characterId, UUID enchantmentId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));

        ItemEnchantment enchantment = itemEnchantmentRepository.findById(enchantmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Зачарование не найдено с id: " + enchantmentId));

        if (enchantment.getItemInstance().getOwnerCharacter() == null
                || !enchantment.getItemInstance().getOwnerCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Зачарование не принадлежит указанному персонажу");
        }

        PlayerCharacter character = enchantment.getItemInstance().getOwnerCharacter();
        checkOwnerOrGmOrAdmin(user, character);

        itemEnchantmentRepository.deleteById(enchantmentId);
        log.info("Удалено зачарование {} с предмета персонажа {}", enchantmentId, characterId);
    }

    @Transactional(readOnly = true)
    public List<EnchantmentResponse> getItemEnchantments(UUID characterId, UUID instanceId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));

        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден с id: " + characterId));

        checkReadAccess(user, character);

        ItemInstance item = itemInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет не найден с id: " + instanceId));

        if (item.getOwnerCharacter() == null || !item.getOwnerCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Предмет не принадлежит указанному персонажу");
        }

        return itemEnchantmentRepository.findByItemInstanceId(instanceId).stream()
                .map(this::toItemEnchantmentResponse)
                .collect(Collectors.toList());
    }

    // ==================== Access checks ====================

    private void checkOwnerOrGmOrAdmin(User user, PlayerCharacter character) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.PLAYER && character.getOwner().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == Role.GAME_MASTER && character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId())) {
            return;
        }
        throw new AccessDeniedException("У вас нет доступа к этому персонажу");
    }

    private void checkReadAccess(User user, PlayerCharacter character) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.PLAYER && character.getOwner().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == Role.GAME_MASTER && character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId())) {
            return;
        }
        throw new AccessDeniedException("У вас нет доступа к этому персонажу");
    }

    // ==================== Validation ====================

    private void validateEnchantmentTypeRequest(CreateEnchantmentTypeRequest request, UUID excludeId) {
        if (request.getDamageDice() != null && request.getDamageType() == null) {
            throw new BadRequestException("При указании damageDice необходимо указать damageType");
        }

        if (request.getDamageType() != null) {
            try {
                DamageType.valueOf(request.getDamageType());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Недопустимый тип урона: " + request.getDamageType());
            }
        }

        if (excludeId == null) {
            if (enchantmentTypeRepository.existsByName(request.getName())) {
                throw new DuplicateResourceException("Тип зачарования с именем '" + request.getName() + "' уже существует");
            }
        } else {
            if (enchantmentTypeRepository.existsByNameAndIdNot(request.getName(), excludeId)) {
                throw new DuplicateResourceException("Тип зачарования с именем '" + request.getName() + "' уже существует");
            }
        }

        if (request.getBuffDebuffId() != null) {
            if (!buffDebuffRepository.existsById(request.getBuffDebuffId())) {
                throw new ResourceNotFoundException("Бафф/дебафф не найден с id: " + request.getBuffDebuffId());
            }
        }
    }

    private void applyEnchantmentTypeFields(EnchantmentType enchantmentType, CreateEnchantmentTypeRequest request) {
        enchantmentType.setName(request.getName());
        enchantmentType.setDescription(request.getDescription());
        enchantmentType.setDamageDice(request.getDamageDice());
        enchantmentType.setDamageBonus(request.getDamageBonus());
        enchantmentType.setDamageType(request.getDamageType() != null ? DamageType.valueOf(request.getDamageType()) : null);

        if (request.getBuffDebuffId() != null) {
            BuffDebuff buffDebuff = buffDebuffRepository.findById(request.getBuffDebuffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден с id: " + request.getBuffDebuffId()));
            enchantmentType.setBuffDebuff(buffDebuff);
        } else {
            enchantmentType.setBuffDebuff(null);
        }
    }

    // ==================== Response mapping ====================

    private EnchantmentTypeResponse toEnchantmentTypeResponse(EnchantmentType et) {
        return EnchantmentTypeResponse.builder()
                .id(et.getId()).name(et.getName()).description(et.getDescription())
                .damageDice(et.getDamageDice())
                .damageBonus(et.getDamageBonus())
                .damageType(et.getDamageType() != null ? et.getDamageType().name() : null)
                .buffDebuff(et.getBuffDebuff() != null ? toBuffDebuffResponse(et.getBuffDebuff()) : null)
                .build();
    }

    private BuffDebuffResponse toBuffDebuffResponse(BuffDebuff bd) {
        return BuffDebuffResponse.builder()
                .id(bd.getId()).name(bd.getName()).description(bd.getDescription())
                .effectType(bd.getEffectType())
                .targetStatId(bd.getTargetStat() != null ? bd.getTargetStat().getId() : null)
                .targetStatName(bd.getTargetStat() != null ? bd.getTargetStat().getName() : null)
                .modifierValue(bd.getModifierValue())
                .durationRounds(bd.getDurationRounds())
                .isBuff(bd.getIsBuff())
                .createdAt(bd.getCreatedAt())
                .build();
    }

    private EnchantmentResponse toItemEnchantmentResponse(ItemEnchantment ie) {
        return EnchantmentResponse.builder()
                .id(ie.getId())
                .enchantmentType(toEnchantmentTypeResponse(ie.getEnchantmentType()))
                .appliedAt(ie.getAppliedAt())
                .notes(ie.getNotes())
                .build();
    }
}
