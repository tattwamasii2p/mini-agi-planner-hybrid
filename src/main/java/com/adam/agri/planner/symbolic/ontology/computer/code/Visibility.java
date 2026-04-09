package com.adam.agri.planner.symbolic.ontology.computer.code;

/**
 * Language-agnostic visibility levels for code entities.
 * This replaces Java's specific visibility system with a more generic approach
 * that can be mapped to various programming language visibility concepts.
 */
public enum Visibility {
    // Highest visibility - accessible from anywhere
    PUBLIC("public", "Accessible from anywhere"),

    // Package/namespace/module level visibility
    INTERNAL("internal", "Accessible within the same assembly or module"),
    PACKAGE_PRIVATE("package", "Accessible within the same package or namespace"),
    MODULE_PRIVATE("module", "Accessible within the same module"),

    // File-level visibility
    FILE_PRIVATE("file", "Accessible within the same file"),

    // Intermediate visibility levels
    PROTECTED("protected", "Accessible within the same hierarchy or inheritance chain"),
    INTERNAL_PROTECTED("internal_protected", "Protected within the same module"),

    // Lowest visibility - only accessible from current scope
    PRIVATE("private", "Accessible only within the declaring scope"),

    // Language-specific intermediate levels
    FILE("file", "Accessible within the same file (some languages)"),

    // Scoped visibility (for lambdas, inner classes, etc.)
    LOCAL("local", "Accessible only within current lexical scope"),

    // Default visibility (varies by language)
    DEFAULT("default", "Default visibility for the language"),

    // No visibility specified
    UNSPECIFIED("unspecified", "Visibility not explicitly defined");

    private final String keyword;
    private final String description;

    Visibility(String keyword, String description) {
        this.keyword = keyword;
        this.description = description;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAccessibleFrom(Visibility other) {
        if (this == PUBLIC) {
            return true; // Public is always accessible
        }

        if (this == PRIVATE) {
            return this == other; // Private only accessible from private
        }

        // Hierarchy-based accessibility
        switch (this) {
            case PROTECTED:
                return other == PROTECTED || other == PUBLIC;
            case INTERNAL:
                return other == INTERNAL || other == PUBLIC;
            case PACKAGE_PRIVATE:
                return other == PACKAGE_PRIVATE || other == PUBLIC;
            default:
                return this == other;
        }
    }

    public static Visibility fromKeyword(String keyword) {
        for (Visibility visibility : values()) {
            if (visibility.keyword.equalsIgnoreCase(keyword)) {
                return visibility;
            }
        }
        return DEFAULT;
    }
}