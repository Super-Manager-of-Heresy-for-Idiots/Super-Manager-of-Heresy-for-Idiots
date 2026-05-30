package com.dnd.app.domain;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomebrewRatingId implements Serializable {
    private UUID userId;
    private UUID packageId;
}
