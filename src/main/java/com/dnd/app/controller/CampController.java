package com.dnd.app.controller;

import com.dnd.app.dto.request.ApplyPartialRestRequest;
import com.dnd.app.dto.request.AssignCampActivityRequest;
import com.dnd.app.dto.request.CompleteCampRestRequest;
import com.dnd.app.dto.request.CreateCampActivityRequest;
import com.dnd.app.dto.request.CreateCampEventRequest;
import com.dnd.app.dto.request.CreateCampRequest;
import com.dnd.app.dto.request.InterruptCampRequest;
import com.dnd.app.dto.request.StartCampRestRequest;
import com.dnd.app.dto.request.UpdateCampRequest;
import com.dnd.app.dto.request.UpdateCampWatchOrderRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.CampActivityResponse;
import com.dnd.app.dto.response.CampEventResponse;
import com.dnd.app.dto.response.CampRestSummaryResponse;
import com.dnd.app.dto.response.CampSessionResponse;
import com.dnd.app.service.CampActivityService;
import com.dnd.app.service.CampEventService;
import com.dnd.app.service.CampRestService;
import com.dnd.app.service.CampService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс CampController описывает REST-контроллер лагеря и привала: мастер управляет
 * состоянием привала, составом, дозором и групповым отдыхом, участники кампании читают
 * состояние. Все действия по изменению доступны только мастеру кампании или администратору.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/camp")
@RequiredArgsConstructor
@Tag(name = "Camp", description = "Camp lifecycle, party composition, watch order and group rest")
public class CampController {

    private final CampService campService;
    private final CampRestService campRestService;
    private final CampEventService campEventService;
    private final CampActivityService campActivityService;
    private final Executor controllerTaskExecutor;

    /**
     * Разбивает лагерь: создаёт привал в статусе SETTING_UP.
     * @param campaignId идентификатор кампании
     * @param request параметры привала
     * @param auth текущая аутентификация
     * @return созданный привал
     */
    @PostMapping
    @Operation(summary = "Set up a camp in the SETTING_UP state (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampSessionResponse>>> createCamp(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateCampRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampSessionResponse data = campService.createCamp(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Лагерь разбит"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает текущий незавершённый привал кампании.
     * @param campaignId идентификатор кампании
     * @param auth текущая аутентификация
     * @return состояние привала или пустой ответ, если привал не разбит
     */
    @GetMapping
    @Operation(summary = "Get the current camp of the campaign (members)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampSessionResponse>>> getCurrentCamp(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampSessionResponse data = campService.getCurrentCamp(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает историю привалов кампании.
     * @param campaignId идентификатор кампании
     * @param auth текущая аутентификация
     * @return список привалов, свежие первыми
     */
    @GetMapping("/history")
    @Operation(summary = "List campaign camps, newest first (members)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CampSessionResponse>>>> listCamps(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<CampSessionResponse> data = campService.listCamps(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает конкретный привал, включая завершённые.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param auth текущая аутентификация
     * @return состояние привала
     */
    @GetMapping("/{campId}")
    @Operation(summary = "Get a camp by id, including completed ones (members)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampSessionResponse>>> getCamp(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampSessionResponse data = campService.getCamp(campaignId, campId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Правит привал: название, описание, день, локацию и состав.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request изменяемые поля
     * @param auth текущая аутентификация
     * @return обновлённый привал
     */
    @PutMapping("/{campId}")
    @Operation(summary = "Update camp details and party composition (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampSessionResponse>>> updateCamp(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId,
            @Valid @RequestBody UpdateCampRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampSessionResponse data = campService.updateCamp(campaignId, campId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Привал обновлён"));
        }, controllerTaskExecutor);
    }

    /**
     * Перестраивает дозор целиком.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request новое расписание дозора
     * @param auth текущая аутентификация
     * @return обновлённый привал
     */
    @PutMapping("/{campId}/watch-order")
    @Operation(summary = "Replace the whole watch order (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampSessionResponse>>> updateWatchOrder(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId,
            @Valid @RequestBody UpdateCampWatchOrderRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampSessionResponse data = campService.updateWatchOrder(campaignId, campId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Дозор перестроен"));
        }, controllerTaskExecutor);
    }

    /**
     * Начинает привал (SETTING_UP или INTERRUPTED → ACTIVE).
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param auth текущая аутентификация
     * @return обновлённый привал
     */
    @PostMapping("/{campId}/start")
    @Operation(summary = "Move the camp to ACTIVE (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampSessionResponse>>> startCamp(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampSessionResponse data = campService.startCamp(campaignId, campId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Привал начат"));
        }, controllerTaskExecutor);
    }

    /**
     * Объявляет отдых отряда (ACTIVE → RESTING).
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request тип отдыха
     * @param auth текущая аутентификация
     * @return сводка запущенного отдыха
     */
    @PostMapping("/{campId}/rest")
    @Operation(summary = "Announce a group rest, moving the camp to RESTING (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampRestSummaryResponse>>> startRest(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId,
            @Valid @RequestBody StartCampRestRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampRestSummaryResponse data = campRestService.startRest(campaignId, campId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Отдых объявлен"));
        }, controllerTaskExecutor);
    }

    /**
     * Применяет объявленный отдых участникам привала (per-character транзакции).
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request точечный список персонажей для повтора (опционально)
     * @param auth текущая аутентификация
     * @return сводка отдыха с отдельным списком ошибок
     */
    @PostMapping("/{campId}/rest/complete")
    @Operation(summary = "Apply the announced rest per character; failures do not roll back others (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampRestSummaryResponse>>> completeRest(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId,
            @RequestBody(required = false) CompleteCampRestRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampRestSummaryResponse data = campRestService.completeRest(campaignId, campId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Отдых применён"));
        }, controllerTaskExecutor);
    }

    /**
     * Прерывает привал (ACTIVE или RESTING → INTERRUPTED).
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request причина прерывания
     * @param auth текущая аутентификация
     * @return обновлённый привал
     */
    @PostMapping("/{campId}/interrupt")
    @Operation(summary = "Interrupt the camp, no recovery is applied by default (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampSessionResponse>>> interruptCamp(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId,
            @RequestBody(required = false) InterruptCampRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampSessionResponse data = campService.interruptCamp(campaignId, campId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Привал прерван"));
        }, controllerTaskExecutor);
    }

    /**
     * Засчитывает частичный отдых прерванному привалу.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request точечный список персонажей (опционально)
     * @param auth текущая аутентификация
     * @return сводка частичного отдыха
     */
    @PostMapping("/{campId}/partial-rest")
    @Operation(summary = "Count a partial rest for an interrupted camp (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampRestSummaryResponse>>> applyPartialRest(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId,
            @RequestBody(required = false) ApplyPartialRestRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampRestSummaryResponse data = campRestService.applyPartialRest(
                    campaignId, campId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Частичный отдых засчитан"));
        }, controllerTaskExecutor);
    }

    /**
     * Сворачивает лагерь (терминальный статус COMPLETED).
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param auth текущая аутентификация
     * @return завершённый привал
     */
    @PostMapping("/{campId}/end")
    @Operation(summary = "Complete the camp (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampSessionResponse>>> endCamp(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampSessionResponse data = campService.endCamp(campaignId, campId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Привал завершён"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает журнал привала: мастер видит все записи, игрок — только видимые.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param auth текущая аутентификация
     * @return записи журнала
     */
    @GetMapping("/{campId}/events")
    @Operation(summary = "List camp log entries (GM: all, players: visible only)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CampEventResponse>>>> listEvents(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<CampEventResponse> data = campEventService.listEvents(campaignId, campId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Записывает событие привала; засада может создать бой и прервать привал.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request параметры события
     * @param auth текущая аутентификация
     * @return состояние привала после записи события
     */
    @PostMapping("/{campId}/events")
    @Operation(summary = "Add a camp log entry; an ambush may spawn a battle and interrupt the camp (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampSessionResponse>>> createEvent(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId,
            @Valid @RequestBody CreateCampEventRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampSessionResponse data = campEventService.createEvent(campaignId, campId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Событие записано"));
        }, controllerTaskExecutor);
    }

    /**
     * Триггерит подготовленную заготовку события.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param eventId идентификатор события
     * @param auth текущая аутентификация
     * @return состояние привала после срабатывания
     */
    @PostMapping("/{campId}/events/{eventId}/trigger")
    @Operation(summary = "Trigger a prepared camp event draft (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampSessionResponse>>> triggerEvent(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId,
            @PathVariable UUID eventId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampSessionResponse data = campEventService.triggerEvent(campaignId, campId, eventId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Событие сработало"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет запись журнала привала.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param eventId идентификатор события
     * @param auth текущая аутентификация
     * @return пустой успешный ответ
     */
    @DeleteMapping("/{campId}/events/{eventId}")
    @Operation(summary = "Delete a camp log entry (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteEvent(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId,
            @PathVariable UUID eventId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            campEventService.deleteEvent(campaignId, campId, eventId, auth.getName());
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Событие удалено"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает справочник даунтайм-активностей: системные плюс активности кампании.
     * @param campaignId идентификатор кампании
     * @param auth текущая аутентификация
     * @return доступные активности
     */
    @GetMapping("/activities")
    @Operation(summary = "List downtime activities available to the campaign (members)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CampActivityResponse>>>> listActivities(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<CampActivityResponse> data = campActivityService.listActivities(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Создаёт кастомную даунтайм-активность кампании.
     * @param campaignId идентификатор кампании
     * @param request параметры активности
     * @param auth текущая аутентификация
     * @return созданная активность
     */
    @PostMapping("/activities")
    @Operation(summary = "Create a custom downtime activity for the campaign (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampActivityResponse>>> createActivity(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateCampActivityRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampActivityResponse data = campActivityService.createActivity(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Активность создана"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет кастомную активность кампании.
     * @param campaignId идентификатор кампании
     * @param activityId идентификатор активности
     * @param auth текущая аутентификация
     * @return пустой успешный ответ
     */
    @DeleteMapping("/activities/{activityId}")
    @Operation(summary = "Delete a custom campaign downtime activity (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteActivity(
            @PathVariable UUID campaignId,
            @PathVariable UUID activityId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            campActivityService.deleteActivity(campaignId, activityId, auth.getName());
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Активность удалена"));
        }, controllerTaskExecutor);
    }

    /**
     * Назначает участнику привала даунтайм-активность или снимает её.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param characterId идентификатор персонажа
     * @param request активность и заметка мастера
     * @param auth текущая аутентификация
     * @return состояние привала после назначения
     */
    @PutMapping("/{campId}/participants/{characterId}/activity")
    @Operation(summary = "Assign or clear a downtime activity for a camp member (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampSessionResponse>>> assignActivity(
            @PathVariable UUID campaignId,
            @PathVariable UUID campId,
            @PathVariable UUID characterId,
            @Valid @RequestBody(required = false) AssignCampActivityRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampSessionResponse data = campActivityService.assignActivity(
                    campaignId, campId, characterId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Активность назначена"));
        }, controllerTaskExecutor);
    }
}
