package com.dnd.app.dto.response;

import com.dnd.app.dto.request.RaceSpeedDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterRaceSnapshotResponse {
    private UUID raceId;
    private String raceName;
    private UUID lineageId;
    private String lineageName;
    private String size;
    private RaceSpeedDto speed;
    private Integer darkvisionRange;
    private List<String> traitNames;
    private Boolean allowAbilityScoreBonuses;
}
