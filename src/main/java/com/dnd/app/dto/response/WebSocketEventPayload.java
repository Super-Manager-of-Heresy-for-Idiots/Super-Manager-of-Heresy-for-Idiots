package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketEventPayload {
    private String type;
    private UUID campaignId;
    private UUID characterId;
    private Object data;
    private Instant timestamp;
    private UUID triggeredBy;
}
