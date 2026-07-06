package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureTrigger;
import com.dnd.app.domain.featurerule.PendingGameplayPrompt;
import com.dnd.app.dto.featurerule.PendingPromptResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.FeatureTriggerRepository;
import com.dnd.app.repository.PendingGameplayPromptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingGameplayPromptServiceTest {

    @Mock private PendingGameplayPromptRepository promptRepository;
    @Mock private FeatureTriggerRepository triggerRepository;
    @Mock private GameplayEventService gameplayEventService;
    @Mock private CombatActionEconomyService economyService;

    @InjectMocks private PendingGameplayPromptService service;

    private final UUID charId = UUID.randomUUID();
    private final UUID promptId = UUID.randomUUID();

    private PendingGameplayPrompt prompt(String status, UUID owner) {
        return PendingGameplayPrompt.builder().id(promptId).characterId(owner).status(status).build();
    }

    @Test
    void resolveMarksResolved() {
        when(promptRepository.findById(promptId)).thenReturn(Optional.of(prompt("pending", charId)));
        when(promptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PendingPromptResponse r = service.resolve(charId, promptId);
        assertThat(r.getStatus()).isEqualTo("resolved");
    }

    @Test
    void resolveRejectsAlreadyProcessed() {
        when(promptRepository.findById(promptId)).thenReturn(Optional.of(prompt("resolved", charId)));
        assertThatThrownBy(() -> service.resolve(charId, promptId)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolveRejectsWrongCharacter() {
        when(promptRepository.findById(promptId)).thenReturn(Optional.of(prompt("pending", UUID.randomUUID())));
        assertThatThrownBy(() -> service.resolve(charId, promptId)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolveSpendsReactionWhenTriggerConsumesReaction() {
        UUID combatId = UUID.randomUUID();
        UUID triggerId = UUID.randomUUID();
        PendingGameplayPrompt p = PendingGameplayPrompt.builder()
                .id(promptId).characterId(charId).status("pending")
                .combatId(combatId).featureTriggerId(triggerId).build();
        when(promptRepository.findById(promptId)).thenReturn(Optional.of(p));
        when(promptRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(triggerRepository.findById(triggerId)).thenReturn(Optional.of(
                FeatureTrigger.builder().id(triggerId).consumesReaction(true).build()));

        service.resolve(charId, promptId);

        // The character's one reaction for the round must be consumed on resolve.
        verify(economyService).spend(combatId, charId, "reaction");
    }

    @Test
    void declineMarksDeclined() {
        when(promptRepository.findById(promptId)).thenReturn(Optional.of(prompt("pending", charId)));
        when(promptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PendingPromptResponse r = service.decline(charId, promptId);
        assertThat(r.getStatus()).isEqualTo("declined");
    }
}
