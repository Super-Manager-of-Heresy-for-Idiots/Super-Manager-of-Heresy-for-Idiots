package com.dnd.app.service;

import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.ContentClassMapper;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentReferenceService: campaign-aware видимость новой контент-модели")
class ContentReferenceServiceTest {

    @Mock private ContentCharacterClassRepository classRepository;
    @Mock private CampaignHomebrewRepository campaignHomebrewRepository;
    @Mock private CampaignService campaignService;
    @Mock private UserRepository userRepository;
    @Mock private ContentClassMapper classMapper;

    @InjectMocks private ContentReferenceService service;

    private ContentCharacterClass coreClass(UUID id) {
        return ContentCharacterClass.builder().id(id).slug("fighter").nameRu("Воин").nameEn("Fighter").build();
    }

    private ContentCharacterClass homebrewClass(UUID id, UUID pkgId) {
        HomebrewPackage pkg = HomebrewPackage.builder().id(pkgId).build();
        return ContentCharacterClass.builder()
                .id(id).slug("stormbinder").nameRu("Повелитель бурь").nameEn("Stormbinder").homebrew(pkg).build();
    }

    @Test
    @DisplayName("getCampaignClasses возвращает core + классы активных homebrew-пакетов")
    void campaignClasses_includesCoreAndActivePackages() {
        UUID campaignId = UUID.randomUUID();
        UUID pkgId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).username("gm").build();
        Campaign campaign = mock(Campaign.class);

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(user));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        when(campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId)).thenReturn(Set.of(pkgId));
        when(classRepository.findAllByHomebrewIsNull()).thenReturn(List.of(coreClass(UUID.randomUUID())));
        when(classRepository.findAllByHomebrewIdIn(Set.of(pkgId)))
                .thenReturn(List.of(homebrewClass(UUID.randomUUID(), pkgId)));
        when(classMapper.toDetail(any(ContentCharacterClass.class), anyString()))
                .thenReturn(ContentClassDetailResponse.builder().build());

        List<ContentClassDetailResponse> result = service.getCampaignClasses(campaignId, "gm", "en");

        assertEquals(2, result.size());
        verify(campaignService).enforceMembershipOrAdmin(campaign, user);
    }

    @Test
    @DisplayName("getCampaignClasses без активных пакетов отдаёт только core")
    void campaignClasses_noPackages_coreOnly() {
        UUID campaignId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).username("gm").build();
        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(user));
        when(campaignService.findCampaign(campaignId)).thenReturn(mock(Campaign.class));
        when(campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId)).thenReturn(Set.of());
        when(classRepository.findAllByHomebrewIsNull()).thenReturn(List.of(coreClass(UUID.randomUUID())));
        when(classMapper.toDetail(any(ContentCharacterClass.class), anyString()))
                .thenReturn(ContentClassDetailResponse.builder().build());

        List<ContentClassDetailResponse> result = service.getCampaignClasses(campaignId, "gm", "en");

        assertEquals(1, result.size());
        verify(classRepository, never()).findAllByHomebrewIdIn(any());
    }

    @Test
    @DisplayName("getVanillaClasses не запрашивает homebrew-классы")
    void vanillaClasses_excludeHomebrew() {
        when(classRepository.findAllByHomebrewIsNull()).thenReturn(List.of(coreClass(UUID.randomUUID())));
        when(classMapper.toDetail(any(ContentCharacterClass.class), anyString()))
                .thenReturn(ContentClassDetailResponse.builder().build());

        service.getVanillaClasses("en");

        verify(classRepository, never()).findAllByHomebrewIdIn(any());
    }

    @Test
    @DisplayName("getCampaignClass: homebrew-класс из неактивного пакета => 404")
    void campaignClass_homebrewNotActive_throws404() {
        UUID campaignId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID activePkg = UUID.randomUUID();
        UUID otherPkg = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).username("gm").build();

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(user));
        when(campaignService.findCampaign(campaignId)).thenReturn(mock(Campaign.class));
        when(classRepository.findById(classId)).thenReturn(Optional.of(homebrewClass(classId, otherPkg)));
        when(campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId)).thenReturn(Set.of(activePkg));

        assertThrows(ResourceNotFoundException.class,
                () -> service.getCampaignClass(campaignId, classId, "gm", "en"));
    }

    @Test
    @DisplayName("getVanillaClass: homebrew-класс недоступен как vanilla => 404")
    void vanillaClass_homebrew_throws404() {
        UUID classId = UUID.randomUUID();
        when(classRepository.findById(classId))
                .thenReturn(Optional.of(homebrewClass(classId, UUID.randomUUID())));

        assertThrows(ResourceNotFoundException.class, () -> service.getVanillaClass(classId, "en"));
    }
}
