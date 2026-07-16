package com.dnd.app.service.homebrew;

import com.dnd.app.dto.request.HomebrewSpellRequest;
import com.dnd.app.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Юнит-тесты генерации отображаемых строк заклинания и валидации слагов (HB_UX Фазы 1/3). */
class SpellDisplayStringsTest {

    private HomebrewSpellRequest req() {
        return new HomebrewSpellRequest();
    }

    @Test
    void castingTimeGeneratedFromSlug() {
        HomebrewSpellRequest r = req();
        r.setCastingActionSlug("action");
        assertThat(SpellDisplayStrings.castingTimeRaw(r)).isEqualTo("1 действие");
        r.setCastingActionSlug("bonus-action");
        assertThat(SpellDisplayStrings.castingTimeRaw(r)).isEqualTo("1 бонусное действие");
        r.setCastingActionSlug("reaction");
        assertThat(SpellDisplayStrings.castingTimeRaw(r)).isEqualTo("1 реакция");
        r.setCastingActionSlug("time");
        r.setCastingTimeAmount(10);
        r.setCastingTimeUnit("minute");
        assertThat(SpellDisplayStrings.castingTimeRaw(r)).isEqualTo("10 минут");
        r.setCastingTimeAmount(1);
        r.setCastingTimeUnit("hour");
        assertThat(SpellDisplayStrings.castingTimeRaw(r)).isEqualTo("1 час");
    }

    @Test
    void durationGeneratedFromStructure() {
        HomebrewSpellRequest r = req();
        r.setDurationType("instantaneous");
        assertThat(SpellDisplayStrings.durationRaw(r)).isEqualTo("Мгновенная");
        r.setDurationType("timed");
        r.setDurationAmount(1);
        r.setDurationUnit("minute");
        assertThat(SpellDisplayStrings.durationRaw(r)).isEqualTo("1 минута");
        r.setConcentration(true);
        assertThat(SpellDisplayStrings.durationRaw(r)).isEqualTo("Концентрация, до 1 минута");
    }

    @Test
    void ruPluralIsCorrect() {
        assertThat(SpellDisplayStrings.ruAmount(1, "round")).isEqualTo("1 раунд");
        assertThat(SpellDisplayStrings.ruAmount(2, "round")).isEqualTo("2 раунда");
        assertThat(SpellDisplayStrings.ruAmount(5, "round")).isEqualTo("5 раундов");
        assertThat(SpellDisplayStrings.ruAmount(11, "round")).isEqualTo("11 раундов");
        assertThat(SpellDisplayStrings.ruAmount(21, "day")).isEqualTo("21 день");
    }

    @Test
    void validateRejectsUnknownSlug() {
        HomebrewSpellRequest r = req();
        r.setRangeType("телепорт");
        assertThatThrownBy(() -> SpellDisplayStrings.validate(r))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("дистанция");
    }

    @Test
    void validateRequiresDistanceValue() {
        HomebrewSpellRequest r = req();
        r.setRangeType("distance");
        assertThatThrownBy(() -> SpellDisplayStrings.validate(r))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("дистанции");
    }
}
