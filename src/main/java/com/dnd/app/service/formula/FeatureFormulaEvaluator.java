package com.dnd.app.service.formula;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Safe, dependency-free evaluator for the bounded feature-formula DSL.
 *
 * <p>Supported: decimal numbers; dice literals {@code NdM}; arithmetic {@code + - * /} with parentheses;
 * comparisons {@code < <= > >= == !=}; logic {@code && || !}; boolean literals; an allowlist of functions
 * ({@code floor ceil round abs min max step dice class_level ability_mod feature_resource_count
 * target_condition}); and the bare context scalars {@code character_level proficiency_bonus
 * spell_slot_level monster_cr combat_round}.</p>
 *
 * <p>There is NO access to Java objects, fields, reflection, or arbitrary names — anything outside the
 * allowlist raises {@link FormulaException}. Division by zero and type mismatches also raise it.</p>
 */
@Component
public class FeatureFormulaEvaluator {

    private static final Set<String> SCALARS = Set.of(
            "character_level", "proficiency_bonus", "spell_slot_level", "monster_cr", "combat_round");
    private static final Set<String> KEYED_FUNCTIONS = Set.of(
            "class_level", "ability_mod", "feature_resource_count", "target_condition");
    private static final Set<String> MATH_FUNCTIONS = Set.of("floor", "ceil", "round", "abs", "min", "max");
    private static final String STEP_FUNCTION = "step";

    /** The bare context scalars the DSL allows (source of truth for admin autocomplete). */
    public static Set<String> scalarNames() {
        return new TreeSet<>(SCALARS);
    }

    /** Keyed functions taking a string argument (class_level, ability_mod, …). */
    public static Set<String> keyedFunctionNames() {
        return new TreeSet<>(KEYED_FUNCTIONS);
    }

    /** Every callable function name in the DSL allowlist (math + keyed + step + dice). */
    public static Set<String> functionNames() {
        Set<String> out = new TreeSet<>(MATH_FUNCTIONS);
        out.addAll(KEYED_FUNCTIONS);
        out.add(STEP_FUNCTION);
        out.add("dice");
        return out;
    }

    /** Evaluate an expression against a context. Returns Double, Boolean, or {@link DiceValue}. */
    public Object evaluate(String expression, FormulaContext ctx) {
        return eval(parse(expression), ctx);
    }

    /** Parse-only check + collect the context variables/functions the expression references. */
    public Set<String> requiredContext(String expression) {
        Set<String> out = new TreeSet<>();
        collect(parse(expression), out);
        return out;
    }

    // ── AST ─────────────────────────────────────────────────────────────────

    private sealed interface Node
            permits Num, Str, Dice, Var, Neg, Bin, Cmp, Logic, Not, Fn {}
    private record Num(double v) implements Node {}
    private record Str(String v) implements Node {}
    private record Dice(int count, int sides) implements Node {}
    private record Var(String name) implements Node {}
    private record Neg(Node operand) implements Node {}
    private record Bin(char op, Node l, Node r) implements Node {}
    private record Cmp(String op, Node l, Node r) implements Node {}
    private record Logic(String op, Node l, Node r) implements Node {}
    private record Not(Node operand) implements Node {}
    private record Fn(String name, List<Node> args) implements Node {}

    // ── Tokenizer ───────────────────────────────────────────────────────────

    private enum Kind { NUMBER, DICE, IDENT, STRING, OP, LPAREN, RPAREN, COMMA, EOF }
    private record Token(Kind kind, String text, int count, int sides) {
        static Token of(Kind k, String t) { return new Token(k, t, 0, 0); }
    }

    private List<Token> tokenize(String s) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (Character.isDigit(c)) {
                int start = i;
                while (i < n && Character.isDigit(s.charAt(i))) i++;
                // dice literal NdM
                if (i < n && (s.charAt(i) == 'd' || s.charAt(i) == 'D') && i + 1 < n && Character.isDigit(s.charAt(i + 1))) {
                    int count = Integer.parseInt(s.substring(start, i));
                    i++; // consume 'd'
                    int sStart = i;
                    while (i < n && Character.isDigit(s.charAt(i))) i++;
                    int sides = Integer.parseInt(s.substring(sStart, i));
                    tokens.add(new Token(Kind.DICE, s.substring(start, i), count, sides));
                    continue;
                }
                if (i < n && s.charAt(i) == '.') {
                    i++;
                    while (i < n && Character.isDigit(s.charAt(i))) i++;
                }
                tokens.add(Token.of(Kind.NUMBER, s.substring(start, i)));
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) i++;
                tokens.add(Token.of(Kind.IDENT, s.substring(start, i)));
                continue;
            }
            if (c == '"' || c == '\'') {
                char quote = c;
                i++;
                int start = i;
                while (i < n && s.charAt(i) != quote) i++;
                if (i >= n) throw new FormulaException("Незакрытая строка в формуле");
                String str = s.substring(start, i);
                i++; // closing quote
                tokens.add(Token.of(Kind.STRING, str));
                continue;
            }
            // multi-char operators
            String two = i + 1 < n ? s.substring(i, i + 2) : "";
            if (two.equals("<=") || two.equals(">=") || two.equals("==") || two.equals("!=")
                    || two.equals("&&") || two.equals("||")) {
                tokens.add(Token.of(Kind.OP, two));
                i += 2;
                continue;
            }
            switch (c) {
                case '+', '-', '*', '/', '<', '>', '!' -> tokens.add(Token.of(Kind.OP, String.valueOf(c)));
                case '(' -> tokens.add(Token.of(Kind.LPAREN, "("));
                case ')' -> tokens.add(Token.of(Kind.RPAREN, ")"));
                case ',' -> tokens.add(Token.of(Kind.COMMA, ","));
                default -> throw new FormulaException("Недопустимый символ в формуле: '" + c + "'");
            }
            i++;
        }
        tokens.add(Token.of(Kind.EOF, ""));
        return tokens;
    }

    // ── Parser (recursive descent) ──────────────────────────────────────────

    private Node parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new FormulaException("Пустая формула");
        }
        Parser p = new Parser(tokenize(expression));
        Node node = p.parseOr();
        p.expect(Kind.EOF);
        return node;
    }

    private final class Parser {
        private final List<Token> tokens;
        private int pos = 0;

        Parser(List<Token> tokens) { this.tokens = tokens; }

        Token peek() { return tokens.get(pos); }
        Token next() { return tokens.get(pos++); }
        boolean isOp(String op) { Token t = peek(); return t.kind() == Kind.OP && t.text().equals(op); }
        void expect(Kind k) {
            if (peek().kind() != k) throw new FormulaException("Ожидалось " + k + ", получено '" + peek().text() + "'");
        }

        Node parseOr() {
            Node left = parseAnd();
            while (isOp("||")) { next(); left = new Logic("||", left, parseAnd()); }
            return left;
        }

        Node parseAnd() {
            Node left = parseComparison();
            while (isOp("&&")) { next(); left = new Logic("&&", left, parseComparison()); }
            return left;
        }

        Node parseComparison() {
            Node left = parseAdditive();
            if (isOp("<") || isOp("<=") || isOp(">") || isOp(">=") || isOp("==") || isOp("!=")) {
                String op = next().text();
                return new Cmp(op, left, parseAdditive());
            }
            return left;
        }

        Node parseAdditive() {
            Node left = parseMultiplicative();
            while (isOp("+") || isOp("-")) {
                char op = next().text().charAt(0);
                left = new Bin(op, left, parseMultiplicative());
            }
            return left;
        }

        Node parseMultiplicative() {
            Node left = parseUnary();
            while (isOp("*") || isOp("/")) {
                char op = next().text().charAt(0);
                left = new Bin(op, left, parseUnary());
            }
            return left;
        }

        Node parseUnary() {
            if (isOp("-")) { next(); return new Neg(parseUnary()); }
            if (isOp("!")) { next(); return new Not(parseUnary()); }
            return parsePrimary();
        }

        Node parsePrimary() {
            Token t = peek();
            switch (t.kind()) {
                case NUMBER -> { next(); return new Num(Double.parseDouble(t.text())); }
                case DICE -> { next(); return new Dice(t.count(), t.sides()); }
                case STRING -> { next(); return new Str(t.text()); }
                case LPAREN -> {
                    next();
                    Node inner = parseOr();
                    expect(Kind.RPAREN);
                    next();
                    return inner;
                }
                case IDENT -> {
                    next();
                    String name = t.text();
                    if (peek().kind() == Kind.LPAREN) {
                        next();
                        List<Node> args = new ArrayList<>();
                        if (peek().kind() != Kind.RPAREN) {
                            args.add(parseOr());
                            while (peek().kind() == Kind.COMMA) { next(); args.add(parseOr()); }
                        }
                        expect(Kind.RPAREN);
                        next();
                        return new Fn(name, args);
                    }
                    return new Var(name);
                }
                default -> throw new FormulaException("Неожиданный токен: '" + t.text() + "'");
            }
        }
    }

    // ── Evaluation ──────────────────────────────────────────────────────────

    private Object eval(Node node, FormulaContext ctx) {
        return switch (node) {
            case Num num -> num.v();
            case Dice d -> new DiceValue(d.count(), d.sides());
            case Str s -> s.v();
            case Var v -> evalVar(v.name(), ctx);
            case Neg neg -> -num(eval(neg.operand(), ctx));
            case Not not -> !bool(eval(not.operand(), ctx));
            case Bin bin -> evalBin(bin, ctx);
            case Cmp cmp -> evalCmp(cmp, ctx);
            case Logic logic -> evalLogic(logic, ctx);
            case Fn fn -> evalFn(fn, ctx);
        };
    }

    private Object evalVar(String name, FormulaContext ctx) {
        if (name.equals("true")) return Boolean.TRUE;
        if (name.equals("false")) return Boolean.FALSE;
        if (SCALARS.contains(name)) {
            Double v = ctx.scalar(name);
            if (v == null) throw new FormulaException("Переменная недоступна в контексте: " + name);
            return v;
        }
        throw new FormulaException("Неизвестная переменная: " + name);
    }

    private Object evalBin(Bin bin, FormulaContext ctx) {
        double l = num(eval(bin.l(), ctx));
        double r = num(eval(bin.r(), ctx));
        return switch (bin.op()) {
            case '+' -> l + r;
            case '-' -> l - r;
            case '*' -> l * r;
            case '/' -> {
                if (r == 0) throw new FormulaException("Деление на ноль");
                yield l / r;
            }
            default -> throw new FormulaException("Неизвестный оператор: " + bin.op());
        };
    }

    private Object evalCmp(Cmp cmp, FormulaContext ctx) {
        double l = num(eval(cmp.l(), ctx));
        double r = num(eval(cmp.r(), ctx));
        return switch (cmp.op()) {
            case "<" -> l < r;
            case "<=" -> l <= r;
            case ">" -> l > r;
            case ">=" -> l >= r;
            case "==" -> l == r;
            case "!=" -> l != r;
            default -> throw new FormulaException("Неизвестное сравнение: " + cmp.op());
        };
    }

    private Object evalLogic(Logic logic, FormulaContext ctx) {
        boolean l = bool(eval(logic.l(), ctx));
        if (logic.op().equals("&&")) {
            return l && bool(eval(logic.r(), ctx));
        }
        return l || bool(eval(logic.r(), ctx));
    }

    private Object evalFn(Fn fn, FormulaContext ctx) {
        String name = fn.name();
        if (name.equals("dice")) {
            String spec = str(fn, 0);
            return parseDiceSpec(spec);
        }
        if (KEYED_FUNCTIONS.contains(name)) {
            String key = str(fn, 0);
            return switch (name) {
                case "class_level" -> requireCtx(ctx.classLevel(key), "class_level(" + key + ")");
                case "ability_mod" -> requireCtx(ctx.abilityMod(key), "ability_mod(" + key + ")");
                case "feature_resource_count" -> requireCtx(ctx.featureResourceCount(key), "feature_resource_count(" + key + ")");
                case "target_condition" -> {
                    Boolean b = ctx.targetCondition(key);
                    if (b == null) throw new FormulaException("Условие недоступно в контексте: target_condition(" + key + ")");
                    yield b;
                }
                default -> throw new FormulaException("Неизвестная функция: " + name);
            };
        }
        if (MATH_FUNCTIONS.contains(name)) {
            return evalMathFn(name, fn, ctx);
        }
        if (name.equals(STEP_FUNCTION)) {
            return evalStep(fn, ctx);
        }
        throw new FormulaException("Запрещённая функция: " + name);
    }

    /**
     * Level-step lookup: {@code step(value, t1, v1, t2, v2, …)} returns the {@code v} of the highest threshold
     * {@code t} that is {@code <= value} (0 when {@code value} is below every threshold). Models non-linear
     * class tables like Rage: {@code step(character_level, 1,2, 3,3, 6,4, 12,5, 17,6)}.
     */
    private Object evalStep(Fn fn, FormulaContext ctx) {
        List<Node> args = fn.args();
        if (args.size() < 3 || args.size() % 2 == 0) {
            throw new FormulaException("step ожидает значение и пары порог,результат (нечётное число аргументов ≥ 3)");
        }
        double value = num(eval(args.get(0), ctx));
        double result = 0.0;
        double bestThreshold = Double.NEGATIVE_INFINITY;
        for (int k = 1; k + 1 < args.size(); k += 2) {
            double threshold = num(eval(args.get(k), ctx));
            double v = num(eval(args.get(k + 1), ctx));
            if (value >= threshold && threshold >= bestThreshold) {
                bestThreshold = threshold;
                result = v;
            }
        }
        return result;
    }

    private Object evalMathFn(String name, Fn fn, FormulaContext ctx) {
        List<Node> args = fn.args();
        switch (name) {
            case "floor", "ceil", "round", "abs" -> {
                if (args.size() != 1) throw new FormulaException(name + " ожидает 1 аргумент");
                double v = num(eval(args.get(0), ctx));
                return switch (name) {
                    case "floor" -> Math.floor(v);
                    case "ceil" -> Math.ceil(v);
                    case "round" -> Math.rint(v);
                    default -> Math.abs(v);
                };
            }
            case "min", "max" -> {
                if (args.isEmpty()) throw new FormulaException(name + " ожидает хотя бы 1 аргумент");
                double acc = num(eval(args.get(0), ctx));
                for (int k = 1; k < args.size(); k++) {
                    double v = num(eval(args.get(k), ctx));
                    acc = name.equals("min") ? Math.min(acc, v) : Math.max(acc, v);
                }
                return acc;
            }
            default -> throw new FormulaException("Неизвестная функция: " + name);
        }
    }

    private DiceValue parseDiceSpec(String spec) {
        String s = spec.trim().toLowerCase();
        int d = s.indexOf('d');
        if (d < 0) throw new FormulaException("Некорректная кость: " + spec);
        try {
            int count = d == 0 ? 1 : Integer.parseInt(s.substring(0, d));
            int sides = Integer.parseInt(s.substring(d + 1));
            if (count <= 0 || sides <= 0) throw new FormulaException("Некорректная кость: " + spec);
            return new DiceValue(count, sides);
        } catch (NumberFormatException e) {
            throw new FormulaException("Некорректная кость: " + spec);
        }
    }

    // ── Static analysis (required context) ──────────────────────────────────

    private void collect(Node node, Set<String> out) {
        switch (node) {
            case Var v -> { if (SCALARS.contains(v.name())) out.add(v.name()); }
            case Neg neg -> collect(neg.operand(), out);
            case Not not -> collect(not.operand(), out);
            case Bin bin -> { collect(bin.l(), out); collect(bin.r(), out); }
            case Cmp cmp -> { collect(cmp.l(), out); collect(cmp.r(), out); }
            case Logic logic -> { collect(logic.l(), out); collect(logic.r(), out); }
            case Fn fn -> {
                if (KEYED_FUNCTIONS.contains(fn.name()) && !fn.args().isEmpty() && fn.args().get(0) instanceof Str str) {
                    out.add(fn.name() + "(" + str.v() + ")");
                }
                fn.args().forEach(a -> collect(a, out));
            }
            case Num ignored -> { }
            case Str ignored -> { }
            case Dice ignored -> { }
        }
    }

    // ── Coercion helpers ────────────────────────────────────────────────────

    private double num(Object o) {
        if (o instanceof Double d) return d;
        if (o instanceof DiceValue dice) return dice.average();
        throw new FormulaException("Ожидалось число, получено " + typeName(o));
    }

    private boolean bool(Object o) {
        if (o instanceof Boolean b) return b;
        throw new FormulaException("Ожидалось логическое значение, получено " + typeName(o));
    }

    private String str(Fn fn, int idx) {
        if (fn.args().size() <= idx || !(fn.args().get(idx) instanceof Str str)) {
            throw new FormulaException(fn.name() + " ожидает строковый аргумент в кавычках");
        }
        return str.v();
    }

    private double requireCtx(Double value, String what) {
        if (value == null) throw new FormulaException("Значение недоступно в контексте: " + what);
        return value;
    }

    private String typeName(Object o) {
        if (o instanceof Double) return "число";
        if (o instanceof Boolean) return "логическое";
        if (o instanceof DiceValue) return "кость";
        if (o instanceof String) return "строку";
        return "неизвестный тип";
    }
}
