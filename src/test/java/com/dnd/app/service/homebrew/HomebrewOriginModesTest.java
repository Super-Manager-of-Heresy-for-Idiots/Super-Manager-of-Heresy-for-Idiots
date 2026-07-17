package com.dnd.app.service.homebrew;

import com.dnd.app.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Тесты HB_MODES: нормализация режима и валидация ссылки на оригинал (NEW/DERIVED/OVERRIDE). */
class HomebrewOriginModesTest {

    private final UUID src = UUID.randomUUID();
    private final UUID ownPkg = UUID.randomUUID();
    private final UUID otherPkg = UUID.randomUUID();

    @Test
    void normalizeDefaultsToNewAndRejectsUnknown() {
        assertThat(HomebrewOriginModes.normalize(null)).isEqualTo("NEW");
        assertThat(HomebrewOriginModes.normalize("  ")).isEqualTo("NEW");
        assertThat(HomebrewOriginModes.normalize("override")).isEqualTo("OVERRIDE");
        assertThatThrownBy(() -> HomebrewOriginModes.normalize("REPLACE"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void newModeIgnoresSource() {
        assertThat(HomebrewOriginModes.validateSource("NEW", src, true, null, ownPkg)).isNull();
    }

    @Test
    void overrideRequiresSource() {
        assertThatThrownBy(() -> HomebrewOriginModes.validateSource("OVERRIDE", null, false, null, ownPkg))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void derivedWithoutSourceIsAllowed() {
        assertThat(HomebrewOriginModes.validateSource("DERIVED", null, false, null, ownPkg)).isNull();
    }

    @Test
    void missingSourceRejected() {
        assertThatThrownBy(() -> HomebrewOriginModes.validateSource("OVERRIDE", src, false, null, ownPkg))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void vanillaAndOwnPackageSourcesAccepted() {
        assertThat(HomebrewOriginModes.validateSource("OVERRIDE", src, true, null, ownPkg)).isEqualTo(src);
        assertThat(HomebrewOriginModes.validateSource("DERIVED", src, true, ownPkg, ownPkg)).isEqualTo(src);
    }

    @Test
    void foreignHomebrewSourceRejected() {
        assertThatThrownBy(() -> HomebrewOriginModes.validateSource("OVERRIDE", src, true, otherPkg, ownPkg))
                .isInstanceOf(BadRequestException.class);
    }
}
