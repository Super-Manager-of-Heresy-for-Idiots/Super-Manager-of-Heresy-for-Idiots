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
    private final InventoryEnchantmentRepository inventoryEnchantmentRepository;
    private final InventorySlotRepository inventorySlotRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final BuffDebuffRepository buffDebuffRepository;
    private final UserRepository userRepository;

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

        long count = inventoryEnchantmentRepository.countByEnchantmentTypeId(id);
        if (count > 0) {
            throw new DuplicateResourceException(
                    "Невозможно удалить: тип зачарования используется в " + count + " зачарованиях инвентаря");
        }

        enchantmentTypeRepository.deleteById(id);
        log.info("Удалён тип зачарования с id: {}", id);
    }

    // ==================== Player enchantment operations ====================

    @Transactional
    public EnchantmentResponse addEnchantment(UUID characterId, UUID slotId, AddEnchantmentRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));

        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден с id: " + characterId));

        checkOwnerOrAdmin(user, character);

        InventorySlot slot = inventorySlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Слот инвентаря не найден с id: " + slotId));

        if (!slot.getCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Слот не принадлежит указанному персонажу");
        }

        if (slot.getItemType() == null) {
            throw new DuplicateResourceException("Невозможно зачаровать пустой слот");
        }

        EnchantmentType enchantmentType = enchantmentTypeRepository.findById(request.getEnchantmentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Тип зачарования не найден с id: " + request.getEnchantmentTypeId()));

        if (inventoryEnchantmentRepository.existsByInventorySlotIdAndEnchantmentTypeId(slotId, request.getEnchantmentTypeId())) {
            throw new DuplicateResourceException("Данное зачарование уже применено к этому слоту");
        }

        InventoryEnchantment enchantment = new InventoryEnchantment();
        enchantment.setInventorySlot(slot);
        enchantment.setEnchantmentType(enchantmentType);
        enchantment.setAppliedAt(Instant.now());
        enchantment.setNotes(request.getNotes());

        InventoryEnchantment saved = inventoryEnchantmentRepository.save(enchantment);
        log.info("Добавлено зачарование {} к слоту {} персонажа {}", enchantmentType.getName(), slotId, characterId);
        return toEnchantmentResponse(saved);
    }

    @Transactional
    public void removeEnchantment(UUID characterId, UUID enchantmentId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));

        InventoryEnchantment enchantment = inventoryEnchantmentRepository.findById(enchantmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Зачарование не найдено с id: " + enchantmentId));

        if (!enchantment.getInventorySlot().getCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Зачарование не принадлежит указанному персонажу");
        }

        PlayerCharacter character = enchantment.getInventorySlot().getCharacter();
        checkOwnerOrAdmin(user, character);

        inventoryEnchantmentRepository.deleteById(enchantmentId);
        log.info("Удалено зачарование {} с персонажа {}", enchantmentId, characterId);
    }

    @Transactional(readOnly = true)
    public List<EnchantmentResponse> getSlotEnchantments(UUID characterId, UUID slotId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));

        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден с id: " + characterId));

        checkReadAccess(user, character);

        InventorySlot slot = inventorySlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Слот инвентаря не найден с id: " + slotId));

        if (!slot.getCharacter().getId().equals(characterId)) {
            throw new BadRequestException("Слот не принадлежит указанному персонажу");
        }

        return inventoryEnchantmentRepository.findAllByInventorySlotId(slotId).stream()
                .map(this::toEnchantmentResponse)
                .collect(Collectors.toList());
    }

    // ==================== Access checks ====================

    private void checkOwnerOrAdmin(User user, PlayerCharacter character) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.PLAYER && character.getOwner().getId().equals(user.getId())) {
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
        if (user.getRole() == Role.GAME_MASTER
                && playerCharacterRepository.isPlayerInGameMasterTeam(character.getOwner().getId(), user.getId())) {
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
            enchantmentTypeRepository.findAll().stream()
                    .filter(et -> et.getName().equals(request.getName()) && !et.getId().equals(excludeId))
                    .findFirst()
                    .ifPresent(et -> {
                        throw new DuplicateResourceException("Тип зачарования с именем '" + request.getName() + "' уже существует");
                    });
        }

        if (request.getBuffDebuffId() != null) {
            if (!buffDebuffRepository.findById(request.getBuffDebuffId()).isPresent()) {
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

    private EnchantmentResponse toEnchantmentResponse(InventoryEnchantment ie) {
        return EnchantmentResponse.builder()
                .id(ie.getId())
                .enchantmentType(toEnchantmentTypeResponse(ie.getEnchantmentType()))
                .appliedAt(ie.getAppliedAt())
                .notes(ie.getNotes())
                .build();
    }
}
