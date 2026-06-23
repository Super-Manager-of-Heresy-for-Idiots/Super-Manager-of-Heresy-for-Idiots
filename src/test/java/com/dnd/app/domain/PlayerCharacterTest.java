package com.dnd.app.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PlayerCharacter.applyHpDelta: единая HP-математика для всех путей записи")
class PlayerCharacterTest {

    private PlayerCharacter character(Integer currentHp, Integer tempHp, Integer maxHp) {
        return PlayerCharacter.builder()
                .name("Hero").currentHp(currentHp).tempHp(tempHp).maxHp(maxHp).build();
    }

    @Test
    @DisplayName("Урон без временных HP уменьшает текущие HP")
    void damage_noTempHp() {
        PlayerCharacter c = character(20, 0, 20);
        c.applyHpDelta(-7, 20);
        assertEquals(13, c.getCurrentHp());
        assertEquals(0, c.getTempHp());
    }

    @Test
    @DisplayName("Временные HP поглощают урон первыми, остаток уходит в текущие")
    void damage_absorbedByTempHp() {
        PlayerCharacter c = character(10, 5, 20);
        c.applyHpDelta(-8, 20);
        assertEquals(7, c.getCurrentHp(), "5 поглощено temp HP, 3 ушло в текущие: 10-3=7");
        assertEquals(0, c.getTempHp());
    }

    @Test
    @DisplayName("Урон не уводит текущие HP ниже нуля")
    void damage_flooredAtZero() {
        PlayerCharacter c = character(5, 0, 20);
        c.applyHpDelta(-10, 20);
        assertEquals(0, c.getCurrentHp());
        assertEquals(0, c.getTempHp());
    }

    @Test
    @DisplayName("Лечение ограничено максимумом HP")
    void heal_cappedAtMax() {
        PlayerCharacter c = character(15, 0, 20);
        c.applyHpDelta(10, 20);
        assertEquals(20, c.getCurrentHp());
    }

    @Test
    @DisplayName("При неизвестном максимуме (0) лечение не ограничивается")
    void heal_uncappedWhenMaxUnknown() {
        PlayerCharacter c = character(5, 0, 0);
        c.applyHpDelta(10, 0);
        assertEquals(15, c.getCurrentHp());
    }

    @Test
    @DisplayName("Нулевая дельта ничего не меняет")
    void zeroDelta_noChange() {
        PlayerCharacter c = character(12, 4, 20);
        c.applyHpDelta(0, 20);
        assertEquals(12, c.getCurrentHp());
        assertEquals(4, c.getTempHp());
    }

    @Test
    @DisplayName("Null текущих HP трактуется как 0")
    void nullCurrentHp_treatedAsZero() {
        PlayerCharacter c = PlayerCharacter.builder().name("Hero").maxHp(10).build();
        c.applyHpDelta(-3, 10);
        assertEquals(0, c.getCurrentHp());
        assertEquals(0, c.getTempHp());
    }
}
