package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBackgroundRequest {

    @NotBlank(message = "Название предыстории обязательно")
    @Size(max = 100)
    private String name;

    private String description;

    private List<String> skillProficiencyNames;

    private String grantedExtras;
}
