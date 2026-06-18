package com.dnd.app.controller;

import com.dnd.app.dto.content.ClassSaveResult;
import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.dto.request.ClassWriteRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.ClassAuthoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Aggregate class authoring (Phase 8). Same request body for admin/core and homebrew;
 * only scope/ownership differ. Replaces the legacy rich class-authoring path.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Class Authoring", description = "Aggregate create/update/delete of classes on the new content model")
public class ClassAuthoringController {

    private final ClassAuthoringService authoringService;
    private final Executor controllerTaskExecutor;

    // --- admin / core ---

    @GetMapping("/api/admin/character-classes/{classId}")
    @Operation(summary = "Get a core class detail (admin) with concurrency ETag")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentClassDetailResponse>>> getCore(
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ContentClassDetailResponse detail = authoringService.getCoreClass(classId, auth.getName(), lang);
            return ResponseEntity.ok().eTag(authoringService.etagFor(detail)).body(ApiResponse.ok(detail));
        }, controllerTaskExecutor);
    }

    @PostMapping("/api/admin/character-classes")
    @Operation(summary = "Create a core class (admin)")
    public CompletableFuture<ResponseEntity<ApiResponse<ClassSaveResult>>> createCore(
            @RequestParam(defaultValue = "en") String lang,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ClassWriteRequest request,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ClassSaveResult result = authoringService.createCoreClass(request, auth.getName(), lang, idempotencyKey);
            return ResponseEntity.created(URI.create(result.getResourceUrl()))
                    .body(ApiResponse.ok(result, "Класс создан"));
        }, controllerTaskExecutor);
    }

    @PutMapping("/api/admin/character-classes/{classId}")
    @Operation(summary = "Update a core class (admin); honours If-Match for optimistic concurrency")
    public CompletableFuture<ResponseEntity<ApiResponse<ClassSaveResult>>> updateCore(
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "en") String lang,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ClassWriteRequest request,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ClassSaveResult result = authoringService.updateCoreClass(classId, request, ifMatch, auth.getName(), lang);
            return ResponseEntity.ok().eTag(result.getEtag()).body(ApiResponse.ok(result, "Класс обновлён"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/api/admin/character-classes/{classId}")
    @Operation(summary = "Delete a core class (admin)")
    public CompletableFuture<ResponseEntity<Void>> deleteCore(
            @PathVariable UUID classId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            authoringService.deleteCoreClass(classId, auth.getName());
            return ResponseEntity.noContent().build();
        }, controllerTaskExecutor);
    }

    // --- homebrew package ---

    @GetMapping("/api/homebrew/packages/{packageId}/classes/{classId}")
    @Operation(summary = "Get a homebrew package class detail with concurrency ETag")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentClassDetailResponse>>> getPackageClass(
            @PathVariable UUID packageId,
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ContentClassDetailResponse detail =
                    authoringService.getPackageClass(packageId, classId, auth.getName(), lang);
            return ResponseEntity.ok().eTag(authoringService.etagFor(detail)).body(ApiResponse.ok(detail));
        }, controllerTaskExecutor);
    }

    @PostMapping("/api/homebrew/packages/{packageId}/classes")
    @Operation(summary = "Create a class in a homebrew package")
    public CompletableFuture<ResponseEntity<ApiResponse<ClassSaveResult>>> createPackageClass(
            @PathVariable UUID packageId,
            @RequestParam(defaultValue = "en") String lang,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ClassWriteRequest request,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ClassSaveResult result =
                    authoringService.createPackageClass(packageId, request, auth.getName(), lang, idempotencyKey);
            return ResponseEntity.created(URI.create(result.getResourceUrl()))
                    .body(ApiResponse.ok(result, "Класс создан"));
        }, controllerTaskExecutor);
    }

    @PutMapping("/api/homebrew/packages/{packageId}/classes/{classId}")
    @Operation(summary = "Update a class in a homebrew package; honours If-Match for optimistic concurrency")
    public CompletableFuture<ResponseEntity<ApiResponse<ClassSaveResult>>> updatePackageClass(
            @PathVariable UUID packageId,
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "en") String lang,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ClassWriteRequest request,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ClassSaveResult result =
                    authoringService.updatePackageClass(packageId, classId, request, ifMatch, auth.getName(), lang);
            return ResponseEntity.ok().eTag(result.getEtag()).body(ApiResponse.ok(result, "Класс обновлён"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/api/homebrew/packages/{packageId}/classes/{classId}")
    @Operation(summary = "Delete a class from a homebrew package")
    public CompletableFuture<ResponseEntity<Void>> deletePackageClass(
            @PathVariable UUID packageId,
            @PathVariable UUID classId,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            authoringService.deletePackageClass(packageId, classId, auth.getName());
            return ResponseEntity.noContent().build();
        }, controllerTaskExecutor);
    }
}
