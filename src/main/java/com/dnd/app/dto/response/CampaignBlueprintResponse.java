package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Класс CampaignBlueprintResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignBlueprintResponse {
    private UUID id;
    private String title;
    private String loreDescription;
    private String universeSlug;
    private String universeName;
    private String status;
    private Integer version;
    private Boolean allowForks;
    private Integer downloadCount;
    private String authorUsername;
    private String coverUrl;
    private List<String> tags;
    private Instant createdAt;
    private Instant publishedAt;
    private Boolean isDeleted;
    private UUID parentId;
    private Integer originVersion;
}
