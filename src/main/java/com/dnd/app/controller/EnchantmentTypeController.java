package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateEnchantmentTypeRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.EnchantmentTypeResponse;
import com.dnd.app.service.EnchantmentService;
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
 * Класс EnchantmentTypeController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequiredArgsConstructor
public class EnchantmentTypeController {

    private final EnchantmentService enchantmentService;
    private final Executor controllerTaskExecutor;

    /**
     * Возвращает список для операции "list admin" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/api/admin/enchantment-types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<EnchantmentTypeResponse>>>> listAdmin() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(enchantmentService.listEnchantmentTypes())),
                controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/api/admin/enchantment-types")
    public CompletableFuture<ResponseEntity<ApiResponse<EnchantmentTypeResponse>>> create(
            @Valid @RequestBody CreateEnchantmentTypeRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(enchantmentService.createEnchantmentType(request), "Тип зачарования создан")),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/api/admin/enchantment-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<EnchantmentTypeResponse>>> get(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(enchantmentService.getEnchantmentType(id))),
                controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/api/admin/enchantment-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<EnchantmentTypeResponse>>> update(
            @PathVariable UUID id, @Valid @RequestBody CreateEnchantmentTypeRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(enchantmentService.updateEnchantmentType(id, request), "Тип зачарования обновлен")),
                controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/api/admin/enchantment-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> delete(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            enchantmentService.deleteEnchantmentType(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Тип зачарования удален"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list public" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/api/enchantment-types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<EnchantmentTypeResponse>>>> listPublic() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(enchantmentService.listEnchantmentTypes())),
                controllerTaskExecutor);
    }
}
