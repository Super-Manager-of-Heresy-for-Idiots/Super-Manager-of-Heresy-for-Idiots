package com.dnd.app.service;

import com.dnd.app.domain.CustomResourceType;
import com.dnd.app.domain.Feat;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.featurerule.FeatureFormulaValidationResponse;
import com.dnd.app.dto.request.CustomResourceTypeRequest;
import com.dnd.app.dto.response.CustomResourceTypeAdminResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CharacterResourceRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.CustomResourceTypeRepository;
import com.dnd.app.repository.FeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Admin CRUD for class resource templates ({@code custom_resource_types}) — the single, player-facing resource
 * system (see [[existing-resource-system]]). Lets a GM/admin set a resource's max (fixed or a per-character DSL
 * formula such as {@code class_level("monk")}) and bind it to a class so members auto-provision it.
 */
@Service
@RequiredArgsConstructor
public class CustomResourceTypeAdminService {

    private final CustomResourceTypeRepository repository;
    private final ContentCharacterClassRepository classRepository;
    private final CharacterResourceRepository characterResourceRepository;
    private final FeatRepository featRepository;
    private final FeatureFormulaService formulaService;

    @Transactional(readOnly = true)
    public List<CustomResourceTypeAdminResponse> list() {
        return repository.findByHomebrewIsNull().stream()
                .sorted(Comparator.comparing(CustomResourceType::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CustomResourceTypeAdminResponse create(CustomResourceTypeRequest req) {
        CustomResourceType type = new CustomResourceType();
        apply(type, req);
        return toResponse(repository.save(type));
    }

    @Transactional
    public CustomResourceTypeAdminResponse update(UUID id, CustomResourceTypeRequest req) {
        CustomResourceType type = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
        apply(type, req);
        return toResponse(repository.save(type));
    }

    @Transactional
    public void delete(UUID id) {
        CustomResourceType type = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден"));
        // Remove any per-character instances first (no DB cascade is defined), then the template.
        characterResourceRepository.deleteByResourceTypeId(id);
        repository.delete(type);
    }

    private void apply(CustomResourceType type, CustomResourceTypeRequest req) {
        type.setName(req.getName().trim());
        type.setDescription(blankToNull(req.getDescription()));
        type.setMaxValue(req.getMaxValue());
        type.setMaxFormula(blankToNull(req.getMaxFormula()));
        type.setResetOn(normalizeReset(req.getResetOn()));
        if (req.getClassBoundId() != null) {
            ContentCharacterClass clazz = classRepository.findById(req.getClassBoundId())
                    .orElseThrow(() -> new BadRequestException("Класс не найден"));
            type.setClassBound(clazz);
        } else {
            type.setClassBound(null);
        }
        if (req.getFeatBoundId() != null) {
            Feat feat = featRepository.findById(req.getFeatBoundId())
                    .orElseThrow(() -> new BadRequestException("Черта не найдена"));
            type.setFeatBound(feat);
        } else {
            type.setFeatBound(null);
        }
    }

    private CustomResourceTypeAdminResponse toResponse(CustomResourceType type) {
        String status = null;
        String message = null;
        if (type.getMaxFormula() != null && !type.getMaxFormula().isBlank()) {
            FeatureFormulaValidationResponse v = formulaService.validate(type.getMaxFormula(), "integer");
            status = v.isValid() ? "valid" : "invalid";
            message = v.getMessage();
        }
        ContentCharacterClass clazz = type.getClassBound();
        Feat feat = type.getFeatBound();
        return CustomResourceTypeAdminResponse.builder()
                .id(type.getId())
                .name(type.getName())
                .description(type.getDescription())
                .maxValue(type.getMaxValue())
                .maxFormula(type.getMaxFormula())
                .maxFormulaStatus(status)
                .maxFormulaMessage(message)
                .classBoundId(clazz != null ? clazz.getId() : null)
                .className(clazz != null ? clazz.getNameRu() : null)
                .featBoundId(feat != null ? feat.getId() : null)
                .featName(feat != null ? feat.getNameRu() : null)
                .resetOn(type.getResetOn())
                .homebrew(type.getHomebrew() != null)
                .build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Only {@code short_rest}/{@code long_rest} are accepted; anything else means no automatic reset. */
    private static String normalizeReset(String s) {
        if (s == null) {
            return "none";
        }
        String v = s.trim().toLowerCase();
        return (v.equals("short_rest") || v.equals("long_rest")) ? v : "none";
    }
}
