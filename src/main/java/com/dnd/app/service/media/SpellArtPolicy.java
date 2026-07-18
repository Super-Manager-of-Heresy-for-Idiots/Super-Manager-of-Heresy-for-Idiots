package com.dnd.app.service.media;

import com.dnd.app.domain.Spell;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.SpellRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс SpellArtPolicy — политика прав на арт заклинания ({@code SPELL_ART}).
 * Делегирует проверку в {@link HomebrewContentMediaAccess} по {@code homebrew}-пакету заклинания
 * (системное заклинание правит только ADMIN; homebrew — автор пакета).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class SpellArtPolicy implements MediaOwnerPolicy {

    private final SpellRepository spellRepository;
    private final HomebrewContentMediaAccess access;

    /** @return тип владельца — арт заклинания */
    @Override
    public MediaOwnerType type() {
        return MediaOwnerType.SPELL_ART;
    }

    @Override
    public void checkUpload(UUID ownerId, MediaUser user) {
        access.checkEdit(requireSpell(ownerId).getHomebrew(), user);
    }

    @Override
    public void checkRead(UUID ownerId, MediaUser user) {
        access.checkRead(requireSpell(ownerId).getHomebrew(), user);
    }

    /**
     * Находит заклинание или бросает 404.
     * @param id идентификатор заклинания
     * @return сущность заклинания
     */
    private Spell requireSpell(UUID id) {
        return spellRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заклинание не найдено."));
    }
}
