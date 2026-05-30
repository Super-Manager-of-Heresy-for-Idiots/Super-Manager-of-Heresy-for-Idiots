package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedStorageResponse {
    private UUID id;
    private String name;
    private UUID campaignId;
    private List<ItemInstanceResponse> items;
    private Instant createdAt;
}
