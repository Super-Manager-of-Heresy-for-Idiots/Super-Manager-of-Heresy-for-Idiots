package com.dnd.app.repository;

import com.dnd.app.domain.ItemTemplateBuff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemTemplateBuffRepository extends JpaRepository<ItemTemplateBuff, UUID> {

    List<ItemTemplateBuff> findByTemplateId(UUID templateId);
}
