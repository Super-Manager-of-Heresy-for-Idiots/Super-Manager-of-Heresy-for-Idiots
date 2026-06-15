package com.dnd.app.domain;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintHomebrewId implements Serializable {
    private UUID blueprintId;
    private UUID packageId;
}
