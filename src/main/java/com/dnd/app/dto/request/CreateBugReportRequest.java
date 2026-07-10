package com.dnd.app.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс CreateBugReportRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBugReportRequest {

    @Size(max = 4000)
    private String userDescription;

    @Size(max = 80)
    private String severity;

    @Size(max = 120)
    private String source;

    @Size(max = 500)
    private String errorName;

    @Size(max = 4000)
    private String errorMessage;

    private String errorStack;

    @Size(max = 4000)
    private String url;

    @Size(max = 2000)
    private String route;

    @Size(max = 200)
    private String appVersion;

    @Size(max = 80)
    private String clientTimestamp;

    @Size(max = 4000)
    private String userAgent;

    private JsonNode consoleLogs;
    private JsonNode networkLogs;
    private JsonNode serviceLogs;
    private JsonNode device;
    private JsonNode performance;
    private JsonNode breadcrumbs;
    private JsonNode extra;
}
