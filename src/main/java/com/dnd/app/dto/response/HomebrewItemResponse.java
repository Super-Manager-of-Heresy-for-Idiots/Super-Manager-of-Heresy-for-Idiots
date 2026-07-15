package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO HomebrewItemResponse — единый homebrew-предмет (P1.5 / IT-2), пригодный для round-trip в редакторе.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomebrewItemResponse {

    private UUID id;
    private String kind;
    private String name;
    private String nameEn;
    private String description;
    private String rarity;
    private Boolean attunementRequired;
    private String attunementRequirement;
    private String source;
    private UUID homebrewPackageId;
    private String homebrewPackageTitle;
}
