package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс ClassAuthoringIdempotencyRecord описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "class_authoring_idempotency",
        uniqueConstraints = @UniqueConstraint(name = "uq_class_authoring_idem", columnNames = {"scope", "idem_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassAuthoringIdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "scope", nullable = false, columnDefinition = "text")
    private String scope;

    @Column(name = "idem_key", nullable = false, columnDefinition = "text")
    private String idemKey;

    @Column(name = "request_hash", nullable = false, columnDefinition = "text")
    private String requestHash;

    @Column(name = "result_class_id", nullable = false)
    private UUID resultClassId;

    @Column(name = "package_id")
    private UUID packageId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
