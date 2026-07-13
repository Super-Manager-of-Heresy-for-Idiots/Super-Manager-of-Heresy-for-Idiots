package com.dnd.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на настройку предмета.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttuneItemRequest {
    private Boolean gmOverride;
    private String clientCommandId;
}
