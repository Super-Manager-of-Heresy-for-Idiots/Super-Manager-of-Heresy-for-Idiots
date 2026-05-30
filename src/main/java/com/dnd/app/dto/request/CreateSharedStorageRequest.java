package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSharedStorageRequest {

    @NotBlank(message = "Storage name is required")
    @Size(max = 100, message = "Storage name must not exceed 100 characters")
    private String name;
}
