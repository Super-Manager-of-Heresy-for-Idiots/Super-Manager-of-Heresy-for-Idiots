package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** A feat a character has, with its display name and provenance. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterFeatResponse {
    private UUID id;
    private UUID featId;
    private String featName;
    private String source;
    private Instant grantedAt;
}
