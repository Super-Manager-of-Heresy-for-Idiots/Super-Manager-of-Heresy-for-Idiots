package com.dnd.app.controller;

import com.dnd.app.dto.response.CampaignAccessResponse;
import com.dnd.app.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Класс InternalCampaignController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/internal/campaigns/{campaignId}")
@RequiredArgsConstructor
@Tag(name = "Internal Campaign", description = "Service-to-service campaign access checks")
public class InternalCampaignController {

    private final CampaignService campaignService;

    /**
     * Возвращает результат операции "get campaign access" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/access")
    @Operation(summary = "What a user may do with campaign maps")
    public ResponseEntity<CampaignAccessResponse> getCampaignAccess(
            @PathVariable UUID campaignId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(campaignService.getCampaignAccess(campaignId, userId));
    }
}
