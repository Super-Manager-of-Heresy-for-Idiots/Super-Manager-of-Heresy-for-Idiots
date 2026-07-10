package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateUniverseRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.UniverseResponse;
import com.dnd.app.service.UniverseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс UniverseController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/universes")
@RequiredArgsConstructor
@Tag(name = "Universes", description = "Игровые вселенные для шаблонов кампаний")
public class UniverseController {

    private final UniverseService universeService;
    private final Executor controllerTaskExecutor;

    /**
     * Возвращает список для операции "list universes" в рамках бизнес-логики API.
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "Список вселенных (для выбора)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<UniverseResponse>>>> listUniverses(Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<UniverseResponse> list = universeService.listUniverses(auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(list));
        }, controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create universe" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    @Operation(summary = "Создать вселенную (мастер игры/админ)")
    public CompletableFuture<ResponseEntity<ApiResponse<UniverseResponse>>> createUniverse(
            @Valid @RequestBody CreateUniverseRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            UniverseResponse response = universeService.createUniverse(request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response, "Вселенная создана"));
        }, controllerTaskExecutor);
    }
}
