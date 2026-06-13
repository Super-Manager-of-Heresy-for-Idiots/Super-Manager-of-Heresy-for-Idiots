package com.dnd.app.service;

import com.dnd.app.dto.request.CreateBuffDebuffRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.repository.BuffDebuffRepository;
import com.dnd.app.repository.EnchantmentTypeRepository;
import com.dnd.app.repository.SkillEffectRepository;
import com.dnd.app.repository.StatTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuffDebuffService: создание и удаление баффов/дебаффов")
class BuffDebuffServiceTest {

    @Mock private BuffDebuffRepository buffDebuffRepository;
    @Mock private SkillEffectRepository skillEffectRepository;
    @Mock private EnchantmentTypeRepository enchantmentTypeRepository;
    @Mock private StatTypeRepository statTypeRepository;

    @InjectMocks private BuffDebuffService buffDebuffService;

    @Test
    @DisplayName("STAT_MODIFIER требует указания targetStatId")
    void create_withStatModifier_requiresTargetStatId() {
        CreateBuffDebuffRequest req = CreateBuffDebuffRequest.builder()
                .name("TestBuff").effectType("STAT_MODIFIER").targetStatId(null).isBuff(true).build();
        assertThrows(BadRequestException.class, () -> buffDebuffService.create(req));
    }

    @Test
    @DisplayName("Удаление невозможно, если бафф используется эффектом умения")
    void delete_whenReferencedBySkillEffect_throws409() {
        UUID id = UUID.randomUUID();
        when(buffDebuffRepository.existsById(id)).thenReturn(true);
        when(skillEffectRepository.countByBuffDebuffId(id)).thenReturn(2L);
        when(enchantmentTypeRepository.countByBuffDebuffId(id)).thenReturn(0L);
        assertThrows(DuplicateResourceException.class, () -> buffDebuffService.delete(id));
    }

    @Test
    @DisplayName("Удаление невозможно, если бафф используется типом зачарования")
    void delete_whenReferencedByEnchantmentType_throws409() {
        UUID id = UUID.randomUUID();
        when(buffDebuffRepository.existsById(id)).thenReturn(true);
        when(skillEffectRepository.countByBuffDebuffId(id)).thenReturn(0L);
        when(enchantmentTypeRepository.countByBuffDebuffId(id)).thenReturn(1L);
        assertThrows(DuplicateResourceException.class, () -> buffDebuffService.delete(id));
    }
}
