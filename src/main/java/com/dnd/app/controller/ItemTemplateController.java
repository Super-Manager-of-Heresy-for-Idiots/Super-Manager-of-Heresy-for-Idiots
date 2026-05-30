package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateItemTemplateRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.ItemTemplateResponse;
import com.dnd.app.service.ItemTemplateService;
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

@RestController
@RequestMapping("/api/item-templates")
@RequiredArgsConstructor
@Tag(name = "Item Templates", description = "Item template management")
public class ItemTemplateController {

    private final ItemTemplateService itemTemplateService;

    @PostMapping
    @Operation(summary = "Create item template (Admin/GM)")
    public ResponseEntity<ApiResponse<ItemTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateItemTemplateRequest request, Authentication auth) {
        ItemTemplateResponse response = itemTemplateService.createTemplate(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Item template created"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item template")
    public ResponseEntity<ApiResponse<ItemTemplateResponse>> getTemplate(@PathVariable UUID id) {
        ItemTemplateResponse response = itemTemplateService.getTemplate(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/campaign/{campaignId}")
    @Operation(summary = "List available item templates for campaign")
    public ResponseEntity<ApiResponse<List<ItemTemplateResponse>>> listTemplates(
            @PathVariable UUID campaignId, Authentication auth) {
        List<ItemTemplateResponse> templates = itemTemplateService.listTemplates(campaignId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(templates));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update item template")
    public ResponseEntity<ApiResponse<ItemTemplateResponse>> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody CreateItemTemplateRequest request, Authentication auth) {
        ItemTemplateResponse response = itemTemplateService.updateTemplate(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Item template updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete item template (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable UUID id, Authentication auth) {
        itemTemplateService.deleteTemplate(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Item template deleted"));
    }
}
