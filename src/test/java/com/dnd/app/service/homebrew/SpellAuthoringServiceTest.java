package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.SpellSchool;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.dto.request.HomebrewSpellRequest;
import com.dnd.app.dto.response.HomebrewSpellResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.HomebrewContentItemRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.SpellSchoolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тест SpellAuthoringServiceTest фиксирует авторинг homebrew-заклинания (P2-1): создание строит spell с homebrew_id +
 * автором + резолвнутой школой и регистрирует content-item SPELL; неизвестная школа → 400.
 */
@ExtendWith(MockitoExtension.class)
class SpellAuthoringServiceTest {

    @Mock private SpellRepository spellRepository;
    @Mock private SpellSchoolRepository spellSchoolRepository;
    @Mock private ContentCharacterClassRepository classRepository;
    @Mock private HomebrewAccessService homebrewAccessService;
    @Mock private HomebrewContentItemRepository contentItemRepository;
    @Mock private SpellMechanicsService spellMechanicsService;

    @InjectMocks private SpellAuthoringService service;

    private final UUID packageId = UUID.randomUUID();

    private HomebrewPackage draftPackage() {
        User author = new User();
        author.setId(UUID.randomUUID());
        author.setUsername("gm");
        HomebrewPackage pkg = HomebrewPackage.builder().author(author).title("Pkg").status(HomebrewStatus.DRAFT).build();
        pkg.setId(packageId);
        return pkg;
    }

    @Test
    @DisplayName("create: строит spell (homebrew_id + автор + школа) и регистрирует content-item SPELL")
    void create_buildsSpellAndContentItem() {
        HomebrewPackage pkg = draftPackage();
        SpellSchool evocation = new SpellSchool();
        evocation.setId(UUID.randomUUID());
        evocation.setSlug("evocation");

        when(homebrewAccessService.enforceOwner(eq(packageId), eq("gm"))).thenReturn(pkg);
        when(spellRepository.existsBySlugAndHomebrew_Id(any(), eq(packageId))).thenReturn(false);
        when(spellSchoolRepository.findBySlug("evocation")).thenReturn(Optional.of(evocation));
        when(spellRepository.save(any(Spell.class))).thenAnswer(inv -> {
            Spell s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });
        when(contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(any(), any(), any()))
                .thenReturn(false);

        HomebrewSpellRequest req = new HomebrewSpellRequest();
        req.setName("Огненный шёпот");
        req.setLevel(3);
        req.setSchool("evocation");
        req.setConcentration(true);

        HomebrewSpellResponse resp = service.create(packageId, req, "gm");

        ArgumentCaptor<Spell> captor = ArgumentCaptor.forClass(Spell.class);
        verify(spellRepository).save(captor.capture());
        Spell saved = captor.getValue();
        assertEquals("Огненный шёпот", saved.getNameRu());
        assertEquals(3, saved.getLevel());
        assertSame(pkg, saved.getHomebrew());
        assertSame(evocation, saved.getSchool());
        assertSame(pkg.getAuthor(), saved.getCreatedBy());
        assertNotNull(saved.getSlug());

        verify(contentItemRepository).save(argThat(ci ->
                ci.getContentType() == ContentType.SPELL && ci.getContentId().equals(saved.getId())));
        assertEquals("HOMEBREW", resp.getSource());
        assertEquals("evocation", resp.getSchool());
    }

    @Test
    @DisplayName("create: неизвестная школа → 400")
    void create_unknownSchool_rejected() {
        HomebrewPackage pkg = draftPackage();
        when(homebrewAccessService.enforceOwner(eq(packageId), eq("gm"))).thenReturn(pkg);
        when(spellRepository.existsBySlugAndHomebrew_Id(any(), eq(packageId))).thenReturn(false);
        when(spellSchoolRepository.findBySlug("nonesuch")).thenReturn(Optional.empty());

        HomebrewSpellRequest req = new HomebrewSpellRequest();
        req.setName("Кривое");
        req.setLevel(1);
        req.setSchool("nonesuch");

        assertThrows(BadRequestException.class, () -> service.create(packageId, req, "gm"));
        verify(spellRepository, never()).save(any());
    }
}
