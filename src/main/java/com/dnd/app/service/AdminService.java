package com.dnd.app.service;

import com.dnd.app.domain.CharacterClass;
import com.dnd.app.domain.CharacterRace;
import com.dnd.app.domain.ItemType;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.dto.request.CreateCharacterClassRequest;
import com.dnd.app.dto.request.CreateCharacterRaceRequest;
import com.dnd.app.dto.request.CreateItemTypeRequest;
import com.dnd.app.dto.request.CreateStatTypeRequest;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.ReferenceDataMapper;
import com.dnd.app.mapper.TeamMapper;
import com.dnd.app.mapper.UserMapper;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final StatTypeRepository statTypeRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final CharacterClassRepository classRepository;
    private final CharacterRaceRepository raceRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ReferenceDataMapper refMapper;
    private final UserMapper userMapper;
    private final TeamMapper teamMapper;

    // --- Stat Types ---

    @Transactional(readOnly = true)
    public List<StatTypeResponse> listStatTypes() {
        return statTypeRepository.findAll().stream().map(refMapper::toStatTypeResponse).toList();
    }

    @Transactional
    public StatTypeResponse createStatType(CreateStatTypeRequest request) {
        if (statTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Stat type name already exists");
        }
        StatType st = StatType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isDefault(false)
                .build();
        return refMapper.toStatTypeResponse(statTypeRepository.save(st));
    }

    @Transactional(readOnly = true)
    public StatTypeResponse getStatType(UUID id) {
        return refMapper.toStatTypeResponse(statTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stat type not found")));
    }

    @Transactional
    public StatTypeResponse updateStatType(UUID id, CreateStatTypeRequest request) {
        StatType st = statTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stat type not found"));
        if (!st.getName().equals(request.getName()) && statTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Stat type name already exists");
        }
        st.setName(request.getName());
        st.setDescription(request.getDescription());
        return refMapper.toStatTypeResponse(statTypeRepository.save(st));
    }

    @Transactional
    public void deleteStatType(UUID id) {
        if (!statTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Stat type not found");
        }
        statTypeRepository.deleteById(id);
    }

    // --- Item Types ---

    @Transactional(readOnly = true)
    public List<ItemTypeResponse> listItemTypes() {
        return itemTypeRepository.findAll().stream().map(refMapper::toItemTypeResponse).toList();
    }

    @Transactional
    public ItemTypeResponse createItemType(CreateItemTypeRequest request) {
        if (itemTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Item type name already exists");
        }
        EquipmentSlot slot = parseSlot(request.getSlot());
        ItemType it = ItemType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .slot(slot)
                .build();
        return refMapper.toItemTypeResponse(itemTypeRepository.save(it));
    }

    @Transactional(readOnly = true)
    public ItemTypeResponse getItemType(UUID id) {
        return refMapper.toItemTypeResponse(itemTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item type not found")));
    }

    @Transactional
    public ItemTypeResponse updateItemType(UUID id, CreateItemTypeRequest request) {
        ItemType it = itemTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item type not found"));
        if (!it.getName().equals(request.getName()) && itemTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Item type name already exists");
        }
        it.setName(request.getName());
        it.setDescription(request.getDescription());
        it.setSlot(parseSlot(request.getSlot()));
        return refMapper.toItemTypeResponse(itemTypeRepository.save(it));
    }

    @Transactional
    public void deleteItemType(UUID id) {
        if (!itemTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item type not found");
        }
        itemTypeRepository.deleteById(id);
    }

    // --- Character Classes ---

    @Transactional(readOnly = true)
    public List<CharacterClassResponse> listCharacterClasses() {
        return classRepository.findAll().stream().map(refMapper::toCharacterClassResponse).toList();
    }

    @Transactional
    public CharacterClassResponse createCharacterClass(CreateCharacterClassRequest request) {
        if (classRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Character class name already exists");
        }
        CharacterClass cc = CharacterClass.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return refMapper.toCharacterClassResponse(classRepository.save(cc));
    }

    @Transactional(readOnly = true)
    public CharacterClassResponse getCharacterClass(UUID id) {
        return refMapper.toCharacterClassResponse(classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found")));
    }

    @Transactional
    public CharacterClassResponse updateCharacterClass(UUID id, CreateCharacterClassRequest request) {
        CharacterClass cc = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
        if (!cc.getName().equals(request.getName()) && classRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Character class name already exists");
        }
        cc.setName(request.getName());
        cc.setDescription(request.getDescription());
        return refMapper.toCharacterClassResponse(classRepository.save(cc));
    }

    @Transactional
    public void deleteCharacterClass(UUID id) {
        if (!classRepository.existsById(id)) {
            throw new ResourceNotFoundException("Character class not found");
        }
        classRepository.deleteById(id);
    }

    // --- Character Races ---

    @Transactional(readOnly = true)
    public List<CharacterRaceResponse> listCharacterRaces() {
        return raceRepository.findAll().stream().map(refMapper::toCharacterRaceResponse).toList();
    }

    @Transactional
    public CharacterRaceResponse createCharacterRace(CreateCharacterRaceRequest request) {
        if (raceRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Character race name already exists");
        }
        CharacterRace cr = CharacterRace.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return refMapper.toCharacterRaceResponse(raceRepository.save(cr));
    }

    @Transactional(readOnly = true)
    public CharacterRaceResponse getCharacterRace(UUID id) {
        return refMapper.toCharacterRaceResponse(raceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character race not found")));
    }

    @Transactional
    public CharacterRaceResponse updateCharacterRace(UUID id, CreateCharacterRaceRequest request) {
        CharacterRace cr = raceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character race not found"));
        if (!cr.getName().equals(request.getName()) && raceRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Character race name already exists");
        }
        cr.setName(request.getName());
        cr.setDescription(request.getDescription());
        return refMapper.toCharacterRaceResponse(raceRepository.save(cr));
    }

    @Transactional
    public void deleteCharacterRace(UUID id) {
        if (!raceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Character race not found");
        }
        raceRepository.deleteById(id);
    }

    // --- Users & Teams (read-only) ---

    @Transactional(readOnly = true)
    public List<UserResponse> listAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listAllTeams() {
        return teamRepository.findAll().stream().map(teamMapper::toResponse).toList();
    }

    private EquipmentSlot parseSlot(String slot) {
        try {
            return EquipmentSlot.valueOf(slot);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid equipment slot: " + slot);
        }
    }
}
