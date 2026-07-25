package com.dnd.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс CompleteCampRestRequest описывает применение группового отдыха.
 * Пустой список персонажей — применить всем участникам, у которых отдых ещё не применён;
 * непустой используется для точечного повтора после ошибки транзакции.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteCampRestRequest {

    private List<UUID> characterIds;
}
