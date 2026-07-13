package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO AttachableContentResponse описывает элемент существующего homebrew-контента, который автор может
 * прицепить к пакету. Заменяет ручной ввод UUID: клиент показывает браузируемый список с именем/описанием
 * и пакетом-источником, а прикрепление идёт по {@code contentId}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttachableContentResponse {

    private UUID contentId;
    private String name;
    private String description;
    private UUID sourcePackageId;
    private String sourcePackageTitle;
}
