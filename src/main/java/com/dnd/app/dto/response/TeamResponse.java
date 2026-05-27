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
public class TeamResponse {
    private UUID id;
    private String name;
    private UUID gameMasterId;
    private String gameMasterUsername;
    private List<TeamMemberResponse> members;
    private Instant createdAt;
    private Instant updatedAt;
}
