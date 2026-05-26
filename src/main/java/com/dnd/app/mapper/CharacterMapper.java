package com.dnd.app.mapper;

import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.InventorySlot;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.dto.response.CharacterResponse;
import com.dnd.app.dto.response.CharacterStatResponse;
import com.dnd.app.dto.response.InventorySlotResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CharacterMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    CharacterResponse toResponse(PlayerCharacter character);

    @Mapping(target = "statTypeId", source = "statType.id")
    @Mapping(target = "statTypeName", source = "statType.name")
    @Mapping(target = "effectiveValue", ignore = true)
    @Mapping(target = "activeModifiers", ignore = true)
    CharacterStatResponse toStatResponse(CharacterStat stat);

    List<CharacterStatResponse> toStatResponseList(List<CharacterStat> stats);

    @Mapping(target = "slot", expression = "java(slot.getSlot().name())")
    @Mapping(target = "itemTypeId", source = "itemType.id")
    @Mapping(target = "itemTypeName", source = "itemType.name")
    @Mapping(target = "artifactId", source = "artifact.id")
    @Mapping(target = "artifactName", source = "artifact.name")
    @Mapping(target = "artifactRarity", expression = "java(slot.getArtifact() != null ? slot.getArtifact().getRarity().name() : null)")
    InventorySlotResponse toInventorySlotResponse(InventorySlot slot);

    List<InventorySlotResponse> toInventorySlotResponseList(List<InventorySlot> slots);
}
