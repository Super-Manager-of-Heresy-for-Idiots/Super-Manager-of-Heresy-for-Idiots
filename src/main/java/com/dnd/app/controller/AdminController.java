package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // --- Stat Types ---

    @GetMapping("/stat-types")
    public ResponseEntity<ApiResponse<List<StatTypeResponse>>> listStatTypes() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listStatTypes()));
    }

    @PostMapping("/stat-types")
    public ResponseEntity<ApiResponse<StatTypeResponse>> createStatType(
            @Valid @RequestBody CreateStatTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminService.createStatType(request), "Stat type created"));
    }

    @GetMapping("/stat-types/{id}")
    public ResponseEntity<ApiResponse<StatTypeResponse>> getStatType(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getStatType(id)));
    }

    @PutMapping("/stat-types/{id}")
    public ResponseEntity<ApiResponse<StatTypeResponse>> updateStatType(
            @PathVariable UUID id, @Valid @RequestBody CreateStatTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateStatType(id, request), "Stat type updated"));
    }

    @DeleteMapping("/stat-types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStatType(@PathVariable UUID id) {
        adminService.deleteStatType(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Stat type deleted"));
    }

    // --- Item Types ---

    @GetMapping("/item-types")
    public ResponseEntity<ApiResponse<List<ItemTypeResponse>>> listItemTypes() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listItemTypes()));
    }

    @PostMapping("/item-types")
    public ResponseEntity<ApiResponse<ItemTypeResponse>> createItemType(
            @Valid @RequestBody CreateItemTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminService.createItemType(request), "Item type created"));
    }

    @GetMapping("/item-types/{id}")
    public ResponseEntity<ApiResponse<ItemTypeResponse>> getItemType(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getItemType(id)));
    }

    @PutMapping("/item-types/{id}")
    public ResponseEntity<ApiResponse<ItemTypeResponse>> updateItemType(
            @PathVariable UUID id, @Valid @RequestBody CreateItemTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateItemType(id, request), "Item type updated"));
    }

    @DeleteMapping("/item-types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItemType(@PathVariable UUID id) {
        adminService.deleteItemType(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Item type deleted"));
    }

    // --- Character Classes ---

    @GetMapping("/character-classes")
    public ResponseEntity<ApiResponse<List<CharacterClassResponse>>> listClasses() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listCharacterClasses()));
    }

    @PostMapping("/character-classes")
    public ResponseEntity<ApiResponse<CharacterClassResponse>> createClass(
            @Valid @RequestBody CreateCharacterClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminService.createCharacterClass(request), "Character class created"));
    }

    @GetMapping("/character-classes/{id}")
    public ResponseEntity<ApiResponse<CharacterClassResponse>> getClass(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getCharacterClass(id)));
    }

    @PutMapping("/character-classes/{id}")
    public ResponseEntity<ApiResponse<CharacterClassResponse>> updateClass(
            @PathVariable UUID id, @Valid @RequestBody CreateCharacterClassRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateCharacterClass(id, request), "Character class updated"));
    }

    @DeleteMapping("/character-classes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable UUID id) {
        adminService.deleteCharacterClass(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Character class deleted"));
    }

    // --- Character Races ---

    @GetMapping("/character-races")
    public ResponseEntity<ApiResponse<List<CharacterRaceResponse>>> listRaces() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listCharacterRaces()));
    }

    @PostMapping("/character-races")
    public ResponseEntity<ApiResponse<CharacterRaceResponse>> createRace(
            @Valid @RequestBody CreateCharacterRaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminService.createCharacterRace(request), "Character race created"));
    }

    @GetMapping("/character-races/{id}")
    public ResponseEntity<ApiResponse<CharacterRaceResponse>> getRace(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getCharacterRace(id)));
    }

    @PutMapping("/character-races/{id}")
    public ResponseEntity<ApiResponse<CharacterRaceResponse>> updateRace(
            @PathVariable UUID id, @Valid @RequestBody CreateCharacterRaceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateCharacterRace(id, request), "Character race updated"));
    }

    @DeleteMapping("/character-races/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRace(@PathVariable UUID id) {
        adminService.deleteCharacterRace(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Character race deleted"));
    }

    // --- Skills ---

    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> listSkills() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listSkills()));
    }

    @PostMapping("/skills")
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(
            @Valid @RequestBody CreateSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminService.createSkill(request), "Skill created"));
    }

    @GetMapping("/skills/{id}")
    public ResponseEntity<ApiResponse<SkillResponse>> getSkill(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getSkill(id)));
    }

    @PutMapping("/skills/{id}")
    public ResponseEntity<ApiResponse<SkillResponse>> updateSkill(
            @PathVariable UUID id, @Valid @RequestBody CreateSkillRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateSkill(id, request), "Skill updated"));
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable UUID id) {
        adminService.deleteSkill(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Skill deleted"));
    }

    // --- Subclasses ---

    @GetMapping("/subclasses")
    public ResponseEntity<ApiResponse<List<SubclassResponse>>> listSubclasses() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listSubclasses()));
    }

    @PostMapping("/subclasses")
    public ResponseEntity<ApiResponse<SubclassResponse>> createSubclass(
            @Valid @RequestBody CreateSubclassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminService.createSubclass(request), "Subclass created"));
    }

    @GetMapping("/subclasses/{id}")
    public ResponseEntity<ApiResponse<SubclassResponse>> getSubclass(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getSubclass(id)));
    }

    @PutMapping("/subclasses/{id}")
    public ResponseEntity<ApiResponse<SubclassResponse>> updateSubclass(
            @PathVariable UUID id, @Valid @RequestBody CreateSubclassRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateSubclass(id, request), "Subclass updated"));
    }

    @DeleteMapping("/subclasses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubclass(@PathVariable UUID id) {
        adminService.deleteSubclass(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Subclass deleted"));
    }

    // --- Feats ---

    @GetMapping("/feats")
    public ResponseEntity<ApiResponse<List<FeatResponse>>> listFeats() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listFeats()));
    }

    @PostMapping("/feats")
    public ResponseEntity<ApiResponse<FeatResponse>> createFeat(
            @Valid @RequestBody CreateFeatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminService.createFeat(request), "Feat created"));
    }

    @GetMapping("/feats/{id}")
    public ResponseEntity<ApiResponse<FeatResponse>> getFeat(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getFeat(id)));
    }

    @PutMapping("/feats/{id}")
    public ResponseEntity<ApiResponse<FeatResponse>> updateFeat(
            @PathVariable UUID id, @Valid @RequestBody CreateFeatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateFeat(id, request), "Feat updated"));
    }

    @DeleteMapping("/feats/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFeat(@PathVariable UUID id) {
        adminService.deleteFeat(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Feat deleted"));
    }

    // --- Class Level Rewards ---

    @GetMapping("/classes/{classId}/level-rewards")
    public ResponseEntity<ApiResponse<List<ClassLevelRewardResponse>>> listClassLevelRewards(
            @PathVariable UUID classId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listClassLevelRewards(classId)));
    }

    @PostMapping("/classes/{classId}/level-rewards")
    public ResponseEntity<ApiResponse<ClassLevelRewardResponse>> createClassLevelReward(
            @PathVariable UUID classId, @Valid @RequestBody CreateClassLevelRewardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(adminService.createClassLevelReward(classId, request), "Level reward created"));
    }

    @DeleteMapping("/classes/{classId}/level-rewards/{rewardEntryId}")
    public ResponseEntity<ApiResponse<Void>> deleteClassLevelReward(
            @PathVariable UUID classId, @PathVariable UUID rewardEntryId) {
        adminService.deleteClassLevelReward(classId, rewardEntryId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Level reward deleted"));
    }

    // --- Users & Teams (read-only) ---

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listAllUsers()));
    }

    @GetMapping("/teams")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> listTeams() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listAllTeams()));
    }
}
