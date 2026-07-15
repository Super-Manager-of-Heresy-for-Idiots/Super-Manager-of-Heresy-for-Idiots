package com.dnd.app.service.homebrew;

import com.dnd.app.domain.CreatureSize;
import com.dnd.app.domain.HomebrewContentItem;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.content.Species;
import com.dnd.app.domain.content.SpeciesSpeed;
import com.dnd.app.domain.content.SpeciesTrait;
import com.dnd.app.domain.content.SpeciesTraitEffect;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CreatureSizeRepository;
import com.dnd.app.repository.HomebrewContentItemRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.SpeciesRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Сервис авторинга homebrew-видов (P0.5 / SP-1). Виды раньше были read-only; здесь GM создаёт/редактирует
 * вид внутри своего DRAFT-пакета.
 *
 * <p>Богатый FE-контракт RaceRequest не ложится в реляционную модель species, поэтому канонический payload
 * авторинга хранится лоссово в {@code species.authoring_json} (round-trip для RaceEditor), а реляционный граф
 * (creature_type/size_options/speeds/traits) строится как проекция для визарда и чтения через
 * {@code /reference/species}. Механика трейтов (резисты/darkvision через движок) — SP-4, после P1-3; сейчас,
 * как и у vanilla-видов, всё живёт на снапшоте/проекции.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeciesAuthoringService {

    private final SpeciesRepository speciesRepository;
    private final HomebrewAccessService homebrewAccessService;
    private final HomebrewContentItemRepository contentItemRepository;
    private final CreatureSizeRepository creatureSizeRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final ObjectMapper objectMapper;

    /**
     * Создаёт вид внутри пакета.
     * @param packageId пакет-владелец (DRAFT, принадлежит пользователю)
     * @param body payload авторинга (FE RaceRequest)
     * @param username автор
     * @return вид в форме RaceResponse (JSON)
     */
    @Transactional
    public JsonNode create(UUID packageId, JsonNode body, String username) {
        HomebrewPackage pkg = editablePackage(packageId, username);
        Species sp = new Species();
        sp.setHomebrew(pkg);
        sp.setCreatedBy(pkg.getAuthor());
        sp.setSlug(uniqueSlug(bodySlugBase(body), pkg.getId()));
        applyBody(sp, body, pkg);
        sp.setUpdatedBy(pkg.getAuthor());
        Species saved = speciesRepository.save(sp);
        registerContentItem(pkg, saved.getId());
        log.info("Homebrew species created: id={}, packageId={}, by={}", saved.getId(), packageId, username);
        return toResponse(saved);
    }

    /**
     * Обновляет вид пакета.
     * @param packageId пакет-владелец
     * @param speciesId вид
     * @param body payload авторинга
     * @param username автор
     * @return обновлённый вид (JSON)
     */
    @Transactional
    public JsonNode update(UUID packageId, UUID speciesId, JsonNode body, String username) {
        HomebrewPackage pkg = editablePackage(packageId, username);
        Species sp = requirePackageSpecies(packageId, speciesId);
        String requested = bodySlugBase(body);
        if (requested != null && !requested.equals(sp.getSlug())) {
            sp.setSlug(uniqueSlug(requested, pkg.getId()));
        }
        applyBody(sp, body, pkg);
        sp.setUpdatedBy(pkg.getAuthor());
        Species saved = speciesRepository.save(sp);
        log.info("Homebrew species updated: id={}, packageId={}, by={}", speciesId, packageId, username);
        return toResponse(saved);
    }

    /**
     * Читает вид пакета (для загрузки в редактор).
     * @param packageId пакет
     * @param speciesId вид
     * @param username пользователь (владелец/ADMIN, либо PUBLISHED)
     * @return вид (JSON)
     */
    @Transactional(readOnly = true)
    public JsonNode get(UUID packageId, UUID speciesId, String username) {
        homebrewAccessService.enforceReadable(packageId, username);
        Species sp = requirePackageSpecies(packageId, speciesId);
        return toResponse(sp);
    }

    /**
     * Включает вид (виден игрокам/выбираем).
     */
    @Transactional
    public JsonNode enable(UUID packageId, UUID speciesId, String username) {
        return setActive(packageId, speciesId, username, true);
    }

    /**
     * Выключает вид (скрыт).
     */
    @Transactional
    public JsonNode disable(UUID packageId, UUID speciesId, String username) {
        return setActive(packageId, speciesId, username, false);
    }

    private JsonNode setActive(UUID packageId, UUID speciesId, String username, boolean active) {
        editablePackage(packageId, username);
        Species sp = requirePackageSpecies(packageId, speciesId);
        sp.setActive(active);
        return toResponse(speciesRepository.save(sp));
    }

    /**
     * Удаляет вид пакета. 409, если вид используется персонажами (по образцу P1-2).
     * @param packageId пакет
     * @param speciesId вид
     * @param username автор
     */
    @Transactional
    public void delete(UUID packageId, UUID speciesId, String username) {
        editablePackage(packageId, username);
        Species sp = requirePackageSpecies(packageId, speciesId);
        long dependents = playerCharacterRepository.countByRaceId(speciesId);
        if (dependents > 0) {
            throw new DuplicateResourceException(
                    "Нельзя удалить вид: его используют персонажей — " + dependents);
        }
        contentItemRepository.findAllByHomebrewPackageId(packageId).stream()
                .filter(ci -> ci.getContentType() == ContentType.SPECIES && ci.getContentId().equals(speciesId))
                .forEach(contentItemRepository::delete);
        speciesRepository.delete(sp);
        log.info("Homebrew species deleted: id={}, packageId={}, by={}", speciesId, packageId, username);
    }

    /**
     * SP-2: глубокий клон vanilla-вида в пакет (главный сценарий «раса на базе эльфа»).
     * @param packageId пакет-владелец
     * @param vanillaSpeciesId исходный vanilla-вид
     * @param username автор
     * @return клон (JSON)
     */
    @Transactional
    public JsonNode duplicateFromVanilla(UUID packageId, UUID vanillaSpeciesId, String username) {
        HomebrewPackage pkg = editablePackage(packageId, username);
        Species source = speciesRepository.findById(vanillaSpeciesId)
                .orElseThrow(() -> new ResourceNotFoundException("Исходный вид не найден"));

        Species clone = new Species();
        clone.setHomebrew(pkg);
        clone.setCreatedBy(pkg.getAuthor());
        clone.setUpdatedBy(pkg.getAuthor());
        clone.setNameRu(source.getNameRu());
        clone.setNameEn(source.getNameEn());
        clone.setDescription(source.getDescription());
        clone.setCreatureType(source.getCreatureType());
        clone.setActive(true);
        clone.setSlug(uniqueSlug(source.getSlug() + "-copy", pkg.getId()));
        clone.getSizeOptions().addAll(source.getSizeOptions());
        for (SpeciesSpeed s : source.getSpeeds()) {
            SpeciesSpeed c = new SpeciesSpeed();
            c.setSpecies(clone);
            c.setSpeedTypeSlug(s.getSpeedTypeSlug());
            c.setAmountFt(s.getAmountFt());
            c.setRawText(s.getRawText());
            clone.getSpeeds().add(c);
        }
        int i = 0;
        for (SpeciesTrait t : source.getTraits()) {
            SpeciesTrait c = new SpeciesTrait();
            c.setSpecies(clone);
            c.setSlug(t.getSlug());
            c.setSortOrder(i++);
            c.setName(t.getName());
            c.setDescription(t.getDescription());
            // Глубокий клон эффектов трейта (darkvision/резисты/врождённые заклинания) — иначе
            // «раса на базе эльфа» теряет тёмное зрение и прочую механику снапшота.
            java.util.List<SpeciesTraitEffect> effects = new java.util.ArrayList<>();
            if (t.getEffects() != null) {
                for (SpeciesTraitEffect e : t.getEffects()) {
                    SpeciesTraitEffect ce = new SpeciesTraitEffect();
                    ce.setTrait(c);
                    ce.setEffectType(e.getEffectType());
                    ce.setDamageType(e.getDamageType());
                    ce.setSpell(e.getSpell());
                    ce.setRangeFt(e.getRangeFt());
                    effects.add(ce);
                }
            }
            c.setEffects(effects);
            clone.getTraits().add(c);
        }
        // У vanilla-вида нет authoring_json — реконструируем его из графа, чтобы RaceEditor показал данные клона.
        clone.setAuthoringJson(source.getAuthoringJson() != null && !source.getAuthoringJson().isBlank()
                ? source.getAuthoringJson()
                : reconstructAuthoringJson(clone));
        Species saved = speciesRepository.save(clone);
        registerContentItem(pkg, saved.getId());
        log.info("Homebrew species duplicated from vanilla: source={}, clone={}, packageId={}, by={}",
                vanillaSpeciesId, saved.getId(), packageId, username);
        return toResponse(saved);
    }

    // ================= helpers =================

    private HomebrewPackage editablePackage(UUID packageId, String username) {
        HomebrewPackage pkg = homebrewAccessService.enforceOwner(packageId, username);
        if (pkg.isDeleted() || !pkg.getStatus().isEditable()) {
            throw new BadRequestException("Виды можно менять только в DRAFT/PUBLISHED-пакете");
        }
        return pkg;
    }

    private Species requirePackageSpecies(UUID packageId, UUID speciesId) {
        return speciesRepository.findByIdAndHomebrew_Id(speciesId, packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Вид не найден в этом пакете"));
    }

    /** Переносит поля payload в сущность: имя/описание/active + best-effort граф (размеры/скорости/трейты). */
    private void applyBody(Species sp, JsonNode body, HomebrewPackage pkg) {
        String name = text(body, "name");
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Название вида обязательно");
        }
        sp.setNameRu(name);
        if (text(body, "nameEn") != null) {
            sp.setNameEn(text(body, "nameEn"));
        }
        sp.setDescription(text(body, "description"));
        sp.setActive(body.hasNonNull("active") ? body.get("active").asBoolean(true) : true);
        sp.setAuthoringJson(body.toString());

        // sizeOptions (best-effort by slug)
        Set<CreatureSize> sizes = new HashSet<>();
        if (body.has("sizeOptions") && body.get("sizeOptions").isArray()) {
            for (JsonNode s : body.get("sizeOptions")) {
                resolveSize(s.asText()).ifPresent(sizes::add);
            }
        }
        sp.setSizeOptions(sizes);

        // speeds — пересобираем на месте (orphanRemoval удалит старые)
        sp.getSpeeds().clear();
        JsonNode speed = body.get("speed");
        if (speed != null && speed.isObject()) {
            addSpeed(sp, "walk", speed.get("walk"));
            addSpeed(sp, "fly", speed.get("fly"));
            addSpeed(sp, "swim", speed.get("swim"));
            addSpeed(sp, "climb", speed.get("climb"));
            addSpeed(sp, "burrow", speed.get("burrow"));
        }

        // traits — пересобираем (без effects; механика SP-4)
        sp.getTraits().clear();
        if (body.has("traits") && body.get("traits").isArray()) {
            int i = 0;
            for (JsonNode t : body.get("traits")) {
                String tName = text(t, "name");
                if (tName == null || tName.isBlank()) {
                    continue;
                }
                SpeciesTrait trait = new SpeciesTrait();
                trait.setSpecies(sp);
                trait.setName(tName);
                trait.setDescription(text(t, "description"));
                trait.setSortOrder(i);
                trait.setSlug(slugify(tName) + "-" + i);
                trait.setEffects(new java.util.ArrayList<>());
                sp.getTraits().add(trait);
                i++;
            }
        }

        // Трансляция плоского darkvisionRange в трейт-эффект (словарь effect_type = "darkvision"),
        // чтобы тёмное зрение homebrew-вида попадало в снапшот персонажа так же, как у vanilla
        // (SpeciesService.darkvisionRange читает effectType содержащий "darkvision" → range_ft).
        // Прочие плоские поля (resistances/immunities/innateSpells) снапшот не читает — их исполнение
        // через движок это SP-4; до тех пор они лоссово лежат в authoring_json.
        JsonNode darkvision = body.get("darkvisionRange");
        if (darkvision != null && darkvision.isNumber() && darkvision.asInt() > 0) {
            SpeciesTrait dv = new SpeciesTrait();
            dv.setSpecies(sp);
            dv.setName("Тёмное зрение");
            dv.setSortOrder(sp.getTraits().size());
            dv.setSlug("darkvision");
            SpeciesTraitEffect eff = new SpeciesTraitEffect();
            eff.setTrait(dv);
            eff.setEffectType("darkvision");
            eff.setRangeFt(darkvision.asInt());
            dv.setEffects(new java.util.ArrayList<>(List.of(eff)));
            sp.getTraits().add(dv);
        }
    }

    private void addSpeed(Species sp, String typeSlug, JsonNode value) {
        if (value == null || value.isNull() || !value.isNumber()) {
            return;
        }
        SpeciesSpeed speed = new SpeciesSpeed();
        speed.setSpecies(sp);
        speed.setSpeedTypeSlug(typeSlug);
        speed.setAmountFt(value.asInt());
        sp.getSpeeds().add(speed);
    }

    private Optional<CreatureSize> resolveSize(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        Optional<CreatureSize> byLower = creatureSizeRepository.findBySlugAndHomebrewIsNull(slug.toLowerCase(Locale.ROOT));
        if (byLower.isPresent()) {
            return byLower;
        }
        return creatureSizeRepository.findBySlugAndHomebrewIsNull(slug);
    }

    private void registerContentItem(HomebrewPackage pkg, UUID speciesId) {
        if (!contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(
                pkg.getId(), ContentType.SPECIES, speciesId)) {
            contentItemRepository.save(HomebrewContentItem.builder()
                    .homebrewPackage(pkg)
                    .contentType(ContentType.SPECIES)
                    .contentId(speciesId)
                    .build());
        }
    }

    /** Собирает RaceResponse из хранимого authoring_json + авторитетных полей сущности. */
    private JsonNode toResponse(Species sp) {
        ObjectNode node;
        try {
            node = sp.getAuthoringJson() != null && !sp.getAuthoringJson().isBlank()
                    ? (ObjectNode) objectMapper.readTree(sp.getAuthoringJson())
                    : objectMapper.createObjectNode();
        } catch (Exception e) {
            node = objectMapper.createObjectNode();
        }
        node.put("id", sp.getId().toString());
        node.put("slug", sp.getSlug());
        node.put("name", sp.getNameRu());
        node.put("active", Boolean.TRUE.equals(sp.getActive()));
        node.put("sourceType", "HOMEBREW");
        if (sp.getHomebrew() != null) {
            node.put("homebrewPackageId", sp.getHomebrew().getId().toString());
            node.put("homebrewPackageTitle", sp.getHomebrew().getTitle());
        }
        return node;
    }

    /** Обратная проекция графа в RaceRequest-подобный JSON — чтобы клон vanilla-вида открывался в RaceEditor. */
    private String reconstructAuthoringJson(Species sp) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", sp.getNameRu());
        if (sp.getNameEn() != null) {
            node.put("nameEn", sp.getNameEn());
        }
        node.put("description", sp.getDescription());
        node.put("sourceType", "HOMEBREW");
        node.put("active", Boolean.TRUE.equals(sp.getActive()));

        var sizes = objectMapper.createArrayNode();
        sp.getSizeOptions().forEach(sz -> {
            if (sz.getSlug() != null) {
                sizes.add(sz.getSlug().toUpperCase(Locale.ROOT));
            }
        });
        node.set("sizeOptions", sizes);

        ObjectNode speed = objectMapper.createObjectNode();
        for (SpeciesSpeed s : sp.getSpeeds()) {
            if (s.getSpeedTypeSlug() != null && s.getAmountFt() != null) {
                speed.put(s.getSpeedTypeSlug().toLowerCase(Locale.ROOT), s.getAmountFt());
            }
        }
        node.set("speed", speed);

        var traits = objectMapper.createArrayNode();
        Integer darkvision = null;
        for (SpeciesTrait t : sp.getTraits()) {
            ObjectNode tn = objectMapper.createObjectNode();
            tn.put("name", t.getName());
            tn.put("description", t.getDescription());
            traits.add(tn);
            if (t.getEffects() != null) {
                for (SpeciesTraitEffect e : t.getEffects()) {
                    if (e.getEffectType() != null && e.getEffectType().toLowerCase(Locale.ROOT).contains("darkvision")
                            && e.getRangeFt() != null) {
                        darkvision = darkvision == null ? e.getRangeFt() : Math.max(darkvision, e.getRangeFt());
                    }
                }
            }
        }
        node.set("traits", traits);
        if (darkvision != null) {
            node.put("darkvisionRange", darkvision);
        }
        return node.toString();
    }

    private String bodySlugBase(JsonNode body) {
        String slug = text(body, "slug");
        if (slug != null && !slug.isBlank()) {
            return slugify(slug);
        }
        String name = text(body, "name");
        return name != null ? slugify(name) : null;
    }

    private String uniqueSlug(String base, UUID packageId) {
        String root = (base == null || base.isBlank()) ? "species-" + UUID.randomUUID().toString().substring(0, 8) : base;
        String candidate = root;
        int n = 2;
        while (speciesRepository.existsBySlugAndHomebrew_Id(candidate, packageId)) {
            candidate = root + "-" + n++;
        }
        return candidate;
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String slugify(String s) {
        if (s == null) {
            return "";
        }
        String slug = s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9а-я]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isBlank() ? "species" : slug;
    }
}
