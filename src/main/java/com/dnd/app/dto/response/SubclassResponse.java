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
public class SubclassResponse {
    private UUID id;
    private String name;
    private UUID classId;
    private String className;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
