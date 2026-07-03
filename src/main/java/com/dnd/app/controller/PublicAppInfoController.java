package com.dnd.app.controller;

import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.LoginPageStatsResponse;
import com.dnd.app.service.LoginPageStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicAppInfoController {

    private final LoginPageStatsService loginPageStatsService;

    @GetMapping("/login-stats")
    public ApiResponse<LoginPageStatsResponse> loginStats() {
        return ApiResponse.ok(loginPageStatsService.getStats());
    }
}
