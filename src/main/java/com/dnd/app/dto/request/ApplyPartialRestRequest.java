package com.dnd.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс ApplyPartialRestRequest описывает решение мастера по прерванному привалу:
 * засчитать ли отряду частичный отдых. По умолчанию прерванный длинный отдых
 * восстановления не даёт, поэтому применение — явное действие мастера.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyPartialRestRequest {

    /** Пустой список — применить всем участникам привала. */
    private List<UUID> characterIds;
}
