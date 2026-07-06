package com.dnd.app.dto.response;

import com.dnd.app.dto.combat.HpChangeResult;
import com.dnd.app.dto.featurerule.RestResourcePreview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Combined outcome of a single orchestrated rest, so the client gets one response instead of firing
 * (and having to reconcile) three or four separate rest calls. Sub-results are null/empty when the
 * rest type or feature flags do not touch that subsystem.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestResult {

    /** The canonical rest type applied ({@code long_rest} / {@code short_rest}). */
    private String restType;

    /** Legacy custom resources after recovery (Rage/Ki/…). */
    private List<ResourceResponse> resources;

    /** Feature-rules resources restored (empty unless the resources subsystem is enabled). */
    private List<RestResourcePreview> featureResources;

    /** Spell slots after restoration (null on a short rest — no generic short-rest recovery yet). */
    private SpellSlotsResponse spellSlots;

    /** HP after the rest (null on a short rest — pending hit dice). */
    private HpChangeResult hp;
}
