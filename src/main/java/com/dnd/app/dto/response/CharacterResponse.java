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
public class CharacterResponse {
    private UUID id;
    private String name;
    private Integer totalLevel;
    private Long experience;
    private List<ClassLevelResponse> classLevels;
    private CharacterRaceResponse race;
    private UUID selectedLineageId;
    private CharacterRaceSnapshotResponse raceSnapshot;
    private UUID ownerId;
    private String ownerUsername;
    private List<CharacterStatResponse> stats;
    private Integer currentHp;
    private Integer maxHp;

    private String alignment;
    private BackgroundResponse background;
    private String avatarUrl;
    private Integer armorClass;
    private Integer speed;
    private Boolean inspiration;
    private String hitDiceType;
    private String hitDiceTotal;
    private Integer deathSaveSuccesses;
    private Integer deathSaveFailures;
    private List<String> savingThrowProficiencyStatNames;
    private List<CharacterSkillProficiencyResponse> skillProficiencies;
    private List<CharacterKnownSpellResponse> knownSpells;
    private BiographyResponse biography;
    private String features;
    private List<CharacterAttackResponse> attacks;

    private Instant createdAt;
    private Instant updatedAt;
}
