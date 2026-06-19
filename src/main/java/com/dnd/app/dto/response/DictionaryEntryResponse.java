package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DictionaryEntryResponse {
    private UUID id;
    private String code;
    private String nameRusloc;
    private String nameEngloc;
    private String bookCode;
    private UUID homebrewId;
    private Boolean isUnique;
    private Instant createdAt;
    private Instant updatedAt;
}
