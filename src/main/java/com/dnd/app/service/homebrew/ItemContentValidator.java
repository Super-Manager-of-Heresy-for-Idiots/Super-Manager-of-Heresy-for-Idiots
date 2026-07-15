package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.ItemTemplate;
import com.dnd.app.domain.content.EquipmentItem;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.EquipmentItemRepository;
import com.dnd.app.repository.ItemTemplateRepository;
import com.dnd.app.repository.MagicItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Валидатор единого homebrew-предмета (P1.5 / IT-2). «Предмет» снаружи — одна сущность, но в БД пока три
 * таблицы: magic_item, equipment_item, item_templates (легаси). Резолвит id по всем трём и определяет владельца
 * через homebrew.author.
 */
@Component
@RequiredArgsConstructor
public class ItemContentValidator implements HomebrewContentValidator {

    private final MagicItemRepository magicItemRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final ItemTemplateRepository itemTemplateRepository;

    @Override
    public String getSupportedType() {
        return "ITEM";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!magicItemRepository.existsById(contentId)
                && !equipmentItemRepository.existsById(contentId)
                && !itemTemplateRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Предмет не найден: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        MagicItem magic = magicItemRepository.findById(contentId).orElse(null);
        if (magic != null) {
            return ContentSummaryDto.builder()
                    .id(magic.getId()).name(magic.getNameRu()).description(magic.getDescription()).build();
        }
        EquipmentItem equip = equipmentItemRepository.findById(contentId).orElse(null);
        if (equip != null) {
            return ContentSummaryDto.builder().id(equip.getId()).name(equip.getNameRu()).build();
        }
        ItemTemplate tpl = itemTemplateRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет не найден: " + contentId));
        return ContentSummaryDto.builder()
                .id(tpl.getId()).name(tpl.getName()).description(tpl.getDescription()).build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        HomebrewPackage hb = magicItemRepository.findById(contentId).map(MagicItem::getHomebrew).orElse(null);
        if (hb == null) {
            hb = equipmentItemRepository.findById(contentId).map(EquipmentItem::getHomebrew).orElse(null);
        }
        if (hb == null) {
            hb = itemTemplateRepository.findById(contentId).map(ItemTemplate::getHomebrew).orElse(null);
        }
        return hb != null && hb.getAuthor() != null ? hb.getAuthor().getId() : null;
    }
}
