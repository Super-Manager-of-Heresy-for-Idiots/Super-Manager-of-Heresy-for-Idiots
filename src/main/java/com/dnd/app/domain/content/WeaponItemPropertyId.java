package com.dnd.app.domain.content;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WeaponItemPropertyId implements Serializable {

    private UUID equipmentItem;
    private UUID weaponProperty;
}
