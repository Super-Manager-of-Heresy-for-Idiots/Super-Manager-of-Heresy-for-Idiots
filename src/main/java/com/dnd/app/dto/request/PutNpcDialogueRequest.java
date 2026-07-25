package com.dnd.app.dto.request;

import com.dnd.app.dto.response.DialogueNodeDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс PutNpcDialogueRequest описывает запрос мастера на сохранение диалога NPC целиком
 * (WORLD_PLAN Этап 4). Дерево заменяется полностью; пустой список узлов равнозначен удалению.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PutNpcDialogueRequest {

    private String rootNodeId;

    @NotNull(message = "nodes is required (may be empty)")
    private List<DialogueNodeDto> nodes;
}
