package com.dnd.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс SetNpcLocationRequest описывает DTO запроса размещения NPC в локации кампании.
 * locationId = null снимает NPC с локации ("нигде").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetNpcLocationRequest {

    /** Целевая локация; null — убрать NPC из мира. */
    private UUID locationId;
}
