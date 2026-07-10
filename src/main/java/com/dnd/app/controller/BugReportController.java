package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateBugReportRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.BugReportResponse;
import com.dnd.app.service.BugReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Класс BugReportController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/bug-reports")
@RequiredArgsConstructor
public class BugReportController {

    private final BugReportService bugReportService;

    /**
     * Создает результат операции "create" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @param httpRequest входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BugReportResponse>> create(
            @Valid @RequestBody CreateBugReportRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String username = authentication != null ? authentication.getName() : null;
        BugReportResponse response = bugReportService.create(
                request,
                username,
                clientIp(httpRequest),
                httpRequest.getHeader(HttpHeaders.USER_AGENT));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Bug report saved"));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
