package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.Species;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.repository.CreatureSizeRepository;
import com.dnd.app.repository.HomebrewContentItemRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.SpeciesRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тест SpeciesAuthoringServiceTest фиксирует авторинг homebrew-видов (SP-1): создание строит проекцию-граф
 * и регистрирует content-item; удаление даёт 409 при зависимых персонажах.
 */
@ExtendWith(MockitoExtension.class)
class SpeciesAuthoringServiceTest {

    @Mock private SpeciesRepository speciesRepository;
    @Mock private HomebrewAccessService homebrewAccessService;
    @Mock private HomebrewContentItemRepository contentItemRepository;
    @Mock private CreatureSizeRepository creatureSizeRepository;
    @Mock private PlayerCharacterRepository playerCharacterRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private SpeciesAuthoringService service;

    private final UUID packageId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();

    private HomebrewPackage draftPackage() {
        User author = new User();
        author.setId(authorId);
        author.setUsername("gm");
        HomebrewPackage pkg = HomebrewPackage.builder().author(author).title("Pkg").status(HomebrewStatus.DRAFT).build();
        pkg.setId(packageId);
        return pkg;
    }

    @Test
    @DisplayName("create: строит вид (имя/скорость/трейт) и регистрирует content-item SPECIES")
    void create_buildsGraphAndContentItem() throws Exception {
        HomebrewPackage pkg = draftPackage();
        when(homebrewAccessService.enforceOwner(eq(packageId), eq("gm"))).thenReturn(pkg);
        when(speciesRepository.existsBySlugAndHomebrew_Id(any(), eq(packageId))).thenReturn(false);
        when(speciesRepository.save(any(Species.class))).thenAnswer(inv -> {
            Species s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });
        when(contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(any(), any(), any()))
                .thenReturn(false);

        JsonNode body = objectMapper.readTree(
                "{\"name\":\"Лунорождённый\",\"darkvisionRange\":60,\"speed\":{\"walk\":30}," +
                "\"traits\":[{\"name\":\"Лунный дар\"}]}");

        JsonNode resp = service.create(packageId, body, "gm");

        ArgumentCaptor<Species> captor = ArgumentCaptor.forClass(Species.class);
        verify(speciesRepository).save(captor.capture());
        Species saved = captor.getValue();
        assertEquals("Лунорождённый", saved.getNameRu());
        assertNotNull(saved.getSlug());
        assertTrue(saved.getActive());
        assertSame(pkg, saved.getHomebrew());
        assertEquals(1, saved.getSpeeds().size(), "walk speed sp 1");
        assertNotNull(saved.getAuthoringJson(), "канонический payload сохранён");
        // darkvisionRange транслирован в трейт-эффект (для снапшота персонажа)
        var darkvision = saved.getTraits().stream()
                .flatMap(t -> t.getEffects().stream())
                .filter(e -> "darkvision".equals(e.getEffectType()))
                .findFirst();
        assertTrue(darkvision.isPresent(), "darkvision-эффект создан");
        assertEquals(60, darkvision.get().getRangeFt());

        verify(contentItemRepository).save(argThat(ci ->
                ci.getContentType() == ContentType.SPECIES && ci.getContentId().equals(saved.getId())));
        assertEquals("HOMEBREW", resp.get("sourceType").asText());
    }

    @Test
    @DisplayName("duplicate: клонирует эффекты трейтов (darkvision) и реконструирует authoring_json")
    void duplicate_deepClonesEffects() {
        HomebrewPackage pkg = draftPackage();
        UUID vanillaId = UUID.randomUUID();

        com.dnd.app.domain.content.Species source = new com.dnd.app.domain.content.Species();
        source.setId(vanillaId);
        source.setNameRu("Эльф");
        source.setSlug("elf");
        source.setSizeOptions(new java.util.HashSet<>());
        source.setSpeeds(new java.util.ArrayList<>());
        com.dnd.app.domain.content.SpeciesTrait trait = new com.dnd.app.domain.content.SpeciesTrait();
        trait.setName("Тёмное зрение");
        trait.setSlug("darkvision");
        trait.setSortOrder(0);
        com.dnd.app.domain.content.SpeciesTraitEffect eff = new com.dnd.app.domain.content.SpeciesTraitEffect();
        eff.setEffectType("darkvision");
        eff.setRangeFt(60);
        trait.setEffects(new java.util.ArrayList<>(java.util.List.of(eff)));
        source.setTraits(new java.util.ArrayList<>(java.util.List.of(trait)));

        when(homebrewAccessService.enforceOwner(eq(packageId), eq("gm"))).thenReturn(pkg);
        when(speciesRepository.findById(vanillaId)).thenReturn(Optional.of(source));
        when(speciesRepository.existsBySlugAndHomebrew_Id(any(), eq(packageId))).thenReturn(false);
        when(speciesRepository.save(any(Species.class))).thenAnswer(inv -> {
            Species s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });
        when(contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(any(), any(), any()))
                .thenReturn(false);

        service.duplicateFromVanilla(packageId, vanillaId, "gm");

        ArgumentCaptor<Species> captor = ArgumentCaptor.forClass(Species.class);
        verify(speciesRepository).save(captor.capture());
        Species clone = captor.getValue();
        assertSame(pkg, clone.getHomebrew());
        var dv = clone.getTraits().stream().flatMap(t -> t.getEffects().stream())
                .filter(e -> "darkvision".equals(e.getEffectType())).findFirst();
        assertTrue(dv.isPresent(), "эффект darkvision склонирован");
        assertEquals(60, dv.get().getRangeFt());
        assertNotNull(clone.getAuthoringJson());
        assertTrue(clone.getAuthoringJson().contains("darkvisionRange"),
                "authoring_json реконструирован с darkvisionRange");
    }

    @Test
    @DisplayName("delete: 409, если вид используют персонажи")
    void delete_conflictWhenUsed() {
        HomebrewPackage pkg = draftPackage();
        UUID speciesId = UUID.randomUUID();
        Species sp = new Species();
        sp.setId(speciesId);
        sp.setHomebrew(pkg);
        when(homebrewAccessService.enforceOwner(eq(packageId), eq("gm"))).thenReturn(pkg);
        when(speciesRepository.findByIdAndHomebrew_Id(speciesId, packageId)).thenReturn(Optional.of(sp));
        when(playerCharacterRepository.countByRaceId(speciesId)).thenReturn(2L);

        assertThrows(DuplicateResourceException.class, () -> service.delete(packageId, speciesId, "gm"));
        verify(speciesRepository, never()).delete(any());
    }
}
