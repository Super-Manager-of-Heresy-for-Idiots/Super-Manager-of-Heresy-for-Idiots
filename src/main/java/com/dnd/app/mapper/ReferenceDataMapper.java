package com.dnd.app.mapper;

import com.dnd.app.domain.CharacterClass;
import com.dnd.app.domain.CharacterRace;
import com.dnd.app.domain.ItemType;
import com.dnd.app.domain.StatType;
import com.dnd.app.dto.response.CharacterClassResponse;
import com.dnd.app.dto.response.CharacterRaceResponse;
import com.dnd.app.dto.response.ItemTypeResponse;
import com.dnd.app.dto.response.StatTypeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReferenceDataMapper {

    @Mapping(target = "name", source = "nameRu")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    StatTypeResponse toStatTypeResponse(StatType statType);

    @Mapping(target = "slot", expression = "java(itemType.getSlot().getCode())")
    @Mapping(target = "damageDice", source = "damageDice")
    @Mapping(target = "damageBonus", source = "damageBonus")
    @Mapping(target = "damageType", expression = "java(itemType.getDamageType() != null ? itemType.getDamageType().getSlug() : null)")
    @Mapping(target = "skillId", expression = "java(itemType.getSkill() != null ? itemType.getSkill().getId() : null)")
    @Mapping(target = "skillName", expression = "java(itemType.getSkill() != null ? itemType.getSkill().getName() : null)")
    @Mapping(target = "skillActivation", expression = "java(itemType.getSkillActivation() != null ? itemType.getSkillActivation().name() : null)")
    ItemTypeResponse toItemTypeResponse(ItemType itemType);

    CharacterClassResponse toCharacterClassResponse(CharacterClass characterClass);

    CharacterRaceResponse toCharacterRaceResponse(CharacterRace characterRace);
}
