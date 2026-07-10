package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateBuffDebuffRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.BuffDebuffResponse;
import com.dnd.app.service.BuffDebuffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс BuffDebuffController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/admin/buffs-debuffs")
@RequiredArgsConstructor
public class BuffDebuffController {

    private final BuffDebuffService buffDebuffService;
    private final Executor controllerTaskExecutor;

    /**
     * Возвращает список для операции "list" в рамках бизнес-логики API.
     * @param isBuff входящее значение is buff, используемое бизнес-сценарием
     * @param effectType входящее значение effect type, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    public CompletableFuture<ResponseEntity<ApiResponse<List<BuffDebuffResponse>>>> list(
            @RequestParam(required = false) Boolean isBuff,
            @RequestParam(required = false) String effectType) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(buffDebuffService.findAll(isBuff, effectType))),
                controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<ApiResponse<BuffDebuffResponse>>> create(
            @Valid @RequestBody CreateBuffDebuffRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(buffDebuffService.create(request), "Бафф/дебафф создан")),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<BuffDebuffResponse>>> get(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(buffDebuffService.findById(id))),
                controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<BuffDebuffResponse>>> update(
            @PathVariable UUID id, @Valid @RequestBody CreateBuffDebuffRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(buffDebuffService.update(id, request), "Бафф/дебафф обновлен")),
                controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> delete(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            buffDebuffService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Бафф/дебафф удален"));
        }, controllerTaskExecutor);
    }
}
