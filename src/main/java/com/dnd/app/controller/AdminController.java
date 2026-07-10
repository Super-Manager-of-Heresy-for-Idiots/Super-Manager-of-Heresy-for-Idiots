package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.AdminService;
import com.dnd.app.service.homebrew.HomebrewAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс AdminController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final HomebrewAdminService homebrewAdminService;
    private final Executor controllerTaskExecutor;

    // --- Stat Types ---

    /**
     * Возвращает список для операции "list stat types" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/stat-types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<StatTypeResponse>>>> listStatTypes() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listStatTypes())),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get stat type" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/stat-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<StatTypeResponse>>> getStatType(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getStatType(id))),
                controllerTaskExecutor);
    }

    // --- Item Types ---

    /**
     * Возвращает список для операции "list item types" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/item-types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ItemTypeResponse>>>> listItemTypes() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listItemTypes())),
                controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create item type" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/item-types")
    public CompletableFuture<ResponseEntity<ApiResponse<ItemTypeResponse>>> createItemType(
            @Valid @RequestBody CreateItemTypeRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createItemType(request), "Тип предмета создан")),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get item type" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/item-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<ItemTypeResponse>>> getItemType(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getItemType(id))),
                controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update item type" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/item-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<ItemTypeResponse>>> updateItemType(
            @PathVariable UUID id, @Valid @RequestBody CreateItemTypeRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.updateItemType(id, request), "Тип предмета обновлен")),
                controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete item type" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/item-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteItemType(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            adminService.deleteItemType(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Тип предмета удален"));
        }, controllerTaskExecutor);
    }

    // Character class CRUD lives entirely in ClassAuthoringController on the new content model.

    // Legacy character-race / race admin CRUD removed in S5 — species are authored on the
    // new content model; homebrew species attach via SpeciesContentValidator ("SPECIES").

    // --- Skills ---

    /**
     * Возвращает список для операции "list skills" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/skills")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SkillResponse>>>> listSkills() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listSkills())),
                controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create skill" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/skills")
    public CompletableFuture<ResponseEntity<ApiResponse<SkillResponse>>> createSkill(
            @Valid @RequestBody CreateSkillRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createSkill(request), "Умение создано")),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get skill" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/skills/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<SkillResponse>>> getSkill(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getSkill(id))),
                controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update skill" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/skills/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<SkillResponse>>> updateSkill(
            @PathVariable UUID id, @Valid @RequestBody CreateSkillRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.updateSkill(id, request), "Умение обновлено")),
                controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete skill" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/skills/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteSkill(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            adminService.deleteSkill(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Умение удалено"));
        }, controllerTaskExecutor);
    }

    // --- Skill Effects ---

    /**
     * Возвращает результат операции "get skill effects" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/skills/{id}/effects")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SkillEffectResponse>>>> getSkillEffects(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getSkillEffects(id))),
                controllerTaskExecutor);
    }

    /**
     * Устанавливает результат операции "set skill effects" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/skills/{id}/effects")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SkillEffectResponse>>>> setSkillEffects(
            @PathVariable UUID id, @Valid @RequestBody SetSkillEffectsRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.setSkillEffects(id, request), "Эффекты умения обновлены")),
                controllerTaskExecutor);
    }

    // Subclass CRUD lives entirely in ClassAuthoringController on the new content model.

    // --- Feats ---

    /**
     * Возвращает список для операции "list feats" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/feats")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatResponse>>>> listFeats() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listFeats())),
                controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create feat" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/feats")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatResponse>>> createFeat(
            @Valid @RequestBody CreateFeatRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createFeat(request), "Черта создана")),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get feat" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/feats/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatResponse>>> getFeat(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getFeat(id))),
                controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update feat" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/feats/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatResponse>>> updateFeat(
            @PathVariable UUID id, @Valid @RequestBody CreateFeatRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.updateFeat(id, request), "Черта обновлена")),
                controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete feat" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/feats/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteFeat(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            adminService.deleteFeat(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Черта удалена"));
        }, controllerTaskExecutor);
    }

    // Legacy class level-rewards endpoints removed — rewards are authored on the new content
    // model via the class-builder (ClassAuthoringController reward groups/options/grants).

    // --- Backgrounds ---

    /**
     * Возвращает список для операции "list backgrounds" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/backgrounds")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BackgroundResponse>>>> listBackgrounds() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listBackgrounds())),
                controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create background" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/backgrounds")
    public CompletableFuture<ResponseEntity<ApiResponse<BackgroundResponse>>> createBackground(
            @Valid @RequestBody CreateBackgroundRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createBackground(request), "Предыстория создана")),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get background" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/backgrounds/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<BackgroundResponse>>> getBackground(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getBackground(id))),
                controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update background" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/backgrounds/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<BackgroundResponse>>> updateBackground(
            @PathVariable UUID id, @Valid @RequestBody CreateBackgroundRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.updateBackground(id, request), "Предыстория обновлена")),
                controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete background" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/backgrounds/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteBackground(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            adminService.deleteBackground(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Предыстория удалена"));
        }, controllerTaskExecutor);
    }

    // --- Spells ---

    /**
     * Возвращает список для операции "list spells" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/spells")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SpellResponse>>>> listSpells() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listSpells())),
                controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create spell" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/spells")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellResponse>>> createSpell(
            @Valid @RequestBody CreateSpellRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createSpell(request), "Заклинание создано")),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get spell" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/spells/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellResponse>>> getSpell(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getSpell(id))),
                controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update spell" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/spells/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellResponse>>> updateSpell(
            @PathVariable UUID id, @Valid @RequestBody CreateSpellRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.updateSpell(id, request), "Заклинание обновлено")),
                controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete spell" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/spells/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteSpell(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            adminService.deleteSpell(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Заклинание удалено"));
        }, controllerTaskExecutor);
    }

    // --- Proficiency Skills ---

    /**
     * Возвращает список для операции "list proficiency skills" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/proficiency-skills")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ProficiencySkillResponse>>>> listProficiencySkills() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listProficiencySkills())),
                controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create proficiency skill" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/proficiency-skills")
    public CompletableFuture<ResponseEntity<ApiResponse<ProficiencySkillResponse>>> createProficiencySkill(
            @Valid @RequestBody CreateProficiencySkillRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createProficiencySkill(request), "Навык-владение создан")),
                controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete proficiency skill" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/proficiency-skills/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteProficiencySkill(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            adminService.deleteProficiencySkill(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Навык-владение удален"));
        }, controllerTaskExecutor);
    }

    // --- Users & Teams (read-only) ---

    /**
     * Возвращает список для операции "list users" в рамках бизнес-логики API.
     * @param pageable параметры постраничной выдачи для бизнес-списка
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/users")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<UserResponse>>>> listUsers(Pageable pageable) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listAllUsers(pageable))),
                controllerTaskExecutor);
    }

    // --- Homebrew Admin ---

    /**
     * Возвращает список для операции "list all homebrew packages" в рамках бизнес-логики API.
     * @param status входящее значение status, используемое бизнес-сценарием
     * @param authorId идентификатор author, используемый для выбора нужного бизнес-объекта
     * @param pageable параметры постраничной выдачи для бизнес-списка
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/homebrew")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<HomebrewPackageResponse>>>> listAllHomebrewPackages(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID authorId,
            Pageable pageable) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(homebrewAdminService.listAllPackages(status, authorId, pageable))),
                controllerTaskExecutor);
    }

    /**
     * Выполняет операции "hard delete homebrew" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/homebrew/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Map<String, Object>>>> hardDeleteHomebrew(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = homebrewAdminService.hardDelete(id);
            org.slf4j.LoggerFactory.getLogger("AUDIT").info(
                    "admin_action action=hardDeleteHomebrew actor={} target={} result={}",
                    auth.getName(), id, result);
            return ResponseEntity.ok(ApiResponse.ok(result));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list homebrew tags" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/homebrew/tags")
    public CompletableFuture<ResponseEntity<ApiResponse<List<HomebrewTagResponse>>>> listHomebrewTags() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(homebrewAdminService.listTagsWithUsageCount())),
                controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete homebrew tag" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/homebrew/tags/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteHomebrewTag(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            homebrewAdminService.deleteTag(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Тег удален"));
        }, controllerTaskExecutor);
    }
}
