package com.dnd.app.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Класс WebSocketBroadcastListener описывает событие домена, которое передает изменения бизнес-состояния подписчикам.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Обрабатывает событие операции "on campaign broadcast" в рамках бизнес-логики приложения.
     * @param event входящее значение event, используемое бизнес-сценарием
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCampaignBroadcast(WsCampaignBroadcastEvent event) {
        messagingTemplate.convertAndSend(event.destination(), event.payload());
        log.debug("WS campaign broadcast delivered: destination={}, type={}",
                event.destination(), event.payload().getType());
    }

    /**
     * Обрабатывает событие операции "on user broadcast" в рамках бизнес-логики приложения.
     * @param event входящее значение event, используемое бизнес-сценарием
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserBroadcast(WsUserBroadcastEvent event) {
        messagingTemplate.convertAndSendToUser(event.username(), event.destination(), event.payload());
        log.debug("WS user broadcast delivered: user={}, type={}",
                event.username(), event.payload().getType());
    }
}
