package com.dnd.app.service;

import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.CampaignQuest;
import com.dnd.app.domain.CurrencyType;
import com.dnd.app.domain.ItemTemplate;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.QuestReward;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.QuestStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CompleteQuestRequest;
import com.dnd.app.dto.request.DistributeXpRequest;
import com.dnd.app.dto.request.GrantItemRequest;
import com.dnd.app.dto.request.ModifyCurrencyRequest;
import com.dnd.app.dto.response.QuestCompletionResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.CampaignQuestRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.QuestRewardRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QuestService: завершение квеста и выдача награды")
class QuestServiceTest {

    @Mock private CampaignQuestRepository questRepository;
    @Mock private com.dnd.app.repository.QuestNoteRepository noteRepository;
    @Mock private com.dnd.app.repository.QuestNpcRepository questNpcRepository;
    @Mock private com.dnd.app.repository.QuestLocationRepository questLocationRepository;
    @Mock private QuestRewardRepository questRewardRepository;
    @Mock private com.dnd.app.repository.CampaignNpcRepository npcRepository;
    @Mock private com.dnd.app.repository.CampaignLocationRepository locationRepository;
    @Mock private com.dnd.app.repository.ItemTemplateRepository itemTemplateRepository;
    @Mock private com.dnd.app.repository.CurrencyTypeRepository currencyTypeRepository;
    @Mock private PlayerCharacterRepository playerCharacterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignService campaignService;
    @Mock private WebSocketEventService webSocketEventService;
    @Mock private ItemInstanceService itemInstanceService;
    @Mock private WalletService walletService;
    @Mock private XpService xpService;

    @InjectMocks
    private QuestService questService;

    private static final String USERNAME = "gm";

    private UUID campaignId;
    private UUID questId;
    private UUID recipientId;
    private User user;
    private Campaign campaign;
    private PlayerCharacter recipient;
    private CampaignQuest quest;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        questId = UUID.randomUUID();
        recipientId = UUID.randomUUID();

        user = User.builder().id(UUID.randomUUID()).username(USERNAME).role(Role.PLAYER).build();
        campaign = Campaign.builder().id(campaignId).name("Camp").build();
        recipient = PlayerCharacter.builder()
                .id(recipientId).name("Hero").owner(user).campaign(campaign).experience(100L).build();
        quest = CampaignQuest.builder()
                .id(questId).campaign(campaign).title("Slay the dragon")
                .status(QuestStatus.ACTIVE).isVisibleToPlayers(true).createdBy(user).build();

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(questRepository.findById(questId)).thenReturn(Optional.of(quest));
        when(questRepository.save(any(CampaignQuest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(playerCharacterRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
    }

    private QuestReward itemReward(int quantity) {
        ItemTemplate template = ItemTemplate.builder().id(UUID.randomUUID()).name("Potion").build();
        return QuestReward.builder().id(UUID.randomUUID()).quest(quest)
                .itemTemplate(template).quantity(quantity).build();
    }

    private QuestReward currencyReward(BigDecimal amount) {
        CurrencyType gold = CurrencyType.builder().id(UUID.randomUUID()).name("Gold").build();
        return QuestReward.builder().id(UUID.randomUUID()).quest(quest)
                .currencyType(gold).currencyAmount(amount).build();
    }

    private QuestReward xpReward(int xp) {
        return QuestReward.builder().id(UUID.randomUUID()).quest(quest).xpAmount(xp).build();
    }

    @Test
    @DisplayName("Завершение начисляет предметы, валюту и опыт получателю, статус -> COMPLETED")
    void completeQuest_grantsItemsCurrencyAndXp() {
        when(questRewardRepository.findByQuestId(questId)).thenReturn(List.of(
                itemReward(2), currencyReward(new BigDecimal("50")), xpReward(300)));

        CompleteQuestRequest request = CompleteQuestRequest.builder()
                .recipientCharacterId(recipientId).build();

        QuestCompletionResponse response = questService.completeQuest(questId, request, USERNAME);

        assertEquals(QuestStatus.COMPLETED.name(), response.getStatus());
        assertEquals(recipientId, response.getRecipientCharacterId());
        assertEquals(1, response.getItemsGranted());
        assertEquals(300L, response.getXpGranted());
        assertEquals(QuestStatus.COMPLETED, quest.getStatus());

        ArgumentCaptor<GrantItemRequest> itemCaptor = ArgumentCaptor.forClass(GrantItemRequest.class);
        verify(itemInstanceService).grantItem(eq(campaignId), eq(recipientId), itemCaptor.capture(), eq(USERNAME));
        assertEquals(2, itemCaptor.getValue().getQuantity());

        ArgumentCaptor<ModifyCurrencyRequest> currencyCaptor = ArgumentCaptor.forClass(ModifyCurrencyRequest.class);
        verify(walletService).modifyCurrency(eq(recipientId), currencyCaptor.capture(), eq(USERNAME));
        assertEquals(0, new BigDecimal("50").compareTo(currencyCaptor.getValue().getAmount()));

        ArgumentCaptor<DistributeXpRequest> xpCaptor = ArgumentCaptor.forClass(DistributeXpRequest.class);
        verify(xpService).distributeXp(eq(campaignId), xpCaptor.capture(), eq(USERNAME));
        assertEquals(300L, xpCaptor.getValue().getAmount());
        assertEquals("SINGLE", xpCaptor.getValue().getTarget());
        assertEquals(List.of(recipientId), xpCaptor.getValue().getCharacterIds());
    }

    @Test
    @DisplayName("Опыт, заданный при выдаче, переопределяет XP записей награды")
    void completeQuest_xpOverride() {
        when(questRewardRepository.findByQuestId(questId)).thenReturn(List.of(xpReward(300)));

        CompleteQuestRequest request = CompleteQuestRequest.builder()
                .recipientCharacterId(recipientId).xpAmount(1000L).build();

        QuestCompletionResponse response = questService.completeQuest(questId, request, USERNAME);

        assertEquals(1000L, response.getXpGranted());
        ArgumentCaptor<DistributeXpRequest> xpCaptor = ArgumentCaptor.forClass(DistributeXpRequest.class);
        verify(xpService).distributeXp(eq(campaignId), xpCaptor.capture(), eq(USERNAME));
        assertEquals(1000L, xpCaptor.getValue().getAmount());
    }

    @Test
    @DisplayName("Без опыта в награде XP-сервис не вызывается")
    void completeQuest_noXp_skipsXpService() {
        when(questRewardRepository.findByQuestId(questId)).thenReturn(List.of(itemReward(1)));

        CompleteQuestRequest request = CompleteQuestRequest.builder()
                .recipientCharacterId(recipientId).build();

        QuestCompletionResponse response = questService.completeQuest(questId, request, USERNAME);

        assertEquals(0L, response.getXpGranted());
        verify(xpService, never()).distributeXp(any(), any(), any());
    }

    @Test
    @DisplayName("Получатель не из этой кампании -> ошибка, награда не выдаётся")
    void completeQuest_recipientFromOtherCampaign_throws() {
        Campaign other = Campaign.builder().id(UUID.randomUUID()).name("Other").build();
        recipient.setCampaign(other);

        CompleteQuestRequest request = CompleteQuestRequest.builder()
                .recipientCharacterId(recipientId).build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> questService.completeQuest(questId, request, USERNAME));
        assertTrue(ex.getMessage().contains("does not belong to this campaign"));
        verify(itemInstanceService, never()).grantItem(any(), any(), any(), any());
        verify(xpService, never()).distributeXp(any(), any(), any());
    }

    @Test
    @DisplayName("Повторное завершение запрещено")
    void completeQuest_alreadyCompleted_throws() {
        quest.setStatus(QuestStatus.COMPLETED);

        CompleteQuestRequest request = CompleteQuestRequest.builder()
                .recipientCharacterId(recipientId).build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> questService.completeQuest(questId, request, USERNAME));
        assertTrue(ex.getMessage().contains("already completed"));
        verify(questRewardRepository, never()).findByQuestId(any());
    }
}
