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
public interface CharacterMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    CharacterResponse toResponse(PlayerCharacter character);

    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "className", expression = "java(ccl.getCharacterClass() != null ? ccl.getCharacterClass().getName() : null)")
    ClassLevelResponse toClassLevelResponse(CharacterClassLevel ccl);

    List<ClassLevelResponse> toClassLevelResponseList(List<CharacterClassLevel> levels);

    @Mapping(target = "statTypeId", source = "statType.id")
    @Mapping(target = "statTypeName", source = "statType.name")
    @Mapping(target = "effectiveValue", ignore = true)
    @Mapping(target = "activeModifiers", ignore = true)
    CharacterStatResponse toStatResponse(CharacterStat stat);

    List<CharacterStatResponse> toStatResponseList(List<CharacterStat> stats);
}
