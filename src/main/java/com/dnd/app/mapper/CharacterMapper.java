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

/**
 * Класс CharacterMapper описывает маппер, который преобразует доменные модели и DTO без изменения бизнес-правил.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Mapper(componentModel = "spring")
public abstract class CharacterMapper {

    /**
     * Преобразует данные операции "to response" в рамках бизнес-логики преобразования данных.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerUsername", source = "owner.username")
    @Mapping(target = "campaignId", source = "campaign.id")
    @Mapping(target = "status", expression = "java(character.getStatus() != null ? character.getStatus().name() : null)")
    @Mapping(target = "race", ignore = true)
    @Mapping(target = "raceSnapshot", ignore = true)
    @Mapping(target = "background", ignore = true)
    @Mapping(target = "savingThrowProficiencyStatNames", ignore = true)
    @Mapping(target = "skillProficiencies", ignore = true)
    @Mapping(target = "knownSpells", ignore = true)
    @Mapping(target = "biography", ignore = true)
    @Mapping(target = "attacks", ignore = true)
    public abstract CharacterResponse toResponse(PlayerCharacter character);

    /**
     * Преобразует данные операции "to class level response" в рамках бизнес-логики преобразования данных.
     * @param ccl входящее значение ccl, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Mapping(target = "classId", source = "classId")
    @Mapping(target = "className", expression = "java(ccl.getCharacterClass() != null ? ccl.getCharacterClass().getNameRu() : null)")
    public abstract ClassLevelResponse toClassLevelResponse(CharacterClassLevel ccl);

    /**
     * Преобразует данные операции "to class level response list" в рамках бизнес-логики преобразования данных.
     * @param levels входящее значение levels, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public abstract List<ClassLevelResponse> toClassLevelResponseList(List<CharacterClassLevel> levels);

    /**
     * Преобразует данные операции "to stat response" в рамках бизнес-логики преобразования данных.
     * @param stat входящее значение stat, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Mapping(target = "statTypeId", source = "statType.id")
    @Mapping(target = "statTypeName", expression = "java(stat.getStatType() != null ? stat.getStatType().getNameRu() : null)")
    @Mapping(target = "effectiveValue", ignore = true)
    @Mapping(target = "activeModifiers", ignore = true)
    public abstract CharacterStatResponse toStatResponse(CharacterStat stat);

    /**
     * Преобразует данные операции "to stat response list" в рамках бизнес-логики преобразования данных.
     * @param stats входящее значение stats, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public abstract List<CharacterStatResponse> toStatResponseList(List<CharacterStat> stats);
}
