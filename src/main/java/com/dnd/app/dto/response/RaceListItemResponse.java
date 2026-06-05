package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceListItemResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String sourceType;
    private String sourceName;
    private Boolean active;
    private UUID homebrewId;
    private String homebrewTitle;
    private Boolean lineageRequired;
    private Boolean allowAbilityScoreBonuses;
}
