package com.dnd.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelUpRequest {

    @NotNull(message = "ID класса обязателен")
    private UUID classId;

    @Valid
    private List<RewardSelection> selections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardSelection {
        @NotNull(message = "Тип награды обязателен")
        private String rewardType;

        @NotNull(message = "ID записи награды обязателен")
        private UUID rewardEntryId;
    }
}
