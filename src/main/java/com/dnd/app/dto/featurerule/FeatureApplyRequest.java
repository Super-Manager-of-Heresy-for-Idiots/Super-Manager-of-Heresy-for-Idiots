package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Apply an already-rolled combat outcome of a feature to a target character. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureApplyRequest {

    @NotNull
    private UUID targetCharacterId;

    /** Rolled/adjudicated damage to apply (0 if none). */
    private Integer damage;

    /** Rolled/adjudicated healing to apply (0 if none). */
    private Integer healing;

    /**
     * Damage type of the rolled damage (from the feature's {@code plan()} → {@code damage.damageTypeId}).
     * When set, the target's resistance/vulnerability to that type is applied to the damage.
     */
    private UUID damageTypeId;
}
