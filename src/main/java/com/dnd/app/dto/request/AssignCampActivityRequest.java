package com.dnd.app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс AssignCampActivityRequest описывает назначение даунтайм-активности участнику привала.
 * Пустой activityId снимает активность.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignCampActivityRequest {

    private UUID activityId;

    @Size(max = 300, message = "Activity note must not exceed 300 characters")
    private String note;
}
