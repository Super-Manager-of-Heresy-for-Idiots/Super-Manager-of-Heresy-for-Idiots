package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterHitDie;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.combat.HpChangeResult;
import com.dnd.app.dto.response.HitDiceResponse;
import com.dnd.app.dto.response.HitDiceSpendResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterHitDieRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.util.AbilityScores;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс CharacterHitDiceService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterHitDiceService {

    private static final String CON = "con";

    private final CharacterHitDieRepository repository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final CharacterHpService characterHpService;

    /**
     * Возвращает список для операции "list and provision" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public List<HitDiceResponse> listAndProvision(UUID characterId, String username) {
        PlayerCharacter character = findCharacter(characterId);
        enforceView(character, getUser(username));
        provision(character);
        return repository.findByCharacterId(characterId).stream()
                .sorted(Comparator.comparingInt(CharacterHitDie::getDie).reversed())
                .map(this::toResponse)
                .toList();
    }

    /**
     * Выполняет операции "provision" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     */
    @Transactional
    public void provision(PlayerCharacter character) {
        Map<Integer, Integer> expected = new HashMap<>();
        for (CharacterClassLevel ccl : classLevelRepository.findAllByCharacterId(character.getId())) {
            Integer die = ccl.getCharacterClass() != null ? ccl.getCharacterClass().getHitDie() : null;
            int level = ccl.getClassLevel() != null ? ccl.getClassLevel() : 0;
            if (die != null && die > 0 && level > 0) {
                expected.merge(die, level, Integer::sum);
            }
        }
        for (Map.Entry<Integer, Integer> e : expected.entrySet()) {
            int die = e.getKey();
            int total = e.getValue();
            CharacterHitDie row = repository.findByCharacterIdAndDie(character.getId(), die).orElse(null);
            if (row == null) {
                repository.save(CharacterHitDie.builder()
                        .characterId(character.getId()).die(die).total(total).remaining(total).build());
            } else if (row.getTotal() < total) {
                row.setRemaining(row.getRemaining() + (total - row.getTotal()));
                row.setTotal(total);
                repository.save(row);
            }
        }
    }

    /**
     * Выполняет операции "spend" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param die входящее значение die, используемое бизнес-сценарием
     * @param count входящее значение count, используемое бизнес-сценарием
     * @param rolledTotal входящее значение rolled total, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HitDiceSpendResponse spend(UUID campaignId, UUID characterId, int die, int count,
                                      int rolledTotal, String username) {
        PlayerCharacter character = findCharacter(characterId);
        User user = getUser(username);
        enforceEdit(character, user);
        if (count <= 0) {
            throw new BadRequestException("Число костей должно быть положительным");
        }
        CharacterHitDie row = repository.findByCharacterIdAndDie(characterId, die)
                .orElseThrow(() -> new BadRequestException("У персонажа нет костей хитов d" + die));
        if (row.getRemaining() < count) {
            throw new BadRequestException("Недостаточно костей хитов d" + die
                    + " (осталось " + row.getRemaining() + ")");
        }
        int heal = Math.max(0, rolledTotal + conModifier(character) * count);
        row.setRemaining(row.getRemaining() - count);
        repository.save(row);

        UUID cId = character.getCampaign() != null ? character.getCampaign().getId() : campaignId;
        HpChangeResult hp = characterHpService.applyDelta(characterId, heal, cId, user.getId());
        log.info("Hit dice spent: character={}, die=d{}, count={}, healed={}, by={}",
                characterId, die, count, heal, username);
        return HitDiceSpendResponse.builder()
                .die(die).remaining(row.getRemaining()).healed(heal)
                .currentHp(hp.currentHp()).tempHp(hp.tempHp()).maxHp(hp.maxHp())
                .build();
    }

    /**
     * Выполняет операции "restore on long rest" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     */
    @Transactional
    public void restoreOnLongRest(PlayerCharacter character) {
        provision(character);
        List<CharacterHitDie> rows = new ArrayList<>(repository.findByCharacterId(character.getId()));
        rows.sort(Comparator.comparingInt(CharacterHitDie::getDie).reversed());
        int total = rows.stream().mapToInt(CharacterHitDie::getTotal).sum();
        int remaining = rows.stream().mapToInt(CharacterHitDie::getRemaining).sum();
        int spent = total - remaining;
        if (spent <= 0) {
            return;
        }
        int regain = Math.min(spent, Math.max(1, total / 2));
        for (CharacterHitDie row : rows) {
            if (regain <= 0) {
                break;
            }
            int canRestore = row.getTotal() - row.getRemaining();
            int add = Math.min(canRestore, regain);
            if (add > 0) {
                row.setRemaining(row.getRemaining() + add);
                repository.save(row);
                regain -= add;
            }
        }
    }

    private int conModifier(PlayerCharacter character) {
        int conValue = character.getStats().stream()
                .filter(s -> s.getStatType() != null && CON.equals(s.getStatType().getSlug()))
                .findFirst()
                .map(CharacterStat::getValue)
                .orElse(10);
        return AbilityScores.modifier(conValue);
    }

    private HitDiceResponse toResponse(CharacterHitDie row) {
        return HitDiceResponse.builder()
                .die(row.getDie()).total(row.getTotal()).remaining(row.getRemaining())
                .build();
    }

    private void enforceView(PlayerCharacter character, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (character.getCampaign() != null) {
            if (!campaignService.isMemberOfCampaign(character.getCampaign().getId(), user.getId())) {
                throw new AccessDeniedException("Вы не участник кампании этого персонажа");
            }
        } else if (character.getOwner() == null || !character.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Нет доступа к костям хитов этого персонажа");
        }
    }

    private void enforceEdit(PlayerCharacter character, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (character.getOwner() != null && character.getOwner().getId().equals(user.getId())) {
            return;
        }
        if (character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId())) {
            return;
        }
        throw new AccessDeniedException("Только владелец, ГМ кампании или ADMIN могут тратить кости хитов");
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private PlayerCharacter findCharacter(UUID characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
    }
}
