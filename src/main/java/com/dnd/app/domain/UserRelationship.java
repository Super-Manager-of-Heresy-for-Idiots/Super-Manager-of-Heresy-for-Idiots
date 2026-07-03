package com.dnd.app.domain;

import com.dnd.app.domain.enums.RelationshipStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A single social-graph edge between two users. The pair is normalized so {@code userAId < userBId}
 * (PostgreSQL uuid ordering — see {@link com.dnd.app.util.UuidOrdering}); {@code requesterId} and
 * {@code blockedById} preserve direction for PENDING and BLOCKED rows respectively.
 */
@Entity
@Table(name = "user_relationships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_a_id", nullable = false)
    private UUID userAId;

    @Column(name = "user_b_id", nullable = false)
    private UUID userBId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RelationshipStatus status;

    @Column(name = "requester_id")
    private UUID requesterId;

    @Column(name = "blocked_by_id")
    private UUID blockedById;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
