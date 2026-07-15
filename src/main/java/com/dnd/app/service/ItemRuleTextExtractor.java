package com.dnd.app.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс ItemRuleTextExtractor — чистый (без БД и репозиториев) парсер описания магического
 * предмета (ru/en) в неизменяемый результат {@link ItemRuleExtraction}.
 * <p>
 * Используется бэкфилл-сервисом {@code ItemRuleBackfillService} как первый (детерминированный)
 * шаг разбора текста: извлекает статические бонусы, заряды, имена заклинаний-кандидатов и
 * фрагменты, которые ничего распознаваемого не дали (для ручной адъюдикации). Никакой работы
 * с БД здесь нет — сопоставление имён заклинаний со справочником и создание правил делает сервис.
 * <p>
 * Робастен к null/blank-входу: возвращает пустую (но не null) экстракцию.
 */
@Component
public class ItemRuleTextExtractor {

    // ── Целевые коды статических бонусов ─────────────────────────────────────
    /** Бонус к броскам атаки и урона (оружие). */
    public static final String TARGET_ATTACK_AND_DAMAGE = "ATTACK_AND_DAMAGE";
    /** Бонус к классу доспеха (КД / AC). */
    public static final String TARGET_AC = "AC";
    /** Бонус к спасброскам. */
    public static final String TARGET_SAVING_THROWS = "SAVING_THROWS";

    /** Код отдыха для сброса зарядов «на рассвете» / "at dawn" (по решению D5 — длинный отдых). */
    public static final String RESET_LONG_REST = "long_rest";

    /**
     * Общие флаги для всех паттернов. UNICODE_CHARACTER_CLASS обязателен, иначе {@code \w}/{@code \b}
     * не считают кириллицу словом (например «восстанавливает» не матчилось по {@code \w*}).
     */
    private static final int FLAGS =
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS;

    /**
     * Необязательное слово-связка между числом и целью бонуса в англ. тексте
     * (например «+1 bonus to attack…»). Допускает 0..2 слов до «to».
     */
    private static final String EN_BONUS_GAP = "(?:\\s+\\p{L}+){0,2}";

    // ── Статические бонусы: «+N к броскам атаки и урона» / "+N to attack and damage rolls" ──
    private static final Pattern ATTACK_AND_DAMAGE_PATTERN = Pattern.compile(
            "\\+\\s*(\\d+)\\s*(?:к\\s+броскам\\s+атаки\\s+и\\s+урона"
                    + "|" + EN_BONUS_GAP + "\\s+to\\s+attack\\s+and\\s+damage\\s+rolls)",
            FLAGS);

    // ── Статические бонусы: «+N к КД» / "+N to AC" ───────────────────────────
    private static final Pattern AC_PATTERN = Pattern.compile(
            "\\+\\s*(\\d+)\\s*(?:к\\s+КД|" + EN_BONUS_GAP + "\\s+to\\s+AC)\\b",
            FLAGS);

    // ── Статические бонусы: «+N к спасброскам» / "+N to saving throws" ───────
    private static final Pattern SAVING_THROWS_PATTERN = Pattern.compile(
            "\\+\\s*(\\d+)\\s*(?:к\\s+спасброскам|" + EN_BONUS_GAP + "\\s+to\\s+saving\\s+throws)",
            FLAGS);

    // ── Заряды: «имеет N зарядов» / "has N charges» → max=N ──────────────────
    private static final Pattern CHARGES_MAX_PATTERN = Pattern.compile(
            "(?:имеет|has)\\s+(\\d+)\\s+(?:заряд\\w*|charges?)",
            FLAGS);

    // ── Заряды: формула восстановления «восстанавливает 1d6+1 ...» / "regains 1d6 + 1 ..." ──
    private static final Pattern CHARGES_RESET_PATTERN = Pattern.compile(
            "(?:восстанавлива\\w*|regains?)\\s+(\\d*\\s*[dкd]\\s*\\d+(?:\\s*\\+\\s*\\d+)?)",
            FLAGS);

    // ── Сброс на рассвете / at dawn → длинный отдых ──────────────────────────
    private static final Pattern DAWN_PATTERN = Pattern.compile(
            "(?:на\\s+рассвете|at\\s+dawn)",
            FLAGS);

    // ── Кандидаты заклинаний: слова после «заклинание» / "cast the spell" ────
    private static final Pattern SPELL_NAME_PATTERN = Pattern.compile(
            "(?:заклинание|cast\\s+the\\s+spell)\\s+"
                    + "([\\p{L}][\\p{L}'’\\- ]*?[\\p{L}])"
                    + "(?=[.,;:!?)\\n]|\\s+(?:на|из|с|for|as|at|using|from)\\b|$)",
            FLAGS);

    // ── Разбиение на фрагменты/предложения для manual_adjudication ───────────
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?\\n])\\s+");

    /**
     * Разбирает описание магического предмета в неизменяемую экстракцию.
     * @param description сырой текст описания предмета (ru или en; может быть null/blank)
     * @return извлечённые данные; при null/blank-входе — пустая экстракция (никогда не null)
     */
    public ItemRuleExtraction extract(String description) {
        if (description == null || description.isBlank()) {
            return ItemRuleExtraction.empty();
        }

        List<StaticBonus> staticBonuses = new ArrayList<>();
        List<String> spellNames = new ArrayList<>();
        List<String> manualFragments = new ArrayList<>();

        extractStaticBonuses(description, staticBonuses);
        Charges charges = extractCharges(description);
        extractSpellNames(description, spellNames);

        // Фрагменты, ничего не давшие ни одному экстрактору — на ручную адъюдикацию.
        collectManualFragments(description, staticBonuses, charges, spellNames, manualFragments);

        return new ItemRuleExtraction(
                List.copyOf(staticBonuses),
                charges,
                List.copyOf(spellNames),
                List.copyOf(manualFragments));
    }

    /**
     * Извлекает все статические бонусы (атака+урон / КД / спасброски) из текста.
     * @param text описание предмета
     * @param out аккумулятор найденных бонусов
     */
    private void extractStaticBonuses(String text, List<StaticBonus> out) {
        addStaticBonus(ATTACK_AND_DAMAGE_PATTERN, text, TARGET_ATTACK_AND_DAMAGE, out);
        addStaticBonus(AC_PATTERN, text, TARGET_AC, out);
        addStaticBonus(SAVING_THROWS_PATTERN, text, TARGET_SAVING_THROWS, out);
    }

    /**
     * Применяет один паттерн бонуса ко всему тексту и добавляет совпадения в аккумулятор.
     * @param pattern скомпилированный паттерн с группой-числом
     * @param text описание предмета
     * @param target целевой код бонуса ({@link #TARGET_ATTACK_AND_DAMAGE} и т.п.)
     * @param out аккумулятор найденных бонусов
     */
    private void addStaticBonus(Pattern pattern, String text, String target, List<StaticBonus> out) {
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            out.add(new StaticBonus(Integer.parseInt(m.group(1)), target));
        }
    }

    /**
     * Извлекает заряды предмета: максимум и (опционально) формулу/тип отдыха для сброса.
     * @param text описание предмета
     * @return {@link Charges} либо null, если упоминания зарядов нет
     */
    private Charges extractCharges(String text) {
        Matcher maxMatcher = CHARGES_MAX_PATTERN.matcher(text);
        if (!maxMatcher.find()) {
            return null;
        }
        int max = Integer.parseInt(maxMatcher.group(1));

        String resetFormula = null;
        Matcher resetMatcher = CHARGES_RESET_PATTERN.matcher(text);
        if (resetMatcher.find()) {
            resetFormula = normalizeDice(resetMatcher.group(1));
        }

        String resetRest = DAWN_PATTERN.matcher(text).find() ? RESET_LONG_REST : null;
        return new Charges(max, resetFormula, resetRest);
    }

    /**
     * Извлекает имена-кандидаты заклинаний из текста (best-effort). Точное сопоставление со
     * справочником делает сервис — здесь только поверхностный отбор слов после якорных фраз.
     * @param text описание предмета
     * @param out аккумулятор имён заклинаний
     */
    private void extractSpellNames(String text, List<String> out) {
        Matcher m = SPELL_NAME_PATTERN.matcher(text);
        while (m.find()) {
            String name = m.group(1).trim();
            if (!name.isEmpty() && !out.contains(name)) {
                out.add(name);
            }
        }
    }

    /**
     * Собирает предложения-фрагменты, из которых ни один экстрактор ничего не извлёк, — они
     * пойдут в issue ручной адъюдикации ({@code manual_adjudication}).
     * @param text полное описание
     * @param staticBonuses найденные статические бонусы
     * @param charges найденные заряды (или null)
     * @param spellNames найденные имена заклинаний
     * @param out аккумулятор нераспознанных фрагментов
     */
    private void collectManualFragments(String text, List<StaticBonus> staticBonuses, Charges charges,
                                        List<String> spellNames, List<String> out) {
        for (String raw : SENTENCE_SPLIT.split(text)) {
            String fragment = raw.trim();
            if (fragment.isEmpty()) {
                continue;
            }
            if (fragmentIsExtractable(fragment, staticBonuses, charges, spellNames)) {
                continue;
            }
            out.add(fragment);
        }
    }

    /**
     * Проверяет, дал ли фрагмент хоть какое-то извлекаемое совпадение.
     * @param fragment одно предложение/фрагмент
     * @param staticBonuses найденные бонусы (для проверки, есть ли они вообще)
     * @param charges найденные заряды
     * @param spellNames найденные заклинания
     * @return true, если фрагмент содержит распознанные данные и не требует ручного разбора
     */
    private boolean fragmentIsExtractable(String fragment, List<StaticBonus> staticBonuses,
                                          Charges charges, List<String> spellNames) {
        if (ATTACK_AND_DAMAGE_PATTERN.matcher(fragment).find()
                || AC_PATTERN.matcher(fragment).find()
                || SAVING_THROWS_PATTERN.matcher(fragment).find()) {
            return true;
        }
        if (charges != null
                && (CHARGES_MAX_PATTERN.matcher(fragment).find()
                || CHARGES_RESET_PATTERN.matcher(fragment).find()
                || DAWN_PATTERN.matcher(fragment).find())) {
            return true;
        }
        return !spellNames.isEmpty() && SPELL_NAME_PATTERN.matcher(fragment).find();
    }

    /**
     * Нормализует кубиковую формулу: убирает внутренние пробелы и кириллическую «к» → «d».
     * Например «1d6 + 1» → «1d6+1», «2 к 6» → «2d6».
     * @param raw сырой фрагмент формулы из текста
     * @return нормализованная формула без пробелов
     */
    private static String normalizeDice(String raw) {
        return raw.replaceAll("\\s+", "")
                .replace('к', 'd')
                .replace('К', 'd')
                .toLowerCase();
    }

    /**
     * Неизменяемый результат разбора описания предмета.
     * @param staticBonuses статические бонусы (атака+урон / КД / спасброски)
     * @param charges заряды предмета (или null, если не найдены)
     * @param spellNames имена-кандидаты заклинаний, упомянутых в тексте
     * @param manualFragments фрагменты, не давшие извлекаемых данных (для ручной адъюдикации)
     */
    public record ItemRuleExtraction(
            List<StaticBonus> staticBonuses,
            Charges charges,
            List<String> spellNames,
            List<String> manualFragments) {

        /**
         * Создаёт пустую экстракцию (для null/blank-входа).
         * @return экстракция со всеми пустыми коллекциями и charges=null
         */
        public static ItemRuleExtraction empty() {
            return new ItemRuleExtraction(List.of(), null, List.of(), List.of());
        }
    }

    /**
     * Статический бонус предмета.
     * @param amount величина бонуса (обычно 1..3)
     * @param target цель бонуса: {@link #TARGET_ATTACK_AND_DAMAGE}, {@link #TARGET_AC},
     *               {@link #TARGET_SAVING_THROWS}
     */
    public record StaticBonus(int amount, String target) {
    }

    /**
     * Заряды предмета и правило их сброса.
     * @param max максимальное число зарядов
     * @param resetFormula формула восстановления зарядов (например «1d6+1»), либо null
     * @param resetRest тип отдыха для сброса (например {@link #RESET_LONG_REST}), либо null
     */
    public record Charges(int max, String resetFormula, String resetRest) {
    }
}
