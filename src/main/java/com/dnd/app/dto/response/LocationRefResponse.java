package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс LocationRefResponse описывает короткую ссылку на локацию кампании (id + имя)
 * для встраивания в ответы других доменов (персонаж, NPC, бой).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationRefResponse {
    private UUID id;
    private String name;
}
