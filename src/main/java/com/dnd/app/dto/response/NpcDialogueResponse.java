package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс NpcDialogueResponse описывает опциональный диалог NPC (WORLD_PLAN Этап 4): корневой узел
 * и дерево узлов. null/отсутствует, если у NPC диалог не настроен.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NpcDialogueResponse {
    private String rootNodeId;
    private List<DialogueNodeDto> nodes;
}
