package com.dnd.app.dto.response;

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
public class CampaignAvailableContentResponse {

    private List<AvailableContentItem> classes;
    private List<AvailableContentItem> races;
    private List<AvailableContentItem> itemTypes;
    private List<AvailableContentItem> skills;
    private List<AvailableContentItem> feats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableContentItem {
        private UUID id;
        private String name;
        private String source; // GLOBAL or HOMEBREW
        private String homebrewTitle;
    }
}
