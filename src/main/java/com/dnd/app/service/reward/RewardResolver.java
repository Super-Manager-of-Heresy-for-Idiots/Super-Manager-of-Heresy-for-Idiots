package com.dnd.app.service.reward;

import com.dnd.app.dto.response.RewardDetailDto;

import java.util.UUID;

/**
 * Extensibility contract: to add a new reward type (e.g. SPELL):
 * 1. Create table `spells` with id UUID PK, name, description.
 * 2. Add "SPELL" to RewardType enum.
 * 3. Implement SpellRewardResolver (this interface).
 * 4. Spring auto-registers it in RewardResolverRegistry.
 * 5. Seed class_level_rewards rows with reward_type = 'SPELL'.
 * Zero changes to LevelUpService, LevelUpController, or existing resolvers.
 */
public interface RewardResolver {
    String getSupportedType();
    RewardDetailDto resolve(UUID rewardId);
    void validateRewardId(UUID rewardId);
}
