package com.dnd.app.dto.response;

import com.dnd.app.domain.enums.RelationshipView;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Public profile projection for the "add a friend" search. Never carries the email. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResultResponse {
    private UUID id;
    private String username;
    private String role;
    private RelationshipView relationship;
}
