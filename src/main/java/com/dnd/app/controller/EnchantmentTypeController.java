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

@RestController
@RequiredArgsConstructor
public class EnchantmentTypeController {

    private final EnchantmentService enchantmentService;

    @GetMapping("/api/admin/enchantment-types")
    public ResponseEntity<ApiResponse<List<EnchantmentTypeResponse>>> listAdmin() {
        return ResponseEntity.ok(ApiResponse.ok(enchantmentService.listEnchantmentTypes()));
    }

    @PostMapping("/api/admin/enchantment-types")
    public ResponseEntity<ApiResponse<EnchantmentTypeResponse>> create(
            @Valid @RequestBody CreateEnchantmentTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(enchantmentService.createEnchantmentType(request), "Тип зачарования создан"));
    }

    @GetMapping("/api/admin/enchantment-types/{id}")
    public ResponseEntity<ApiResponse<EnchantmentTypeResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(enchantmentService.getEnchantmentType(id)));
    }

    @PutMapping("/api/admin/enchantment-types/{id}")
    public ResponseEntity<ApiResponse<EnchantmentTypeResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CreateEnchantmentTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(enchantmentService.updateEnchantmentType(id, request), "Тип зачарования обновлен"));
    }

    @DeleteMapping("/api/admin/enchantment-types/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        enchantmentService.deleteEnchantmentType(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Тип зачарования удален"));
    }

    @GetMapping("/api/enchantment-types")
    public ResponseEntity<ApiResponse<List<EnchantmentTypeResponse>>> listPublic() {
        return ResponseEntity.ok(ApiResponse.ok(enchantmentService.listEnchantmentTypes()));
    }
}
