package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс DialogueNodeDto описывает узел диалога NPC (WORLD_PLAN Этап 4): реплика NPC и варианты
 * ответа игрока. Идентификатор узла ({@code id}) уникален в пределах дерева и используется как
 * ссылка {@code nextNodeId} у вариантов.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DialogueNodeDto {
    private String id;
    private String npcText;
    @Builder.Default
    private List<DialogueOptionDto> options = new ArrayList<>();
}
