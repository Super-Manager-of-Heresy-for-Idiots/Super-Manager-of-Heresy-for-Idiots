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

    StatTypeResponse toStatTypeResponse(StatType statType);

    @Mapping(target = "slot", expression = "java(itemType.getSlot().name())")
    ItemTypeResponse toItemTypeResponse(ItemType itemType);

    CharacterClassResponse toCharacterClassResponse(CharacterClass characterClass);

    CharacterRaceResponse toCharacterRaceResponse(CharacterRace characterRace);
}
