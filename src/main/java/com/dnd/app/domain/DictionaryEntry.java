package com.dnd.app.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Shared shape of the 10 bestiary reference dictionaries (code + localized names +
 * optional homebrew owner). Lets one generic service/repository handle all of them.
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
