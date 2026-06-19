package com.dnd.app.dto.content.grant;

/**
 * Discriminated union of typed grant payloads. The discriminator lives on the
 * owning {@code RewardGrantDto.grantType} (external property), so concrete payload
 * objects do not repeat it.
 *
 * <p>Known implementations: {@link FeatureGrantPayload}, {@link SubclassGrantPayload},
 * {@link FeatGrantPayload}, {@link SpellGrantPayload}, {@link SkillProficiencyGrantPayload},
 * {@link AbilityScoreGrantPayload}, {@link NumericModifierGrantPayload},
 * {@link CustomTextGrantPayload}. Unknown grant types fall back to
 * {@link CustomTextGrantPayload}.</p>
 */
public interface GrantPayload {
}
