package com.dnd.app.service.media;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Класс HomebrewContentMediaAccess — общая проверка прав на арт homebrew-контента (заклинания, виды,
 * классы, черты, предыстории, предметы и т.п.). Все такие сущности имеют одинаковую модель владения:
 * системный контент (homebrew == null) правит только ADMIN, а homebrew-контент — автор владеющего
 * пакета в редактируемом статусе. Per-type политики ({@code SpellArtPolicy} и др.) загружают сущность
 * своим репозиторием и делегируют сюда её {@code homebrew}-пакет, чтобы не дублировать логику.
 */
@Component
@ConditionalOnProperty(name = "minio.endpoint")
class HomebrewContentMediaAccess {

    /**
     * Проверяет право менять арт контента по владеющему homebrew-пакету.
     * @param pkg владеющий homebrew-пакет (null для системного контента)
     * @param user текущий пользователь
     */
    void checkEdit(HomebrewPackage pkg, MediaUser user) {
        if (user.isAdmin()) {
            return;
        }
        if (pkg == null) {
            throw new AccessDeniedException("Системный контент может редактировать только администратор.");
        }
        if (pkg.getAuthor() == null || !pkg.getAuthor().getId().equals(user.id())) {
            throw new AccessDeniedException("Изображение может менять только автор homebrew-пакета.");
        }
        if (pkg.isDeleted() || !pkg.getStatus().isEditable()) {
            throw new BadRequestException("Менять можно только в редактируемом пакете (DRAFT/PUBLISHED).");
        }
    }

    /**
     * Проверяет право видеть арт контента: системный/опубликованный — всем, черновой homebrew — автору.
     * @param pkg владеющий homebrew-пакет (null для системного контента)
     * @param user текущий пользователь
     */
    void checkRead(HomebrewPackage pkg, MediaUser user) {
        if (user.isAdmin() || pkg == null) {
            return;
        }
        boolean readable = (pkg.getAuthor() != null && pkg.getAuthor().getId().equals(user.id()))
                || (pkg.getStatus() == HomebrewStatus.PUBLISHED && !pkg.isDeleted());
        if (!readable) {
            throw new AccessDeniedException("Контент из недоступного homebrew-пакета.");
        }
    }
}
