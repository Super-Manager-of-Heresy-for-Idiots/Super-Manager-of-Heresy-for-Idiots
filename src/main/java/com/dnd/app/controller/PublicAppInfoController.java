package com.dnd.app.controller;

import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.LoginPageStatsResponse;
import com.dnd.app.service.LoginPageStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Класс PublicAppInfoController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicAppInfoController {

    private final LoginPageStatsService loginPageStatsService;

    /**
     * Выполняет операции "login stats" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/login-stats")
    public ApiResponse<LoginPageStatsResponse> loginStats() {
        return ApiResponse.ok(loginPageStatsService.getStats());
    }
}
