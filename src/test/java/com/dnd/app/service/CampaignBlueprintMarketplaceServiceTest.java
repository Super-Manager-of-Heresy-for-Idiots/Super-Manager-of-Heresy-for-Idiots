package com.dnd.app.service;

import com.dnd.app.domain.CampaignBlueprint;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.CampaignBlueprintResponse;
import com.dnd.app.repository.BlueprintHomebrewRepository;
import com.dnd.app.repository.BlueprintLocationRepository;
import com.dnd.app.repository.BlueprintNpcRepository;
import com.dnd.app.repository.BlueprintQuestRepository;
import com.dnd.app.repository.CampaignBlueprintRepository;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.CampaignLocationRepository;
import com.dnd.app.repository.CampaignMemberRepository;
import com.dnd.app.repository.CampaignNpcRepository;
import com.dnd.app.repository.CampaignQuestRepository;
import com.dnd.app.repository.CampaignRepository;
import com.dnd.app.repository.HomebrewPackageRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignBlueprintMarketplaceServiceTest {

    @Mock private CampaignBlueprintRepository blueprintRepository;
    @Mock private CampaignBlueprintService blueprintService;
    @Mock private BlueprintNpcRepository npcRepository;
    @Mock private BlueprintQuestRepository questRepository;
    @Mock private BlueprintLocationRepository locationRepository;
    @Mock private BlueprintHomebrewRepository blueprintHomebrewRepository;
    @Mock private HomebrewPackageRepository homebrewPackageRepository;
    @Mock private PlayerCharacterRepository playerCharacterRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignMemberRepository campaignMemberRepository;
    @Mock private CampaignHomebrewRepository campaignHomebrewRepository;
    @Mock private CampaignNpcRepository campaignNpcRepository;
    @Mock private CampaignQuestRepository campaignQuestRepository;
    @Mock private CampaignLocationRepository campaignLocationRepository;
    @Mock private CampaignService campaignService;

    @InjectMocks private CampaignBlueprintMarketplaceService service;

    private CampaignBlueprint blueprint;
    private CampaignBlueprintResponse response;

    @BeforeEach
    void setUp() {
        blueprint = CampaignBlueprint.builder()
                .id(UUID.randomUUID())
                .title("Lost Mine")
                .build();
        response = CampaignBlueprintResponse.builder()
                .id(blueprint.getId())
                .title(blueprint.getTitle())
                .build();
    }

    @Test
    @DisplayName("Blank marketplace filters use the unfiltered query")
    void browseMarketplace_withoutSearchOrUniverse_usesUnfilteredQuery() {
        givenAuthenticatedUser();
        when(blueprintRepository.findMarketplace(any(Pageable.class)))
                .thenReturn(pageWithBlueprint());
        when(blueprintService.toResponse(blueprint)).thenReturn(response);

        Page<CampaignBlueprintResponse> result =
                service.browseMarketplace("   ", "", "downloads", 0, 12, "GM2");

        assertThat(result.getContent()).containsExactly(response);
        verify(blueprintRepository).findMarketplace(any(Pageable.class));
        verify(blueprintRepository, never()).findMarketplaceBySearch(any(), any(Pageable.class));
        verify(blueprintRepository, never()).findMarketplaceByUniverseSlug(any(), any(Pageable.class));
        verify(blueprintRepository, never()).findMarketplaceBySearchAndUniverseSlug(
                any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Marketplace search uses the search query with a trimmed term")
    void browseMarketplace_withSearch_usesSearchQuery() {
        givenAuthenticatedUser();
        when(blueprintRepository.findMarketplaceBySearch(eq("dragon"), any(Pageable.class)))
                .thenReturn(pageWithBlueprint());
        when(blueprintService.toResponse(blueprint)).thenReturn(response);

        Page<CampaignBlueprintResponse> result =
                service.browseMarketplace(" dragon ", null, "newest", 0, 12, "GM2");

        assertThat(result.getContent()).containsExactly(response);
        verify(blueprintRepository).findMarketplaceBySearch(eq("dragon"), any(Pageable.class));
        verify(blueprintRepository, never()).findMarketplace(any(Pageable.class));
    }

    @Test
    @DisplayName("Marketplace universe filter uses the universe query")
    void browseMarketplace_withUniverse_usesUniverseQuery() {
        givenAuthenticatedUser();
        when(blueprintRepository.findMarketplaceByUniverseSlug(eq("forgotten-realms"), any(Pageable.class)))
                .thenReturn(pageWithBlueprint());
        when(blueprintService.toResponse(blueprint)).thenReturn(response);

        Page<CampaignBlueprintResponse> result =
                service.browseMarketplace(null, " forgotten-realms ", "newest", 0, 12, "GM2");

        assertThat(result.getContent()).containsExactly(response);
        verify(blueprintRepository).findMarketplaceByUniverseSlug(eq("forgotten-realms"), any(Pageable.class));
        verify(blueprintRepository, never()).findMarketplace(any(Pageable.class));
    }

    @Test
    @DisplayName("Marketplace search and universe filters use the combined query")
    void browseMarketplace_withSearchAndUniverse_usesCombinedQuery() {
        givenAuthenticatedUser();
        when(blueprintRepository.findMarketplaceBySearchAndUniverseSlug(
                eq("dragon"), eq("forgotten-realms"), any(Pageable.class)))
                .thenReturn(pageWithBlueprint());
        when(blueprintService.toResponse(blueprint)).thenReturn(response);

        Page<CampaignBlueprintResponse> result =
                service.browseMarketplace(" dragon ", " forgotten-realms ", "newest", 0, 12, "GM2");

        assertThat(result.getContent()).containsExactly(response);
        verify(blueprintRepository).findMarketplaceBySearchAndUniverseSlug(
                eq("dragon"), eq("forgotten-realms"), any(Pageable.class));
        verify(blueprintRepository, never()).findMarketplace(any(Pageable.class));
    }

    private void givenAuthenticatedUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("GM2")
                .role(Role.GAME_MASTER)
                .build();
        when(blueprintService.getUser("GM2")).thenReturn(user);
    }

    private Page<CampaignBlueprint> pageWithBlueprint() {
        return new PageImpl<>(List.of(blueprint));
    }
}
