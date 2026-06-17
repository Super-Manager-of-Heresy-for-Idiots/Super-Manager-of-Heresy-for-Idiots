package com.dnd.app.service;

import com.dnd.app.domain.CharacterClass;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.content.RuntimeMigrationReport;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.CharacterClassRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.ProficiencySkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RuntimeDataMigrationService: миграция legacy class_id/skill_id по имени")
class RuntimeDataMigrationServiceTest {

    @Mock private JdbcTemplate jdbc;
    @Mock private ContentCharacterClassRepository contentClassRepository;
    @Mock private CharacterClassRepository legacyClassRepository;
    @Mock private ContentSkillRepository contentSkillRepository;
    @Mock private ProficiencySkillRepository legacySkillRepository;

    @InjectMocks private RuntimeDataMigrationService service;

    private ContentCharacterClass newFighter;
    private UUID legacyFighterId;
    private UUID legacyBardId;
    private UUID legacyGhostId;

    @BeforeEach
    void setUp() {
        newFighter = content("Fighter", "Воин");
        ContentCharacterClass bardA = content("Bard", "Бард-А");
        ContentCharacterClass bardB = content("Bard", "Бард-Б");

        legacyFighterId = UUID.randomUUID();
        legacyBardId = UUID.randomUUID();
        legacyGhostId = UUID.randomUUID();

        lenient().when(contentClassRepository.findAllByHomebrewIsNull())
                .thenReturn(List.of(newFighter, bardA, bardB));
        lenient().when(contentSkillRepository.findAllByHomebrewIsNull()).thenReturn(List.of());

        // distinct class ids: one already-new + three legacy
        lenient().when(jdbc.queryForList(contains("character_class_levels"), eq(UUID.class)))
                .thenReturn(List.of(newFighter.getId(), legacyFighterId, legacyBardId, legacyGhostId));
        lenient().when(jdbc.queryForList(contains("character_skill_proficiencies"), eq(UUID.class)))
                .thenReturn(List.of());

        lenient().when(contentClassRepository.existsById(newFighter.getId())).thenReturn(true);
        lenient().when(contentClassRepository.existsById(legacyFighterId)).thenReturn(false);
        lenient().when(contentClassRepository.existsById(legacyBardId)).thenReturn(false);
        lenient().when(contentClassRepository.existsById(legacyGhostId)).thenReturn(false);

        lenient().when(legacyClassRepository.findById(legacyFighterId))
                .thenReturn(Optional.of(legacy("Fighter")));
        lenient().when(legacyClassRepository.findById(legacyBardId))
                .thenReturn(Optional.of(legacy("Bard")));
        lenient().when(legacyClassRepository.findById(legacyGhostId))
                .thenReturn(Optional.of(legacy("Ghost")));

        lenient().when(jdbc.queryForObject(any(String.class), eq(Integer.class))).thenReturn(0);
    }

    private ContentCharacterClass content(String nameEn, String nameRu) {
        return ContentCharacterClass.builder()
                .id(UUID.randomUUID()).slug(nameEn.toLowerCase()).nameEn(nameEn).nameRu(nameRu).build();
    }

    private CharacterClass legacy(String name) {
        return CharacterClass.builder().id(UUID.randomUUID()).name(name).build();
    }

    @Test
    @DisplayName("Dry-run классифицирует: alreadyNew / mapped / ambiguous / unmapped, без записи")
    void dryRun_classifiesWithoutWriting() {
        RuntimeMigrationReport report = service.migrate(true, false);

        RuntimeMigrationReport.EntityMigration classes = report.getClasses();
        assertEquals(1, classes.getAlreadyNew());
        assertEquals(1, classes.getMapped().size());
        assertEquals(newFighter.getId(), classes.getMapped().get(0).getNewId());
        assertEquals(1, classes.getAmbiguous().size());
        assertEquals(2, classes.getAmbiguous().get(0).getCandidateCount());
        assertEquals(1, classes.getUnmapped().size());
        assertEquals(0, classes.getRowsUpdated());
        assertTrue(report.isDryRun());
        verify(jdbc, never()).update(any(String.class), any(), any());
    }

    @Test
    @DisplayName("Применение без подтверждения бэкапа отклоняется")
    void apply_withoutBackupConfirmation_rejected() {
        assertThrows(BadRequestException.class, () -> service.migrate(false, false));
        verify(jdbc, never()).update(any(String.class), any(), any());
    }

    @Test
    @DisplayName("Применение с подтверждением обновляет только однозначно сопоставленные строки")
    void apply_updatesUniquelyMappedRows() {
        when(jdbc.update(contains("character_class_levels"), eq(newFighter.getId()), eq(legacyFighterId)))
                .thenReturn(3);

        RuntimeMigrationReport report = service.migrate(false, true);

        assertEquals(3, report.getClasses().getRowsUpdated());
        verify(jdbc, times(1)).update(
                contains("character_class_levels"), eq(newFighter.getId()), eq(legacyFighterId));
        // ambiguous/unmapped are never written
        verify(jdbc, never()).update(contains("character_class_levels"), eq(legacyBardId), any());
    }
}
