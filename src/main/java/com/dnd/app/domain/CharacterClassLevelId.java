package com.dnd.app.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharacterClassLevelId implements Serializable {
    private UUID characterId;
    private UUID classId;
}
