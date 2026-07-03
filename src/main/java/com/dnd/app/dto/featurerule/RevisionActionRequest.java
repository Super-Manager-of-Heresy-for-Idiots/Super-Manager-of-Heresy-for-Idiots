package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Body for revision lifecycle actions (approve, disable, create-draft-from-approved, rollback).
 * All fields optional; {@code targetRevisionId} is only used by rollback.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisionActionRequest {

    /** For rollback: the prior revision to make the active approved one. */
    private UUID targetRevisionId;

    @Size(max = 2000)
    private String changeReason;
}
