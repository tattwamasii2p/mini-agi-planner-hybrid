package com.adam.agri.planner.jls.reader;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Classifies JLS sections by topic for semantic navigation.
 * Maps section numbers to linguistic categories.
 */
public class JlsClassifier {

    // Chapter categories
    public enum Category {
        INTRODUCTION,
        GRAMMAR,
        LEXICAL,
        TYPES,
        CONVERSIONS,
        NAMES,
        CLASSES,
        INTERFACES,
        ARRAYS,
        EXCEPTIONS,
        EXECUTION,
        BINARY,
        COMPILATION,
        SYNTAX,
        EXPRESSIONS,
        DEFINITE_ASSIGNMENT,
        THREADS,
        TYPE_INFERENCE,
        SYNTAX_INDEX
    }

    private static final Map<String, Category> CHAPTER_CATEGORIES = Map.ofEntries(
        Map.entry("1", Category.INTRODUCTION),
        Map.entry("2", Category.GRAMMAR),
        Map.entry("3", Category.LEXICAL),
        Map.entry("4", Category.TYPES),
        Map.entry("5", Category.CONVERSIONS),
        Map.entry("6", Category.NAMES),
        Map.entry("7", Category.CLASSES),
        Map.entry("8", Category.CLASSES),
        Map.entry("9", Category.INTERFACES),
        Map.entry("10", Category.ARRAYS),
        Map.entry("11", Category.EXCEPTIONS),
        Map.entry("12", Category.EXECUTION),
        Map.entry("13", Category.BINARY),
        Map.entry("14", Category.COMPILATION),
        Map.entry("15", Category.EXPRESSIONS),
        Map.entry("16", Category.DEFINITE_ASSIGNMENT),
        Map.entry("17", Category.THREADS),
        Map.entry("18", Category.TYPE_INFERENCE)
    );

    // Pattern matching for rule extraction
    private static final Pattern GRAMMAR_RULE = Pattern.compile(
        "(?m)^\\s*([A-Za-z]+):?\\s*$|" +
        "([A-Za-z]+:\\s*[A-Za-z\\[\\]<>{}|]+)"
    );

    private static final Pattern TYPE_RULE = Pattern.compile(
        "(subtype|supertype|assignment|widening|narrowing|boxing|unboxing)" +
        "\\s+(conversion|context|compatible)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SEMANTIC_RULE = Pattern.compile(
        "(compile-time|run-time|evaluation|execution)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Classify a section by its chapter.
     */
    public static Category classifyByChapter(String chapterNumber) {
        return CHAPTER_CATEGORIES.getOrDefault(chapterNumber, Category.INTRODUCTION);
    }

    /**
     * Extract formal grammar rules from section content.
     */
    public static List<GrammarRule> extractGrammarRules(String content) {
        List<GrammarRule> rules = new ArrayList<>();
        if (content == null) return rules;

        String[] lines = content.split("\n");
        String currentNonTerminal = null;
        List<String> alternatives = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();

            // Non-terminal definition: Name:
            if (line.matches("^[A-Za-z][A-Za-z0-9_]*:\\s*$")) {
                if (currentNonTerminal != null && !alternatives.isEmpty()) {
                    rules.add(new GrammarRule(currentNonTerminal, new ArrayList<>(alternatives)));
                }
                currentNonTerminal = line.substring(0, line.length() - 1).trim();
                alternatives.clear();

            // Alternative production
            } else if (line.startsWith("|") || line.startsWith(":")) {
                String alt = line.substring(1).trim();
                if (!alt.isEmpty()) {
                    alternatives.add(alt);
                }

            // Simple production
            } else if (currentNonTerminal != null && !line.isEmpty() && !line.startsWith("//")) {
                alternatives.add(line);
            }
        }

        if (currentNonTerminal != null && !alternatives.isEmpty()) {
            rules.add(new GrammarRule(currentNonTerminal, alternatives));
        }

        return rules;
    }

    /**
     * Extract type conversion rules from section.
     */
    public static List<TypeRule> extractTypeRules(String content) {
        List<TypeRule> rules = new ArrayList<>();
        if (content == null) return rules;

        // Identity conversion
        if (content.contains("identity conversion")) {
            rules.add(new TypeRule(
                "identity",
                "T -> T",
                "Any type to itself"
            ));
        }

        // Widening primitive
        if (content.contains("widening primitive conversion")) {
            rules.add(new TypeRule(
                "widening-primitive",
                "byte -> short -> int -> long -> float -> double",
                "Widening primitive conversion"
            ));
            rules.add(new TypeRule(
                "widening-primitive",
                "char -> int",
                "Char to int"
            ));
        }

        // Narrowing primitive
        if (content.contains("narrowing primitive conversion")) {
            rules.add(new TypeRule(
                "narrowing-primitive",
                "double -> float -> long -> int -> short -> byte",
                "Narrowing primitive conversion"
            ));
        }

        // Boxing
        if (content.contains("boxing conversion")) {
            rules.add(new TypeRule(
                "boxing",
                "primitive -> wrapper",
                "Box primitive to corresponding wrapper type"
            ));
        }

        // Unboxing
        if (content.contains("unboxing conversion")) {
            rules.add(new TypeRule(
                "unboxing",
                "wrapper -> primitive",
                "Unbox wrapper to corresponding primitive type"
            ));
        }

        // Widening reference
        if (content.contains("widening reference conversion")) {
            rules.add(new TypeRule(
                "widening-reference",
                "S <: T implies S -> T",
                "Subtype to supertype"
            ));
        }

        // Narrowing reference
        if (content.contains("narrowing reference conversion")) {
            rules.add(new TypeRule(
                "narrowing-reference",
                "S -> T where T <: S",
                "Supertype to subtype with cast"
            ));
        }

        return rules;
    }

    /**
     * Extract execution semantics from section.
     */
    public static List<ExecutionRule> extractExecutionRules(String content) {
        List<ExecutionRule> rules = new ArrayList<>();
        if (content == null) return rules;

        // Normal vs abrupt completion
        if (content.contains("normal completion") || content.contains("abrupt completion")) {
            rules.add(new ExecutionRule(
                "completion",
                "Statement executes normally or completes abruptly"
            ));
        }

        // Evaluation order
        if (content.contains("left-hand operand") && content.contains("right-hand operand")) {
            rules.add(new ExecutionRule(
                "evaluation-order",
                "Left operand evaluated before right operand"
            ));
        }

        // Definite assignment
        if (content.contains("definitely assigned")) {
            rules.add(new ExecutionRule(
                "definite-assignment",
                "Variable must be definitely assigned before use"
            ));
        }

        return rules;
    }

    /**
     * Check if section is about binary compatibility.
     */
    public static boolean isBinaryCompatibilitySection(JlsSection section) {
        return section.getTitle().toLowerCase().contains("binary") ||
               section.getTitle().toLowerCase().contains("compatibility");
    }

    /**
     * Check if section defines type system rules.
     */
    public static boolean isTypeSystemSection(JlsSection section) {
        String title = section.getTitle().toLowerCase();
        return title.contains("type") ||
               title.contains("subtype") ||
               title.contains("conversion") ||
               title.contains("assignment");
    }

    // Data classes
    public record GrammarRule(String nonTerminal, List<String> productions) {
        @Override
        public String toString() {
            return nonTerminal + " ::= " + String.join(" | ", productions);
        }
    }

    public record TypeRule(String kind, String pattern, String description) {}

    public record ExecutionRule(String kind, String description) {}
}
