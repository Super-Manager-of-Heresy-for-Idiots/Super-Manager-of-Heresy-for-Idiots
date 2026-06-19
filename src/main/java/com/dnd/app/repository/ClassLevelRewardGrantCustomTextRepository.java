package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantCustomText;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassLevelRewardGrantCustomTextRepository extends JpaRepository<ClassLevelRewardGrantCustomText, UUID> {

    List<ClassLevelRewardGrantCustomText> findAllByUserEditable(Boolean userEditable);
}
