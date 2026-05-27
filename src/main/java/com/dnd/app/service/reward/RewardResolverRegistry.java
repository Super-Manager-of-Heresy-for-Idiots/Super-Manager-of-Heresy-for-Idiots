package com.dnd.app.service.reward;

import com.dnd.app.dto.response.RewardDetailDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RewardResolverRegistry {

    private final Map<String, RewardResolver> resolvers;

    public RewardResolverRegistry(List<RewardResolver> resolverList) {
        this.resolvers = resolverList.stream()
                .collect(Collectors.toMap(RewardResolver::getSupportedType, Function.identity()));
    }

    public RewardDetailDto resolve(String rewardType, UUID rewardId) {
        return getResolver(rewardType).resolve(rewardId);
    }

    public void validate(String rewardType, UUID rewardId) {
        getResolver(rewardType).validateRewardId(rewardId);
    }

    private RewardResolver getResolver(String rewardType) {
        RewardResolver resolver = resolvers.get(rewardType);
        if (resolver == null) {
            throw new IllegalArgumentException("Unknown reward type: " + rewardType);
        }
        return resolver;
    }
}
