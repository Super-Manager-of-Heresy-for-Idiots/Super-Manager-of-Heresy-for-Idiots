package com.dnd.app.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Контракт DictionaryEntry описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface DictionaryEntry {

    UUID getId();

    String getCode();
    void setCode(String code);

    String getNameRusloc();
    void setNameRusloc(String nameRusloc);

    String getNameEngloc();
    void setNameEngloc(String nameEngloc);

    HomebrewPackage getHomebrew();
    void setHomebrew(HomebrewPackage homebrew);

    Boolean getIsUnique();
    void setIsUnique(Boolean isUnique);

    Instant getCreatedAt();

    Instant getUpdatedAt();
}
