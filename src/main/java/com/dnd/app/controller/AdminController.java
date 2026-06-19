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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final HomebrewAdminService homebrewAdminService;
    private final Executor controllerTaskExecutor;

    // --- Stat Types ---

    @GetMapping("/stat-types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<StatTypeResponse>>>> listStatTypes() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listStatTypes())),
                controllerTaskExecutor);
    }

    @GetMapping("/stat-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<StatTypeResponse>>> getStatType(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getStatType(id))),
                controllerTaskExecutor);
    }

    // --- Item Types ---

    @GetMapping("/item-types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ItemTypeResponse>>>> listItemTypes() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listItemTypes())),
                controllerTaskExecutor);
    }

    @PostMapping("/item-types")
    public CompletableFuture<ResponseEntity<ApiResponse<ItemTypeResponse>>> createItemType(
            @Valid @RequestBody CreateItemTypeRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createItemType(request), "Тип предмета создан")),
                controllerTaskExecutor);
    }

    @GetMapping("/item-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<ItemTypeResponse>>> getItemType(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getItemType(id))),
                controllerTaskExecutor);
    }

    @PutMapping("/item-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<ItemTypeResponse>>> updateItemType(
            @PathVariable UUID id, @Valid @RequestBody CreateItemTypeRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.updateItemType(id, request), "Тип предмета обновлен")),
                controllerTaskExecutor);
    }

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

    @GetMapping("/skills")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SkillResponse>>>> listSkills() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listSkills())),
                controllerTaskExecutor);
    }

    @PostMapping("/skills")
    public CompletableFuture<ResponseEntity<ApiResponse<SkillResponse>>> createSkill(
            @Valid @RequestBody CreateSkillRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createSkill(request), "Умение создано")),
                controllerTaskExecutor);
    }

    @GetMapping("/skills/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<SkillResponse>>> getSkill(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getSkill(id))),
                controllerTaskExecutor);
    }

    @PutMapping("/skills/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<SkillResponse>>> updateSkill(
            @PathVariable UUID id, @Valid @RequestBody CreateSkillRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.updateSkill(id, request), "Умение обновлено")),
                controllerTaskExecutor);
    }

    @DeleteMapping("/skills/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteSkill(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            adminService.deleteSkill(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Умение удалено"));
        }, controllerTaskExecutor);
    }

    // --- Skill Effects ---

    @GetMapping("/skills/{id}/effects")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SkillEffectResponse>>>> getSkillEffects(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getSkillEffects(id))),
                controllerTaskExecutor);
    }

    @PutMapping("/skills/{id}/effects")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SkillEffectResponse>>>> setSkillEffects(
            @PathVariable UUID id, @Valid @RequestBody SetSkillEffectsRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.setSkillEffects(id, request), "Эффекты умения обновлены")),
                controllerTaskExecutor);
    }

    // Subclass CRUD lives entirely in ClassAuthoringController on the new content model.

    // --- Feats ---

    @GetMapping("/feats")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatResponse>>>> listFeats() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listFeats())),
                controllerTaskExecutor);
    }

    @PostMapping("/feats")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatResponse>>> createFeat(
            @Valid @RequestBody CreateFeatRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createFeat(request), "Черта создана")),
                controllerTaskExecutor);
    }

    @GetMapping("/feats/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatResponse>>> getFeat(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getFeat(id))),
                controllerTaskExecutor);
    }

    @PutMapping("/feats/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatResponse>>> updateFeat(
            @PathVariable UUID id, @Valid @RequestBody CreateFeatRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.updateFeat(id, request), "Черта обновлена")),
                controllerTaskExecutor);
    }

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

    @GetMapping("/backgrounds")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BackgroundResponse>>>> listBackgrounds() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listBackgrounds())),
                controllerTaskExecutor);
    }

    @PostMapping("/backgrounds")
    public CompletableFuture<ResponseEntity<ApiResponse<BackgroundResponse>>> createBackground(
            @Valid @RequestBody CreateBackgroundRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createBackground(request), "Предыстория создана")),
                controllerTaskExecutor);
    }

    @GetMapping("/backgrounds/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<BackgroundResponse>>> getBackground(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getBackground(id))),
                controllerTaskExecutor);
    }

    @PutMapping("/backgrounds/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<BackgroundResponse>>> updateBackground(
            @PathVariable UUID id, @Valid @RequestBody CreateBackgroundRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.updateBackground(id, request), "Предыстория обновлена")),
                controllerTaskExecutor);
    }

    @DeleteMapping("/backgrounds/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteBackground(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            adminService.deleteBackground(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Предыстория удалена"));
        }, controllerTaskExecutor);
    }

    // --- Spells ---

    @GetMapping("/spells")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SpellResponse>>>> listSpells() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listSpells())),
                controllerTaskExecutor);
    }

    @PostMapping("/spells")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellResponse>>> createSpell(
            @Valid @RequestBody CreateSpellRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createSpell(request), "Заклинание создано")),
                controllerTaskExecutor);
    }

    @GetMapping("/spells/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellResponse>>> getSpell(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.getSpell(id))),
                controllerTaskExecutor);
    }

    @PutMapping("/spells/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellResponse>>> updateSpell(
            @PathVariable UUID id, @Valid @RequestBody CreateSpellRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.updateSpell(id, request), "Заклинание обновлено")),
                controllerTaskExecutor);
    }

    @DeleteMapping("/spells/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteSpell(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            adminService.deleteSpell(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Заклинание удалено"));
        }, controllerTaskExecutor);
    }

    // --- Proficiency Skills ---

    @GetMapping("/proficiency-skills")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ProficiencySkillResponse>>>> listProficiencySkills() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listProficiencySkills())),
                controllerTaskExecutor);
    }

    @PostMapping("/proficiency-skills")
    public CompletableFuture<ResponseEntity<ApiResponse<ProficiencySkillResponse>>> createProficiencySkill(
            @Valid @RequestBody CreateProficiencySkillRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(adminService.createProficiencySkill(request), "Навык-владение создан")),
                controllerTaskExecutor);
    }

    @DeleteMapping("/proficiency-skills/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteProficiencySkill(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            adminService.deleteProficiencySkill(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Навык-владение удален"));
        }, controllerTaskExecutor);
    }

    // --- Users & Teams (read-only) ---

    @GetMapping("/users")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<UserResponse>>>> listUsers(Pageable pageable) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(adminService.listAllUsers(pageable))),
                controllerTaskExecutor);
    }

    // --- Homebrew Admin ---

    @GetMapping("/homebrew")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<HomebrewPackageResponse>>>> listAllHomebrewPackages(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID authorId,
            Pageable pageable) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(homebrewAdminService.listAllPackages(status, authorId, pageable))),
                controllerTaskExecutor);
    }

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

    @GetMapping("/homebrew/tags")
    public CompletableFuture<ResponseEntity<ApiResponse<List<HomebrewTagResponse>>>> listHomebrewTags() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(homebrewAdminService.listTagsWithUsageCount())),
                controllerTaskExecutor);
    }

    @DeleteMapping("/homebrew/tags/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteHomebrewTag(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            homebrewAdminService.deleteTag(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Тег удален"));
        }, controllerTaskExecutor);
    }
}
