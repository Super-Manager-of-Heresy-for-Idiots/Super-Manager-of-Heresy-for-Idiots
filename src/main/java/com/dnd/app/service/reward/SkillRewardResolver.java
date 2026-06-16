package com.dnd.app.service.reward;

import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.domain.Skill;
import com.dnd.app.domain.SkillEffect;
import com.dnd.app.dto.response.BuffDebuffResponse;
import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.dto.response.RewardDetailInfo;
import com.dnd.app.dto.response.SkillEffectResponse;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SkillRewardResolver implements RewardResolver {

    private final SkillRepository skillRepository;

    @Override
    public String getSupportedType() {
        return "SKILL";
    }

    @Override
    public RewardDetailDto resolve(UUID rewardId) {
        Skill skill = skillRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено: " + rewardId));

        RewardDetailInfo detail = RewardDetailInfo.builder()
                .skillActivation(skill.getSkillType())
                .damageDice(skill.getDamageDice())
                .damageBonus(skill.getDamageBonus())
                .damageType(skill.getDamageType() != null ? skill.getDamageType().getSlug() : null)
                .effects(mapEffects(skill))
                .build();

        return RewardDetailDto.builder()
                .rewardId(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .detail(detail)
                .build();
    }

    private List<SkillEffectResponse> mapEffects(Skill skill) {
        if (skill.getEffects() == null || skill.getEffects().isEmpty()) {
            return List.of();
        }
        return skill.getEffects().stream().map(this::mapEffect).toList();
    }

    private SkillEffectResponse mapEffect(SkillEffect se) {
        BuffDebuff bd = se.getBuffDebuff();
        BuffDebuffResponse bdResp = bd == null ? null : BuffDebuffResponse.builder()
                .id(bd.getId())
                .name(bd.getName())
                .description(bd.getDescription())
                .effectType(bd.getEffectType())
                .targetStatId(bd.getTargetStat() != null ? bd.getTargetStat().getId() : null)
                .targetStatName(bd.getTargetStat() != null ? bd.getTargetStat().getNameRu() : null)
                .modifierValue(bd.getModifierValue())
                .durationRounds(bd.getDurationRounds())
                .isBuff(bd.getIsBuff())
                .createdAt(bd.getCreatedAt())
                .build();
        return SkillEffectResponse.builder()
                .id(se.getId())
                .buffDebuff(bdResp)
                .effectRole(se.getEffectRole() != null ? se.getEffectRole().name() : null)
                .chancePercent(se.getChancePercent())
                .build();
    }

    @Override
    public void validateRewardId(UUID rewardId) {
        if (!skillRepository.existsById(rewardId)) {
            throw new ResourceNotFoundException("Умение не найдено: " + rewardId);
        }
    }
}
