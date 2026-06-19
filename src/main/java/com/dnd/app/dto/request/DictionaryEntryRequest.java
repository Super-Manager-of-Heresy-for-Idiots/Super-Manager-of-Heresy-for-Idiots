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
public class DictionaryEntryRequest {

    @NotBlank(message = "code is required")
    @Size(max = 60, message = "code must not exceed 60 characters")
    private String code;

    @NotBlank(message = "nameRusloc is required")
    private String nameRusloc;

    private String nameEngloc;

    // Only used by the "sources" dictionary; ignored elsewhere.
    @Size(max = 20, message = "bookCode must not exceed 20 characters")
    private String bookCode;

    private Boolean isUnique;
}
