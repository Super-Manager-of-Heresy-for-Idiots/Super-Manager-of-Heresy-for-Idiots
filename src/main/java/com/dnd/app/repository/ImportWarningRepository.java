package com.dnd.app.repository;

import com.dnd.app.domain.content.ImportWarning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ImportWarningRepository extends JpaRepository<ImportWarning, UUID> {

    List<ImportWarning> findAllByOrderByCreatedAtDesc();

    List<ImportWarning> findAllByEntityKindOrderByCreatedAtDesc(String entityKind);
}
