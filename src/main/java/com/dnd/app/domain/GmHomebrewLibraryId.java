package com.dnd.app.domain;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GmHomebrewLibraryId implements Serializable {
    private UUID gmUserId;
    private UUID packageId;
}
