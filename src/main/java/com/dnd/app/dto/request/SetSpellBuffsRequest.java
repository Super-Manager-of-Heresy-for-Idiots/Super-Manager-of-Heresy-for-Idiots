package com.dnd.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Replaces the full set of buffs/debuffs linked to a spell. A null or empty list clears the links.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetSpellBuffsRequest {

    private List<UUID> buffDebuffIds;
}
