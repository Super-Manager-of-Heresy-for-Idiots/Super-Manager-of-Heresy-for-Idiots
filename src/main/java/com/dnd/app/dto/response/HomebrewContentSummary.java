package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomebrewContentSummary {

    @Builder.Default
    private int itemTypeCount = 0;
    @Builder.Default
    private int classCount = 0;
    @Builder.Default
    private int skillCount = 0;
    @Builder.Default
    private int featCount = 0;
}
