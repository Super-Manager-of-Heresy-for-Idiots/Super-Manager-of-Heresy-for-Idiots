package com.dnd.app.dto.response;

import com.dnd.app.domain.enums.NpcRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс LocationOccupantsResponse описывает "кто сейчас находится в локации":
 * персонажи игроков (characters.current_location_id) и размещённые NPC
 * (campaign_npcs.location_id). Для игроков NPC фильтруются по видимости.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationOccupantsResponse {

    private UUID locationId;
    private List<CharacterOccupant> characters;
    private List<NpcOccupant> npcs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CharacterOccupant {
        private UUID id;
        private String name;
        private UUID ownerUserId;
        private String ownerUsername;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NpcOccupant {
        private UUID id;
        private String name;
        private NpcRole npcRole;
        private String portraitUrl;
        private Boolean isVisibleToPlayers;
    }
}
