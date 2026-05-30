package com.dnd.app.domain;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignHomebrewId implements Serializable {
    private UUID campaignId;
    private UUID packageId;
}
