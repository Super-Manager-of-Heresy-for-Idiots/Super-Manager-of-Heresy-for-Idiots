package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-side record of one issued refresh token. Each login starts a {@code familyId}
 * chain; every silent refresh rotates the token, marking the previous row consumed
 * ({@code revoked} + {@code replacedBy}). Presenting an already-consumed token is a theft
 * signal — the whole family is then revoked. Logout revokes the family too.
 */
@Entity
@Table(name = "refresh_token", indexes = @Index(name = "idx_refresh_token_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Matches the "jti" claim of the issued JWT. */
    @Column(nullable = false, unique = true)
    private UUID jti;

    /** Shared across all rotations originating from a single login. */
    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    /** Id of the row that superseded this one on rotation; null while current. */
    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "ip")
    private String ip;
}
