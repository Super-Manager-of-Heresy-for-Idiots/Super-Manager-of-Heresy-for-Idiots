package com.dnd.app.service;

import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.Species;
import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.dto.content.SpeciesDetailResponse;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.ContentClassMapper;
import com.dnd.app.mapper.SpeciesMapper;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.SpeciesRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Класс ContentReferenceService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentReferenceService {

    private final ContentCharacterClassRepository classRepository;
    private final SpeciesRepository speciesRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final CampaignService campaignService;
    private final UserRepository userRepository;
    private final ContentClassMapper classMapper;
    private final SpeciesMapper speciesMapper;

    // --- campaign-aware ---

    /**
     * Возвращает результат операции "get campaign classes" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ContentClassDetailResponse> getCampaignClasses(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        List<ContentCharacterClass> classes = new ArrayList<>(classRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            classes.addAll(classRepository.findAllByHomebrewIdIn(pkgIds));
        }
        return classes.stream().map(c -> classMapper.toDetail(c, resolvedLang)).toList();
    }

    /**
     * Возвращает результат операции "get campaign class" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public ContentClassDetailResponse getCampaignClass(UUID campaignId, UUID classId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        ContentCharacterClass clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        enforceVisibleInCampaign(campaignId, clazz);
        return classMapper.toDetail(clazz, resolvedLang);
    }

    // --- vanilla / core only ---

    /**
     * Возвращает результат операции "get vanilla classes" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @org.springframework.cache.annotation.Cacheable(
            value = com.dnd.app.config.CacheConfig.CONTENT_VANILLA_CLASSES, key = "#lang")
    @Transactional(readOnly = true)
    public List<ContentClassDetailResponse> getVanillaClasses(String lang) {
        String resolvedLang = Localization.normalize(lang);
        return classRepository.findAllByHomebrewIsNull().stream()
                .map(c -> classMapper.toDetail(c, resolvedLang))
                .toList();
    }

    /**
     * Возвращает результат операции "get vanilla class" в рамках бизнес-логики домена.
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public ContentClassDetailResponse getVanillaClass(UUID classId, String lang) {
        String resolvedLang = Localization.normalize(lang);
        ContentCharacterClass clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (clazz.getHomebrew() != null) {
            throw new ResourceNotFoundException("Class not found");
        }
        return classMapper.toDetail(clazz, resolvedLang);
    }

    // --- species: campaign-aware ---

    /**
     * Возвращает результат операции "get campaign species" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<SpeciesDetailResponse> getCampaignSpecies(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        List<Species> species = new ArrayList<>(speciesRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            species.addAll(speciesRepository.findAllByHomebrewIdIn(pkgIds));
        }
        // SP-1: выключенный (active=false) homebrew-вид скрыт из выбора. Ваниль всегда active=true.
        return species.stream()
                .filter(s -> s.getHomebrew() == null || Boolean.TRUE.equals(s.getActive()))
                .map(s -> speciesMapper.toDetail(s, resolvedLang)).toList();
    }

    /**
     * Возвращает результат операции "get campaign species by id" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param speciesId идентификатор species, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public SpeciesDetailResponse getCampaignSpeciesById(UUID campaignId, UUID speciesId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Species species = speciesRepository.findById(speciesId)
                .orElseThrow(() -> new ResourceNotFoundException("Species not found"));
        enforceSpeciesVisibleInCampaign(campaignId, species);
        return speciesMapper.toDetail(species, resolvedLang);
    }

    // --- species: vanilla / core only ---

    /**
     * Возвращает результат операции "get vanilla species" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<SpeciesDetailResponse> getVanillaSpecies(String lang) {
        String resolvedLang = Localization.normalize(lang);
        return speciesRepository.findAllByHomebrewIsNull().stream()
                .map(s -> speciesMapper.toDetail(s, resolvedLang))
                .toList();
    }

    /**
     * Возвращает результат операции "get vanilla species by id" в рамках бизнес-логики домена.
     * @param speciesId идентификатор species, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public SpeciesDetailResponse getVanillaSpeciesById(UUID speciesId, String lang) {
        String resolvedLang = Localization.normalize(lang);
        Species species = speciesRepository.findById(speciesId)
                .orElseThrow(() -> new ResourceNotFoundException("Species not found"));
        if (species.getHomebrew() != null) {
            throw new ResourceNotFoundException("Species not found");
        }
        return speciesMapper.toDetail(species, resolvedLang);
    }

    // --- access helpers ---

    private void enforceSpeciesVisibleInCampaign(UUID campaignId, Species species) {
        if (species.getHomebrew() == null) {
            return; // core content always visible
        }
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!pkgIds.contains(species.getHomebrew().getId())) {
            throw new ResourceNotFoundException("Species not found");
        }
    }

    private void enforceAccess(UUID campaignId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
    }

    private void enforceVisibleInCampaign(UUID campaignId, ContentCharacterClass clazz) {
        if (clazz.getHomebrew() == null) {
            return; // core content always visible
        }
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!pkgIds.contains(clazz.getHomebrew().getId())) {
            throw new ResourceNotFoundException("Class not found");
        }
    }
}
