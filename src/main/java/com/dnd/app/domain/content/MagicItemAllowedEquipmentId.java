package com.dnd.app.domain.content;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MagicItemAllowedEquipmentId implements Serializable {

    private UUID magicItem;
    private UUID equipmentItem;
}
