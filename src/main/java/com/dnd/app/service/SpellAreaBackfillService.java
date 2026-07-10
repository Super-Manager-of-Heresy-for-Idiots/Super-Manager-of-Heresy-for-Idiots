package com.dnd.app.service;

import com.dnd.app.domain.Spell;
import com.dnd.app.repository.SpellRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс SpellAreaBackfillService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpellAreaBackfillService {

    // Shape patterns over the lowercased description. Sizes are digits + «фут…».
    private static final Pattern SPHERE = Pattern.compile("сфер\\w*\\s+(?:с\\s+)?радиусом\\s+(\\d+)\\s*фут");
    private static final Pattern SPHERE_PRE = Pattern.compile("(\\d+)[-\\s]футов\\w*\\s+сфер");
    private static final Pattern CUBE = Pattern.compile("куб\\w*\\s+с\\s+(?:длиной\\s+)?ребр\\w*\\s+(\\d+)\\s*фут");
    private static final Pattern CUBE_PRE = Pattern.compile("(\\d+)[-\\s]футов\\w*\\s+куб");
    private static final Pattern CYLINDER = Pattern.compile("цилиндр\\w*\\s+(?:с\\s+)?радиусом\\s+(\\d+)\\s*фут");
    private static final Pattern CONE = Pattern.compile("конус\\w*\\s+(?:длиной\\s+)?(\\d+)\\s*фут");
    private static final Pattern CONE_PRE = Pattern.compile("(\\d+)[-\\s]футов\\w*\\s+конус");
    private static final Pattern LINE = Pattern.compile("лини\\w*\\s+длиной\\s+(\\d+)\\s*фут");

    private final SpellRepository spellRepository;

    /** One parsed shape candidate: its kind, size and where in the text it matched (earliest wins). */
    private record ShapeMatch(String shape, int sizeFt, int position) {
    }

    /**
     * Выполняет обратное заполнение операции "backfill" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public int backfill() {
        List<Spell> spells = spellRepository.findAll();
        int updated = 0;
        for (Spell spell : spells) {
            // Non-destructive gate: skip as soon as anything is already set (parser or admin).
            if (spell.getAreaShape() != null || spell.getZoneTerrain() != null
                    || spell.getZoneObscurement() != null || Boolean.TRUE.equals(spell.getZonePersists())) {
                continue;
            }
            String text = spell.getDescription();
            if (text == null || text.isBlank()) {
                continue;
            }
            String lower = text.toLowerCase(Locale.ROOT);

            ShapeMatch shape = earliestShape(lower);
            String terrain = lower.contains("труднопроходим") ? "DIFFICULT" : null;
            String obscurement = lower.contains("сильно заслоня") ? "HEAVY"
                    : lower.contains("слабо заслоня") ? "LIGHT" : null;
            if (shape == null && terrain == null && obscurement == null) {
                continue;
            }

            if (shape != null) {
                spell.setAreaShape(shape.shape());
                spell.setAreaSizeFt(shape.sizeFt());
            }
            spell.setZoneTerrain(terrain);
            spell.setZoneObscurement(obscurement);
            spell.setZonePersists(terrain != null || obscurement != null);
            spellRepository.save(spell);
            updated++;
        }
        if (updated > 0) {
            log.info("Spell area/zone backfill: {} spells parsed into structured columns", updated);
        }
        return updated;
    }

    /** The shape whose mention appears first in the text (a description may reference several). */
    private ShapeMatch earliestShape(String lower) {
        ShapeMatch best = null;
        best = better(best, find(lower, "SPHERE", SPHERE, SPHERE_PRE));
        best = better(best, find(lower, "CUBE", CUBE, CUBE_PRE));
        best = better(best, find(lower, "CYLINDER", CYLINDER));
        best = better(best, find(lower, "CONE", CONE, CONE_PRE));
        best = better(best, find(lower, "LINE", LINE));
        return best;
    }

    private ShapeMatch find(String lower, String shape, Pattern... patterns) {
        ShapeMatch best = null;
        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(lower);
            if (m.find()) {
                ShapeMatch candidate = new ShapeMatch(shape, Integer.parseInt(m.group(1)), m.start());
                best = better(best, candidate);
            }
        }
        return best;
    }

    private static ShapeMatch better(ShapeMatch a, ShapeMatch b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.position() <= b.position() ? a : b;
    }
}
