package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO ReportHomebrewRequest — тело жалобы на homebrew-пакет (P2-6): причина жалобы.
 */
@Data
public class ReportHomebrewRequest {

    @NotBlank(message = "Укажите причину жалобы")
    @Size(max = 2000, message = "Причина не длиннее 2000 символов")
    private String reason;
}
