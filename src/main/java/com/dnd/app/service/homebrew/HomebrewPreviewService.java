package com.dnd.app.service.homebrew;

import com.dnd.app.dto.content.ItemDefinitionResponse;
import com.dnd.app.dto.response.HomebrewDetailResponse;
import com.dnd.app.dto.response.HomebrewPreviewResponse;
import com.dnd.app.mapper.BackgroundMapper;
import com.dnd.app.mapper.ContentClassMapper;
import com.dnd.app.mapper.EquipmentItemMapper;
import com.dnd.app.mapper.FeatMapper;
import com.dnd.app.mapper.MagicItemMapper;
import com.dnd.app.mapper.SpeciesMapper;
import com.dnd.app.repository.BackgroundRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.EquipmentItemRepository;
import com.dnd.app.repository.FeatRepository;
import com.dnd.app.repository.MagicItemRepository;
import com.dnd.app.repository.SpeciesRepository;
import com.dnd.app.service.MonsterService;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Агрегатор богатого предпросмотра homebrew-пакета для витрины («что я добавляю» — флагманская фича).
 * Переиспользует существующие detail-мапперы (не плодит новые): полная механика/статы каждого
 * элемента, чтобы игрок раскрыл любую запись как в игре. Доступ и мета-шапку (+ рейтинг + counts +
 * contentByType для лёгких типов) даёт {@link HomebrewMarketplaceService#getMarketplacePackage}.
 */
@Service
@RequiredArgsConstructor
public class HomebrewPreviewService {

    private final HomebrewMarketplaceService marketplaceService;
    private final SpellAuthoringService spellAuthoringService;
    private final MonsterService monsterService;

    private final MagicItemMapper magicItemMapper;
    private final EquipmentItemMapper equipmentItemMapper;
    private final ContentClassMapper contentClassMapper;
    private final SpeciesMapper speciesMapper;
    private final FeatMapper featMapper;
    private final BackgroundMapper backgroundMapper;

    private final MagicItemRepository magicItemRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final ContentCharacterClassRepository classRepository;
    private final SpeciesRepository speciesRepository;
    private final FeatRepository featRepository;
    private final BackgroundRepository backgroundRepository;

    /**
     * Собирает полный предпросмотр пакета: мета-шапка (с проверкой доступа) + богатые списки по типам.
     * @param id идентификатор пакета
     * @param username пользователь (владелец видит и свой черновик, чужой — только PUBLISHED)
     * @param lang язык локализации ({@code null} = дефолтный)
     * @return богатый предпросмотр
     */
    @Transactional(readOnly = true)
    public HomebrewPreviewResponse getPreview(UUID id, String username, String lang) {
        String loc = (lang == null || lang.isBlank()) ? Localization.DEFAULT_LANG : lang;

        // Мета-шапка + контроль доступа (PUBLISHED или владелец) + рейтинг + contentByType.
        HomebrewDetailResponse header = marketplaceService.getMarketplacePackage(id, username);

        Set<UUID> pkgIds = Set.of(id);

        // Предметы: magic + equipment сводятся к единой сущности «Предмет» через существующие фабрики.
        List<ItemDefinitionResponse> items = Stream.concat(
                magicItemRepository.findAllByHomebrewIdIn(pkgIds).stream()
                        .map(m -> ItemDefinitionResponse.fromMagic(magicItemMapper.toDetail(m, loc))),
                equipmentItemRepository.findAllByHomebrewIdIn(pkgIds).stream()
                        .map(e -> ItemDefinitionResponse.fromEquipment(equipmentItemMapper.toDetail(e, loc)))
        ).collect(Collectors.toList());

        return HomebrewPreviewResponse.builder()
                .header(header)
                .spells(spellAuthoringService.previewByPackage(id))
                .items(items)
                .classes(classRepository.findAllByHomebrewIdIn(pkgIds).stream()
                        .map(c -> contentClassMapper.toDetail(c, loc)).collect(Collectors.toList()))
                .species(speciesRepository.findAllByHomebrewIdIn(pkgIds).stream()
                        .map(s -> speciesMapper.toDetail(s, loc)).collect(Collectors.toList()))
                .feats(featRepository.findAllByHomebrewIdIn(pkgIds).stream()
                        .map(f -> featMapper.toDetail(f, loc)).collect(Collectors.toList()))
                .backgrounds(backgroundRepository.findAllByHomebrewIdIn(pkgIds).stream()
                        .map(b -> backgroundMapper.toDetail(b, loc)).collect(Collectors.toList()))
                .monsters(monsterService.listHomebrewMonstersDetailed(id, loc))
                .build();
    }
}
