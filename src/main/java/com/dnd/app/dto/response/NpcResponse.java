package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NpcResponse {
    private UUID id;
    private String name;
    private String publicDescription;
    private String privateDescription;
    private Boolean isVisibleToPlayers;
    private List<NoteResponse> notes;
    private Instant createdAt;
    private Instant updatedAt;
}
