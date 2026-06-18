package com.dnd.app.mapper;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.dto.response.CharacterResponse;
import com.dnd.app.dto.response.CharacterStatResponse;
import com.dnd.app.dto.response.ClassLevelResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class CharacterMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    @Mapping(target = "campaignId", source = "campaign.id")
    @Mapping(target = "status", expression = "java(character.getStatus() != null ? character.getStatus().name() : null)")
    @Mapping(target = "raceSnapshot", ignore = true)
    @Mapping(target = "background", ignore = true)
    @Mapping(target = "savingThrowProficiencyStatNames", ignore = true)
    @Mapping(target = "skillProficiencies", ignore = true)
    @Mapping(target = "knownSpells", ignore = true)
    @Mapping(target = "biography", ignore = true)
    @Mapping(target = "attacks", ignore = true)
    public abstract CharacterResponse toResponse(PlayerCharacter character);

    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "className", expression = "java(ccl.getCharacterClass() != null ? ccl.getCharacterClass().getNameRu() : null)")
    public abstract ClassLevelResponse toClassLevelResponse(CharacterClassLevel ccl);

    public abstract List<ClassLevelResponse> toClassLevelResponseList(List<CharacterClassLevel> levels);

    @Mapping(target = "statTypeId", source = "statType.id")
    @Mapping(target = "statTypeName", expression = "java(stat.getStatType() != null ? stat.getStatType().getNameRu() : null)")
    @Mapping(target = "effectiveValue", ignore = true)
    @Mapping(target = "activeModifiers", ignore = true)
    public abstract CharacterStatResponse toStatResponse(CharacterStat stat);

    public abstract List<CharacterStatResponse> toStatResponseList(List<CharacterStat> stats);
}
