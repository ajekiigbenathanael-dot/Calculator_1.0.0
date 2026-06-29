package com.swe2023020.calculator;

import java.util.Locale;

/**
 * Recursive descent expression evaluator for a scientific calculator.
 *
 * Supported input syntax:
 *   Arithmetic      :  2+3  4-1  6*7  8/2  5%3  2^10
 *   Implicit mult   :  2π  2sin30  3cos60  (2+3)π  π(5)
 *   Trig            :  sin(30)  cos(60)  tan(45)  asin(1)  acos(0)  atan(1)
 *   Hyperbolic      :  sinh(1)  cosh(0)  tanh(1)
 *   Logarithms      :  ln(e)  log(100)  log2(8)
 *   Roots           :  sqrt(9)  cbrt(27)  abs(-5)
 *   Constants       :  π  e
 *   Combinatorics   :  5P2  12P4  5C2  8C2   ← Casio infix style, no function call
 *   Factorial       :  5!  10!
 *   Rounding        :  ceil(1.2)  floor(1.9)  round(1.5)
 *   Scientific nota :  1.5e10  2.5E-3
 *   Auto-close paren:  sin(30  →  sin(30)
 */
public class CalculatorEngine {

    public enum AngleMode { DEGREES, RADIANS }

    private static final double MAX_FACTORIAL_INPUT = 170;

    private AngleMode angleMode = AngleMode.DEGREES;
    private String    expr;
    private int       pos;

    public void setAngleMode(AngleMode mode) { this.angleMode = mode; }
    public AngleMode getAngleMode()          { return angleMode; }

    // ── Public entry point
    public double evaluate(String expression) throws Exception {
        if (expression == null || expression.trim().isEmpty()) {
            throw new Exception("Empty expression");
        }
        String cleaned = autoCloseParen(preprocess(expression));
        this.expr = cleaned;
        this.pos  = 0;
        double result = parseExpression();
        if (pos < expr.length()) {
            throw new Exception("Unexpected character: " + expr.charAt(pos));
        }
        return result;
    }

    // ── Pre-processing
    /**
     * Converts display symbols and inserts * for implicit multiplication.
     *
     * Key rules (applied in this exact order):
     *   1. Symbol substitution  (×→*  ÷→/  π→pi  √→sqrt)
     *   2. Parenthesis implicit multiplication  (2( → 2*(   )2 → )*2   )( → )*(  )
     *   3. pi-specific rules   (2pi → 2*pi   pi2 → pi*2   etc.)
     *   4. e-constant rules with negative lookahead to protect 1.5e10
     *   5. P and C are PROTECTED — digit P digit stays as-is so parsePower
     *      can handle 5P2 and 12P4 as infix operators
     *   6. General )letter rule  ()sin → )*sin)
     *   7. Catch-all digit-before-letter  (2sin → 2*sin)
     *      — this fires last and SKIPS P and C between digits
     */
    private String preprocess(String s) {

        // ── 1. Symbol substitution
        s = s.replace("×",      "*")
                .replace("÷",      "/")
                .replace("−",      "-")
                .replace("π",      "pi")
                .replace("√",      "sqrt")
                .replace("\u00B2", "^2")
                .trim();

        // ── 2. Parenthesis implicit multiplication
        s = s.replaceAll("(\\d)\\(", "$1*(");    // 2(  →  2*(
        s = s.replaceAll("\\)(\\d)", ")*$1");    // )2  →  )*2
        s = s.replaceAll("\\)\\(",   ")*(");      // )(  →  )*(

        // ── 3. pi rules
        s = s.replaceAll("(\\d)pi", "$1*pi");    // 2pi  →  2*pi
        s = s.replaceAll("pi(\\d)", "pi*$1");    // pi2  →  pi*2
        s = s.replaceAll("\\)pi",   ")*pi");     // )pi  →  )*pi
        s = s.replaceAll("pi\\(",   "pi*(");     // pi(  →  pi*(

        // ── 4. e-constant rules (protect scientific notation)
        // Negative lookahead (?![0-9+\-]) stops 1.5e10 from becoming 1.5*e*10
        s = s.replaceAll("(\\d)(e)(?![0-9+\\-])",  "$1*$2");  // 2e  →  2*e
        s = s.replaceAll("(?<![0-9.])(e)(\\d)",     "$1*$2");  // e2  →  e*2
        s = s.replaceAll("\\)e(?![0-9+\\-])",       ")*e");    // )e  →  )*e
        s = s.replaceAll("e(?![0-9+\\-])\\(",       "e*(");    // e(  →  e*(

        // ── 5. P and C are LEFT ALONE between digits
        //
        //   Do NOT insert * here. parsePower() reads the left operand as a
        //   number, then checks the next character. If it is P or C it treats
        //   them as infix operators exactly like ^.
        //
        //   12P4  stays  12P4   → parsePower sees base=12, op='P', r=4
        //   5C2   stays  5C2    → parsePower sees base=5,  op='C', r=2
        //
        //   If we inserted * here:  12*P4 → parseTerm multiplies 12 by ...
        //   then tries to read P4 as a primary → crash.

        // ── 6. )letter  →  )*letter  (e.g. )sin, )cos, )sqrt)
        s = s.replaceAll("\\)([a-zA-Z])", ")*$1");

        // ── 7. Catch-all: digit before letter → insert *
        //   Covers: 2sin, 3cos, 4tan, 5log, 2ln, 4sqrt, 2cbrt, etc.
        //   EXCLUDES P and C between digits — the negative lookahead
        //   (?![PC]) is NOT needed here because step 5 left them intact
        //   and this regex (\d)([a-zA-Z]) WOULD match 12P4 → 12*P4 and
        //   break everything.
        //
        //   Solution: exclude P and C from the letter class in this rule.
        s = s.replaceAll("(\\d)([a-boq-zA-BOQ-Z])", "$1*$2");        //                         ^^^^^^^^^^^
        //   [a-oq-z] = all lowercase letters except p
        //   [A-OQ-Z] = all uppercase letters except P
        //   This leaves digit+P and digit+C untouched for parsePower().

        return s;
    }

    private String autoCloseParen(String s) {
        int open = 0;
        for (char c : s.toCharArray()) {
            if      (c == '(') open++;
            else if (c == ')') open--;
        }
        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < open; i++) sb.append(')');
        return sb.toString();
    }

    // ── Parser
    //   Precedence chain (lowest → highest):
    //
    //     parseExpression   +  −
    //     parseTerm         *  /  %
    //     parsePower        ^  P  C  !
    //     parseUnary        unary−  unary+
    //     parsePrimary      number | (expr) | namedToken
    //

    private double parseExpression() throws Exception {
        double result = parseTerm();
        while (pos < expr.length()) {
            char op = expr.charAt(pos);
            if      (op == '+') { pos++; result += parseTerm(); }
            else if (op == '-') { pos++; result -= parseTerm(); }
            else break;
        }
        return result;
    }

    private double parseTerm() throws Exception {
        double result = parsePower();
        while (pos < expr.length()) {
            char op = expr.charAt(pos);
            if (op == '*') {
                pos++;
                result *= parsePower();
            } else if (op == '/') {
                pos++;
                double divisor = parsePower();
                if (divisor == 0) throw new ArithmeticException("Division by zero");
                result /= divisor;
            } else if (op == '%') {
                pos++;
                result %= parsePower();
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Handles:
     *   ^   right-associative power          2^3^2 = 512
     *   P   Casio infix permutation          12P4  = 11880
     *   C   Casio infix combination          8C2   = 28
     *   !   postfix factorial                5!    = 120
     *
     * P and C arrive here as raw characters because preprocess() deliberately
     * did NOT insert * before them when they appear between digits.
     */
    private double parsePower() throws Exception {
        double base = parseUnary();
        while (pos < expr.length()) {
            char op = expr.charAt(pos);
            System.out.println("Operator = " + op);

            if (op == '^') {
                pos++;
                double exp = parsePower();   // recurse for right-associativity
                base = Math.pow(base, exp);

            } else if (op == 'P') {
                pos++;
                double r = parseUnary();
                int n = toWholeNumber(base, "nPr — n");
                int ri = toWholeNumber(r,    "nPr — r");
                try {
                    base = PermutationCombination.nPr(n, ri);
                } catch (Exception e) {
                    throw new Exception(e.getMessage());
                }

            } else if (op == 'C') {
                pos++;
                double r = parseUnary();
                int n  = toWholeNumber(base, "nCr — n");
                int ri = toWholeNumber(r,    "nCr — r");
                try {
                    base = PermutationCombination.nCr(n, ri);
                } catch (Exception e) {
                    throw new Exception(e.getMessage());
                }

            } else if (op == '!') {
                pos++;
                base = factorial(base);

            } else {
                break;
            }
        }
        return base;
    }

    private double parseUnary() throws Exception {
        if (pos < expr.length()) {
            char c = expr.charAt(pos);
            if (c == '-') { pos++; return -parsePrimary(); }
            if (c == '+') { pos++; }
        }
        return parsePrimary();
    }

    private double parsePrimary() throws Exception {
        skipWhitespace();
        if (pos >= expr.length()) {
            throw new Exception("Unexpected end of expression");
        }
        char c = expr.charAt(pos);

        if (c == '(') {
            pos++;
            double value = parseExpression();
            if (pos < expr.length() && expr.charAt(pos) == ')') pos++;
            return value;
        }
        if (Character.isDigit(c) || c == '.') return parseNumber();
        if (Character.isLetter(c))            return parseNamedToken();

        throw new Exception("Unexpected character: " + c);
    }

    private double parseNumber() {
        int start = pos;
        while (pos < expr.length()
                && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) {
            pos++;
        }
        // Scientific notation: 1.5e10 or 2.5E-3
        if (pos < expr.length()
                && (expr.charAt(pos) == 'e' || expr.charAt(pos) == 'E')) {
            int peek = pos + 1;
            if (peek < expr.length()
                    && (Character.isDigit(expr.charAt(peek))
                    || expr.charAt(peek) == '+'
                    || expr.charAt(peek) == '-')) {
                pos++;
                if (expr.charAt(pos) == '+' || expr.charAt(pos) == '-') pos++;
                while (pos < expr.length() && Character.isDigit(expr.charAt(pos))) pos++;
            }
        }
        return Double.parseDouble(expr.substring(start, pos));
    }

    private double parseNamedToken() throws Exception {
        int start = pos;
        while (pos < expr.length()
                && (Character.isLetterOrDigit(expr.charAt(pos))
                || expr.charAt(pos) == '_')) {
            pos++;
        }
        String name = expr.substring(start, pos).toLowerCase(Locale.ROOT);

        switch (name) {
            case "pi":    return Math.PI;
            case "e":     return Math.E;

            case "sin":   return Math.sin(toRadians(parseArg()));
            case "cos":   return Math.cos(toRadians(parseArg()));
            case "tan":   return Math.tan(toRadians(parseArg()));
            case "asin":  return fromRadians(Math.asin(parseArg()));
            case "acos":  return fromRadians(Math.acos(parseArg()));
            case "atan":  return fromRadians(Math.atan(parseArg()));

            case "sinh":  return Math.sinh(parseArg());
            case "cosh":  return Math.cosh(parseArg());
            case "tanh":  return Math.tanh(parseArg());

            case "ln":    return Math.log(parseArg());
            case "log":   return Math.log10(parseArg());
            case "log2":  return Math.log(parseArg()) / Math.log(2);

            case "sqrt":  return Math.sqrt(parseArg());
            case "cbrt":  return Math.cbrt(parseArg());
            case "abs":   return Math.abs(parseArg());

            case "fact":  return factorial(parseArg());
            case "ceil":  return Math.ceil(parseArg());
            case "floor": return Math.floor(parseArg());
            case "round": return (double) Math.round(parseArg());
            case "sign":  return Math.signum(parseArg());

            default:
                throw new Exception("Unknown function: \"" + name + "\"");
        }
    }

    // ── Argument readers

    private double parseArg() throws Exception {
        skipWhitespace();
        if (pos < expr.length() && expr.charAt(pos) == '(') {
            pos++;
            double value = parseExpression();
            if (pos < expr.length() && expr.charAt(pos) == ')') pos++;
            return value;
        }
        return parsePrimary();
    }

    private void skipWhitespace() {
        while (pos < expr.length() && expr.charAt(pos) == ' ') pos++;
    }

    // ── Helpers
    private double toRadians(double angle) {
        return (angleMode == AngleMode.DEGREES) ? Math.toRadians(angle) : angle;
    }

    private double fromRadians(double radians) {
        return (angleMode == AngleMode.DEGREES) ? Math.toDegrees(radians) : radians;
    }

    /**
     * Validates that a double is a non-negative whole number suitable for
     * use as n or r in combinatoric functions, and returns it as int.
     */
    private int toWholeNumber(double v, String label) throws Exception {
        if (v != Math.floor(v) || v < 0) {
            throw new Exception(label + " must be a non-negative whole number");
        }
        return (int) v;
    }

    public static double factorial(double n) throws Exception {
        if (n < 0)                   throw new Exception("Factorial undefined for negative numbers");
        if (n != Math.floor(n))      throw new Exception("Factorial requires a whole number");
        if (n > MAX_FACTORIAL_INPUT) throw new Exception("Factorial overflow (max input: 170)");
        if (n == 0 || n == 1)        return 1;
        double result = 1;
        for (int i = 2; i <= (int) n; i++) result *= i;
        return result;
    }
}