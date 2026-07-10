package com.dnd.app.domain;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Класс GmHomebrewLibraryId описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GmHomebrewLibraryId implements Serializable {
    private UUID gmUserId;
    private UUID packageId;
}
