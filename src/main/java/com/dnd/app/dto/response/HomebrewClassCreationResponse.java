package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomebrewClassCreationResponse {
    private CharacterClassResponse characterClass;
    private List<ClassLevelRewardResponse> rewards;
    private Map<String, List<ContentSummaryDto>> createdContent;
    private HomebrewDetailResponse packageDetail;
}
