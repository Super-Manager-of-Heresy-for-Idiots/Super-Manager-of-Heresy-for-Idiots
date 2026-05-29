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

@RestController
@RequestMapping("/api/admin/buffs-debuffs")
@RequiredArgsConstructor
public class BuffDebuffController {

    private final BuffDebuffService buffDebuffService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BuffDebuffResponse>>> list(
            @RequestParam(required = false) Boolean isBuff,
            @RequestParam(required = false) String effectType) {
        return ResponseEntity.ok(ApiResponse.ok(buffDebuffService.findAll(isBuff, effectType)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BuffDebuffResponse>> create(
            @Valid @RequestBody CreateBuffDebuffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(buffDebuffService.create(request), "Бафф/дебафф создан"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BuffDebuffResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(buffDebuffService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BuffDebuffResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CreateBuffDebuffRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(buffDebuffService.update(id, request), "Бафф/дебафф обновлен"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        buffDebuffService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Бафф/дебафф удален"));
    }
}
