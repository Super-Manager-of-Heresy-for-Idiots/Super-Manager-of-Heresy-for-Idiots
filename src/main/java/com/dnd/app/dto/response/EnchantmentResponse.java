package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnchantmentResponse {
    private UUID id;
    private EnchantmentTypeResponse enchantmentType;
    private Instant appliedAt;
    private String notes;
}
