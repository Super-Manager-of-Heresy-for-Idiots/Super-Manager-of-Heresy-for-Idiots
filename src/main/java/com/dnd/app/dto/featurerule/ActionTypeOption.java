package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** An action-economy type option (action, bonus_action, reaction, …) for the admin editor dropdown. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionTypeOption {
    private UUID id;
    private String code;
    private String label;
}
