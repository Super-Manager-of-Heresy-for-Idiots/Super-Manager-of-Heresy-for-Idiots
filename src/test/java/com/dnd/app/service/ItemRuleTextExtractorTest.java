package com.dnd.app.service;

import com.dnd.app.service.ItemRuleTextExtractor.Charges;
import com.dnd.app.service.ItemRuleTextExtractor.ItemRuleExtraction;
import com.dnd.app.service.ItemRuleTextExtractor.StaticBonus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Класс ItemRuleTextExtractorTest — юнит-тесты чистого экстрактора описаний предметов.
 * Без Spring и БД: проверяет ru/en-паттерны бонусов, зарядов, сброса, нормализацию формул,
 * пустой/null-вход и сбор нераспознанных фрагментов.
 */
class ItemRuleTextExtractorTest {

    private final ItemRuleTextExtractor extractor = new ItemRuleTextExtractor();

    // ── Статические бонусы: атака и урон ─────────────────────────────────────

    /** Проверяет русский паттерн «+N к броскам атаки и урона». */
    @Test
    void attackAndDamageBonusRu() {
        ItemRuleExtraction r = extractor.extract("Вы получаете +2 к броскам атаки и урона этим оружием.");
        assertThat(r.staticBonuses()).containsExactly(
                new StaticBonus(2, ItemRuleTextExtractor.TARGET_ATTACK_AND_DAMAGE));
    }

    /** Проверяет английский паттерн "+N to attack and damage rolls". */
    @Test
    void attackAndDamageBonusEn() {
        ItemRuleExtraction r = extractor.extract("You have a +1 bonus to attack and damage rolls made with this weapon.");
        assertThat(r.staticBonuses()).containsExactly(
                new StaticBonus(1, ItemRuleTextExtractor.TARGET_ATTACK_AND_DAMAGE));
    }

    // ── Статические бонусы: КД / AC ──────────────────────────────────────────

    /** Проверяет русский паттерн «+N к КД». */
    @Test
    void acBonusRu() {
        ItemRuleExtraction r = extractor.extract("Пока вы носите эту броню, вы получаете +2 к КД.");
        assertThat(r.staticBonuses()).containsExactly(
                new StaticBonus(2, ItemRuleTextExtractor.TARGET_AC));
    }

    /** Проверяет английский паттерн "+N to AC". */
    @Test
    void acBonusEn() {
        ItemRuleExtraction r = extractor.extract("While wearing this armor you gain a +3 to AC.");
        assertThat(r.staticBonuses()).containsExactly(
                new StaticBonus(3, ItemRuleTextExtractor.TARGET_AC));
    }

    // ── Статические бонусы: спасброски ───────────────────────────────────────

    /** Проверяет русский паттерн «+N к спасброскам». */
    @Test
    void savingThrowsBonusRu() {
        ItemRuleExtraction r = extractor.extract("Вы получаете +1 к спасброскам, пока держите этот амулет.");
        assertThat(r.staticBonuses()).containsExactly(
                new StaticBonus(1, ItemRuleTextExtractor.TARGET_SAVING_THROWS));
    }

    /** Проверяет английский паттерн "+N to saving throws". */
    @Test
    void savingThrowsBonusEn() {
        ItemRuleExtraction r = extractor.extract("You gain a +2 to saving throws while wearing this ring.");
        assertThat(r.staticBonuses()).containsExactly(
                new StaticBonus(2, ItemRuleTextExtractor.TARGET_SAVING_THROWS));
    }

    // ── Заряды ───────────────────────────────────────────────────────────────

    /** Заряды без формулы сброса: только max. */
    @Test
    void chargesWithoutResetFormula() {
        ItemRuleExtraction r = extractor.extract("Этот жезл имеет 7 зарядов.");
        assertThat(r.charges()).isNotNull();
        assertThat(r.charges().max()).isEqualTo(7);
        assertThat(r.charges().resetFormula()).isNull();
        assertThat(r.charges().resetRest()).isNull();
    }

    /** Английские заряды без формулы. */
    @Test
    void chargesWithoutResetFormulaEn() {
        ItemRuleExtraction r = extractor.extract("The wand has 3 charges.");
        assertThat(r.charges()).isNotNull();
        assertThat(r.charges().max()).isEqualTo(3);
        assertThat(r.charges().resetFormula()).isNull();
    }

    /** Заряды с формулой восстановления и «на рассвете» → long_rest (ru). */
    @Test
    void chargesWithResetFormulaRu() {
        ItemRuleExtraction r = extractor.extract(
                "Жезл имеет 7 зарядов. Он восстанавливает 1к6+1 зарядов на рассвете.");
        Charges c = r.charges();
        assertThat(c).isNotNull();
        assertThat(c.max()).isEqualTo(7);
        assertThat(c.resetFormula()).isEqualTo("1d6+1");
        assertThat(c.resetRest()).isEqualTo(ItemRuleTextExtractor.RESET_LONG_REST);
    }

    /** Заряды с формулой восстановления и "at dawn" → long_rest (en), нормализация пробелов. */
    @Test
    void chargesWithResetFormulaEn() {
        ItemRuleExtraction r = extractor.extract(
                "The wand has 7 charges. It regains 1d6 + 1 expended charges daily at dawn.");
        Charges c = r.charges();
        assertThat(c).isNotNull();
        assertThat(c.max()).isEqualTo(7);
        assertThat(c.resetFormula()).isEqualTo("1d6+1");
        assertThat(c.resetRest()).isEqualTo(ItemRuleTextExtractor.RESET_LONG_REST);
    }

    /** Нормализация формулы: пробелы убираются, «d» без ведущего числа допустим. */
    @Test
    void diceFormulaNormalization() {
        ItemRuleExtraction r = extractor.extract("Он восстанавливает d4 + 2 заряда на рассвете. Имеет 4 зарядов.");
        assertThat(r.charges()).isNotNull();
        assertThat(r.charges().resetFormula()).isEqualTo("d4+2");
    }

    // ── Заклинания ───────────────────────────────────────────────────────────

    /** Имена заклинаний-кандидаты извлекаются после якорных фраз. */
    @Test
    void spellNamesExtracted() {
        ItemRuleExtraction r = extractor.extract(
                "Вы можете потратить 1 заряд, чтобы наложить заклинание Огненный шар из этого жезла.");
        assertThat(r.spellNames()).contains("Огненный шар");
    }

    /** Английский якорь "cast the spell". */
    @Test
    void spellNamesExtractedEn() {
        ItemRuleExtraction r = extractor.extract("You can cast the spell Fireball from this wand.");
        assertThat(r.spellNames()).contains("Fireball");
    }

    // ── Пустой / null вход ───────────────────────────────────────────────────

    /** null-вход → пустая экстракция без исключений. */
    @Test
    void nullInputYieldsEmpty() {
        ItemRuleExtraction r = extractor.extract(null);
        assertThat(r.staticBonuses()).isEmpty();
        assertThat(r.charges()).isNull();
        assertThat(r.spellNames()).isEmpty();
        assertThat(r.manualFragments()).isEmpty();
    }

    /** blank-вход → пустая экстракция. */
    @Test
    void blankInputYieldsEmpty() {
        ItemRuleExtraction r = extractor.extract("   \n  ");
        assertThat(r.staticBonuses()).isEmpty();
        assertThat(r.charges()).isNull();
        assertThat(r.spellNames()).isEmpty();
        assertThat(r.manualFragments()).isEmpty();
    }

    // ── Manual fragments ─────────────────────────────────────────────────────

    /** Описание с неизвлекаемым эффектом → фрагмент попадает в manualFragments. */
    @Test
    void unparseableDescriptionYieldsManualFragment() {
        String text = "Проклятие: когда вы касаетесь этого предмета, вы должны пройти испытание воли или обратиться в камень.";
        ItemRuleExtraction r = extractor.extract(text);
        assertThat(r.staticBonuses()).isEmpty();
        assertThat(r.charges()).isNull();
        assertThat(r.manualFragments()).isNotEmpty();
        assertThat(r.manualFragments().get(0)).contains("Проклятие");
    }

    /** Извлекаемые предложения не попадают в manualFragments, неизвлекаемые — попадают. */
    @Test
    void mixedDescriptionSplitsExtractableFromManual() {
        String text = "Вы получаете +1 к КД. Кроме того, раз в день предмет источает облако тьмы неизвестной природы.";
        ItemRuleExtraction r = extractor.extract(text);
        assertThat(r.staticBonuses()).containsExactly(
                new StaticBonus(1, ItemRuleTextExtractor.TARGET_AC));
        assertThat(r.manualFragments()).anySatisfy(f -> assertThat(f).contains("облако тьмы"));
        assertThat(r.manualFragments()).noneSatisfy(f -> assertThat(f).contains("к КД"));
    }

    /** Несколько разных статических бонусов в одном описании. */
    @Test
    void multipleStaticBonuses() {
        String text = "Вы получаете +1 к КД и +1 к спасброскам, пока носите этот плащ.";
        ItemRuleExtraction r = extractor.extract(text);
        assertThat(r.staticBonuses()).containsExactlyInAnyOrder(
                new StaticBonus(1, ItemRuleTextExtractor.TARGET_AC),
                new StaticBonus(1, ItemRuleTextExtractor.TARGET_SAVING_THROWS));
    }

    /** Санити: результат неизменяем (копии списков). */
    @Test
    void listsAreImmutable() {
        ItemRuleExtraction r = extractor.extract("Вы получаете +1 к КД.");
        List<StaticBonus> bonuses = r.staticBonuses();
        try {
            bonuses.add(new StaticBonus(9, "X"));
            assertThat(false).as("expected immutable list").isTrue();
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }
}
