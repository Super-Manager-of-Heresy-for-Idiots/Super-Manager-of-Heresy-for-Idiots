package com.dnd.app.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс ImportWarning описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "import_warning")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "import_warning_id")
    private UUID id;

    @Column(name = "source_slug", columnDefinition = "text")
    private String sourceSlug;

    @Column(name = "entity_kind", columnDefinition = "text")
    private String entityKind;

    @Column(name = "entity_slug", columnDefinition = "text")
    private String entitySlug;

    @Column(name = "warning_code", nullable = false, columnDefinition = "text")
    private String warningCode;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
