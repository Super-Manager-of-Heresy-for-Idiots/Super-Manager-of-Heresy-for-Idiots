package com.dnd.app.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers buffered WebSocket broadcasts to the STOMP broker (in-memory SimpleBroker or
 * the RabbitMQ relay) once the surrounding DB transaction has committed.
 *
 * <p>Using {@link TransactionalEventListener} with {@code AFTER_COMMIT} guarantees the
 * client is only notified about changes that are actually durable; {@code fallbackExecution}
 * keeps it working for the rare publisher that runs outside a transaction. {@code @Async}
 * keeps STOMP serialization/delivery off the request (and commit) thread.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCampaignBroadcast(WsCampaignBroadcastEvent event) {
        messagingTemplate.convertAndSend(event.destination(), event.payload());
        log.debug("WS campaign broadcast delivered: destination={}, type={}",
                event.destination(), event.payload().getType());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserBroadcast(WsUserBroadcastEvent event) {
        messagingTemplate.convertAndSendToUser(event.username(), event.destination(), event.payload());
        log.debug("WS user broadcast delivered: user={}, type={}",
                event.username(), event.payload().getType());
    }
}
