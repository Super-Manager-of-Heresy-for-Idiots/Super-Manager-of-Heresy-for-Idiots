package com.dnd.app.service;

import com.dnd.app.domain.Background;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.featurerule.FeatureProficiencyGrant;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.repository.BackgroundRepository;
import com.dnd.app.repository.FeatureProficiencyGrantRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackgroundProficiencyBackfillServiceTest {

    @Mock private BackgroundRepository backgroundRepository;
    @Mock private FeatureRuleRepository ruleRepository;
    @Mock private FeatureProficiencyGrantRepository proficiencyGrantRepository;
    @Mock private FeatureRuleRevisionService revisionService;

    @InjectMocks private BackgroundProficiencyBackfillService service;

    private Background backgroundWithSkills(UUID id, UUID... skillIds) {
        List<ContentSkill> skills = java.util.Arrays.stream(skillIds)
                .map(sid -> ContentSkill.builder().id(sid).build())
                .toList();
        return Background.builder().id(id).skillProficiencies(new java.util.ArrayList<>(skills)).build();
    }

    @Test
    void backfillCreatesApprovedRuleWithOneGrantPerSkill() {
        UUID bgId = UUID.randomUUID();
        when(backgroundRepository.findAll())
                .thenReturn(List.of(backgroundWithSkills(bgId, UUID.randomUUID(), UUID.randomUUID())));
        when(ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(eq("BACKGROUND"), eq(bgId)))
                .thenReturn(List.of());
        UUID ruleId = UUID.randomUUID();
        when(ruleRepository.save(any())).thenAnswer(i -> {
            FeatureRule r = i.getArgument(0);
            r.setId(ruleId);
            return r;
        });

        int created = service.backfill(true);

        assertThat(created).isEqualTo(1);
        verify(proficiencyGrantRepository, times(2)).save(any(FeatureProficiencyGrant.class));
        verify(revisionService).createInitialDraft(any(), anyString());
        verify(revisionService).approveCurrent(eq(ruleId), anyString(), anyString());
    }

    @Test
    void backfillIsIdempotentWhenBackgroundAlreadyHasRules() {
        UUID bgId = UUID.randomUUID();
        when(backgroundRepository.findAll())
                .thenReturn(List.of(backgroundWithSkills(bgId, UUID.randomUUID())));
        when(ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(eq("BACKGROUND"), eq(bgId)))
                .thenReturn(List.of(FeatureRule.builder().id(UUID.randomUUID()).build()));

        int created = service.backfill(true);

        assertThat(created).isZero();
        verify(ruleRepository, never()).save(any());
        verify(proficiencyGrantRepository, never()).save(any());
    }

    @Test
    void dryRunCountsButPersistsNothing() {
        UUID bgId = UUID.randomUUID();
        when(backgroundRepository.findAll())
                .thenReturn(List.of(backgroundWithSkills(bgId, UUID.randomUUID())));
        when(ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(eq("BACKGROUND"), eq(bgId)))
                .thenReturn(List.of());

        int would = service.backfill(false);

        assertThat(would).isEqualTo(1);
        verify(ruleRepository, never()).save(any());
        verify(revisionService, never()).createInitialDraft(any(), anyString());
    }
}
